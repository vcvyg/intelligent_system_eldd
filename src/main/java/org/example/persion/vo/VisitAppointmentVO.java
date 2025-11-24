package org.example.persion.vo;

import lombok.Data;
import org.example.persion.enums.VisitAppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class VisitAppointmentVO {
    private Long id;
    private Long elderlyId;
    private Long familyUserId;
    private String familyUsername;
    private String elderlyName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String purpose;
    private String note;
    private VisitAppointmentStatus status;
    private String reviewRemark;
}

