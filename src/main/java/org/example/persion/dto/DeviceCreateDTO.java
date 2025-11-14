package org.example.persion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建设备DTO
 */
@Data
public class DeviceCreateDTO {

    @NotBlank(message = "设备编号不能为空")
    private String deviceCode;

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    @NotBlank(message = "设备类型不能为空")
    private String deviceType;

    private String manufacturer;

    private String model;

    private Long elderlyId;

    private String status = "在线";

    private LocalDate purchaseDate;

    private LocalDate warrantyExpireDate;

    private String remark;
}
