package org.example.persion.controller.admin;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.entity.MedicalSchedule;
import org.example.persion.service.MedicalScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.example.persion.vo.MedicalScheduleVO;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

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
    public Result<List<MedicalScheduleVO>> getAllSchedules() {
        List<MedicalScheduleVO> list = scheduleService.getAllSchedules();
        return Result.success(list);
    }

    /**
     * 获取某个医护人员的排班列表
     */
    @GetMapping("/medical/{medicalUserId}")
    public Result<List<MedicalScheduleVO>> getSchedulesByMedicalUser(@PathVariable Long medicalUserId) {
        List<MedicalScheduleVO> list = scheduleService.getSchedulesByMedicalUser(medicalUserId);
        return Result.success(list);
    }

    /**
     * 获取指定日期范围的排班列表
     */
    @GetMapping("/range")
    public Result<List<MedicalScheduleVO>> getSchedulesByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<MedicalScheduleVO> list = scheduleService.getSchedulesByDateRange(startDate, endDate);
        return Result.success(list);
    }

    /**
     * 导出指定日期范围的排班为CSV
     */
    @GetMapping("/export")
    public void exportSchedules(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"schedules_" + startDate + "_to_" + endDate + ".csv\"");
        
        response.getWriter().write('\uFEFF');

        List<MedicalScheduleVO> schedules = scheduleService.getSchedulesByDateRange(startDate, endDate);

        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,医护姓名,排班日期,班次类型,开始时间,结束时间,状态,房间号,备注");

            for (MedicalScheduleVO schedule : schedules) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        schedule.getId(),
                        schedule.getMedicalUserName(),
                        schedule.getScheduleDate(),
                        schedule.getShiftType(),
                        schedule.getStartTime(),
                        schedule.getEndTime(),
                        schedule.getStatus(),
                        schedule.getRoomNumber(),
                        schedule.getRemark()
                );
            }
        }
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
