package org.example.persion.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 医护 AI 助手回答。计划、工具轨迹、数据来源与最终答案分开返回，
 * 便于前端解释和演示 Agent 的执行过程。
 */
@Data
public class MedicalAiAnswerVO {
    private String traceId;
    private String sessionId;
    private Long elderlyId;
    private String elderlyName;
    private String answer;
    private boolean modelEnhanced;
    private String safetyNote;
    private long elapsedMs;
    private String planReason;
    private List<String> plan = new ArrayList<>();
    private List<ToolTrace> tools = new ArrayList<>();
    private List<String> sources = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class ToolTrace {
        private String tool;
        private String status;
        private String summary;
        private Long elapsedMs;

        public ToolTrace(String tool, String status, String summary) {
            this(tool, status, summary, null);
        }

        public ToolTrace(String tool, String status, String summary, Long elapsedMs) {
            this.tool = tool;
            this.status = status;
            this.summary = summary;
            this.elapsedMs = elapsedMs;
        }
    }
}
