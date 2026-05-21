package org.example.persion.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.persion.service.ApiCallLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class ApiCallLimitServiceImpl implements ApiCallLimitService {

    private final int dailyLimit;
    private final int hourlyLimit;

    private LocalDate currentDay = LocalDate.now();
    private LocalDateTime currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
    private int dailyCalls;
    private int hourlyCalls;

    public ApiCallLimitServiceImpl(
            @Value("${location.api.limit.daily:900}") int dailyLimit,
            @Value("${location.api.limit.hourly:100}") int hourlyLimit
    ) {
        this.dailyLimit = dailyLimit;
        this.hourlyLimit = hourlyLimit;
    }

    @Override
    public synchronized boolean canCallApi() {
        refreshWindows();
        return dailyCalls < dailyLimit && hourlyCalls < hourlyLimit;
    }

    @Override
    public synchronized void recordApiCall() {
        refreshWindows();
        if (!canCallApi()) {
            log.warn("Map API quota exhausted: {}", getCallStats());
            return;
        }
        dailyCalls++;
        hourlyCalls++;
    }

    @Override
    public synchronized String getCallStats() {
        refreshWindows();
        return String.format(
                "API调用统计 - 今日: %d/%d, 本小时: %d/%d",
                dailyCalls,
                dailyLimit,
                hourlyCalls,
                hourlyLimit
        );
    }

    @Override
    public synchronized int getRemainingDailyCalls() {
        refreshWindows();
        return Math.max(0, dailyLimit - dailyCalls);
    }

    @Override
    public synchronized int getRemainingHourlyCalls() {
        refreshWindows();
        return Math.max(0, hourlyLimit - hourlyCalls);
    }

    private void refreshWindows() {
        LocalDate nowDay = LocalDate.now();
        if (!nowDay.equals(currentDay)) {
            currentDay = nowDay;
            dailyCalls = 0;
        }

        LocalDateTime nowHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        if (!nowHour.equals(currentHour)) {
            currentHour = nowHour;
            hourlyCalls = 0;
        }
    }
}
