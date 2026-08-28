package org.example.persion.ai.session;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicalAiSessionStoreTest {

    @Test
    void storesOnlyCurrentElderlyIdInRedisWithTwoHourTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("medical-ai:session:7:session-a")).thenReturn("11");

        MedicalAiSessionStore store = new MedicalAiSessionStore(redis);
        store.remember(7L, "session-a", 11L);

        verify(values).set("medical-ai:session:7:session-a", "11", Duration.ofHours(2));
        assertEquals(11L, store.currentElderlyId(7L, "session-a").orElseThrow());
    }

    @Test
    void fallsBackToLocalMinimalContextWhenRedisIsUnavailable() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
        MedicalAiSessionStore store = new MedicalAiSessionStore(redis);

        store.remember(7L, "session-b", 11L);
        assertEquals(11L, store.currentElderlyId(7L, "session-b").orElseThrow());

        store.clear(7L, "session-b");
        assertTrue(store.currentElderlyId(7L, "session-b").isEmpty());
    }
}
