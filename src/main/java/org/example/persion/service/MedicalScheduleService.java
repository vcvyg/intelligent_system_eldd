package org.example.persion.service;

import org.example.persion.entity.MedicalSchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 医护排班Service
 */
public interface MedicalScheduleService {

    /**
     * 获取某个医护人员的排班列表
     */
    List<Map<String, Object>> getSchedulesByMedicalUser(Long medicalUserId);

    /**
     * 获取指定日期范围的排班列表
     */
    List<Map<String, Object>> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 获取所有排班列表
     */
    List<Map<String, Object>> getAllSchedules();

    /**
     * 添加排班
     */
    boolean addSchedule(MedicalSchedule schedule);

    /**
     * 更新排班
     */
    boolean updateSchedule(MedicalSchedule schedule);

    /**
     * 删除排班
     */
    boolean deleteSchedule(Long scheduleId);

    /**
     * 批量添加排班
     */
    boolean batchAddSchedules(List<MedicalSchedule> schedules);
}
