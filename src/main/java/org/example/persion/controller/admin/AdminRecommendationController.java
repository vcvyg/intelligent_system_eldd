package org.example.persion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.service.RecommendationService;
import org.example.persion.service.RecommendationTriggerService;
import org.example.persion.vo.RecommendationItemVO;
import org.example.persion.vo.RecommendationTriggerVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recommendations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationTriggerService triggerService;

    @GetMapping("/preview/{elderlyId}")
    public Result<List<RecommendationItemVO>> preview(
            @PathVariable Long elderlyId,
            @RequestParam(required = false) Long familyUserId) {
        return Result.success(recommendationService.preview(elderlyId, familyUserId));
    }

    @GetMapping("/triggers")
    public Result<List<RecommendationTriggerVO>> pendingTriggers(
            @RequestParam(required = false) Long elderlyId) {
        return Result.success(triggerService.pending(elderlyId));
    }

    @PostMapping("/triggers/{triggerId}/approve")
    public Result<RecommendationTriggerVO> approveTrigger(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long triggerId,
            @RequestParam(required = false) String reason) {
        return Result.success(triggerService.approve(triggerId, adminUserId, reason), "复核已通过");
    }

    @PostMapping("/triggers/{triggerId}/reject")
    public Result<RecommendationTriggerVO> rejectTrigger(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long triggerId,
            @RequestParam(required = false) String reason) {
        return Result.success(triggerService.reject(triggerId, adminUserId, reason), "复核已拒绝");
    }

    @PostMapping("/deliver/{elderlyId}")
    public Result<Integer> deliver(@PathVariable Long elderlyId) {
        if (triggerService.hasPending(elderlyId) && !triggerService.hasApproved(elderlyId)) {
            throw new BusinessException("当前存在待复核事件，请先通过至少一条关怀触发事件再投放");
        }

        int count = recommendationService.deliver(elderlyId);
        if (count > 0) {
            triggerService.markDelivered(elderlyId);
        }
        return Result.success(count, "推荐投放完成");
    }
}
