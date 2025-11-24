package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.persion.enums.ServiceProgressStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 老人服务进度记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("family_service_record")
public class FamilyServiceRecord extends BaseEntity {

    private Long elderlyId;

    private String serviceType;

    private LocalDate serviceDate;

    private LocalTime serviceTime;

    private String medicalStaff;

    private ServiceProgressStatus status;

    private String description;
}

