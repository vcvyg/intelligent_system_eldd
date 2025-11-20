package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.persion.dto.AlertCreateDTO;
import org.example.persion.entity.ElderlyFamilyRelation;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.HealthData;
import org.example.persion.enums.TimePeriod;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.service.AlertService;
import org.example.persion.service.MedicalRoundService;
import org.example.persion.vo.AlertRecordVO;
import org.example.persion.vo.DailyHealthSummaryVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRoundServiceImpl extends ServiceImpl<HealthDataMapper, HealthData> implements MedicalRoundService {

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final HealthDataMapper healthDataMapper;
    private final AlertService alertService;
    private final ElderlyFamilyRelationMapper elderlyFamilyRelationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public List<DailyHealthSummaryVO> getDailySummary(LocalDate date, Long elderlyId, String keyword) {
        // ... (此部分逻辑保持不变)
        List<ElderlyInfo> elderlyToQuery;
        if (elderlyId != null) {
            elderlyToQuery = List.of(elderlyInfoMapper.selectById(elderlyId));
        } else if (keyword != null && !keyword.isBlank()) {
            elderlyToQuery = elderlyInfoMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ElderlyInfo>().like("name", keyword));
        } else {
            return List.of();
        }

        if (elderlyToQuery.isEmpty()) {
            return List.of();
        }

        List<Long> elderlyIds = elderlyToQuery.stream().map(ElderlyInfo::getId).collect(Collectors.toList());
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<HealthData> healthDataList = healthDataMapper.findByDateTimeRangeAndElderlyIds(startOfDay, endOfDay, elderlyIds);

        Map<Long, Map<TimePeriod, HealthData>> groupedData = healthDataList.stream()
                .collect(Collectors.groupingBy(
                        HealthData::getElderlyId,
                        Collectors.toMap(
                                this::getTimePeriod,
                                hd -> hd,
                                (existing, replacement) -> existing
                        )
                ));

        return elderlyToQuery.stream().map(elderly -> {
            DailyHealthSummaryVO vo = new DailyHealthSummaryVO();
            vo.setElderlyId(elderly.getId());
            vo.setElderlyName(elderly.getName());
            vo.setRecords(groupedData.getOrDefault(elderly.getId(), Map.of()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public HealthData saveRecord(HealthData record) {
        this.saveOrUpdate(record);
        checkHealthDataAndCreateAlert(record);
        return record;
    }

    /**
     * 检查健康数据是否异常，并在需要时创建告警
     * @param healthData 最新的健康数据记录
     */
    private void checkHealthDataAndCreateAlert(HealthData healthData) {
        if (healthData.getElderlyId() == null) {
            return;
        }

        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(healthData.getElderlyId());
        if (elderlyInfo == null) {
            return;
        }

        // 检查心率
        if (healthData.getHeartRate() != null) {
            if (elderlyInfo.getHeartRateHigh() != null && healthData.getHeartRate().intValue() > elderlyInfo.getHeartRateHigh()) {
                createAlertAndNotify(elderlyInfo, "健康告警", "心率过高",
                    String.format("%.0f bpm", healthData.getHeartRate()), "警告");
            }
            if (elderlyInfo.getHeartRateLow() != null && healthData.getHeartRate().intValue() < elderlyInfo.getHeartRateLow()) {
                createAlertAndNotify(elderlyInfo, "健康告警", "心率过低",
                    String.format("%.0f bpm", healthData.getHeartRate()), "警告");
            }
        }

        // 检查收缩压
        if (healthData.getBloodPressureHigh() != null) {
            if (elderlyInfo.getSystolicPressureHigh() != null && healthData.getBloodPressureHigh().intValue() > elderlyInfo.getSystolicPressureHigh()) {
                createAlertAndNotify(elderlyInfo, "健康告警", "收缩压过高",
                    String.format("%.0f mmHg", healthData.getBloodPressureHigh()), "警告");
            }
            if (elderlyInfo.getSystolicPressureLow() != null && healthData.getBloodPressureHigh().intValue() < elderlyInfo.getSystolicPressureLow()) {
                createAlertAndNotify(elderlyInfo, "健康告警", "收缩压过低",
                    String.format("%.0f mmHg", healthData.getBloodPressureHigh()), "警告");
            }
        }

        // 检查舒张压
        if (healthData.getBloodPressureLow() != null) {
            if (elderlyInfo.getDiastolicPressureHigh() != null && healthData.getBloodPressureLow().intValue() > elderlyInfo.getDiastolicPressureHigh()) {
                createAlertAndNotify(elderlyInfo, "健康告警", "舒张压过高",
                    String.format("%.0f mmHg", healthData.getBloodPressureLow()), "警告");
            }
            if (elderlyInfo.getDiastolicPressureLow() != null && healthData.getBloodPressureLow().intValue() < elderlyInfo.getDiastolicPressureLow()) {
                createAlertAndNotify(elderlyInfo, "健康告警", "舒张压过低",
                    String.format("%.0f mmHg", healthData.getBloodPressureLow()), "警告");
            }
        }

        // 检查体温
        if (healthData.getTemperature() != null) {
            if (elderlyInfo.getTemperatureHigh() != null && healthData.getTemperature().doubleValue() > elderlyInfo.getTemperatureHigh()) {
                createAlertAndNotify(elderlyInfo, "健康告警", "体温过高",
                    String.format("%.1f °C", healthData.getTemperature()), "警告");
            }
            if (elderlyInfo.getTemperatureLow() != null && healthData.getTemperature().doubleValue() < elderlyInfo.getTemperatureLow()) {
                createAlertAndNotify(elderlyInfo, "健康告警", "体温过低",
                    String.format("%.1f °C", healthData.getTemperature()), "警告");
            }
        }
    }


    /**
     * 创建告警、存入数据库并实时推送给相关人员
     * @param elderlyInfo 关联的老人
     * @param type 告警类型
     * @param content 告警内容
     * @param value 告警数值
     * @param level 告警级别
     */
    private void createAlertAndNotify(ElderlyInfo elderlyInfo, String type, String content, String value, String level) {
        // 1. 创建并保存告警记录
        AlertCreateDTO dto = new AlertCreateDTO();
        dto.setElderlyId(elderlyInfo.getId());
        dto.setAlertType(type);
        dto.setAlertLevel(level);
        dto.setAlertContent(String.format("老人 [%s] %s，检测值为 %s", elderlyInfo.getName(), content, value));
        dto.setAlertValue(value); // 设置告警数值
        Long alertId = alertService.createAlert(dto);

        // 2. 获取完整的告警信息用于推送
        AlertRecordVO alertVO = alertService.getAlertById(alertId);
        if (alertVO == null) {
            return;
        }

        // 3. 推送到通用的告警主题，供医护端和管理端使用
        messagingTemplate.convertAndSend("/topic/alerts", alertVO);

        // 4. 确定需要通知的家属用户ID列表
        QueryWrapper<ElderlyFamilyRelation> relationQuery = new QueryWrapper<>();
        relationQuery.eq("elderly_id", elderlyInfo.getId());
        List<ElderlyFamilyRelation> familyRelations = elderlyFamilyRelationMapper.selectList(relationQuery);
        
        Set<Long> familyUserIds = familyRelations.stream()
                                                 .map(ElderlyFamilyRelation::getFamilyUserId)
                                                 .collect(Collectors.toSet());

        // 5. 通过WebSocket为每个家属推送个人相关的告警
        familyUserIds.forEach(userId -> {
            messagingTemplate.convertAndSend("/topic/alerts/user/" + userId, alertVO);
        });
    }


    private TimePeriod getTimePeriod(HealthData healthData) {
        if (healthData.getTimePeriod() == TimePeriod.DAILY) {
            return TimePeriod.DAILY;
        }
        int hour = healthData.getMeasureTime().getHour();
        if (hour >= 6 && hour < 11) {
            return TimePeriod.MORNING;
        } else if (hour >= 11 && hour < 14) {
            return TimePeriod.NOON;
        } else if (hour >= 14 && hour < 19) {
            return TimePeriod.AFTERNOON;
        } else {
            return TimePeriod.EVENING;
        }
    }
}