package org.example.persion.ai.agent;

import lombok.RequiredArgsConstructor;
import org.example.persion.ai.tool.MedicalAiTool;
import org.example.persion.ai.tool.MedicalAiToolContext;
import org.example.persion.ai.tool.MedicalAiToolRegistry;
import org.example.persion.ai.tool.MedicalAiToolResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 医护 Agent Tool 执行器。
 *
 * <p>Planner 只负责决定要执行哪些只读 Tool；Executor 负责按顺序执行、隔离单 Tool 故障、
 * 记录耗时并聚合事实。单个 Tool 失败不会让整轮 Agent 崩溃，也不会把异常信息暴露给用户。</p>
 */
@Component
@RequiredArgsConstructor
public class MedicalAiExecutor {

    private final MedicalAiToolRegistry toolRegistry;

    public MedicalAiExecutionResult execute(MedicalAiPlan plan, MedicalAiToolContext context) {
        if (plan == null || plan.toolNames().isEmpty()) {
            return new MedicalAiExecutionResult("", List.of(), List.of(), false);
        }

        StringBuilder answer = new StringBuilder();
        List<MedicalAiToolExecution> executions = new ArrayList<>();
        Set<String> sources = new LinkedHashSet<>();
        boolean partial = false;

        for (String toolName : plan.toolNames()) {
            long startedAt = System.nanoTime();
            try {
                MedicalAiTool tool = toolRegistry.require(toolName);
                MedicalAiToolResult result = tool.execute(context);
                appendSection(answer, result.sectionTitle(), result.body());
                sources.addAll(result.sources());
                executions.add(new MedicalAiToolExecution(
                        tool.name(),
                        result.status(),
                        result.summary(),
                        elapsedMs(startedAt)
                ));
            } catch (RuntimeException exception) {
                partial = true;
                executions.add(new MedicalAiToolExecution(
                        toolName,
                        "failed",
                        "业务工具暂时不可用，已跳过并继续执行其余计划",
                        elapsedMs(startedAt)
                ));
            }
        }

        if (partial) {
            if (answer.isEmpty()) {
                answer.append("本轮计划中的业务数据源暂时不可用。系统没有生成未经验证的医疗或业务事实，请稍后重试。");
            } else {
                appendSection(answer, "执行说明", "部分数据源暂时不可用，以上仅包含本轮成功查询到的系统事实。");
            }
        }

        return new MedicalAiExecutionResult(
                answer.toString().trim(),
                executions,
                new ArrayList<>(sources),
                partial
        );
    }

    private void appendSection(StringBuilder answer, String title, String body) {
        if (!answer.isEmpty()) answer.append("\n\n");
        answer.append("【").append(title).append("】").append(body);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
