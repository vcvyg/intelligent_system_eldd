package org.example.persion.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备信息VO
 */
@Data
public class DeviceInfoVO {

    private Long id;

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    private String manufacturer;

    private String model;

    private Long elderlyId;

    private String elderlyName;  // 关联的老人姓名

    private String status;

    private LocalDateTime lastSyncTime;

    private LocalDate purchaseDate;

    private LocalDate warrantyExpireDate;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
