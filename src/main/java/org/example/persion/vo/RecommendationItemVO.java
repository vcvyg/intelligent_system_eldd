package org.example.persion.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationItemVO {
    private Long contentId;
    private Long deliveryId;
    private String title;
    private String summary;
    private String category;
    private BigDecimal score;
    private String reason;
    private String actionLabel;
    private String actionUrl;
    private String feedbackState;
}
