package org.example.persion.service;

import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.entity.CareSignalOutbox;
import org.example.persion.repository.CareSignalOutboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareSignalOutboxServiceTest {

    @Mock
    private CareSignalOutboxMapper outboxMapper;

    @Test
    void enqueueStoresOnlyMinimalIdempotentSignalMetadata() {
        when(outboxMapper.selectCount(any())).thenReturn(0L);
        CareSignalOutboxService service = new CareSignalOutboxService(outboxMapper);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 29, 2, 10);

        service.enqueue(CareSignalEvent.healthRecorded(12L, 45L, occurredAt));

        ArgumentCaptor<CareSignalOutbox> captor = ArgumentCaptor.forClass(CareSignalOutbox.class);
        verify(outboxMapper).insert(captor.capture());
        CareSignalOutbox row = captor.getValue();
        assertEquals("HEALTH_RECORDED:12:45", row.getEventKey());
        assertEquals(12L, row.getElderlyId());
        assertEquals("HEALTH_RECORDED", row.getSignalType());
        assertEquals(45L, row.getReferenceId());
        assertEquals(occurredAt, row.getOccurredAt());
        assertEquals("PENDING", row.getStatus());
        assertEquals(0, row.getRetryCount());
        assertNull(row.getLastErrorType());
    }

    @Test
    void duplicateEventKeyDoesNotCreateSecondOutboxRow() {
        when(outboxMapper.selectCount(any())).thenReturn(1L);
        CareSignalOutboxService service = new CareSignalOutboxService(outboxMapper);

        service.enqueue(CareSignalEvent.serviceScheduled(12L, 46L, LocalDateTime.now()));

        verify(outboxMapper, never()).insert(any(CareSignalOutbox.class));
    }
}
