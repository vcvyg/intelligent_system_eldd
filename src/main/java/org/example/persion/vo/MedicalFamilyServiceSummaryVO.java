package org.example.persion.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 医护端生活服务统计
 */
@Data
public class MedicalFamilyServiceSummaryVO {

    /**
     * 今日已登记的服务次数
     */
    private Long todayServiceCount;

    /**
     * 待支付账单数量
     */
    private Long pendingPaymentCount;

    /**
     * 待支付总金额
     */
    private BigDecimal pendingPaymentAmount;
}


