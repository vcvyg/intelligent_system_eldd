package org.example.persion.vo;

import java.time.LocalDateTime;

/**
 * 主动关怀触发信号视图，不包含告警正文或健康测量值。
 */
public record RecommendationTriggerVO(
        Long id,
        Long elderlyId,
        String signalType,
        String signalLabel,
        String status,
        LocalDateTime triggerTime,
        Long reviewerId,
        LocalDateTime reviewedAt,
        String decisionReason,
        LocalDateTime deliveredAt
) {
}
