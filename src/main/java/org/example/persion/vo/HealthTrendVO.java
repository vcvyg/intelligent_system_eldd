package org.example.persion.vo;

import lombok.Data;
import java.util.List;

/**
 * 健康数据趋势值对象
 * 用于封装ECharts图表所需的数据系列
 */
@Data
public class HealthTrendVO {

    /**
     * 日期/时间标签 (X轴)
     */
    private List<String> dates;

    /**
     * 心率数据系列
     */
    private List<Double> heartRates;

    /**
     * 收缩压 (高压) 数据系列
     */
    private List<Double> bloodPressureHighs;

    /**
     * 舒张压 (低压) 数据系列
     */
    private List<Double> bloodPressureLows;

    /**
     * 血糖数据系列
     */
    private List<Double> bloodSugars;

    /**
     * 步数数据系列
     */
    private List<Integer> steps;
}
