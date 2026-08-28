package org.example.persion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecommendationFeedbackDTO {

    @NotNull
    private Long elderlyId;

    @NotNull
    private Long deliveryId;

    @NotBlank
    private String feedbackType;
}
