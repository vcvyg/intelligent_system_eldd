package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.entity.RecommendationTrigger;
import org.example.persion.repository.RecommendationTriggerMapper;
import org.example.persion.service.RecommendationTriggerService;
import org.example.persion.vo.RecommendationTriggerVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationTriggerServiceImpl implements RecommendationTriggerService {

    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String DELIVERED = "DELIVERED";

    private final RecommendationTriggerMapper triggerMapper;

    @Override
    @Transactional
    public void record(CareSignalEvent event) {
        if (event == null || event.elderlyId() == null || event.signalType() == null || event.signalType().isBlank()) {
            return;
        }

        LambdaQueryWrapper<RecommendationTrigger> duplicateQuery = new LambdaQueryWrapper<RecommendationTrigger>()
                .eq(RecommendationTrigger::getElderlyId, event.elderlyId())
                .eq(RecommendationTrigger::getSignalType, event.signalType());
        if (event.referenceId() == null) {
            duplicateQuery.isNull(RecommendationTrigger::getReferenceId);
        } else {
            duplicateQuery.eq(RecommendationTrigger::getReferenceId, event.referenceId());
        }
        Long duplicate = triggerMapper.selectCount(duplicateQuery);
        if (duplicate != null && duplicate > 0) return;

        RecommendationTrigger trigger = new RecommendationTrigger();
        trigger.setElderlyId(event.elderlyId());
        trigger.setSignalType(event.signalType());
        trigger.setReferenceId(event.referenceId());
        trigger.setStatus(PENDING_REVIEW);
        trigger.setTriggerTime(event.occurredAt() == null ? LocalDateTime.now() : event.occurredAt());
        triggerMapper.insert(trigger);
    }

    @Override
    public List<RecommendationTriggerVO> pending(Long elderlyId) {
        LambdaQueryWrapper<RecommendationTrigger> wrapper = new LambdaQueryWrapper<RecommendationTrigger>()
                .in(RecommendationTrigger::getStatus, PENDING_REVIEW, APPROVED)
                .orderByDesc(RecommendationTrigger::getTriggerTime);
        if (elderlyId != null) wrapper.eq(RecommendationTrigger::getElderlyId, elderlyId);

        List<RecommendationTrigger> triggers = triggerMapper.selectList(wrapper);
        if (triggers == null || triggers.isEmpty()) return List.of();
        return triggers.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public RecommendationTriggerVO approve(Long triggerId, Long reviewerId, String reason) {
        return review(triggerId, reviewerId, APPROVED, reason);
    }

    @Override
    @Transactional
    public RecommendationTriggerVO reject(Long triggerId, Long reviewerId, String reason) {
        return review(triggerId, reviewerId, REJECTED, reason);
    }

    @Override
    public boolean hasPending(Long elderlyId) {
        if (elderlyId == null) return false;
        Long count = triggerMapper.selectCount(new LambdaQueryWrapper<RecommendationTrigger>()
                .eq(RecommendationTrigger::getElderlyId, elderlyId)
                .eq(RecommendationTrigger::getStatus, PENDING_REVIEW));
        return count != null && count > 0;
    }

    @Override
    public boolean hasApproved(Long elderlyId) {
        if (elderlyId == null) return false;
        Long count = triggerMapper.selectCount(new LambdaQueryWrapper<RecommendationTrigger>()
                .eq(RecommendationTrigger::getElderlyId, elderlyId)
                .eq(RecommendationTrigger::getStatus, APPROVED));
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void markDelivered(Long elderlyId) {
        if (elderlyId == null) return;
        List<RecommendationTrigger> approved = triggerMapper.selectList(
                new LambdaQueryWrapper<RecommendationTrigger>()
                        .eq(RecommendationTrigger::getElderlyId, elderlyId)
                        .eq(RecommendationTrigger::getStatus, APPROVED)
        );
        if (approved == null || approved.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        for (RecommendationTrigger trigger : approved) {
            trigger.setStatus(DELIVERED);
            trigger.setDeliveredAt(now);
            triggerMapper.updateById(trigger);
        }
    }

    private RecommendationTriggerVO review(Long triggerId, Long reviewerId, String decision, String reason) {
        if (triggerId == null) throw new BusinessException("缺少推荐触发器ID");
        if (reviewerId == null) throw new BusinessException("未获取到当前复核人员");

        RecommendationTrigger trigger = triggerMapper.selectById(triggerId);
        if (trigger == null) throw new BusinessException("推荐触发事件不存在");
        if (!PENDING_REVIEW.equals(trigger.getStatus())) {
            throw new BusinessException("该触发事件已完成复核，不能重复审批");
        }

        trigger.setStatus(decision);
        trigger.setReviewerId(reviewerId);
        trigger.setReviewedAt(LocalDateTime.now());
        trigger.setDecisionReason(normalizeReason(reason));
        triggerMapper.updateById(trigger);
        return toVO(trigger);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String normalized = reason.trim().replace('\n', ' ').replace('\r', ' ');
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300);
    }

    private RecommendationTriggerVO toVO(RecommendationTrigger trigger) {
        String label = switch (trigger.getSignalType()) {
            case "ALERT_RAISED" -> "新告警触发关怀复核";
            case "HEALTH_RECORDED" -> "新健康记录触发关怀复核";
            case "SERVICE_SCHEDULED" -> "新服务安排触发关怀复核";
            default -> "业务变化触发关怀复核";
        };
        return new RecommendationTriggerVO(
                trigger.getId(),
                trigger.getElderlyId(),
                trigger.getSignalType(),
                label,
                trigger.getStatus(),
                trigger.getTriggerTime(),
                trigger.getReviewerId(),
                trigger.getReviewedAt(),
                trigger.getDecisionReason(),
                trigger.getDeliveredAt()
        );
    }
}
