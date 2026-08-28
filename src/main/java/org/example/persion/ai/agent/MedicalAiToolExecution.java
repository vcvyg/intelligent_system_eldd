package org.example.persion.ai.agent;

/**
 * 单个业务 Tool 的执行结果。
 *
 * <p>不暴露底层异常详情，避免把数据库、内部实现或敏感信息泄露到 Agent 响应。</p>
 */
public record MedicalAiToolExecution(
        String toolName,
        String status,
        String summary,
        long elapsedMs
) {
}
