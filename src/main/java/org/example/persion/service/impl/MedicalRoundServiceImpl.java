package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.persion.ai.event.CareSignalEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<DailyHealthSummaryVO> getDailySummary(LocalDate date, Long elderlyId, String keyword) {
        List<ElderlyInfo> elderlyToQuery;
        if (elderlyId != null) {
            ElderlyInfo elderly = elderlyInfoMapper.selectById(elderlyId);
            elderlyToQuery = elderly == null ? List.of() : List.of(elderly);
        } else if (keyword != null && !keyword.isBlank()) {
            elderlyToQuery = elderlyInfoMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ElderlyInfo>().like("name", keyword)
            );
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
    @Transactional(rollbackFor = Exception.class)
    public HealthData saveRecord(HealthData record) {
        this.saveOrUpdate(record);
        if (record.getElderlyId() != null) {
            eventPublisher.publishEvent(CareSignalEvent.healthRecorded(
                    record.getElderlyId(),
                    record.getId(),
                    record.getMeasureTime() == null ? LocalDateTime.now() : record.getMeasureTime()
            ));
        }
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

        if (healthData.getBloodPressureHigh() != null && healthData.getBloodPressureLow() != null) {
            int systolic = healthData.getBloodPressureHigh().intValue();
            int diastolic = healthData.getBloodPressureLow().intValue();
            String bpValue = String.format("%d/%d", systolic, diastolic);

            if (elderlyInfo.getSystolicPressureHigh() != null && systolic > elderlyInfo.getSystolicPressureHigh()) {
                String level = systolic > elderlyInfo.getSystolicPressureHigh() + 20 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "血压异常",
                    String.format("血压偏高，收缩压%dmmHg", systolic),
                    bpValue, level);
            } else if (elderlyInfo.getSystolicPressureLow() != null && systolic < elderlyInfo.getSystolicPressureLow()) {
                String level = systolic < elderlyInfo.getSystolicPressureLow() - 10 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "血压异常",
                    String.format("血压偏低，收缩压%dmmHg", systolic),
                    bpValue, level);
            } else if (elderlyInfo.getDiastolicPressureHigh() != null && diastolic > elderlyInfo.getDiastolicPressureHigh()) {
                String level = diastolic > elderlyInfo.getDiastolicPressureHigh() + 10 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "血压异常",
                    String.format("血压偏高，舒张压%dmmHg", diastolic),
                    bpValue, level);
            } else if (elderlyInfo.getDiastolicPressureLow() != null && diastolic < elderlyInfo.getDiastolicPressureLow()) {
                String level = diastolic < elderlyInfo.getDiastolicPressureLow() - 10 ? "高" : "中";
                createAlertAndNotify(elderlyInfo, "血压异常",
                    String.format("血压偏低，舒张压%dmmHg", diastolic),
                    bpValue, level);
            }
        }

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

    private void createAlertAndNotify(ElderlyInfo elderlyInfo, String type, String content, String value, String level) {
        AlertCreateDTO dto = new AlertCreateDTO();
        dto.setElderlyId(elderlyInfo.getId());
        dto.setAlertType(type);
        dto.setAlertLevel(level);
        dto.setAlertContent(content);
        dto.setAlertValue(value);
        Long alertId = alertService.createAlert(dto);

        AlertRecordVO alertVO = alertService.getAlertById(alertId);
        if (alertVO == null) {
            return;
        }

        messagingTemplate.convertAndSend("/topic/alerts", alertVO);

        QueryWrapper<ElderlyFamilyRelation> relationQuery = new QueryWrapper<>();
        relationQuery.eq("elderly_id", elderlyInfo.getId());
        List<ElderlyFamilyRelation> familyRelations = elderlyFamilyRelationMapper.selectList(relationQuery);

        Set<Long> familyUserIds = familyRelations.stream()
                .map(ElderlyFamilyRelation::getFamilyUserId)
                .collect(Collectors.toSet());

        familyUserIds.forEach(userId -> messagingTemplate.convertAndSend("/topic/alerts/user/" + userId, alertVO));
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
