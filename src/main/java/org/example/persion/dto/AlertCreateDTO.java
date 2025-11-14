package org.example.persion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建预警记录DTO
 */
@Data
public class AlertCreateDTO {

    @NotNull(message = "老人ID不能为空")
    private Long elderlyId;

    @NotBlank(message = "预警类型不能为空")
    private String alertType;

    @NotBlank(message = "预警等级不能为空")
    private String alertLevel;

    @NotBlank(message = "预警内容不能为空")
    private String alertContent;

    private String alertValue;

    private LocalDateTime alertTime;

    private Long deviceId;
}
