package org.example.persion.ai.event;

import java.time.LocalDateTime;

/**
 * 可触发主动关怀候选刷新的业务信号。
 *
 * <p>事件只携带最小业务标识，不携带健康测量值、告警正文或其他敏感医疗内容。</p>
 */
public record CareSignalEvent(
        Long elderlyId,
        String signalType,
        Long referenceId,
        LocalDateTime occurredAt
) {
    public static CareSignalEvent alertRaised(Long elderlyId, Long alertId, LocalDateTime occurredAt) {
        return new CareSignalEvent(elderlyId, "ALERT_RAISED", alertId, occurredAt);
    }
}
