package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.MedicalPaymentRecordRequestDTO;
import org.example.persion.dto.MedicalServiceRecordRequestDTO;
import org.example.persion.service.MedicalFamilyServicesService;
import org.example.persion.vo.FamilyContactVO;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.example.persion.vo.FamilyServiceRecordVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical/family-services")
@RequiredArgsConstructor
public class MedicalFamilyServicesController {

    private final MedicalFamilyServicesService medicalFamilyServicesService;

    @PostMapping("/service-records")
    public Result<FamilyServiceRecordVO> createServiceRecord(@RequestBody MedicalServiceRecordRequestDTO request) {
        return Result.success(medicalFamilyServicesService.createServiceRecord(request));
    }

    @GetMapping("/elderly/{elderlyId}/service-records")
    public Result<List<FamilyServiceRecordVO>> listServiceRecords(@PathVariable Long elderlyId) {
        return Result.success(medicalFamilyServicesService.listServiceRecords(elderlyId));
    }

    @PostMapping("/payment-records")
    public Result<FamilyPaymentRecordVO> createPaymentRecord(@RequestBody MedicalPaymentRecordRequestDTO request) {
        return Result.success(medicalFamilyServicesService.createPaymentRecord(request));
    }

    @GetMapping("/elderly/{elderlyId}/payment-records")
    public Result<List<FamilyPaymentRecordVO>> listPaymentRecords(@PathVariable Long elderlyId) {
        return Result.success(medicalFamilyServicesService.listPaymentRecords(elderlyId));
    }

    @GetMapping("/elderly/{elderlyId}/family-contacts")
    public Result<List<FamilyContactVO>> listFamilyContacts(@PathVariable Long elderlyId) {
        return Result.success(medicalFamilyServicesService.listFamilyContacts(elderlyId));
    }
}

