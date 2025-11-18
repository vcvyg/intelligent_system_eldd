package org.example.persion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyMedicalRelationMapper;
import org.example.persion.repository.MedicalScheduleMapper;
import org.example.persion.service.MedicalDashboardService;
import org.example.persion.vo.AlertRecordVO;
import org.example.persion.vo.MedicalDashboardVO;
import org.example.persion.vo.MedicalScheduleVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalDashboardServiceImpl implements MedicalDashboardService {

    private final ElderlyMedicalRelationMapper elderlyMedicalRelationMapper;
    private final MedicalScheduleMapper medicalScheduleMapper;
    private final AlertRecordMapper alertRecordMapper;

    @Override
    public MedicalDashboardVO getDashboardData(Long medicalUserId) {
        MedicalDashboardVO vo = new MedicalDashboardVO();

        // 1. 获取负责的老人数量
        long assignedPatientsCount = elderlyMedicalRelationMapper.countByMedicalUserId(medicalUserId);
        vo.setAssignedPatientsCount(assignedPatientsCount);

        // 2. 获取今日排班数量和列表
        LocalDate today = LocalDate.now();
        List<MedicalScheduleVO> todaySchedules = medicalScheduleMapper.selectSchedulesByMedicalUserAndDate(medicalUserId, today);
        vo.setTodaySchedulesCount(todaySchedules.size());
        vo.setTodaySchedules(todaySchedules);

        // 3. 获取待处理告警数量和列表
        List<AlertRecordVO> pendingAlerts = alertRecordMapper.findPendingAlertsByMedicalUser(medicalUserId);
        vo.setPendingAlertsCount(pendingAlerts.size());
        vo.setPendingAlerts(pendingAlerts);

        return vo;
    }
}

