package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.HealthData;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.security.SecurityUtil;
import org.example.persion.service.FamilyHealthService;
import org.example.persion.vo.HealthDataVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 子女端 - 健康数据服务实现
 */
@Service
@RequiredArgsConstructor
public class FamilyHealthServiceImpl implements FamilyHealthService {

    private final ElderlyFamilyRelationMapper familyRelationMapper;
    private final HealthDataMapper healthDataMapper;

    @Override
    public Map<String, Object> getDashboardData(String range) {
        Long familyUserId = SecurityUtil.getUserId();
        if (familyUserId == null) {
            throw new RuntimeException("无法获取当前用户信息");
        }

        DateRange dateRange = resolveDateRange(range);

        // 获取关联的老人列表
        List<Map<String, Object>> elderlyList = familyRelationMapper.selectElderlyListByFamilyUser(familyUserId);
        List<Map<String, Object>> elderlyWithHealth = elderlyList.stream().map(elderly -> {
            Long elderlyId = ((Number) elderly.get("elderly_id")).longValue();

            // 获取最新健康数据
            LambdaQueryWrapper<HealthData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HealthData::getElderlyId, elderlyId);
            if (dateRange.start() != null) {
                wrapper.ge(HealthData::getMeasureTime, dateRange.start());
            }
            if (dateRange.end() != null) {
                wrapper.le(HealthData::getMeasureTime, dateRange.end());
            }
            wrapper.orderByDesc(HealthData::getMeasureTime);
            wrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
            HealthData latestHealth = healthDataMapper.selectOne(wrapper);

            Map<String, Object> result = new HashMap<>(elderly);
            if (latestHealth != null) {
                result.put("latestHeartRate", latestHealth.getHeartRate());
                result.put("latestBloodPressureHigh", latestHealth.getBloodPressureHigh());
                result.put("latestBloodPressureLow", latestHealth.getBloodPressureLow());
                result.put("latestTemperature", latestHealth.getTemperature());
                result.put("latestMeasureTime", latestHealth.getMeasureTime());
                
                // 判断健康状态
                String healthStatus = evaluateHealthStatus(latestHealth);
                result.put("healthStatus", healthStatus);
            } else {
                result.put("healthStatus", "暂无数据");
            }
            return result;
        }).collect(Collectors.toList());

        // 将当天/指定区间有数据的老人排在前面
        elderlyWithHealth.sort((a, b) -> {
            LocalDateTime timeA = toLocalDateTime(a.get("latestMeasureTime"));
            LocalDateTime timeB = toLocalDateTime(b.get("latestMeasureTime"));
            if (timeA == null && timeB == null) {
                return 0;
            }
            if (timeA == null) {
                return 1;
            }
            if (timeB == null) {
                return -1;
            }
            return timeB.compareTo(timeA);
        });

        // 统计信息
        long totalElderly = elderlyList.size();
        long withHealthData = elderlyWithHealth.stream()
                .filter(e -> e.get("latestHeartRate") != null)
                .count();
        long normalStatus = elderlyWithHealth.stream()
                .filter(e -> "正常".equals(e.get("healthStatus")))
                .count();
        long abnormalStatus = elderlyWithHealth.stream()
                .filter(e -> "异常".equals(e.get("healthStatus")))
                .count();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalElderly", totalElderly);
        dashboard.put("withHealthData", withHealthData);
        dashboard.put("normalStatus", normalStatus);
        dashboard.put("abnormalStatus", abnormalStatus);
        dashboard.put("elderlyList", elderlyWithHealth);
        dashboard.put("range", dateRange.key());
        dashboard.put("rangeLabel", dateRange.label());

        return dashboard;
    }

    @Override
    public HealthDataVO getLatestHealthData(Long elderlyId) {
        // 验证该老人是否与当前用户关联
        Long familyUserId = SecurityUtil.getUserId();
        if (familyUserId == null) {
            throw new RuntimeException("无法获取当前用户信息");
        }

        List<Map<String, Object>> myElderlyList = familyRelationMapper.selectElderlyListByFamilyUser(familyUserId);
        boolean isMyElderly = myElderlyList.stream()
                .anyMatch(e -> {
                    Object idObj = e.get("elderly_id");
                    if (idObj == null) return false;
                    Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.valueOf(idObj.toString());
                    return id.equals(elderlyId);
                });
        
        if (!isMyElderly) {
            throw new RuntimeException("您无权查看该老人的健康数据");
        }

        LambdaQueryWrapper<HealthData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthData::getElderlyId, elderlyId);
        wrapper.orderByDesc(HealthData::getMeasureTime);
        wrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        HealthData healthData = healthDataMapper.selectOne(wrapper);

        if (healthData == null) {
            return null;
        }

        HealthDataVO vo = new HealthDataVO();
        BeanUtils.copyProperties(healthData, vo);
        return vo;
    }

    @Override
    public List<HealthDataVO> getHealthDataList(Long elderlyId, Integer days) {
        // 验证该老人是否与当前用户关联
        Long familyUserId = SecurityUtil.getUserId();
        if (familyUserId == null) {
            throw new RuntimeException("无法获取当前用户信息");
        }

        List<Map<String, Object>> myElderlyList = familyRelationMapper.selectElderlyListByFamilyUser(familyUserId);
        boolean isMyElderly = myElderlyList.stream()
                .anyMatch(e -> {
                    Object idObj = e.get("elderly_id");
                    if (idObj == null) return false;
                    Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.valueOf(idObj.toString());
                    return id.equals(elderlyId);
                });
        
        if (!isMyElderly) {
            throw new RuntimeException("您无权查看该老人的健康数据");
        }

        LocalDateTime startTime = LocalDateTime.now().minusDays(days != null ? days : 7);
        List<HealthData> healthDataList = healthDataMapper.findByDateTimeRange(
                startTime, LocalDateTime.now(), elderlyId);

        if (healthDataList == null || healthDataList.isEmpty()) {
            return Collections.emptyList();
        }

        List<HealthDataVO> voList = new ArrayList<>(healthDataList.size());
        for (HealthData healthData : healthDataList) {
            if (healthData == null) {
                continue;
            }
            HealthDataVO vo = new HealthDataVO();
            BeanUtils.copyProperties(healthData, vo);
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 评估健康状态
     */
    private String evaluateHealthStatus(HealthData healthData) {
        if (healthData == null) {
            return "暂无数据";
        }

        boolean hasAbnormal = false;

        // 心率正常范围：60-100
        if (healthData.getHeartRate() != null) {
            double heartRate = healthData.getHeartRate().doubleValue();
            if (heartRate < 60 || heartRate > 100) {
                hasAbnormal = true;
            }
        }

        // 血压正常范围：收缩压90-140，舒张压60-90
        if (healthData.getBloodPressureHigh() != null && healthData.getBloodPressureLow() != null) {
            double high = healthData.getBloodPressureHigh().doubleValue();
            double low = healthData.getBloodPressureLow().doubleValue();
            if (high > 140 || high < 90 || low > 90 || low < 60) {
                hasAbnormal = true;
            }
        }

        // 体温正常范围：36.0-37.5
        if (healthData.getTemperature() != null) {
            double temp = healthData.getTemperature().doubleValue();
            if (temp < 36.0 || temp > 37.5) {
                hasAbnormal = true;
            }
        }

        return hasAbnormal ? "异常" : "正常";
    }

    private DateRange resolveDateRange(String rangeKey) {
        LocalDateTime now = LocalDateTime.now();
        if (rangeKey == null) {
            rangeKey = "today";
        }

        return switch (rangeKey) {
            case "7d" -> new DateRange("7d", "近一周", now.minusDays(7), now);
            case "14d" -> new DateRange("14d", "近两周", now.minusDays(14), now);
            case "30d" -> new DateRange("30d", "近一月", now.minusDays(30), now);
            case "today" -> {
                LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
                yield new DateRange("today", "今日数据", startOfDay, now);
            }
            default -> new DateRange(rangeKey, "自定义区间", now.minusDays(7), now);
        };
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    private record DateRange(String key, String label, LocalDateTime start, LocalDateTime end) {}
}

