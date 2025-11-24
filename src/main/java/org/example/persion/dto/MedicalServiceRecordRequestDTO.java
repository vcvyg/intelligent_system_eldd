package org.example.persion.dto;

import lombok.Data;
import org.example.persion.enums.ServiceProgressStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 医护端录入服务记录请求
 */
@Data
public class MedicalServiceRecordRequestDTO {
    private Long elderlyId;
    private String serviceType;
    private LocalDate serviceDate;
    private LocalTime serviceTime;
    private ServiceProgressStatus status;
    private String description;
}

