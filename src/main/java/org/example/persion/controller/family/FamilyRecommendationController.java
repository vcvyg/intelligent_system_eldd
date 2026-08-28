package org.example.persion.controller.family;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.RecommendationFeedbackDTO;
import org.example.persion.service.RecommendationService;
import org.example.persion.vo.RecommendationItemVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/family/recommendations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FAMILY')")
public class FamilyRecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{elderlyId}")
    public Result<List<RecommendationItemVO>> feed(
            @AuthenticationPrincipal Long familyUserId,
            @PathVariable Long elderlyId) {
        return Result.success(recommendationService.familyFeed(familyUserId, elderlyId));
    }

    @PostMapping("/feedback")
    public Result<Void> feedback(
            @AuthenticationPrincipal Long familyUserId,
            @Valid @RequestBody RecommendationFeedbackDTO dto) {
        recommendationService.feedback(familyUserId, dto);
        return Result.success();
    }
}
