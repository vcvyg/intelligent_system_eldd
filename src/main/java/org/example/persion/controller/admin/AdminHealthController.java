package org.example.persion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.AdminHealthService;
import org.example.persion.vo.HealthTrendVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 管理端 - 健康数据统计控制器
 */
@RestController
@RequestMapping("/api/admin/health")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminHealthController {

    private final AdminHealthService adminHealthService;

    /**
     * 获取健康数据趋势 (例如：近7天)
     * @param days 天数
     * @param elderlyId 老人ID (可选)
     * @return 健康趋势数据
     */
    @GetMapping("/trend")
    public Result<HealthTrendVO> getHealthTrend(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Long elderlyId) {
        HealthTrendVO vo = adminHealthService.getHealthTrend(days, elderlyId);
        return Result.success(vo);
    }

    /**
     * 获取指定日期的健康数据
     * @param date 日期
     * @param elderlyId 老人ID (可选)
     * @return 当日的健康数据
     */
    @GetMapping("/daily")
    public Result<HealthTrendVO> getDailyHealthData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long elderlyId) {
        HealthTrendVO vo = adminHealthService.getDailyHealthData(date, elderlyId);
        return Result.success(vo);
    }
}
