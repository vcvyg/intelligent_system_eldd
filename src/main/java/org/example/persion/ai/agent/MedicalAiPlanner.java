package org.example.persion.ai.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 医护 Agent 规划器。
 *
 * <p>当前使用确定性规则生成计划，为未来接入模型 Planner 保留接口。</p>
 */
@Component
public class MedicalAiPlanner {

    public MedicalAiPlan plan(String question) {
        String q = question == null ? "" : question.toLowerCase();
        List<String> tools = new ArrayList<>();

        if (contains(q, "房间", "房号", "住哪", "room")) {
            tools.add("room_lookup");
        }
        if (contains(q, "健康", "心率", "血压", "指标", "health")) {
            tools.add("health_recent");
        }
        if (contains(q, "告警", "异常", "预警", "alert")) {
            tools.add("alerts_recent");
        }
        if (contains(q, "护理", "照护", "服务", "安排", "care")) {
            tools.add("care_schedule");
        }
        if (contains(q, "推荐", "关怀", "适合推", "recommend")) {
            tools.add("recommendation_preview");
        }

        String reason = tools.isEmpty()
                ? "未识别明确业务查询，返回能力引导"
                : "根据问题语义选择只读业务工具";

        return new MedicalAiPlan(tools, reason);
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
