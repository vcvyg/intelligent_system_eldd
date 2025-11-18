package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.persion.enums.TimePeriod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康数据实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_data")
public class HealthData extends BaseEntity {

    private Long elderlyId; // 老人ID

    private BigDecimal heartRate; // 心率

    private BigDecimal bloodPressureHigh; // 收缩压

    private BigDecimal bloodPressureLow; // 舒张压

    private BigDecimal temperature; // 体温

    private BigDecimal bloodSugar; // 血糖

    private Integer sleepDuration; // 睡眠时长(分钟)

    private Integer steps; // 步数

    private LocalDateTime measureTime; // 测量时间

    private TimePeriod timePeriod; // 时间周期
}
