package org.example.persion.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MedicalRoundRecordVO {
    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private Integer heartRate;
    private Integer bloodPressureHigh;
    private Integer bloodPressureLow;
    private BigDecimal temperature;
    private BigDecimal bloodSugar;
    private Integer steps;
    private Integer sleepDuration;
    private LocalDateTime measureTime;
}

