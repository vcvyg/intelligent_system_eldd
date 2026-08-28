package org.example.persion.ai.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 医护 AI Tool 注册中心。
 *
 * <p>当前先提供扩展骨架，后续 Health/Alert/Care/Recommendation Tool 可以直接注册。</p>
 */
@Component
public class MedicalAiToolRegistry {

    private final List<MedicalAiTool> tools;

    public MedicalAiToolRegistry(List<MedicalAiTool> tools) {
        this.tools = tools == null ? List.of() : new ArrayList<>(tools);
    }

    public List<MedicalAiTool> all() {
        return List.copyOf(tools);
    }

    public List<MedicalAiTool> route(String question) {
        return tools.stream()
                .filter(tool -> tool.supports(question))
                .toList();
    }
}
