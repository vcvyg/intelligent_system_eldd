package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.MedicalRelationService;
import org.example.persion.service.MedicalScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 医护端 - 关联管理Controller
 */
@RestController
@RequestMapping("/api/medical/relation")
@RequiredArgsConstructor
public class MedicalRelationController {

    private final MedicalRelationService medicalRelationService;
    private final MedicalScheduleService scheduleService;

    /**
     * 获取当前医护人员负责的老人列表
     */
    @GetMapping("/my-elderly")
    public Result<List<Map<String, Object>>> getMyElderlyList() {
        List<Map<String, Object>> list = medicalRelationService.getElderlyListByMedicalUser();
        return Result.success(list);
    }

    /**
     * 医护人员主动绑定老人
     */
    @PostMapping("/bind-elderly")
    public Result<Void> bindElderly(
            @RequestParam Long elderlyId,
            @RequestParam(required = false, defaultValue = "0") Integer isPrimaryDoctor) {
        medicalRelationService.bindElderly(elderlyId, isPrimaryDoctor);
        return Result.success();
    }

    /**
     * 医护人员主动解绑老人
     */
    @DeleteMapping("/unbind-elderly/{elderlyId}")
    public Result<Void> unbindElderly(@PathVariable Long elderlyId) {
        medicalRelationService.unbindElderly(elderlyId);
        return Result.success();
    }

    /**
     * 搜索可绑定的老人(根据姓名或身份证号)
     */
    @GetMapping("/search-elderly")
    public Result<List<Map<String, Object>>> searchElderly(@RequestParam String keyword) {
        List<Map<String, Object>> list = medicalRelationService.searchElderlyForBinding(keyword);
        return Result.success(list);
    }

    /**
     * 获取当前医护人员的排班列表
     */
    @GetMapping("/my-schedule")
    public Result<List<Map<String, Object>>> getMySchedule() {
        // 从JWT token中获取当前用户ID的逻辑后续补充
        // 这里先返回空列表,实际使用时需要从SecurityContext获取当前用户
        return Result.success(List.of());
    }
}
