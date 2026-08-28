package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.ai.event.CareSignalEvent;
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
        trigger.setStatus("PENDING_REVIEW");
        trigger.setTriggerTime(event.occurredAt() == null ? LocalDateTime.now() : event.occurredAt());
        triggerMapper.insert(trigger);
    }

    @Override
    public List<RecommendationTriggerVO> pending(Long elderlyId) {
        LambdaQueryWrapper<RecommendationTrigger> wrapper = new LambdaQueryWrapper<RecommendationTrigger>()
                .eq(RecommendationTrigger::getStatus, "PENDING_REVIEW")
                .orderByDesc(RecommendationTrigger::getTriggerTime);
        if (elderlyId != null) wrapper.eq(RecommendationTrigger::getElderlyId, elderlyId);

        List<RecommendationTrigger> triggers = triggerMapper.selectList(wrapper);
        if (triggers == null || triggers.isEmpty()) return List.of();
        return triggers.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public void markDelivered(Long elderlyId) {
        if (elderlyId == null) return;
        List<RecommendationTrigger> pending = triggerMapper.selectList(
                new LambdaQueryWrapper<RecommendationTrigger>()
                        .eq(RecommendationTrigger::getElderlyId, elderlyId)
                        .eq(RecommendationTrigger::getStatus, "PENDING_REVIEW")
        );
        if (pending == null) return;
        for (RecommendationTrigger trigger : pending) {
            trigger.setStatus("DELIVERED");
            triggerMapper.updateById(trigger);
        }
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
                trigger.getTriggerTime()
        );
    }
}
