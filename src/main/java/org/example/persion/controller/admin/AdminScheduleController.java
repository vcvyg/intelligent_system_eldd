package org.example.persion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.entity.MedicalSchedule;
import org.example.persion.service.MedicalScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 医护排班管理Controller
 */
@RestController
@RequestMapping("/api/admin/schedule")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final MedicalScheduleService scheduleService;

    /**
     * 获取所有排班列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getAllSchedules() {
        List<Map<String, Object>> list = scheduleService.getAllSchedules();
        return Result.success(list);
    }

    /**
     * 获取某个医护人员的排班列表
     */
    @GetMapping("/medical/{medicalUserId}")
    public Result<List<Map<String, Object>>> getSchedulesByMedicalUser(@PathVariable Long medicalUserId) {
        List<Map<String, Object>> list = scheduleService.getSchedulesByMedicalUser(medicalUserId);
        return Result.success(list);
    }

    /**
     * 获取指定日期范围的排班列表
     */
    @GetMapping("/range")
    public Result<List<Map<String, Object>>> getSchedulesByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<Map<String, Object>> list = scheduleService.getSchedulesByDateRange(startDate, endDate);
        return Result.success(list);
    }

    /**
     * 添加排班
     */
    @PostMapping("/add")
    public Result<Void> addSchedule(@RequestBody MedicalSchedule schedule) {
        scheduleService.addSchedule(schedule);
        return Result.success();
    }

    /**
     * 批量添加排班
     */
    @PostMapping("/batch-add")
    public Result<Void> batchAddSchedules(@RequestBody List<MedicalSchedule> schedules) {
        scheduleService.batchAddSchedules(schedules);
        return Result.success();
    }

    /**
     * 更新排班
     */
    @PutMapping("/update")
    public Result<Void> updateSchedule(@RequestBody MedicalSchedule schedule) {
        scheduleService.updateSchedule(schedule);
        return Result.success();
    }

    /**
     * 删除排班
     */
    @DeleteMapping("/{scheduleId}")
    public Result<Void> deleteSchedule(@PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return Result.success();
    }
}
