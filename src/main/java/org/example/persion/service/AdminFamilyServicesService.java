package org.example.persion.service;

import org.example.persion.dto.VisitAppointmentReviewDTO;
import org.example.persion.enums.VisitAppointmentStatus;
import org.example.persion.vo.VisitAppointmentVO;

import java.util.List;

public interface AdminFamilyServicesService {

    /**
     * 查询探访预约列表
     *
     * @param status 过滤状态，可为空
     */
    List<VisitAppointmentVO> listAppointments(VisitAppointmentStatus status);

    /**
     * 审批探访预约
     */
    VisitAppointmentVO reviewAppointment(Long appointmentId, VisitAppointmentReviewDTO request);
}

