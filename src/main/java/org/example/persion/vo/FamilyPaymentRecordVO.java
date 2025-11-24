package org.example.persion.vo;

import lombok.Data;
import org.example.persion.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FamilyPaymentRecordVO {
    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private String itemName;
    private BigDecimal amount;
    private PaymentStatus status;
    private String payMethod;
    private LocalDateTime payTime;
    private LocalDate dueDate;
    private String remark;
}

