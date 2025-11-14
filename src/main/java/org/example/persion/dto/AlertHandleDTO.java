package org.example.persion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理预警记录DTO
 */
@Data
public class AlertHandleDTO {

    @NotNull(message = "预警ID不能为空")
    private Long alertId;

    private Long assignedMedicalId;

    private String handleResult;
}
