package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.MedicalScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 医护端 - 排班查看Controller
 */
@RestController
@RequestMapping("/api/medical/schedule")
@RequiredArgsConstructor
public class MedicalScheduleController {

    private final MedicalScheduleService scheduleService;

    /**
     * 获取当前登录医护人员的排班列表
     */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMySchedules() {
        // 从Security上下文获取当前用户ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        List<Map<String, Object>> list = scheduleService.getSchedulesByMedicalUser(userId);
        return Result.success(list);
    }

    /**
     * 获取当前登录医护人员指定日期范围的排班
     */
    @GetMapping("/my/range")
    public Result<List<Map<String, Object>>> getMySchedulesByRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        // 从Security上下文获取当前用户ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        // 获取所有排班后过滤当前用户的
        List<Map<String, Object>> allSchedules = scheduleService.getSchedulesByDateRange(startDate, endDate);
        List<Map<String, Object>> mySchedules = allSchedules.stream()
                .filter(schedule -> userId.equals(schedule.get("medical_user_id")))
                .toList();

        return Result.success(mySchedules);
    }
}
