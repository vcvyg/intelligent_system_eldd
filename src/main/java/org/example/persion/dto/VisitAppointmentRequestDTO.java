package org.example.persion.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class VisitAppointmentRequestDTO {
    private Long elderlyId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String purpose;
    private String note;
}

