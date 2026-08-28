package org.example.persion.ai.action;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores short-lived confirmation proposals for operational Agent actions.
 *
 * <p>Only user/action/target identifiers are stored. Redis provides cross-instance one-time
 * consumption; if Redis is unavailable, a bounded in-process fallback keeps the confirmation
 * boundary available without persisting business or medical content.</p>
 */
@Component
@RequiredArgsConstructor
public class MedicalAiActionProposalStore {

    public static final String START_ALERT_PROCESSING = "START_ALERT_PROCESSING";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "medical-ai:action-proposal:";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, PendingAction> fallback = new ConcurrentHashMap<>();

    public LocalDateTime put(String proposalId, Long medicalUserId, Long elderlyId, Long targetId, String actionType) {
        LocalDateTime expiresAt = LocalDateTime.now().plus(TTL);
        PendingAction action = new PendingAction(medicalUserId, elderlyId, targetId, actionType, expiresAt);
        String key = key(proposalId);
        try {
            redisTemplate.opsForValue().set(key, serialize(action), TTL);
            fallback.remove(key);
            return expiresAt;
        } catch (RuntimeException ignored) {
            fallback.put(key, action);
            return expiresAt;
        }
    }

    public Optional<PendingAction> consume(String proposalId) {
        String key = key(proposalId);
        try {
            String encoded = redisTemplate.opsForValue().getAndDelete(key);
            if (encoded != null && !encoded.isBlank()) {
                fallback.remove(key);
                return parse(encoded).filter(this::notExpired);
            }
        } catch (RuntimeException ignored) {
            // Fall through to local one-time storage.
        }

        PendingAction local = fallback.remove(key);
        return Optional.ofNullable(local).filter(this::notExpired);
    }

    public void cancel(String proposalId) {
        String key = key(proposalId);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ignored) {
            // Local cleanup still has to happen.
        }
        fallback.remove(key);
    }

    private boolean notExpired(PendingAction action) {
        return action.expiresAt() != null && action.expiresAt().isAfter(LocalDateTime.now());
    }

    private String serialize(PendingAction action) {
        return action.medicalUserId() + "|"
                + action.elderlyId() + "|"
                + action.targetId() + "|"
                + action.actionType() + "|"
                + action.expiresAt();
    }

    private Optional<PendingAction> parse(String encoded) {
        try {
            String[] parts = encoded.split("\\|", -1);
            if (parts.length != 5) return Optional.empty();
            return Optional.of(new PendingAction(
                    Long.valueOf(parts[0]),
                    Long.valueOf(parts[1]),
                    Long.valueOf(parts[2]),
                    parts[3],
                    LocalDateTime.parse(parts[4])
            ));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private String key(String proposalId) {
        return KEY_PREFIX + proposalId;
    }

    public record PendingAction(
            Long medicalUserId,
            Long elderlyId,
            Long targetId,
            String actionType,
            LocalDateTime expiresAt
    ) {
    }
}
