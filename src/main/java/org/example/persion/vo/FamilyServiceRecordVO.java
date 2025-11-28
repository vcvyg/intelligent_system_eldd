package org.example.persion.vo;

import lombok.Data;
import org.example.persion.enums.ServiceProgressStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class FamilyServiceRecordVO {
    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private String serviceType;
    private LocalDate serviceDate;
    private LocalTime serviceTime;
    private String medicalStaff;
    private ServiceProgressStatus status;
    private String description;
    private List<ServiceStatusHistoryVO> statusTimeline;
}

