package org.example.persion.service;

import org.example.persion.entity.HealthData;
import org.example.persion.vo.DailyHealthSummaryVO;

import java.time.LocalDate;
import java.util.List;

public interface MedicalRoundService {
    List<DailyHealthSummaryVO> getDailySummary(LocalDate date, Long elderlyId, String keyword);
    HealthData saveRecord(HealthData record);
}

