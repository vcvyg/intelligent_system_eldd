package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommendation_delivery")
public class RecommendationDelivery extends BaseEntity {

    private Long elderlyId;
    private Long familyUserId;
    private Long contentId;
    private String channel;
    private String status;
    private BigDecimal score;
    private String reason;
    private LocalDateTime exposedAt;
    private LocalDateTime clickedAt;
}
