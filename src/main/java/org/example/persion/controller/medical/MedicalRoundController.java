package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.entity.HealthData;
import org.example.persion.service.MedicalRoundService;
import org.example.persion.vo.DailyHealthSummaryVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/medical/rounds")
@RequiredArgsConstructor
public class MedicalRoundController {

    private final MedicalRoundService medicalRoundService;

    @GetMapping("/daily-summary")
    public Result<List<DailyHealthSummaryVO>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long elderlyId,
            @RequestParam(required = false) String keyword) {
        List<DailyHealthSummaryVO> summary = medicalRoundService.getDailySummary(date, elderlyId, keyword);
        return Result.success(summary);
    }

    @PostMapping("/record")
    public Result<HealthData> saveRecord(@RequestBody HealthData record) {
        if (record.getId() == null) {
            record.setCreateTime(LocalDateTime.now());
        }
        record.setUpdateTime(LocalDateTime.now());
        HealthData savedRecord = medicalRoundService.saveRecord(record);
        return Result.success(savedRecord);
    }
}

