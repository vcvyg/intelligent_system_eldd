package org.example.persion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.AdminStatisticsService;
import org.example.persion.vo.SystemStatisticsVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 系统统计控制器
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    /**
     * 获取系统统计数据
     */
    @GetMapping
    public Result<SystemStatisticsVO> getSystemStatistics() {
        SystemStatisticsVO vo = adminStatisticsService.getSystemStatistics();
        return Result.success(vo);
    }

    /**
     * 获取系统概览数据(用于系统设置页面)
     */
    @GetMapping("/overview")
    public Result<SystemStatisticsVO> getSystemOverview() {
        SystemStatisticsVO vo = adminStatisticsService.getSystemStatistics();
        return Result.success(vo);
    }
}
