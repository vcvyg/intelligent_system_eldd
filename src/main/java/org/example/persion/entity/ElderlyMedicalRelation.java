package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 老人-医护人员关系实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("elderly_medical_relation")
public class ElderlyMedicalRelation extends BaseEntity {

    /**
     * 老人ID
     */
    private Long elderlyId;

    /**
     * 医护人员ID
     */
    private Long medicalUserId;

    /**
     * 是否主治医生 0-否 1-是
     */
    private Integer isPrimaryDoctor;

    /**
     * 分配日期
     */
    private LocalDateTime assignDate;

    /**
     * 备注
     */
    private String remark;
}
