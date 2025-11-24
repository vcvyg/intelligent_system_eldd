package org.example.persion.controller.family;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.PaymentActionDTO;
import org.example.persion.dto.VisitAppointmentRequestDTO;
import org.example.persion.service.FamilyServicesService;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.example.persion.vo.FamilyServiceRecordVO;
import org.example.persion.vo.VisitAppointmentVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family/services")
@RequiredArgsConstructor
public class FamilyServicesController {

    private final FamilyServicesService familyServicesService;

    @GetMapping("/progress")
    public Result<List<FamilyServiceRecordVO>> getServiceProgress(@RequestParam Long elderlyId) {
        return Result.success(familyServicesService.getServiceProgress(elderlyId));
    }

    @PostMapping("/appointment")
    public Result<VisitAppointmentVO> createAppointment(@RequestBody VisitAppointmentRequestDTO request) {
        return Result.success(familyServicesService.createAppointment(request));
    }

    @GetMapping("/appointments")
    public Result<List<VisitAppointmentVO>> listAppointments() {
        return Result.success(familyServicesService.listAppointments());
    }

    @DeleteMapping("/appointment/{id}")
    public Result<Void> cancelAppointment(@PathVariable Long id) {
        familyServicesService.cancelAppointment(id);
        return Result.success();
    }

    @GetMapping("/payments/pending")
    public Result<List<FamilyPaymentRecordVO>> listPendingPayments() {
        return Result.success(familyServicesService.listPendingPayments());
    }

    @GetMapping("/payments/history")
    public Result<List<FamilyPaymentRecordVO>> listPaymentHistory() {
        return Result.success(familyServicesService.listPaymentHistory());
    }

    @PostMapping("/payments/{id}/pay")
    public Result<FamilyPaymentRecordVO> pay(@PathVariable Long id, @RequestBody(required = false) PaymentActionDTO request) {
        return Result.success(familyServicesService.pay(id, request));
    }
}

