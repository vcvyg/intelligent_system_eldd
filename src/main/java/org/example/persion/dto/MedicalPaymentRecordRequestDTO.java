package org.example.persion.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 医护端创建家庭支付记录请求
 */
@Data
public class MedicalPaymentRecordRequestDTO {
    private Long elderlyId;
    private Long familyUserId;
    private String itemName;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String remark;
}

