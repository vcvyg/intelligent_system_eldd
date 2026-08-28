package org.example.persion.service.impl;

import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.entity.HealthData;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.service.AlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicalRoundServiceImplTest {

    @Mock private ElderlyInfoMapper elderlyInfoMapper;
    @Mock private HealthDataMapper healthDataMapper;
    @Mock private AlertService alertService;
    @Mock private ElderlyFamilyRelationMapper relationMapper;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    void savedHealthRecordPublishesMinimalHealthSignal() {
        MedicalRoundServiceImpl service = spy(new MedicalRoundServiceImpl(
                elderlyInfoMapper,
                healthDataMapper,
                alertService,
                relationMapper,
                messagingTemplate,
                eventPublisher
        ));

        HealthData record = new HealthData();
        record.setId(91L);
        record.setElderlyId(12L);
        record.setMeasureTime(LocalDateTime.of(2026, 8, 29, 2, 20));
        doReturn(true).when(service).saveOrUpdate(record);

        service.saveRecord(record);

        ArgumentCaptor<CareSignalEvent> captor = ArgumentCaptor.forClass(CareSignalEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        CareSignalEvent event = captor.getValue();
        assertEquals("HEALTH_RECORDED", event.signalType());
        assertEquals(12L, event.elderlyId());
        assertEquals(91L, event.referenceId());
    }
}
