package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 巡诊记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medical_round_record")
public class MedicalRoundRecord extends BaseEntity {

    /**
     * 老人ID
     */
    private Long elderlyId;

    /**
     * 医护人员ID
     */
    private Long medicalUserId;

    /**
     * 巡诊时间
     */
    private LocalDateTime roundTime;

    /**
     * 健康状况描述
     */
    private String healthDescription;

    /**
     * 生命体征 (例如: "体温:36.5°C, 心率:75bpm, 血压:120/80mmHg")
     */
    private String vitalSigns;

    /**
     * 备注
     */
    private String notes;
}

