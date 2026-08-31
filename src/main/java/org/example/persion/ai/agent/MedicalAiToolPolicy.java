package org.example.persion.ai.agent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only Tool allowlist shared by deterministic and model-assisted planning.
 */
public final class MedicalAiToolPolicy {

    public static final List<String> READ_ONLY_TOOLS = List.of(
            "room_lookup",
            "patient_profile",
            "health_recent",
            "alerts_recent",
            "care_schedule",
            "recommendation_preview",
            "recommendation_performance"
    );

    private static final Set<String> ALLOWED = Set.copyOf(READ_ONLY_TOOLS);

    private MedicalAiToolPolicy() {
    }

    public static boolean isAllowed(String toolName) {
        return toolName != null && ALLOWED.contains(toolName);
    }

    public static List<String> sanitizeModelTools(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) return List.of();
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String toolName : toolNames) {
            if (isAllowed(toolName)) {
                sanitized.add(toolName);
            }
            if (sanitized.size() >= 4) break;
        }
        return new ArrayList<>(sanitized);
    }
}
