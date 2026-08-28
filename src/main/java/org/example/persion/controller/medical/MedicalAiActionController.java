package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.MedicalAiActionService;
import org.example.persion.vo.MedicalAiActionProposalVO;
import org.example.persion.vo.MedicalAiActionResultVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medical/ai-assistant/actions")
@PreAuthorize("hasRole('MEDICAL')")
@RequiredArgsConstructor
public class MedicalAiActionController {

    private final MedicalAiActionService actionService;

    @PostMapping("/alerts/{alertId}/proposals")
    public Result<MedicalAiActionProposalVO> proposeStartAlert(
            @AuthenticationPrincipal Long medicalUserId,
            @PathVariable Long alertId) {
        return Result.success(actionService.proposeStartAlert(medicalUserId, alertId));
    }

    @PostMapping("/proposals/{proposalId}/confirm")
    public Result<MedicalAiActionResultVO> confirm(
            @AuthenticationPrincipal Long medicalUserId,
            @PathVariable String proposalId) {
        return Result.success(actionService.confirm(medicalUserId, proposalId));
    }

    @DeleteMapping("/proposals/{proposalId}")
    public Result<Void> cancel(
            @AuthenticationPrincipal Long medicalUserId,
            @PathVariable String proposalId) {
        actionService.cancel(medicalUserId, proposalId);
        return Result.success();
    }
}
