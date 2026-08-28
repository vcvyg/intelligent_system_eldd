package org.example.persion.controller.medical;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.MedicalAiChatRequest;
import org.example.persion.service.MedicalAiAssistantService;
import org.example.persion.service.impl.MedicalAiModelEnhancer;
import org.example.persion.vo.MedicalAiAnswerVO;
import org.example.persion.vo.MedicalAiPatientVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/medical/ai-assistant")
@PreAuthorize("hasRole('MEDICAL')")
@RequiredArgsConstructor
public class MedicalAiAssistantController {

    private final MedicalAiAssistantService medicalAiAssistantService;
    private final MedicalAiModelEnhancer medicalAiModelEnhancer;

    @GetMapping("/patients")
    public Result<List<MedicalAiPatientVO>> patients(@AuthenticationPrincipal Long medicalUserId) {
        return Result.success(medicalAiAssistantService.listAssignedPatients(medicalUserId));
    }

    @PostMapping("/chat")
    public Result<MedicalAiAnswerVO> chat(@AuthenticationPrincipal Long medicalUserId,
                                          @Valid @RequestBody MedicalAiChatRequest request) {
        long startedNanos = System.nanoTime();
        MedicalAiAnswerVO answer = medicalAiAssistantService.chat(medicalUserId, request);
        answer.setTraceId(UUID.randomUUID().toString());

        // The deterministic router chooses tools before execution. Existing ToolTrace
        // entries preserve that execution order, so expose the distinct non-LLM tools
        // as the plan for observability/demo purposes.
        answer.setPlan(new LinkedHashSet<>(answer.getTools().stream()
                .map(MedicalAiAnswerVO.ToolTrace::getTool)
                .filter(tool -> tool != null && !tool.isBlank() && !"llm_polish".equals(tool))
                .toList()).stream().toList());

        boolean safeToPolish = answer.getElderlyId() != null
                && answer.getSources() != null
                && !answer.getSources().isEmpty()
                && answer.getTools().stream().noneMatch(tool -> "blocked".equals(tool.getStatus()));
        if (safeToPolish) {
            medicalAiModelEnhancer.enhance(request.getMessage(), answer.getAnswer(), answer.getSources())
                    .ifPresent(enhanced -> {
                        answer.setAnswer(enhanced);
                        answer.setModelEnhanced(true);
                        answer.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                                "llm_polish", "ok", "仅基于已查询系统事实进行语言组织"
                        ));
                    });
        }

        answer.setElapsedMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
        return Result.success(answer);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> reset(@AuthenticationPrincipal Long medicalUserId,
                              @PathVariable String sessionId) {
        medicalAiAssistantService.resetSession(medicalUserId, sessionId);
        return Result.success();
    }
}
