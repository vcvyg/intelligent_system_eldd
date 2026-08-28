package org.example.persion.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optional OpenAI-compatible semantic planner.
 *
 * <p>Disabled by default. It can only return names from the local read-only Tool
 * allowlist. Any timeout, malformed JSON or policy violation falls back to the
 * deterministic planner in {@link MedicalAiPlanner}.</p>
 */
@Slf4j
@Component
public class OpenAiCompatibleMedicalAiPlanningModel implements MedicalAiPlanningModel {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${medical.ai.planner-enabled:false}")
    private boolean enabled;

    @Value("${medical.ai.base-url:}")
    private String baseUrl;

    @Value("${medical.ai.api-key:}")
    private String apiKey;

    @Value("${medical.ai.model:}")
    private String model;

    @Value("${medical.ai.timeout-ms:5000}")
    private long timeoutMs;

    @Override
    public Optional<MedicalAiPlan> plan(String question) {
        if (!enabled || isBlank(baseUrl) || isBlank(model) || isBlank(question)) {
            return Optional.empty();
        }

        try {
            RestClient.RequestBodySpec request = buildClient().post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON);
            if (!isBlank(apiKey)) {
                request.header("Authorization", "Bearer " + apiKey.trim());
            }

            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0,
                    "max_tokens", 220,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "你是 Java 医护业务系统的只读 Tool Planner。只能从以下工具中选择："
                                            + String.join(", ", MedicalAiToolPolicy.READ_ONLY_TOOLS)
                                            + "。禁止回答医疗问题、禁止生成诊断或建议、禁止输出其他工具。"
                                            + "仅返回严格 JSON：{\"tools\":[\"tool_name\"],\"reason\":\"简短规划原因\"}。"
                                            + "最多选择4个工具；没有业务查询意图时 tools 为空数组。"
                            ),
                            Map.of("role", "user", "content", question)
                    )
            );

            Map<?, ?> response = request.body(body).retrieve().body(Map.class);
            return parseContent(extractContent(response));
        } catch (Exception exception) {
            // The question may contain sensitive context. Never log request text or model output.
            log.warn("Medical AI semantic planner failed; deterministic fallback will be used: {}",
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    Optional<MedicalAiPlan> parseContent(String content) {
        if (isBlank(content)) return Optional.empty();
        try {
            String json = extractJsonObject(content.trim());
            if (json == null) return Optional.empty();
            PlannerPayload payload = objectMapper.readValue(json, PlannerPayload.class);
            if (payload.tools() == null) return Optional.empty();

            List<String> sanitized = MedicalAiToolPolicy.sanitizeModelTools(payload.tools());
            if (!payload.tools().isEmpty() && sanitized.isEmpty()) {
                return Optional.empty();
            }
            String reason = normalizeReason(payload.reason());
            return Optional.of(new MedicalAiPlan(sanitized, reason));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private RestClient buildClient() {
        long boundedTimeout = Math.max(500, Math.min(timeoutMs, 30_000));
        Duration timeout = Duration.ofMillis(boundedTimeout);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .requestFactory(requestFactory)
                .build();
    }

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

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return content.substring(start, end + 1);
    }

    private String normalizeReason(String reason) {
        if (isBlank(reason)) return "模型根据规则未覆盖的自然语言表达补充只读 Tool 计划";
        String normalized = reason.trim().replace('\n', ' ').replace('\r', ' ');
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record PlannerPayload(List<String> tools, String reason) {
    }
}
