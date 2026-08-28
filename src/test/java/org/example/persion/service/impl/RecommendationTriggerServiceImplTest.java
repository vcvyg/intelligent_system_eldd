package org.example.persion.service.impl;

import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.common.exception.BusinessException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationTriggerServiceImplTest {

    @Mock private RecommendationTriggerMapper triggerMapper;

    @Test
    void allSupportedSignalsCreateMinimalPendingReviewTriggers() {
        when(triggerMapper.selectCount(any())).thenReturn(0L);
        RecommendationTriggerServiceImpl service = new RecommendationTriggerServiceImpl(triggerMapper);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 29, 1, 30);

        service.record(CareSignalEvent.alertRaised(11L, 88L, occurredAt));
        service.record(CareSignalEvent.healthRecorded(11L, 99L, occurredAt));
        service.record(CareSignalEvent.serviceScheduled(11L, 100L, occurredAt));

        ArgumentCaptor<RecommendationTrigger> captor = ArgumentCaptor.forClass(RecommendationTrigger.class);
        verify(triggerMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        List<RecommendationTrigger> saved = captor.getAllValues();
        assertEquals(List.of("ALERT_RAISED", "HEALTH_RECORDED", "SERVICE_SCHEDULED"),
                saved.stream().map(RecommendationTrigger::getSignalType).toList());
        assertTrue(saved.stream().allMatch(item -> "PENDING_REVIEW".equals(item.getStatus())));
        assertTrue(saved.stream().allMatch(item -> item.getElderlyId().equals(11L)));
    }

    @Test
    void duplicateBusinessReferenceDoesNotCreateAnotherTrigger() {
        when(triggerMapper.selectCount(any())).thenReturn(1L);
        RecommendationTriggerServiceImpl service = new RecommendationTriggerServiceImpl(triggerMapper);

        service.record(CareSignalEvent.alertRaised(11L, 88L, LocalDateTime.now()));

        verify(triggerMapper, never()).insert(any(RecommendationTrigger.class));
    }

    @Test
    void approveRecordsReviewerAndOnlyApprovedItemsAreDelivered() {
        RecommendationTrigger pending = trigger(1L, "PENDING_REVIEW");
        when(triggerMapper.selectById(1L)).thenReturn(pending);
        RecommendationTriggerServiceImpl service = new RecommendationTriggerServiceImpl(triggerMapper);

        RecommendationTriggerVO approved = service.approve(1L, 7L, "信号明确，进入人工投放确认");

        assertEquals("APPROVED", approved.status());
        assertEquals(7L, approved.reviewerId());
        assertNotNull(approved.reviewedAt());
        assertEquals("信号明确，进入人工投放确认", approved.decisionReason());

        when(triggerMapper.selectList(any())).thenReturn(List.of(pending));
        service.markDelivered(11L);

        assertEquals("DELIVERED", pending.getStatus());
        assertNotNull(pending.getDeliveredAt());
        verify(triggerMapper, org.mockito.Mockito.atLeast(2)).updateById(pending);
    }

    @Test
    void rejectClosesReviewAndRepeatedReviewIsRejected() {
        RecommendationTrigger pending = trigger(2L, "PENDING_REVIEW");
        when(triggerMapper.selectById(2L)).thenReturn(pending);
        RecommendationTriggerServiceImpl service = new RecommendationTriggerServiceImpl(triggerMapper);

        RecommendationTriggerVO rejected = service.reject(2L, 7L, "不适合当前关怀场景");
        assertEquals("REJECTED", rejected.status());

        assertThrows(BusinessException.class, () -> service.approve(2L, 7L, "重复复核"));
    }

    @Test
    void pendingQueueMapsReviewLabelsForDifferentDomains() {
        RecommendationTrigger health = trigger(3L, "PENDING_REVIEW");
        health.setSignalType("HEALTH_RECORDED");
        RecommendationTrigger serviceSignal = trigger(4L, "APPROVED");
        serviceSignal.setSignalType("SERVICE_SCHEDULED");
        when(triggerMapper.selectList(any())).thenReturn(List.of(health, serviceSignal));
        RecommendationTriggerServiceImpl service = new RecommendationTriggerServiceImpl(triggerMapper);

        List<RecommendationTriggerVO> queue = service.pending(11L);

        assertEquals(2, queue.size());
        assertEquals("新健康记录触发关怀复核", queue.get(0).signalLabel());
        assertEquals("新服务安排触发关怀复核", queue.get(1).signalLabel());
    }

    private RecommendationTrigger trigger(Long id, String status) {
        RecommendationTrigger trigger = new RecommendationTrigger();
        trigger.setId(id);
        trigger.setElderlyId(11L);
        trigger.setSignalType("ALERT_RAISED");
        trigger.setReferenceId(88L + id);
        trigger.setStatus(status);
        trigger.setTriggerTime(LocalDateTime.now());
        return trigger;
    }
}
