package org.example.persion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.ElderlyRelationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 老人关联关系管理Controller
 */
@RestController
@RequestMapping("/api/admin/elderly")
@RequiredArgsConstructor
public class AdminElderlyRelationController {

    private final ElderlyRelationService elderlyRelationService;

    /**
     * 获取老人的子女关联列表
     */
    @GetMapping("/{elderlyId}/family")
    public Result<List<Map<String, Object>>> getFamilyRelations(@PathVariable Long elderlyId) {
        List<Map<String, Object>> list = elderlyRelationService.getFamilyRelations(elderlyId);
        return Result.success(list);
    }

    /**
     * 添加子女关联
     */
    @PostMapping("/{elderlyId}/family")
    public Result<Void> addFamilyRelation(
            @PathVariable Long elderlyId,
            @RequestParam Long familyUserId,
            @RequestParam String relationType,
            @RequestParam(required = false, defaultValue = "0") Integer isPrimaryContact) {
        elderlyRelationService.addFamilyRelation(elderlyId, familyUserId, relationType, isPrimaryContact);
        return Result.success();
    }

    /**
     * 删除子女关联
     */
    @DeleteMapping("/{elderlyId}/family/{relationId}")
    public Result<Void> removeFamilyRelation(@PathVariable Long relationId) {
        elderlyRelationService.removeFamilyRelation(relationId);
        return Result.success();
    }

    /**
     * 获取老人的医护分配列表
     */
    @GetMapping("/{elderlyId}/medical")
    public Result<List<Map<String, Object>>> getMedicalRelations(@PathVariable Long elderlyId) {
        List<Map<String, Object>> list = elderlyRelationService.getMedicalRelations(elderlyId);
        return Result.success(list);
    }

    /**
     * 添加医护分配
     */
    @PostMapping("/{elderlyId}/medical")
    public Result<Void> addMedicalRelation(
            @PathVariable Long elderlyId,
            @RequestParam Long medicalUserId,
            @RequestParam(required = false, defaultValue = "0") Integer isPrimaryDoctor) {
        elderlyRelationService.addMedicalRelation(elderlyId, medicalUserId, isPrimaryDoctor);
        return Result.success();
    }

    /**
     * 删除医护分配
     */
    @DeleteMapping("/{elderlyId}/medical/{relationId}")
    public Result<Void> removeMedicalRelation(@PathVariable Long relationId) {
        elderlyRelationService.removeMedicalRelation(relationId);
        return Result.success();
    }

    /**
     * 获取老人绑定的设备列表
     */
    @GetMapping("/{elderlyId}/devices")
    public Result<List<Map<String, Object>>> getDeviceRelations(@PathVariable Long elderlyId) {
        List<Map<String, Object>> list = elderlyRelationService.getDeviceRelations(elderlyId);
        return Result.success(list);
    }
}
