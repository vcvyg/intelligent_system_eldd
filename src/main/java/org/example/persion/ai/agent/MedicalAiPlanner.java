package org.example.persion.ai.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 医护 Agent 混合规划器。
 *
 * <p>确定性规则始终作为稳定基线；可选模型 Planner 只允许补充白名单内的只读 Tool。
 * 模型关闭、超时、返回非法 Tool 或 JSON 解析失败时直接回退规则规划。</p>
 */
@Component
public class MedicalAiPlanner {

    private final MedicalAiPlanningModel planningModel;

    /**
     * 供纯规则单测和离线评测使用，不触发任何模型调用。
     */
    public MedicalAiPlanner() {
        this.planningModel = null;
    }

    @Autowired
    public MedicalAiPlanner(MedicalAiPlanningModel planningModel) {
        this.planningModel = planningModel;
    }

    public MedicalAiPlan plan(String question) {
        MedicalAiPlan rulePlan = deterministicPlan(question);
        if (planningModel == null) return rulePlan;

        Optional<MedicalAiPlan> modelCandidate = planningModel.plan(question);
        if (modelCandidate.isEmpty()) return rulePlan;

        MedicalAiPlan modelPlan = modelCandidate.get();
        List<String> modelTools = MedicalAiToolPolicy.sanitizeModelTools(modelPlan.toolNames());
        if (modelPlan.toolNames() != null && !modelPlan.toolNames().isEmpty() && modelTools.isEmpty()) {
            return rulePlan;
        }

        if (rulePlan.toolNames().isEmpty()) {
            if (modelTools.isEmpty()) return rulePlan;
            return new MedicalAiPlan(
                    modelTools,
                    "模型 Planner 补充规则未覆盖语义：" + safeReason(modelPlan.reason())
            );
        }

        LinkedHashSet<String> merged = new LinkedHashSet<>(rulePlan.toolNames());
        merged.addAll(modelTools);
        List<String> mergedTools = new ArrayList<>(merged);
        if (mergedTools.equals(rulePlan.toolNames())) {
            return new MedicalAiPlan(
                    rulePlan.toolNames(),
                    rulePlan.reason() + "；模型规划与规则基线一致"
            );
        }

        return new MedicalAiPlan(
                mergedTools,
                rulePlan.reason() + "；模型补充只读 Tool：" + String.join(" -> ", modelTools)
        );
    }

    MedicalAiPlan deterministicPlan(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        Set<String> tools = new LinkedHashSet<>();

        if (contains(q, "房间", "房号", "几号房", "住哪", "住在哪里", "room")) {
            tools.add("room_lookup");
        }
        if (contains(q, "档案", "年龄", "性别", "病史", "既往史", "基础病", "过敏", "病情", "profile")) {
            tools.add("patient_profile");
        }
        if (contains(q, "健康", "心率", "血压", "血糖", "体温", "睡眠", "步数", "指标", "身体", "health")) {
            tools.add("health_recent");
        }
        if (contains(q, "告警", "预警", "报警", "异常提醒", "异常", "alarm", "alert")) {
            tools.add("alerts_recent");
        }
        if (contains(q, "护理计划", "照护计划", "护理安排", "照护安排", "近期安排", "服务安排", "护理", "照护", "服务", "巡查", "巡诊", "care", "plan")) {
            tools.add("care_schedule");
        }

        boolean asksRecommendationPerformance = contains(q,
                "推荐效果", "投放效果", "推荐表现", "投放表现", "点击率", "反馈率", "不感兴趣",
                "用户反馈", "家属反馈", "推荐策略", "投放策略", "策略优化", "哪类内容", "performance");
        boolean asksRecommendationPreview = contains(q,
                "适合推", "推什么", "推荐什么", "关怀内容", "主动关怀", "recommend")
                || (!asksRecommendationPerformance && contains(q, "推荐"));

        if (asksRecommendationPreview) {
            tools.add("recommendation_preview");
        }
        if (asksRecommendationPerformance) {
            tools.add("recommendation_performance");
        }

        if (contains(q, "最近怎么样", "近期情况", "整体情况", "概况", "综合看一下")) {
            tools.add("health_recent");
            tools.add("alerts_recent");
            tools.add("care_schedule");
        }

        List<String> orderedTools = new ArrayList<>(tools);
        String reason = orderedTools.isEmpty()
                ? "未识别明确业务查询，返回能力引导"
                : "根据问题语义生成只读业务工具执行计划：" + String.join(" -> ", orderedTools);

        return new MedicalAiPlan(orderedTools, reason);
    }

    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) return "识别到可映射的只读业务查询";
        String normalized = reason.trim().replace('\n', ' ').replace('\r', ' ');
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private boolean contains(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
