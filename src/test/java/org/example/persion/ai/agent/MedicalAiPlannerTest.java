package org.example.persion.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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

    @Test
    void modelPlannerCanCoverNaturalLanguageMissedByRuleBaseline() {
        MedicalAiPlanningModel model = question -> Optional.of(
                new MedicalAiPlan(List.of("care_schedule"), "识别为后续照护事项查询")
        );
        MedicalAiPlanner hybrid = new MedicalAiPlanner(model);

        MedicalAiPlan plan = hybrid.plan("她接下来有什么需要安排的事情？");

        assertEquals(List.of("care_schedule"), plan.toolNames());
        assertTrue(plan.reason().contains("模型 Planner 补充规则未覆盖语义"));
    }

    @Test
    void modelPlannerOnlySupplementsRuleBaselineInsteadOfReplacingIt() {
        MedicalAiPlanningModel model = question -> Optional.of(
                new MedicalAiPlan(List.of("alerts_recent"), "同时关注异常情况")
        );
        MedicalAiPlanner hybrid = new MedicalAiPlanner(model);

        MedicalAiPlan plan = hybrid.plan("她最近心率怎么样？");

        assertEquals(List.of("health_recent", "alerts_recent"), plan.toolNames());
        assertTrue(plan.reason().contains("模型补充只读 Tool"));
    }

    @Test
    void illegalModelToolIsRejectedAndRuleFallbackRemainsAuthoritative() {
        MedicalAiPlanningModel model = question -> Optional.of(
                new MedicalAiPlan(List.of("delete_patient"), "尝试写操作")
        );
        MedicalAiPlanner hybrid = new MedicalAiPlanner(model);

        MedicalAiPlan plan = hybrid.plan("她最近心率怎么样？");

        assertEquals(List.of("health_recent"), plan.toolNames());
        assertTrue(plan.reason().startsWith("根据问题语义生成只读业务工具执行计划"));
    }
}
