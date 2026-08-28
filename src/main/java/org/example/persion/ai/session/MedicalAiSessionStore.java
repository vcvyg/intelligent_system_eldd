package org.example.persion.ai.session;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 医护 Agent 会话上下文存储。
 *
 * <p>Redis 可用时只保存当前老人 ID 并设置 TTL，支持多实例共享；Redis 暂时不可用时退化到
 * 进程内短期缓存，不保存完整问题、回答或医疗事实。</p>
 */
@Component
@RequiredArgsConstructor
public class MedicalAiSessionStore {

    private static final Duration TTL = Duration.ofHours(2);
    private static final String KEY_PREFIX = "medical-ai:session:";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalSession> fallback = new ConcurrentHashMap<>();

    public Optional<Long> currentElderlyId(Long medicalUserId, String sessionId) {
        String key = key(medicalUserId, sessionId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && !value.isBlank()) {
                return Optional.of(Long.parseLong(value));
            }
        } catch (RuntimeException ignored) {
            // Redis 故障不能阻断医护只读查询；退化到本机短期上下文。
        }

        LocalSession local = fallback.get(key);
        if (local == null) return Optional.empty();
        if (local.expiresAt().isBefore(LocalDateTime.now())) {
            fallback.remove(key);
            return Optional.empty();
        }
        return Optional.of(local.elderlyId());
    }

    public void remember(Long medicalUserId, String sessionId, Long elderlyId) {
        if (elderlyId == null) return;
        String key = key(medicalUserId, sessionId);
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(elderlyId), TTL);
            fallback.remove(key);
            return;
        } catch (RuntimeException ignored) {
            // 仅在 Redis 暂时不可用时保留最小化本机会话状态。
        }
        fallback.put(key, new LocalSession(elderlyId, LocalDateTime.now().plus(TTL)));
    }

    public void clear(Long medicalUserId, String sessionId) {
        String key = key(medicalUserId, sessionId);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ignored) {
            // 本地 fallback 仍需继续清理。
        }
        fallback.remove(key);
    }

    private String key(Long medicalUserId, String sessionId) {
        return KEY_PREFIX + medicalUserId + ":" + sessionId;
    }

    private record LocalSession(Long elderlyId, LocalDateTime expiresAt) {
    }
}
