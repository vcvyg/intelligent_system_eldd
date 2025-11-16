package org.example.persion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.AdminReportService;
import org.example.persion.vo.MonthlyHealthReportVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    /**
     * 获取月度健康报告
     * @param month "YYYY-MM"格式的月份字符串
     * @param elderlyId 老人ID (可选, 为null或空则查询所有人)
     * @return 组装好的月度健康报告数据
     */
    @GetMapping("/monthly_health")
    public Result<MonthlyHealthReportVO> getMonthlyHealthReport(
            @RequestParam String month,
            @RequestParam(required = false) Long elderlyId) {
        
        MonthlyHealthReportVO report = adminReportService.generateMonthlyHealthReport(month, elderlyId);
        return Result.success(report);
    }

    /**
     * 获取医护人员月度考勤报告
     * @param month "YYYY-MM"格式的月份字符串
     * @param staffId 医护人员ID (可选, 为null或空则查询所有人)
     * @return 组装好的考勤报告数据
     */
    @GetMapping("/staff_attendance")
    public Result<org.example.persion.vo.StaffAttendanceReportVO> getStaffAttendanceReport(
            @RequestParam String month,
            @RequestParam(required = false) Long staffId) {

        org.example.persion.vo.StaffAttendanceReportVO report = adminReportService.generateStaffAttendanceReport(month, staffId);
        return Result.success(report);
    }
}
