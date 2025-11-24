package org.example.persion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.VisitAppointmentReviewDTO;
import org.example.persion.enums.VisitAppointmentStatus;
import org.example.persion.service.AdminFamilyServicesService;
import org.example.persion.vo.VisitAppointmentVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/family-services")
@RequiredArgsConstructor
public class AdminFamilyServicesController {

    private final AdminFamilyServicesService adminFamilyServicesService;

    /**
     * 获取探访预约列表
     */
    @GetMapping("/appointments")
    public Result<List<VisitAppointmentVO>> listAppointments(@RequestParam(value = "status", required = false) VisitAppointmentStatus status) {
        return Result.success(adminFamilyServicesService.listAppointments(status));
    }

    /**
     * 审批探访预约
     */
    @PutMapping("/appointments/{id}/review")
    public Result<VisitAppointmentVO> reviewAppointment(@PathVariable Long id,
                                                        @RequestBody VisitAppointmentReviewDTO request) {
        return Result.success(adminFamilyServicesService.reviewAppointment(id, request));
    }
}

