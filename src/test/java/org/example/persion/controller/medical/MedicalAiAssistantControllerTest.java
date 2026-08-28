package org.example.persion.controller.medical;

import org.example.persion.dto.MedicalAiChatRequest;
import org.example.persion.service.MedicalAiAssistantService;
import org.example.persion.service.impl.MedicalAiModelEnhancer;
import org.example.persion.vo.MedicalAiAnswerVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicalAiAssistantControllerTest {

    private static final Long MEDICAL_USER_ID = 7L;

    @Test
    void blockedMedicalDecisionNeverCallsExternalModel() {
        MedicalAiAssistantService service = mock(MedicalAiAssistantService.class);
        MedicalAiModelEnhancer enhancer = mock(MedicalAiModelEnhancer.class);
        MedicalAiAssistantController controller = new MedicalAiAssistantController(service, enhancer);

        MedicalAiChatRequest request = request("她这个情况要不要换药？");
        MedicalAiAnswerVO answer = new MedicalAiAnswerVO();
        answer.setElderlyId(11L);
        answer.setAnswer("不提供用药调整建议");
        answer.setSources(List.of("老人档案"));
        answer.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                "medical_safety_guard", "blocked", "拦截用药调整请求"
        ));
        when(service.chat(MEDICAL_USER_ID, request)).thenReturn(answer);

        controller.chat(MEDICAL_USER_ID, request);

        verify(enhancer, never()).enhance(request.getMessage(), answer.getAnswer(), answer.getSources());
        assertFalse(answer.isModelEnhanced());
        assertEquals("不提供用药调整建议", answer.getAnswer());
        assertEquals(List.of("medical_safety_guard"), answer.getPlan());
        assertNotNull(answer.getTraceId());
        assertFalse(answer.getTraceId().isBlank());
        assertTrue(answer.getElapsedMs() >= 0);
    }

    @Test
    void safeToolFactsCanBePolishedWithoutRemovingTrace() {
        MedicalAiAssistantService service = mock(MedicalAiAssistantService.class);
        MedicalAiModelEnhancer enhancer = mock(MedicalAiModelEnhancer.class);
        MedicalAiAssistantController controller = new MedicalAiAssistantController(service, enhancer);

        MedicalAiChatRequest request = request("王阿姨最近心率怎么样？");
        MedicalAiAnswerVO answer = new MedicalAiAnswerVO();
        answer.setElderlyId(11L);
        answer.setAnswer("近7天心率均值约 76 bpm。");
        answer.setSources(List.of("health_data / 近7天健康记录"));
        answer.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                "patient_access", "ok", "已校验负责关系"
        ));
        answer.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                "health_recent", "ok", "读取近7天健康测量"
        ));
        when(service.chat(MEDICAL_USER_ID, request)).thenReturn(answer);
        when(enhancer.enhance(request.getMessage(), answer.getAnswer(), answer.getSources()))
                .thenReturn(Optional.of("系统记录显示，近7天心率均值约 76 bpm。"));

        controller.chat(MEDICAL_USER_ID, request);

        assertTrue(answer.isModelEnhanced());
        assertEquals("系统记录显示，近7天心率均值约 76 bpm。", answer.getAnswer());
        assertEquals(List.of("patient_access", "health_recent"), answer.getPlan());
        assertTrue(answer.getTools().stream().anyMatch(tool -> "health_recent".equals(tool.getTool())));
        assertTrue(answer.getTools().stream().anyMatch(tool -> "llm_polish".equals(tool.getTool())));
        assertFalse(answer.getPlan().contains("llm_polish"));
        assertNotNull(answer.getTraceId());
        assertTrue(answer.getElapsedMs() >= 0);
    }

    private MedicalAiChatRequest request(String message) {
        MedicalAiChatRequest request = new MedicalAiChatRequest();
        request.setMessage(message);
        return request;
    }
}
