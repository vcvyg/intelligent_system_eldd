package org.example.persion.vo;

/**
 * Result of an explicitly confirmed operational action.
 */
public record MedicalAiActionResultVO(
        String proposalId,
        String actionType,
        Long targetId,
        String status,
        String message
) {
}
