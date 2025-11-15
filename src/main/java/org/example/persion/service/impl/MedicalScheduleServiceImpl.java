package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.MedicalSchedule;
import org.example.persion.repository.MedicalScheduleMapper;
import org.example.persion.service.MedicalScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 医护排班Service实现
 */
@Service
@RequiredArgsConstructor
public class MedicalScheduleServiceImpl implements MedicalScheduleService {

    private final MedicalScheduleMapper scheduleMapper;

    @Override
    public List<Map<String, Object>> getSchedulesByMedicalUser(Long medicalUserId) {
        return scheduleMapper.selectScheduleListWithUserInfo(medicalUserId);
    }

    @Override
    public List<Map<String, Object>> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate) {
        return scheduleMapper.selectScheduleListByDateRange(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getAllSchedules() {
        return scheduleMapper.selectAllSchedulesWithUserInfo();
    }

    @Override
    public boolean addSchedule(MedicalSchedule schedule) {
        // 检查该医护人员在该日期该时间段是否已有排班
        LambdaQueryWrapper<MedicalSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalSchedule::getMedicalUserId, schedule.getMedicalUserId())
                .eq(MedicalSchedule::getScheduleDate, schedule.getScheduleDate());
        Long count = scheduleMapper.selectCount(wrapper);

        if (count > 0) {
            throw new RuntimeException("该医护人员在该日期已有排班安排");
        }

        return scheduleMapper.insert(schedule) > 0;
    }

    @Override
    public boolean updateSchedule(MedicalSchedule schedule) {
        return scheduleMapper.updateById(schedule) > 0;
    }

    @Override
    public boolean deleteSchedule(Long scheduleId) {
        return scheduleMapper.deleteById(scheduleId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAddSchedules(List<MedicalSchedule> schedules) {
        for (MedicalSchedule schedule : schedules) {
            addSchedule(schedule);
        }
        return true;
    }
}
