package org.example.persion.service.ai.tool;

import lombok.Builder;
import lombok.Getter;

/**
 * Agent 执行 Tool 时共享上下文。
 */
@Getter
@Builder
public class MedicalAiToolContext {

    private final Long medicalUserId;

    private final Long elderlyId;

    private final String elderlyName;

    private final String question;

    private final String sessionId;
}
