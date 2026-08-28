package org.example.persion.ai.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 医护 AI Tool 注册中心。
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

    public MedicalAiTool require(String name) {
        return tools.stream()
                .filter(tool -> tool.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Medical AI tool not registered: " + name));
    }
}
