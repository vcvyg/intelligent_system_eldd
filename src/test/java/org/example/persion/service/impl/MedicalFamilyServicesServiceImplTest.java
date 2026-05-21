package org.example.persion.service.impl;

import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.FamilyPaymentRecord;
import org.example.persion.enums.PaymentStatus;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.FamilyPaymentRecordMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.FamilyServiceStatusHistoryMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalFamilyServicesServiceImplTest {

    @Mock
    private FamilyServiceRecordMapper familyServiceRecordMapper;

    @Mock
    private FamilyPaymentRecordMapper familyPaymentRecordMapper;

    @Mock
    private FamilyServiceStatusHistoryMapper serviceStatusHistoryMapper;

    @Mock
    private ElderlyInfoMapper elderlyInfoMapper;

    @Mock
    private ElderlyFamilyRelationMapper elderlyFamilyRelationMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MedicalFamilyServicesServiceImpl medicalFamilyServicesService;

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
}
