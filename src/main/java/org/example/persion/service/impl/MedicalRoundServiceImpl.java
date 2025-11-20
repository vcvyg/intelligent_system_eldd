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
            int heartRate = healthData.getHeartRate().intValue();
            if (elderlyInfo.getHeartRateHigh() != null && heartRate > elderlyInfo.getHeartRateHigh()) {
                String level = heartRate > elderlyInfo.getHeartRateHigh() + 20 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "心率异常",
                    String.format("心率偏高，%d bpm", heartRate),
                    String.format("%d", heartRate), level);
            }
            if (elderlyInfo.getHeartRateLow() != null && heartRate < elderlyInfo.getHeartRateLow()) {
                String level = heartRate < elderlyInfo.getHeartRateLow() - 10 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "心率异常",
                    String.format("心率偏低，%d bpm", heartRate),
                    String.format("%d", heartRate), level);
            }
        }

        // 检查血压（收缩压和舒张压一起检查）
        if (healthData.getBloodPressureHigh() != null && healthData.getBloodPressureLow() != null) {
            int systolic = healthData.getBloodPressureHigh().intValue();
            int diastolic = healthData.getBloodPressureLow().intValue();
            String bpValue = String.format("%d/%d", systolic, diastolic);

            // 检查收缩压过高
            if (elderlyInfo.getSystolicPressureHigh() != null && systolic > elderlyInfo.getSystolicPressureHigh()) {
                String level = systolic > elderlyInfo.getSystolicPressureHigh() + 20 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "血压异常",
                    String.format("血压偏高，收缩压%dmmHg", systolic),
                    bpValue, level);
            }
            // 检查收缩压过低
            else if (elderlyInfo.getSystolicPressureLow() != null && systolic < elderlyInfo.getSystolicPressureLow()) {
                String level = systolic < elderlyInfo.getSystolicPressureLow() - 10 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "血压异常",
                    String.format("血压偏低，收缩压%dmmHg", systolic),
                    bpValue, level);
            }
            // 检查舒张压过高
            else if (elderlyInfo.getDiastolicPressureHigh() != null && diastolic > elderlyInfo.getDiastolicPressureHigh()) {
                String level = diastolic > elderlyInfo.getDiastolicPressureHigh() + 10 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "血压异常",
                    String.format("血压偏高，舒张压%dmmHg", diastolic),
                    bpValue, level);
            }
            // 检查舒张压过低
            else if (elderlyInfo.getDiastolicPressureLow() != null && diastolic < elderlyInfo.getDiastolicPressureLow()) {
                String level = diastolic < elderlyInfo.getDiastolicPressureLow() - 10 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "血压异常",
                    String.format("血压偏低，舒张压%dmmHg", diastolic),
                    bpValue, level);
            }
        }

        // 检查体温
        if (healthData.getTemperature() != null) {
            double temp = healthData.getTemperature().doubleValue();
            if (elderlyInfo.getTemperatureHigh() != null && temp > elderlyInfo.getTemperatureHigh()) {
                String level = temp > 38.5 ? "紧急" : (temp > 37.8 ? "高" : "中");
                createAlertAndNotify(elderlyInfo, "体温异常",
                    String.format("体温偏高，%.1f°C", temp),
                    String.format("%.1f", temp), level);
            }
            if (elderlyInfo.getTemperatureLow() != null && temp < elderlyInfo.getTemperatureLow()) {
                String level = temp < 35.0 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "体温异常",
                    String.format("体温偏低，%.1f°C", temp),
                    String.format("%.1f", temp), level);
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
        dto.setAlertContent(content); // 直接使用传入的内容，不添加前缀
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