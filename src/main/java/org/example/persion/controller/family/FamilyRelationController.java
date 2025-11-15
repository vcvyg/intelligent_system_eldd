package org.example.persion.controller.family;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.FamilyRelationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 子女端 - 关联管理Controller
 */
@RestController
@RequestMapping("/api/family/relation")
@RequiredArgsConstructor
public class FamilyRelationController {

    private final FamilyRelationService familyRelationService;

    /**
     * 获取当前子女用户关联的老人列表
     */
    @GetMapping("/my-elderly")
    public Result<List<Map<String, Object>>> getMyElderlyList() {
        List<Map<String, Object>> list = familyRelationService.getElderlyListByFamilyUser();
        return Result.success(list);
    }

    /**
     * 子女主动绑定老人
     */
    @PostMapping("/bind-elderly")
    public Result<Void> bindElderly(
            @RequestParam Long elderlyId,
            @RequestParam String relationType,
            @RequestParam(required = false, defaultValue = "0") Integer isPrimaryContact) {
        familyRelationService.bindElderly(elderlyId, relationType, isPrimaryContact);
        return Result.success();
    }

    /**
     * 子女主动解绑老人
     */
    @DeleteMapping("/unbind-elderly/{elderlyId}")
    public Result<Void> unbindElderly(@PathVariable Long elderlyId) {
        familyRelationService.unbindElderly(elderlyId);
        return Result.success();
    }

    /**
     * 搜索可绑定的老人(根据姓名或身份证号)
     */
    @GetMapping("/search-elderly")
    public Result<List<Map<String, Object>>> searchElderly(@RequestParam String keyword) {
        List<Map<String, Object>> list = familyRelationService.searchElderlyForBinding(keyword);
        return Result.success(list);
    }
}
