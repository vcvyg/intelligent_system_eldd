package org.example.persion.controller.medical;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.AlertHandleDTO;
import org.example.persion.service.AlertService;
import org.example.persion.vo.AlertRecordVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 医护端-预警管理控制器
 */
@RestController
@RequestMapping("/api/medical/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEDICAL')")
public class MedicalAlertController {

    private final AlertService alertService;

    /**
     * 分页查询预警列表
     * (医护人员能看到所有未处理或自己相关的告警)
     */
    @GetMapping
    public Result<Page<AlertRecordVO>> getAlertList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String alertLevel,
            @RequestParam(required = false) String status
    ) {
        // 复用现有的服务方法，后续可根据业务调整为只看和自己相关的
        Page<AlertRecordVO> page = alertService.getAlertList(current, size, alertType, alertLevel, status);
        return Result.success(page);
    }

    /**
     * 开始处理告警
     */
    @PutMapping("/{id}/process")
    public Result<Void> processAlert(@PathVariable Long id) {
        alertService.processAlert(id);
        return Result.success(null, "告警状态已更新为处理中");
    }

    /**
     * 完成处理告警
     */
    @PutMapping("/handle")
    public Result<Void> handleAlert(@Valid @RequestBody AlertHandleDTO dto) {
        alertService.handleAlert(dto);
        return Result.success(null, "告警处理成功");
    }
}
