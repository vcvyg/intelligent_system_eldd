package org.example.persion.service;

import org.example.persion.dto.MedicalPaymentRecordRequestDTO;
import org.example.persion.dto.MedicalServiceRecordRequestDTO;
import org.example.persion.dto.MedicalServiceStatusUpdateDTO;
import org.example.persion.vo.FamilyContactVO;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.example.persion.vo.FamilyServiceRecordVO;
import org.example.persion.vo.MedicalFamilyServiceSummaryVO;

import java.util.List;

public interface MedicalFamilyServicesService {

    FamilyServiceRecordVO createServiceRecord(MedicalServiceRecordRequestDTO request);

    FamilyServiceRecordVO updateServiceRecordStatus(Long recordId, MedicalServiceStatusUpdateDTO request);

    List<FamilyServiceRecordVO> listServiceRecords(Long elderlyId);

    FamilyPaymentRecordVO createPaymentRecord(MedicalPaymentRecordRequestDTO request);

    FamilyPaymentRecordVO cancelPaymentRecord(Long recordId);

    List<FamilyPaymentRecordVO> listPaymentRecords(Long elderlyId);

    List<FamilyContactVO> listFamilyContacts(Long elderlyId);

    MedicalFamilyServiceSummaryVO getSummary();
}

