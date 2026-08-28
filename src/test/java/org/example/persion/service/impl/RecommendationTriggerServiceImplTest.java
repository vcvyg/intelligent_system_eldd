package org.example.persion.service.impl;

import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.entity.RecommendationTrigger;
import org.example.persion.repository.RecommendationTriggerMapper;
import org.example.persion.vo.RecommendationTriggerVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationTriggerServiceImplTest {

    @Mock private RecommendationTriggerMapper triggerMapper;

    @Test
    void alertSignalCreatesPendingReviewTriggerWithoutMedicalPayload() {
        when(triggerMapper.selectCount(any())).thenReturn(0L);
        RecommendationTriggerServiceImpl service = new RecommendationTriggerServiceImpl(triggerMapper);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 29, 1, 30);

        service.record(CareSignalEvent.alertRaised(11L, 88L, occurredAt));

        ArgumentCaptor<RecommendationTrigger> captor = ArgumentCaptor.forClass(RecommendationTrigger.class);
        verify(triggerMapper).insert(captor.capture());
        RecommendationTrigger trigger = captor.getValue();
        assertEquals(11L, trigger.getElderlyId());
        assertEquals(88L, trigger.getReferenceId());
        assertEquals("ALERT_RAISED", trigger.getSignalType());
        assertEquals("PENDING_REVIEW", trigger.getStatus());
        assertEquals(occurredAt, trigger.getTriggerTime());
    }

    @Test
    void duplicateBusinessReferenceDoesNotCreateAnotherTrigger() {
        when(triggerMapper.selectCount(any())).thenReturn(1L);
        RecommendationTriggerServiceImpl service = new RecommendationTriggerServiceImpl(triggerMapper);

        service.record(CareSignalEvent.alertRaised(11L, 88L, LocalDateTime.now()));

        verify(triggerMapper, never()).insert(any(RecommendationTrigger.class));
    }

    @Test
    void pendingQueueMapsHumanReviewLabelAndDeliveryConsumesQueue() {
        RecommendationTrigger pending = new RecommendationTrigger();
        pending.setId(1L);
        pending.setElderlyId(11L);
        pending.setSignalType("ALERT_RAISED");
        pending.setReferenceId(88L);
        pending.setStatus("PENDING_REVIEW");
        pending.setTriggerTime(LocalDateTime.now());
        when(triggerMapper.selectList(any())).thenReturn(List.of(pending));
        RecommendationTriggerServiceImpl service = new RecommendationTriggerServiceImpl(triggerMapper);

        List<RecommendationTriggerVO> queue = service.pending(11L);
        assertEquals(1, queue.size());
        assertEquals("新告警触发关怀复核", queue.get(0).signalLabel());

        service.markDelivered(11L);
        assertEquals("DELIVERED", pending.getStatus());
        verify(triggerMapper).updateById(pending);
        assertTrue(queue.get(0).status().contains("PENDING_REVIEW"));
    }
}
