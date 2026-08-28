package org.example.persion.ai.agent;

import java.util.List;

/**
 * Agent 在执行 Tool 前生成的计划。
 *
 * <p>用于区分问题理解和业务执行，后续可替换为模型 Planner。</p>
 */
public record MedicalAiPlan(
        List<String> toolNames,
        String reason
) {
    public MedicalAiPlan {
        toolNames = toolNames == null ? List.of() : List.copyOf(toolNames);
    }
}
