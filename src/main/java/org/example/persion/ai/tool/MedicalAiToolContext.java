package org.example.persion.ai.tool;

/**
 * 只读业务 Tool 的最小执行上下文。
 *
 * <p>权限校验由 Agent 核心在调用 Tool 前完成，Tool 只接收当前已授权老人和原始问题。</p>
 */
public record MedicalAiToolContext(
        Long elderlyId,
        String elderlyName,
        String question
) {
}
