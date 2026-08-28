package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommendation_trigger")
public class RecommendationTrigger extends BaseEntity {

    private Long elderlyId;
    private String signalType;
    private Long referenceId;
    private String status;
    private LocalDateTime triggerTime;
}
