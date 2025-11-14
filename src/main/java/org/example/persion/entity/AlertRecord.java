package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 预警记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_record")
public class AlertRecord extends BaseEntity {

    /**
     * 老人ID
     */
    private Long elderlyId;

    /**
     * 预警类型: 心率异常/血压异常/血糖异常/体温异常/跌倒/离家
     */
    private String alertType;

    /**
     * 预警等级: 低/中/高/紧急
     */
    private String alertLevel;

    /**
     * 预警内容详情
     */
    private String alertContent;

    /**
     * 异常数值
     */
    private String alertValue;

    /**
     * 预警时间
     */
    private LocalDateTime alertTime;

    /**
     * 触发设备ID
     */
    private Long deviceId;

    /**
     * 处理状态: 待处理/处理中/已处理/已忽略
     */
    private String status;

    /**
     * 分配的医护人员ID
     */
    private Long assignedMedicalId;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理结果
     */
    private String handleResult;
}
