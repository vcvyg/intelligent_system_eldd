package org.example.persion.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预警记录VO
 */
@Data
public class AlertRecordVO {

    private Long id;

    private Long elderlyId;

    private String elderlyName;  // 老人姓名

    private String roomName; // 房间名称

    private String alertType;

    private String alertLevel;

    private String alertContent;

    private String alertValue;

    private LocalDateTime alertTime;

    private Long deviceId;

    private String deviceName;  // 设备名称

    private String status;

    private Long assignedMedicalId;

    private String medicalName;  // 医护人员姓名

    private LocalDateTime handleTime;

    private String handleResult;

    private LocalDateTime createTime;
}
