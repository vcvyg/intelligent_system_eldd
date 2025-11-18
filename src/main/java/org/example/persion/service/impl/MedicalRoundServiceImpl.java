package org.example.persion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.HealthData;
import org.example.persion.enums.TimePeriod;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.service.MedicalRoundService;
import org.example.persion.vo.DailyHealthSummaryVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRoundServiceImpl extends ServiceImpl<HealthDataMapper, HealthData> implements MedicalRoundService {

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final HealthDataMapper healthDataMapper;

    @Override
    public List<DailyHealthSummaryVO> getDailySummary(LocalDate date, Long elderlyId, String keyword) {
        // 1. 根据参数确定要查询的老人列表
        List<ElderlyInfo> elderlyToQuery;
        if (elderlyId != null) {
            elderlyToQuery = List.of(elderlyInfoMapper.selectById(elderlyId));
        } else if (keyword != null && !keyword.isBlank()) {
            elderlyToQuery = elderlyInfoMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ElderlyInfo>().like("name", keyword));
        } else {
            // 如果没有指定老人或关键字，返回空列表，避免加载所有
            return List.of();
        }

        if (elderlyToQuery.isEmpty()) {
            return List.of();
        }

        // 2. 获取指定日期的所有相关健康记录
        List<Long> elderlyIds = elderlyToQuery.stream().map(ElderlyInfo::getId).collect(Collectors.toList());
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<HealthData> healthDataList = healthDataMapper.findByDateTimeRangeAndElderlyIds(startOfDay, endOfDay, elderlyIds);

        // 3. 按老人ID和时段分组
        Map<Long, Map<TimePeriod, HealthData>> groupedData = healthDataList.stream()
                .collect(Collectors.groupingBy(
                        HealthData::getElderlyId,
                        Collectors.toMap(
                                this::getTimePeriod,
                                hd -> hd,
                                (existing, replacement) -> existing // 如果有重复，保留第一个
                        )
                ));

        // 4. 构建VO
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
        return record;
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

