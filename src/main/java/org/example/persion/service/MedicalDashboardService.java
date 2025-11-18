package org.example.persion.service;

import org.example.persion.vo.MedicalDashboardVO;

public interface MedicalDashboardService {
    MedicalDashboardVO getDashboardData(Long medicalUserId);
}

