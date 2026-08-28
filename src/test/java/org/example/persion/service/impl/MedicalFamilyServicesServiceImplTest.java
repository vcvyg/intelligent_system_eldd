package org.example.persion.service.impl;

import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.dto.MedicalServiceRecordRequestDTO;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.FamilyPaymentRecord;
import org.example.persion.entity.FamilyServiceRecord;
import org.example.persion.entity.User;
import org.example.persion.enums.PaymentStatus;
import org.example.persion.enums.ServiceProgressStatus;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.FamilyPaymentRecordMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.FamilyServiceStatusHistoryMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalFamilyServicesServiceImplTest {

    @Mock private FamilyServiceRecordMapper familyServiceRecordMapper;
    @Mock private FamilyPaymentRecordMapper familyPaymentRecordMapper;
    @Mock private FamilyServiceStatusHistoryMapper serviceStatusHistoryMapper;
    @Mock private ElderlyInfoMapper elderlyInfoMapper;
    @Mock private ElderlyFamilyRelationMapper elderlyFamilyRelationMapper;
    @Mock private UserMapper userMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MedicalFamilyServicesServiceImpl medicalFamilyServicesService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cancelPendingPaymentRecordClosesTheNotice() {
        FamilyPaymentRecord record = new FamilyPaymentRecord();
        record.setId(12L);
        record.setElderlyId(5L);
        record.setStatus(PaymentStatus.PENDING);

        ElderlyInfo elderlyInfo = new ElderlyInfo();
        elderlyInfo.setId(5L);
        elderlyInfo.setName("王阿姨");

        when(familyPaymentRecordMapper.selectById(12L)).thenReturn(record);
        when(elderlyInfoMapper.selectById(5L)).thenReturn(elderlyInfo);

        FamilyPaymentRecordVO result = medicalFamilyServicesService.cancelPaymentRecord(12L);

        assertEquals(PaymentStatus.CANCELLED, result.getStatus());
        assertEquals("王阿姨", result.getElderlyName());
        verify(familyPaymentRecordMapper).updateById(record);
    }

    @Test
    void pendingServiceRecordPublishesScheduledSignal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(7L, null, List.of())
        );

        ElderlyInfo elderly = new ElderlyInfo();
        elderly.setId(12L);
        when(elderlyInfoMapper.selectById(12L)).thenReturn(elderly);

        User medicalUser = new User();
        medicalUser.setId(7L);
        medicalUser.setUsername("medical-user");
        when(userMapper.selectById(7L)).thenReturn(medicalUser);
        when(serviceStatusHistoryMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            FamilyServiceRecord record = invocation.getArgument(0);
            record.setId(73L);
            return 1;
        }).when(familyServiceRecordMapper).insert(any(FamilyServiceRecord.class));

        MedicalServiceRecordRequestDTO request = new MedicalServiceRecordRequestDTO();
        request.setElderlyId(12L);
        request.setServiceType("陪诊安排");
        request.setStatus(ServiceProgressStatus.PENDING);

        medicalFamilyServicesService.createServiceRecord(request);

        ArgumentCaptor<CareSignalEvent> captor = ArgumentCaptor.forClass(CareSignalEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("SERVICE_SCHEDULED", captor.getValue().signalType());
        assertEquals(12L, captor.getValue().elderlyId());
        assertEquals(73L, captor.getValue().referenceId());
    }
}
