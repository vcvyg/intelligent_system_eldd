package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.persion.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生活缴费记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("family_payment_record")
public class FamilyPaymentRecord extends BaseEntity {

    private Long familyUserId;

    private Long elderlyId;

    private String itemName;

    private BigDecimal amount;

    private PaymentStatus status;

    private String payMethod;

    private LocalDateTime payTime;

    private LocalDate dueDate;

    private String remark;

    @TableField(exist = false)
    private String elderlyName;
}

