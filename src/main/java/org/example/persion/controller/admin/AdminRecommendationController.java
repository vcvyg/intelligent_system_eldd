package org.example.persion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.RecommendationService;
import org.example.persion.vo.RecommendationItemVO;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/preview/{elderlyId}")
    public Result<List<RecommendationItemVO>> preview(
            @PathVariable Long elderlyId,
            @RequestParam(required = false) Long familyUserId) {
        return Result.success(recommendationService.preview(elderlyId, familyUserId));
    }

    @PostMapping("/deliver/{elderlyId}")
    public Result<Integer> deliver(@PathVariable Long elderlyId) {
        int count = recommendationService.deliver(elderlyId);
        return Result.success(count, "推荐投放完成");
    }
}
