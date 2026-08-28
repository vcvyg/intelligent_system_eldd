package org.example.persion.ai.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalAiPlannerTest {

    private final MedicalAiPlanner planner = new MedicalAiPlanner();

    @Test
    void plansMultipleReadOnlyToolsForCompoundQuestion() {
        MedicalAiPlan plan = planner.plan("王阿姨最近健康怎么样，有没有告警，现在适合推荐什么关怀？");

        assertTrue(plan.tools().contains("health_recent"));
        assertTrue(plan.tools().contains("alerts_recent"));
        assertTrue(plan.tools().contains("recommendation_preview"));
        assertEquals("根据问题语义选择只读业务工具", plan.reason());
    }

    @Test
    void returnsGuidanceWhenNoBusinessIntentFound() {
        MedicalAiPlan plan = planner.plan("你好");

        assertTrue(plan.tools().isEmpty());
        assertEquals("未识别明确业务查询，返回能力引导", plan.reason());
    }
}
