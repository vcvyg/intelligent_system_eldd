package org.example.persion.vo;

import java.time.LocalDateTime;

/**
 * Agent-side operational action proposal. The proposal itself performs no write.
 */
public record MedicalAiActionProposalVO(
        String proposalId,
        String actionType,
        Long targetId,
        String summary,
        LocalDateTime expiresAt,
        boolean confirmationRequired
) {
}
