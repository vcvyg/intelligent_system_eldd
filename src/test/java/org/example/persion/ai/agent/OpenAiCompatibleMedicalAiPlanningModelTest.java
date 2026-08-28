package org.example.persion.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleMedicalAiPlanningModelTest {

    private final OpenAiCompatibleMedicalAiPlanningModel model = new OpenAiCompatibleMedicalAiPlanningModel();

    @Test
    void parsesStrictJsonAndKeepsReadOnlyToolOrder() {
        MedicalAiPlan plan = model.parseContent(
                "{\"tools\":[\"health_recent\",\"alerts_recent\"],\"reason\":\"关注近期状态\"}"
        ).orElseThrow();

        assertEquals(List.of("health_recent", "alerts_recent"), plan.toolNames());
        assertEquals("关注近期状态", plan.reason());
    }

    @Test
    void toleratesMarkdownFenceButStillAppliesAllowlist() {
        MedicalAiPlan plan = model.parseContent(
                "```json\n{\"tools\":[\"health_recent\",\"delete_patient\",\"health_recent\"],\"reason\":\"mixed\"}\n```"
        ).orElseThrow();

        assertEquals(List.of("health_recent"), plan.toolNames());
    }

    @Test
    void rejectsResponseContainingOnlyUnknownWriteTools() {
        assertTrue(model.parseContent(
                "{\"tools\":[\"delete_patient\",\"update_alert\"],\"reason\":\"write\"}"
        ).isEmpty());
    }

    @Test
    void rejectsMalformedPayload() {
        assertTrue(model.parseContent("not-json").isEmpty());
    }
}
