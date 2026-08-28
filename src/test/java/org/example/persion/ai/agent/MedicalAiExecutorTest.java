package org.example.persion.ai.agent;

import org.example.persion.ai.tool.MedicalAiTool;
import org.example.persion.ai.tool.MedicalAiToolContext;
import org.example.persion.ai.tool.MedicalAiToolRegistry;
import org.example.persion.ai.tool.MedicalAiToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalAiExecutorTest {

    @Test
    void continuesOtherToolsWhenOneExecutionFails() {
        MedicalAiTool health = fixedTool("health_recent", "近期健康", "心率记录正常。", "health_data");
        MedicalAiTool alerts = failingTool("alerts_recent");
        MedicalAiTool care = fixedTool("care_schedule", "照护安排", "明天有助浴服务。", "family_service_record");

        MedicalAiExecutor executor = new MedicalAiExecutor(
                new MedicalAiToolRegistry(List.of(health, alerts, care))
        );
        MedicalAiExecutionResult result = executor.execute(
                new MedicalAiPlan(List.of("health_recent", "alerts_recent", "care_schedule"), "compound"),
                new MedicalAiToolContext(11L, "王阿姨", "最近整体情况")
        );

        assertTrue(result.partial());
        assertTrue(result.answer().contains("心率记录正常"));
        assertTrue(result.answer().contains("明天有助浴服务"));
        assertTrue(result.answer().contains("部分数据源暂时不可用"));
        assertEquals(List.of("health_data", "family_service_record"), result.sources());
        assertEquals("failed", result.executions().get(1).status());
        assertTrue(result.executions().stream().allMatch(item -> item.elapsedMs() >= 0));
    }

    @Test
    void allFailuresReturnSafeFallbackWithoutFacts() {
        MedicalAiExecutor executor = new MedicalAiExecutor(
                new MedicalAiToolRegistry(List.of(failingTool("health_recent")))
        );

        MedicalAiExecutionResult result = executor.execute(
                new MedicalAiPlan(List.of("health_recent"), "health"),
                new MedicalAiToolContext(11L, "王阿姨", "最近健康")
        );

        assertTrue(result.partial());
        assertTrue(result.answer().contains("没有生成未经验证的医疗或业务事实"));
        assertTrue(result.sources().isEmpty());
    }

    private MedicalAiTool fixedTool(String name, String section, String body, String source) {
        return new MedicalAiTool() {
            @Override public String name() { return name; }
            @Override public boolean supports(String question) { return true; }
            @Override public MedicalAiToolResult execute(MedicalAiToolContext context) {
                return new MedicalAiToolResult(section, body, "ok", "读取业务数据", List.of(source));
            }
        };
    }

    private MedicalAiTool failingTool(String name) {
        return new MedicalAiTool() {
            @Override public String name() { return name; }
            @Override public boolean supports(String question) { return true; }
            @Override public MedicalAiToolResult execute(MedicalAiToolContext context) {
                throw new IllegalStateException("temporary failure");
            }
        };
    }
}
