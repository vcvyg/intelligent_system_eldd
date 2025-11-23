package org.example.persion.controller.family;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.FamilyHealthService;
import org.example.persion.vo.HealthDataVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 子女端 - 健康监测控制器
 */
@RestController
@RequestMapping("/api/family/health")
@RequiredArgsConstructor
public class FamilyHealthController {

    private final FamilyHealthService familyHealthService;

    /**
     * 获取仪表盘数据
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = familyHealthService.getDashboardData();
        return Result.success(data);
    }

    /**
     * 获取指定老人的最新健康数据
     */
    @GetMapping("/latest/{elderlyId}")
    public Result<HealthDataVO> getLatestHealthData(@PathVariable Long elderlyId) {
        HealthDataVO data = familyHealthService.getLatestHealthData(elderlyId);
        return Result.success(data);
    }

    /**
     * 获取指定老人的健康数据列表
     */
    @GetMapping("/list/{elderlyId}")
    public Result<List<HealthDataVO>> getHealthDataList(
            @PathVariable Long elderlyId,
            @RequestParam(required = false, defaultValue = "7") Integer days) {
        List<HealthDataVO> list = familyHealthService.getHealthDataList(elderlyId, days);
        return Result.success(list);
    }
}
