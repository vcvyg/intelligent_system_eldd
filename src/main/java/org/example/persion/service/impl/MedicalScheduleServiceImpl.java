package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.MedicalSchedule;
import org.example.persion.entity.Room;
import org.example.persion.entity.User;
import org.example.persion.repository.MedicalScheduleMapper;
import org.example.persion.repository.RoomMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.service.MedicalScheduleService;
import org.example.persion.vo.MedicalScheduleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalScheduleServiceImpl implements MedicalScheduleService {

    private final MedicalScheduleMapper medicalScheduleMapper;
    private final UserMapper userMapper;
    private final RoomMapper roomMapper;

    @Override
    public List<MedicalScheduleVO> getMySchedule(Long userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<MedicalSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalSchedule::getMedicalUserId, userId)
               .between(MedicalSchedule::getScheduleDate, startDate, endDate)
               .orderByAsc(MedicalSchedule::getScheduleDate, MedicalSchedule::getStartTime);

        List<MedicalSchedule> schedules = medicalScheduleMapper.selectList(wrapper);

        if (schedules.isEmpty()) {
            return List.of();
        }

        // 批量获取关联的房间信息
        List<Long> roomIds = schedules.stream()
                                      .map(MedicalSchedule::getRoomId)
                                      .filter(id -> id != null)
                                      .distinct()
                                      .toList();
        
        Map<Long, Room> roomMap = roomIds.isEmpty() ? Map.of() : 
            roomMapper.selectBatchIds(roomIds).stream()
                      .collect(Collectors.toMap(Room::getId, Function.identity()));

        // 获取医护人员信息
        User medicalUser = userMapper.selectById(userId);
        String medicalUserName = (medicalUser != null) ? medicalUser.getRealName() : "未知用户";

        // 组装VO
        return schedules.stream().map(schedule -> {
            MedicalScheduleVO vo = new MedicalScheduleVO();
            BeanUtils.copyProperties(schedule, vo);
            vo.setMedicalUserName(medicalUserName);

            if (schedule.getRoomId() != null && roomMap.containsKey(schedule.getRoomId())) {
                vo.setRoomNumber(roomMap.get(schedule.getRoomId()).getRoomNumber());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<MedicalScheduleVO> getAllSchedules() {
        return medicalScheduleMapper.selectAllSchedulesWithDetails();
    }

    @Override
    public List<MedicalScheduleVO> getSchedulesByMedicalUser(Long medicalUserId) {
        return medicalScheduleMapper.selectSchedulesByMedicalUser(medicalUserId);
    }

    @Override
    public List<MedicalScheduleVO> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate) {
        return medicalScheduleMapper.selectSchedulesByDateRange(startDate, endDate);
    }

    @Override
    public void addSchedule(MedicalSchedule schedule) {
        medicalScheduleMapper.insert(schedule);
    }

    @Override
    public void batchAddSchedules(List<MedicalSchedule> schedules) {
        for (MedicalSchedule schedule : schedules) {
            medicalScheduleMapper.insert(schedule);
        }
    }

    @Override
    public void updateSchedule(MedicalSchedule schedule) {
        medicalScheduleMapper.updateById(schedule);
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        medicalScheduleMapper.deleteById(scheduleId);
    }
}