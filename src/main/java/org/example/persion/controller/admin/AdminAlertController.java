package org.example.persion.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.AlertCreateDTO;
import org.example.persion.dto.AlertHandleDTO;
import org.example.persion.service.AlertService;
import org.example.persion.vo.AlertRecordVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 预警管理控制器
 */
@RestController
@RequestMapping("/api/admin/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAlertController {

    private final AlertService alertService;

    /**
     * 分页查询预警列表
     */
    @GetMapping
    public Result<Page<AlertRecordVO>> getAlertList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String alertLevel,
            @RequestParam(required = false) String status
    ) {
        Page<AlertRecordVO> page = alertService.getAlertList(current, size, alertType, alertLevel, status);
        return Result.success(page);
    }

    /**
     * 根据ID查询预警详情
     */
    @GetMapping("/{id}")
    public Result<AlertRecordVO> getAlertById(@PathVariable Long id) {
        AlertRecordVO alert = alertService.getAlertById(id);
        return Result.success(alert);
    }

    /**
     * 根据老人ID查询预警列表
     */
    @GetMapping("/elderly/{elderlyId}")
    public Result<List<AlertRecordVO>> getAlertsByElderlyId(@PathVariable Long elderlyId) {
        List<AlertRecordVO> alerts = alertService.getAlertsByElderlyId(elderlyId);
        return Result.success(alerts);
    }

    /**
     * 创建预警记录
     */
    @PostMapping
    public Result<Long> createAlert(@Valid @RequestBody AlertCreateDTO dto) {
        Long alertId = alertService.createAlert(dto);
        return Result.success(alertId, "预警记录创建成功");
    }

    /**
     * 分配医护人员
     */
    @PutMapping("/{alertId}/assign/{medicalId}")
    public Result<Void> assignMedical(
            @PathVariable Long alertId,
            @PathVariable Long medicalId
    ) {
        alertService.assignMedical(alertId, medicalId);
        return Result.success(null, "医护人员分配成功");
    }

    /**
     * 处理预警
     */
    @PutMapping("/handle")
    public Result<Void> handleAlert(@Valid @RequestBody AlertHandleDTO dto) {
        alertService.handleAlert(dto);
        return Result.success(null, "预警处理成功");
    }

    /**
     * 开始处理告警（将待处理改为处理中）
     */
    @PutMapping("/{id}/process")
    public Result<Void> processAlert(@PathVariable Long id) {
        alertService.processAlert(id);
        return Result.success(null, "告警状态已更新为处理中");
    }

    /**
     * 忽略预警
     */
    @PutMapping("/{alertId}/ignore")
    public Result<Void> ignoreAlert(@PathVariable Long alertId) {
        alertService.ignoreAlert(alertId);
        return Result.success(null, "预警已忽略");
    }

    /**
     * 删除预警记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return Result.success(null, "预警删除成功");
    }

    /**
     * 获取预警统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getAlertStatistics() {
        Map<String, Object> statistics = alertService.getAlertStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取今日预警列表
     */
    @GetMapping("/today")
    public Result<List<AlertRecordVO>> getTodayAlerts() {
        List<AlertRecordVO> alerts = alertService.getTodayAlerts();
        return Result.success(alerts);
    }
}
