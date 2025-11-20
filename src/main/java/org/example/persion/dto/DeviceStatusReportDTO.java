package org.example.persion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 设备状态上报DTO
 */
@Data
public class DeviceStatusReportDTO {

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    @NotBlank(message = "事件类型不能为空")
    private String eventType; // 例如: "FALL_DETECTED", "HEART_RATE_EMERGENCY", "SOS"

    private String eventValue; // 可选，事件相关的数值
}
