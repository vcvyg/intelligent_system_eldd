package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.MedicalPatientService;
import org.example.persion.vo.ElderlyInfoVO;
import org.example.persion.vo.PatientHealthDetailsVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medical")
@PreAuthorize("hasRole('MEDICAL')")
@RequiredArgsConstructor
public class MedicalController {

    private final MedicalPatientService medicalPatientService;

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("医护端接口测试成功");
    }

    /**
     * 获取当前医护人员负责的老人列表
     * @param userId Spring Security注入的当前用户ID
     * @return 老人列表
     */
    @GetMapping("/patients")
    public Result<List<ElderlyInfoVO>> getMyPatients(@AuthenticationPrincipal Long userId) {
        List<ElderlyInfoVO> patients = medicalPatientService.getMyPatients(userId);
        return Result.success(patients);
    }

    /**
     * 获取单个老人的详细健康信息
     * @param id 老人ID
     * @return 包含老人信息和健康数据列表的VO
     */
    @GetMapping("/patients/{id}/health-details")
    public Result<PatientHealthDetailsVO> getPatientHealthDetails(@PathVariable Long id) {
        PatientHealthDetailsVO details = medicalPatientService.getPatientHealthDetails(id);
        return Result.success(details);
    }
}
