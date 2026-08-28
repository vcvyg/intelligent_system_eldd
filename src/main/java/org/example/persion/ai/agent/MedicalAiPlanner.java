package org.example.persion.ai.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 医护 Agent 规划器。
 *
 * <p>当前使用确定性规则生成计划，为未来接入模型 Planner 保留接口。</p>
 */
@Component
public class MedicalAiPlanner {

    public MedicalAiPlan plan(String question) {
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
        if (contains(q, "推荐", "主动关怀", "适合推", "推什么", "关怀内容", "关怀", "recommend")) {
            tools.add("recommendation_preview");
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

    private boolean contains(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
