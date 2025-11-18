package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.MedicalScheduleService;
import org.example.persion.vo.MedicalScheduleVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 医护端 - 排班查看Controller
 */
@RestController
@RequestMapping("/api/medical/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEDICAL')")
public class MedicalScheduleController {

    private final MedicalScheduleService scheduleService;

    /**
     * 获取当前登录医护人员指定日期范围的排班
     */
    @GetMapping("/my/range")
    public Result<List<MedicalScheduleVO>> getMySchedulesByRange(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        List<MedicalScheduleVO> mySchedules = scheduleService.getMySchedule(userId, startDate, endDate);
        return Result.success(mySchedules);
    }
}
