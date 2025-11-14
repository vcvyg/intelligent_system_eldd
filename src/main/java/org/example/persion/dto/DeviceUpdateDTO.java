package org.example.persion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新设备DTO
 */
@Data
public class DeviceUpdateDTO {

    @NotNull(message = "设备ID不能为空")
    private Long id;

    private String deviceName;

    private String deviceType;

    private String manufacturer;

    private String model;

    private Long elderlyId;

    private String status;

    private LocalDate purchaseDate;

    private LocalDate warrantyExpireDate;

    private String remark;
}
