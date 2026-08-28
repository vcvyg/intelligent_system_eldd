package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommendation_feedback")
public class RecommendationFeedback extends BaseEntity {

    private Long elderlyId;
    private Long familyUserId;
    private Long contentId;
    private Long deliveryId;
    private String feedbackType;
    private Integer weight;
}
