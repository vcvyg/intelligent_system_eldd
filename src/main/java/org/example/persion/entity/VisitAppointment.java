package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.persion.enums.VisitAppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 探访预约记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visit_appointment")
public class VisitAppointment extends BaseEntity {

    private Long familyUserId;

    private Long elderlyId;

    private LocalDate appointmentDate;

    private LocalTime appointmentTime;

    private String purpose;

    private String note;

    private VisitAppointmentStatus status;

    private String reviewRemark;
}

