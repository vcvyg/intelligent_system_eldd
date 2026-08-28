package org.example.persion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.ai.action.MedicalAiActionProposalStore;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.entity.AlertRecord;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.vo.MedicalAiActionProposalVO;
import org.example.persion.vo.MedicalAiActionResultVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Human-in-the-loop operational actions for the medical AI workspace.
 *
 * <p>Write actions are deliberately outside the semantic Planner allowlist. The system first
 * creates a short-lived proposal, then executes only after an authenticated medical user sends
 * a second explicit confirmation request. Permission and target state are rechecked at confirm time.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalAiActionService {

    private final AlertRecordMapper alertRecordMapper;
    private final ElderlyInfoMapper elderlyInfoMapper;
    private final AlertService alertService;
    private final MedicalAiActionProposalStore proposalStore;

    public MedicalAiActionProposalVO proposeStartAlert(Long medicalUserId, Long alertId) {
        requireUserAndTarget(medicalUserId, alertId);
        AlertRecord alert = requireAlert(alertId);
        assertAssigned(medicalUserId, alert.getElderlyId());
        assertStartable(alert);

        String proposalId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = proposalStore.put(
                proposalId,
                medicalUserId,
                alert.getElderlyId(),
                alertId,
                MedicalAiActionProposalStore.START_ALERT_PROCESSING
        );

        log.info("medical_ai_action_proposed proposalId={} actionType={} targetId={} medicalUserId={}",
                proposalId, MedicalAiActionProposalStore.START_ALERT_PROCESSING, alertId, medicalUserId);
        return new MedicalAiActionProposalVO(
                proposalId,
                MedicalAiActionProposalStore.START_ALERT_PROCESSING,
                alertId,
                "将告警 #" + alertId + " 标记为处理中；确认前不会修改业务数据",
                expiresAt,
                true
        );
    }

    public MedicalAiActionResultVO confirm(Long medicalUserId, String proposalId) {
        if (medicalUserId == null) throw new BusinessException(401, "未获取到当前登录用户");
        if (proposalId == null || proposalId.isBlank()) throw new BusinessException(400, "缺少操作确认ID");

        MedicalAiActionProposalStore.PendingAction action = proposalStore.consume(proposalId.trim())
                .orElseThrow(() -> new BusinessException(400, "操作确认已失效或已被使用，请重新生成"));
        if (!medicalUserId.equals(action.medicalUserId())) {
            throw new BusinessException(403, "该操作确认不属于当前用户");
        }
        if (!MedicalAiActionProposalStore.START_ALERT_PROCESSING.equals(action.actionType())) {
            throw new BusinessException(400, "不支持的操作类型");
        }

        AlertRecord alert = requireAlert(action.targetId());
        if (!action.elderlyId().equals(alert.getElderlyId())) {
            throw new BusinessException(409, "操作目标已发生变化，请重新生成确认");
        }
        assertAssigned(medicalUserId, alert.getElderlyId());
        assertStartable(alert);

        alertService.processAlert(alert.getId());
        log.info("medical_ai_action_confirmed proposalId={} actionType={} targetId={} medicalUserId={}",
                proposalId, action.actionType(), action.targetId(), medicalUserId);
        return new MedicalAiActionResultVO(
                proposalId,
                action.actionType(),
                action.targetId(),
                "EXECUTED",
                "告警已进入处理中；后续处理结果仍由医护人员填写"
        );
    }

    public void cancel(Long medicalUserId, String proposalId) {
        if (medicalUserId == null) throw new BusinessException(401, "未获取到当前登录用户");
        if (proposalId == null || proposalId.isBlank()) return;
        proposalStore.cancel(proposalId.trim());
        log.info("medical_ai_action_cancelled proposalId={} medicalUserId={}", proposalId, medicalUserId);
    }

    private AlertRecord requireAlert(Long alertId) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) throw new BusinessException("告警不存在");
        return alert;
    }

    private void assertAssigned(Long medicalUserId, Long elderlyId) {
        List<ElderlyInfo> assigned = elderlyInfoMapper.selectElderlyListByMedicalUserId(medicalUserId);
        boolean allowed = assigned != null && assigned.stream()
                .anyMatch(item -> item != null && elderlyId != null && elderlyId.equals(item.getId()));
        if (!allowed) {
            throw new BusinessException(403, "无权对该老人的告警执行 AI 辅助操作");
        }
    }

    private void assertStartable(AlertRecord alert) {
        String status = alert.getStatus();
        if (!"待处理".equals(status) && !"未处理".equals(status)) {
            throw new BusinessException("该告警已在处理中或已关闭，不能生成开始处理操作");
        }
    }

    private void requireUserAndTarget(Long medicalUserId, Long alertId) {
        if (medicalUserId == null) throw new BusinessException(401, "未获取到当前登录用户");
        if (alertId == null) throw new BusinessException(400, "缺少告警ID");
    }
}
