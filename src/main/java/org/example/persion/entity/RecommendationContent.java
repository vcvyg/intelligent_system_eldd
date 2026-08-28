package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommendation_content")
public class RecommendationContent extends BaseEntity {

    private String code;
    private String title;
    private String summary;
    private String category;
    private BigDecimal baseScore;
    private String actionLabel;
    private String actionUrl;
    private Integer enabled;
}
