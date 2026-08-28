package org.example.persion.ai.tool;

import java.util.List;

/**
 * Tool 执行结果。Agent 只负责把结构化结果合并成最终回答和 Trace。
 */
public record MedicalAiToolResult(
        String sectionTitle,
        String body,
        String status,
        String summary,
        List<String> sources
) {
    public MedicalAiToolResult {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
