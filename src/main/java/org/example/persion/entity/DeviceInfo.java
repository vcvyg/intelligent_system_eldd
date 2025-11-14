package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备信息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_info")
public class DeviceInfo extends BaseEntity {

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型: 心率监测器/血压计/血糖仪/定位设备/体温计
     */
    private String deviceType;

    /**
     * 生产厂商
     */
    private String manufacturer;

    /**
     * 设备型号
     */
    private String model;

    /**
     * 绑定的老人ID
     */
    private Long elderlyId;

    /**
     * 设备状态: 在线/离线/故障
     */
    private String status;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 购买日期
     */
    private LocalDate purchaseDate;

    /**
     * 保修到期日期
     */
    private LocalDate warrantyExpireDate;

    /**
     * 备注
     */
    private String remark;
}
