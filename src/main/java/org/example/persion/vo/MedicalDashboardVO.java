package org.example.persion.vo;

import lombok.Data;
import java.util.List;

@Data
public class MedicalDashboardVO {

    private long assignedPatientsCount;
    private long todaySchedulesCount;
    private long pendingAlertsCount;

    private List<MedicalScheduleVO> todaySchedules;
    private List<AlertRecordVO> pendingAlerts;
}

