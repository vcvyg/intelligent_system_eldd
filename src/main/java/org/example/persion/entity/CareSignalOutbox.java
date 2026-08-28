package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 主动关怀领域事件 Outbox。
 *
 * <p>只保存最小业务标识和投递状态，不复制健康数值、告警正文或问答内容。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("care_signal_outbox")
public class CareSignalOutbox extends BaseEntity {

    private String eventKey;
    private Long elderlyId;
    private String signalType;
    private Long referenceId;
    private LocalDateTime occurredAt;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastErrorType;
    private LocalDateTime processedAt;
}
