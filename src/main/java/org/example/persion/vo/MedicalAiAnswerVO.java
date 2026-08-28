package org.example.persion.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 医护 AI 助手回答。答案与工具轨迹分开返回，便于前端解释和演示。
 */
@Data
public class MedicalAiAnswerVO {
    private String sessionId;
    private Long elderlyId;
    private String elderlyName;
    private String answer;
    private boolean modelEnhanced;
    private String safetyNote;
    private List<ToolTrace> tools = new ArrayList<>();
    private List<String> sources = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolTrace {
        private String tool;
        private String status;
        private String summary;
    }
}
