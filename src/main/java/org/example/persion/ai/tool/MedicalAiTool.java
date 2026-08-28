package org.example.persion.ai.tool;

import java.util.List;

/**
 * 医护 AI Tool 扩展接口。
 *
 * <p>所有业务查询能力以 Tool 形式注册，避免 Agent 核心流程随着业务增长不断增加 if/else。</p>
 */
public interface MedicalAiTool {

    /**
     * Tool 唯一标识，用于 trace、评测和前端展示。
     */
    String name();

    /**
     * 判断当前问题是否可能需要该 Tool。
     */
    boolean supports(String question);

    /**
     * 执行前置说明，实际执行仍需要业务上下文。
     */
    default List<String> capabilities() {
        return List.of(name());
    }
}
