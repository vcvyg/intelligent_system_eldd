package org.example.persion.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalAiPlannerTest {

    private final MedicalAiPlanner planner = new MedicalAiPlanner();

    @Test
    void plansMultipleReadOnlyToolsForCompoundQuestion() {
        MedicalAiPlan plan = planner.plan("王阿姨最近健康怎么样，有没有告警，现在适合推荐什么关怀？");

        assertEquals(
                List.of("health_recent", "alerts_recent", "recommendation_preview"),
                plan.toolNames()
        );
        assertTrue(plan.reason().contains("health_recent -> alerts_recent -> recommendation_preview"));
    }

    @Test
    void preservesProfileAndCareRoutingPreviouslyOwnedByService() {
        MedicalAiPlan plan = planner.plan("王阿姨的病史和过敏情况，以及近期护理安排？");

        assertEquals(List.of("patient_profile", "care_schedule"), plan.toolNames());
    }

    @Test
    void expandsSummaryQuestionIntoHealthAlertAndCareTools() {
        MedicalAiPlan plan = planner.plan("把王阿姨近期情况综合看一下");

        assertEquals(List.of("health_recent", "alerts_recent", "care_schedule"), plan.toolNames());
    }

    @Test
    void returnsGuidanceWhenNoBusinessIntentFound() {
        MedicalAiPlan plan = planner.plan("你好");

        assertTrue(plan.toolNames().isEmpty());
        assertEquals("未识别明确业务查询，返回能力引导", plan.reason());
    }
}
