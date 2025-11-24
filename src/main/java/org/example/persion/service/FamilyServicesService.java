package org.example.persion.service;

import org.example.persion.dto.VisitAppointmentRequestDTO;
import org.example.persion.dto.PaymentActionDTO;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.example.persion.vo.FamilyServiceRecordVO;
import org.example.persion.vo.VisitAppointmentVO;

import java.util.List;

public interface FamilyServicesService {

    List<FamilyServiceRecordVO> getServiceProgress(Long elderlyId);

    VisitAppointmentVO createAppointment(VisitAppointmentRequestDTO request);

    List<VisitAppointmentVO> listAppointments();

    void cancelAppointment(Long appointmentId);

    List<FamilyPaymentRecordVO> listPendingPayments();

    List<FamilyPaymentRecordVO> listPaymentHistory();

    FamilyPaymentRecordVO pay(Long recordId, PaymentActionDTO request);
}

