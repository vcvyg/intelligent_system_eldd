package org.example.persion.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 可选的 OpenAI-compatible 文本润色层。
 *
 * <p>工具事实永远由本地系统生成。模型只能重写已经得到的事实，调用失败会静默回退，
 * 默认关闭，避免医疗数据在未明确配置时发送到外部服务。</p>
 */
@Slf4j
@Component
public class MedicalAiModelEnhancer {

    @Value("${medical.ai.enabled:false}")
    private boolean enabled;

    @Value("${medical.ai.base-url:}")
    private String baseUrl;

    @Value("${medical.ai.api-key:}")
    private String apiKey;

    @Value("${medical.ai.model:}")
    private String model;

    public Optional<String> enhance(String question, String factAnswer, List<String> sources) {
        if (!enabled || isBlank(baseUrl) || isBlank(model) || isBlank(factAnswer)) {
            return Optional.empty();
        }

        try {
            RestClient client = RestClient.builder().baseUrl(trimTrailingSlash(baseUrl)).build();
            RestClient.RequestBodySpec request = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON);
            if (!isBlank(apiKey)) {
                request.header("Authorization", "Bearer " + apiKey.trim());
            }

            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.1,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "你是医护工作台中的业务查询助手。只能重写用户提供的系统事实，禁止新增事实、诊断、处方、用药调整或治疗建议。保持简洁，保留不确定性和数据时间范围。"
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", "问题：" + question + "\n\n系统事实：\n" + factAnswer + "\n\n数据来源：" + String.join("；", sources)
                            )
                    )
            );

            Map<?, ?> response = request.body(body).retrieve().body(Map.class);
            String content = extractContent(response);
            return isBlank(content) ? Optional.empty() : Optional.of(content.trim());
        } catch (Exception exception) {
            // 不记录问题或系统事实，避免日志二次落敏感数据。
            log.warn("Medical AI model enhancement failed; falling back to deterministic answer: {}",
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> response) {
        if (response == null) return null;
        Object choicesObject = response.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) return null;
        Object firstObject = choices.get(0);
        if (!(firstObject instanceof Map<?, ?> first)) return null;
        Object messageObject = first.get("message");
        if (!(messageObject instanceof Map<?, ?> message)) return null;
        Object content = message.get("content");
        return content instanceof String ? (String) content : null;
    }

    private String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
