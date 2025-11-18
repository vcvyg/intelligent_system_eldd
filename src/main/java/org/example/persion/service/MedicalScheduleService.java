package org.example.persion.service;

import org.example.persion.entity.MedicalSchedule;
import org.example.persion.vo.MedicalScheduleVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 医护端-排班服务接口
 */
public interface MedicalScheduleService {

    /**
     * 获取指定医护人员在给定日期范围内的排班信息
     * @param userId 医护人员ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班VO列表
     */
    List<MedicalScheduleVO> getMySchedule(Long userId, LocalDate startDate, LocalDate endDate);

    List<MedicalScheduleVO> getAllSchedules();

    List<MedicalScheduleVO> getSchedulesByMedicalUser(Long medicalUserId);

    List<MedicalScheduleVO> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate);

    void addSchedule(MedicalSchedule schedule);

    void batchAddSchedules(List<MedicalSchedule> schedules);

    void updateSchedule(MedicalSchedule schedule);

    void deleteSchedule(Long scheduleId);
}