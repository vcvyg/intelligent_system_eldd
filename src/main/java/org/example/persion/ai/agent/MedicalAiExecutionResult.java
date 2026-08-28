package org.example.persion.ai.agent;

import java.util.List;

/**
 * Planner 计划执行后的聚合结果。
 */
public record MedicalAiExecutionResult(
        String answer,
        List<MedicalAiToolExecution> executions,
        List<String> sources,
        boolean partial
) {
    public MedicalAiExecutionResult {
        executions = executions == null ? List.of() : List.copyOf(executions);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
