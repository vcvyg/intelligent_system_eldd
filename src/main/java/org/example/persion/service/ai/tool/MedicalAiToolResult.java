package org.example.persion.service.ai.tool;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Tool 执行结果，保留事实来源用于 Agent trace 和评估。
 */
@Getter
@Builder
public class MedicalAiToolResult {

    private final String toolName;

    private final String status;

    private final String summary;

    private final List<String> sources;
}
