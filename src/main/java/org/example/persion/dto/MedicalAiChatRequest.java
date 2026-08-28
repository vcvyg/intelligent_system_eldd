package org.example.persion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 医护 AI 助手对话请求。
 */
@Data
public class MedicalAiChatRequest {

    /** 客户端会话 ID；为空时服务端自动创建。 */
    private String sessionId;

    /** 当前显式选择的老人；为空时可从问题或会话上下文解析。 */
    private Long elderlyId;

    @NotBlank(message = "问题不能为空")
    private String message;
}
