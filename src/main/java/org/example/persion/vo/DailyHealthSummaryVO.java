package org.example.persion.vo;

import lombok.Data;
import org.example.persion.entity.HealthData;
import org.example.persion.enums.TimePeriod;

import java.util.Map;

@Data
public class DailyHealthSummaryVO {
    private Long elderlyId;
    private String elderlyName;
    private Map<TimePeriod, HealthData> records;
}

