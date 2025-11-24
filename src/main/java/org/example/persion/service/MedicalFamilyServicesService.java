package org.example.persion.service;

import org.example.persion.dto.MedicalPaymentRecordRequestDTO;
import org.example.persion.dto.MedicalServiceRecordRequestDTO;
import org.example.persion.vo.FamilyContactVO;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.example.persion.vo.FamilyServiceRecordVO;

import java.util.List;

public interface MedicalFamilyServicesService {

    FamilyServiceRecordVO createServiceRecord(MedicalServiceRecordRequestDTO request);

    List<FamilyServiceRecordVO> listServiceRecords(Long elderlyId);

    FamilyPaymentRecordVO createPaymentRecord(MedicalPaymentRecordRequestDTO request);

    List<FamilyPaymentRecordVO> listPaymentRecords(Long elderlyId);

    List<FamilyContactVO> listFamilyContacts(Long elderlyId);
}

