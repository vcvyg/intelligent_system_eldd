package org.example.persion.service.ai.tool;

import org.example.persion.vo.MedicalAiAnswerVO;

/**
 * 医护 AI 助手工具扩展接口。
 *
 * <p>每个业务查询能力作为独立 Tool 接入，避免 Agent 核心流程随着业务增长膨胀。</p>
 */
public interface MedicalAiTool {

    /**
     * Tool 唯一标识，用于 trace 和评估。
     */
    String name();

    /**
     * 判断当前问题是否需要该工具。
     */
    boolean supports(MedicalAiToolContext context);

    /**
     * 执行业务查询，返回可解释结果。
     */
    MedicalAiToolResult execute(MedicalAiToolContext context);
}
