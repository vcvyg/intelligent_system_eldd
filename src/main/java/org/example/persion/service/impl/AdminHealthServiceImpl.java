package org.example.persion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.persion.entity.HealthData;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.service.AdminHealthService;
import org.example.persion.vo.HealthTrendVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AdminHealthServiceImpl implements AdminHealthService {

    private final HealthDataMapper healthDataMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public HealthTrendVO getHealthTrend(Integer days, Long elderlyId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        // 在Java代码中计算好查询的结束时间，避免在MyBatis注解中进行计算
        LocalDateTime exclusiveEndDate = endDate.plusDays(1).atStartOfDay();

        HealthTrendVO vo = new HealthTrendVO();
        List<String> dateLabels = new ArrayList<>();
        List<Double> heartRates = new ArrayList<>();
        List<Double> bloodPressureHighs = new ArrayList<>();
        List<Double> bloodPressureLows = new ArrayList<>();
        List<Double> bloodSugars = new ArrayList<>();
        List<Integer> steps = new ArrayList<>();

        // 准备好X轴的日期标签
        IntStream.range(0, days).forEach(i -> {
            LocalDate currentDate = startDate.plusDays(i);
            dateLabels.add(currentDate.format(DATE_FORMATTER));
        });
        vo.setDates(dateLabels);

        // 1. 获取时间范围内的日均健康数据
        List<Map<String, Object>> dailyAverages = healthDataMapper.findDailyAverageByDateRange(startDate.atStartOfDay(), exclusiveEndDate, elderlyId);

        // 如果查询结果为空，直接返回一个所有值为null的VO对象
        if (dailyAverages == null || dailyAverages.isEmpty()) {
            vo.setHeartRates(dateLabels.stream().map(d -> (Double) null).collect(Collectors.toList()));
            vo.setBloodPressureHighs(dateLabels.stream().map(d -> (Double) null).collect(Collectors.toList()));
            vo.setBloodPressureLows(dateLabels.stream().map(d -> (Double) null).collect(Collectors.toList()));
            vo.setBloodSugars(dateLabels.stream().map(d -> (Double) null).collect(Collectors.toList()));
            vo.setSteps(dateLabels.stream().map(d -> (Integer) null).collect(Collectors.toList()));
            return vo;
        }

        // 2. 创建一个包含所有日期的映射，并用查询结果填充
        Map<LocalDate, Map<String, Object>> dataByDate = dailyAverages.stream()
                .collect(Collectors.toMap(
                        m -> {
                            Object dateObj = m.get("measure_date");
                            if (dateObj instanceof java.sql.Date) {
                                return ((java.sql.Date) dateObj).toLocalDate();
                            }
                            return LocalDate.parse(dateObj.toString());
                        },
                        m -> m
                ));

        // 3. 构建 HealthTrendVO 的数据系列
        IntStream.range(0, days).forEach(i -> {
            LocalDate currentDate = startDate.plusDays(i);
            Map<String, Object> data = dataByDate.get(currentDate);

            Number avgHeartRate = (data != null) ? (Number) data.get("avg_heart_rate") : null;
            heartRates.add(avgHeartRate != null ? avgHeartRate.doubleValue() : null);

            Number avgBloodPressureHigh = (data != null) ? (Number) data.get("avg_blood_pressure_high") : null;
            bloodPressureHighs.add(avgBloodPressureHigh != null ? avgBloodPressureHigh.doubleValue() : null);

            Number avgBloodPressureLow = (data != null) ? (Number) data.get("avg_blood_pressure_low") : null;
            bloodPressureLows.add(avgBloodPressureLow != null ? avgBloodPressureLow.doubleValue() : null);

            Number avgBloodSugar = (data != null) ? (Number) data.get("avg_blood_sugar") : null;
            bloodSugars.add(avgBloodSugar != null ? avgBloodSugar.doubleValue() : null);

            Number totalSteps = (data != null) ? (Number) data.get("total_steps") : null;
            steps.add(totalSteps != null ? totalSteps.intValue() : null);
        });

        vo.setHeartRates(heartRates);
        vo.setBloodPressureHighs(bloodPressureHighs);
        vo.setBloodPressureLows(bloodPressureLows);
        vo.setBloodSugars(bloodSugars);
        vo.setSteps(steps);

        return vo;
    }

    @Override
    public HealthTrendVO getDailyHealthData(LocalDate date, Long elderlyId) {
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        // 1. 获取指定日期内的所有健康数据记录
        List<HealthData> healthDataList = healthDataMapper.findByDateTimeRange(startDateTime, endDateTime, elderlyId);
        healthDataList.sort(Comparator.comparing(HealthData::getMeasureTime));

        // 2. 构建 HealthTrendVO
        HealthTrendVO vo = new HealthTrendVO();
        vo.setDates(healthDataList.stream().map(hd -> hd.getMeasureTime().format(TIME_FORMATTER)).collect(Collectors.toList()));
        vo.setHeartRates(healthDataList.stream().map(hd -> hd.getHeartRate() != null ? hd.getHeartRate().doubleValue() : null).collect(Collectors.toList()));
        vo.setBloodPressureHighs(healthDataList.stream().map(hd -> hd.getBloodPressureHigh() != null ? hd.getBloodPressureHigh().doubleValue() : null).collect(Collectors.toList()));
        vo.setBloodPressureLows(healthDataList.stream().map(hd -> hd.getBloodPressureLow() != null ? hd.getBloodPressureLow().doubleValue() : null).collect(Collectors.toList()));
        vo.setBloodSugars(healthDataList.stream().map(hd -> hd.getBloodSugar() != null ? hd.getBloodSugar().doubleValue() : null).collect(Collectors.toList()));
        vo.setSteps(healthDataList.stream().map(HealthData::getSteps).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public void checkAndGenerateHealthAlerts(Long elderlyId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();

        // 查询当天的健康数据
        List<HealthData> healthDataList = healthDataMapper.findByDateTimeRange(startOfDay, now, elderlyId);

        for (HealthData data : healthDataList) {
            if (data.getHeartRate() != null && (data.getHeartRate().doubleValue() < 60 || data.getHeartRate().doubleValue() > 100)) {
                generateAlert(elderlyId, "心率异常", "心率值为" + data.getHeartRate());
            }
            if (data.getBloodPressureHigh() != null && data.getBloodPressureHigh().doubleValue() > 140) {
                generateAlert(elderlyId, "高血压", "收缩压值为" + data.getBloodPressureHigh());
            }
            if (data.getBloodPressureLow() != null && data.getBloodPressureLow().doubleValue() < 60) {
                generateAlert(elderlyId, "低血压", "��张压值为" + data.getBloodPressureLow());
            }
            if (data.getBloodSugar() != null && data.getBloodSugar().doubleValue() > 7.8) {
                generateAlert(elderlyId, "血糖异常", "血糖值为" + data.getBloodSugar());
            }
        }
    }

    private void generateAlert(Long elderlyId, String alertType, String alertContent) {
        // 生成告警逻辑，例如存储到数据库或调用其他服务
        System.out.println("生成告警: " + alertType + " - " + alertContent + " (老人ID: " + elderlyId + ")");
    }
}
