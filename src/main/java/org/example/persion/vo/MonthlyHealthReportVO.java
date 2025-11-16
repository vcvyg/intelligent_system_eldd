package org.example.persion.vo;

import lombok.Data;

/**
 * 月度健康报告的值对象 (VO)
 */
@Data
public class MonthlyHealthReportVO {

    /**
     * 核心指标概览
     */
    private ReportSummary summary;

    /**
     * 月度每日趋势数据
     */
    private HealthTrendVO dailyTrends;

    /**
     * 内部类，用于封装核心指标
     */
    @Data
    public static class ReportSummary {
        private Double avgHeartRate;
        private Double maxBloodPressureHigh;
        private Double minBloodPressureLow;
        private Double avgBloodSugar;
        private Integer totalSteps;
    }
}
