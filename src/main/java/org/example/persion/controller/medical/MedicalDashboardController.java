package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.MedicalDashboardService;
import org.example.persion.vo.MedicalDashboardVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medical/dashboard")
@RequiredArgsConstructor
public class MedicalDashboardController {

    private final MedicalDashboardService medicalDashboardService;

    @GetMapping
    public Result<MedicalDashboardVO> getDashboardData(@AuthenticationPrincipal Long userId) {
        MedicalDashboardVO dashboardData = medicalDashboardService.getDashboardData(userId);
        return Result.success(dashboardData);
    }
}

