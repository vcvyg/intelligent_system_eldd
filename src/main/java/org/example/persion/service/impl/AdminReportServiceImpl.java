package org.example.persion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.persion.entity.HealthData;
import org.example.persion.entity.LeaveRequest;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.repository.LeaveRequestMapper;
import org.example.persion.repository.MedicalScheduleMapper;
import org.example.persion.service.AdminReportService;
import org.example.persion.service.AdminUserService;
import org.example.persion.vo.HealthTrendVO;
import org.example.persion.vo.MonthlyHealthReportVO;
import org.example.persion.vo.StaffAttendanceReportVO;
import org.example.persion.vo.UserInfoVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final HealthDataMapper healthDataMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final MedicalScheduleMapper medicalScheduleMapper;
    private final AdminUserService adminUserService;

    @Override
    public MonthlyHealthReportVO generateMonthlyHealthReport(String month, Long elderlyId) {
        // ... (existing method remains unchanged)
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);
        List<HealthData> monthlyData = healthDataMapper.findByDateTimeRange(startOfMonth, endOfMonth, elderlyId);
        MonthlyHealthReportVO report = new MonthlyHealthReportVO();
        if (monthlyData == null || monthlyData.isEmpty()) {
            report.setSummary(new MonthlyHealthReportVO.ReportSummary());
            HealthTrendVO emptyTrends = new HealthTrendVO();
            emptyTrends.setDates(new ArrayList<>());
            report.setDailyTrends(emptyTrends);
            return report;
        }
        report.setSummary(calculateSummary(monthlyData));
        report.setDailyTrends(calculateDailyTrends(monthlyData, yearMonth));
        return report;
    }

    @Override
    public StaffAttendanceReportVO generateStaffAttendanceReport(String month, Long staffId) {
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // 1. 获取医护人员列表
        List<UserInfoVO> staffList = adminUserService.getUsersByRole("MEDICAL");
        if (staffId != null) {
            staffList = staffList.stream().filter(s -> s.getId().equals(staffId)).collect(Collectors.toList());
        }
        List<Long> staffIds = staffList.stream().map(UserInfoVO::getId).collect(Collectors.toList());

        // 2. 获取时间段内的排班和请假记录
        Map<Long, Set<LocalDate>> schedules = getSchedules(staffIds, startOfMonth, endOfMonth);
        Map<Long, Set<LocalDate>> leaves = getApprovedLeaves(staffIds, startOfMonth, endOfMonth);

        // 3. 组装报告
        StaffAttendanceReportVO report = new StaffAttendanceReportVO();
        int daysInMonth = yearMonth.lengthOfMonth();
        report.setDaysInMonth(IntStream.rangeClosed(1, daysInMonth).mapToObj(d -> String.format("%02d", d)).collect(Collectors.toList()));

        List<StaffAttendanceReportVO.StaffAttendanceRow> attendanceRows = new ArrayList<>();
        List<StaffAttendanceReportVO.AttendanceSummary> summaryRows = new ArrayList<>();

        for (UserInfoVO staff : staffList) {
            StaffAttendanceReportVO.StaffAttendanceRow row = new StaffAttendanceReportVO.StaffAttendanceRow();
            row.setStaffId(staff.getId());
            row.setStaffName(staff.getRealName() != null ? staff.getRealName() : staff.getUsername());

            List<String> statusByDay = new ArrayList<>();
            for (int i = 1; i <= daysInMonth; i++) {
                LocalDate currentDate = yearMonth.atDay(i);
                String status = "OFF_DUTY"; // 默认休息
                if (leaves.getOrDefault(staff.getId(), Collections.emptySet()).contains(currentDate)) {
                    status = "ON_LEAVE"; // 优先判断请假
                } else if (schedules.getOrDefault(staff.getId(), Collections.emptySet()).contains(currentDate)) {
                    status = "ON_DUTY";
                }
                statusByDay.add(status);
            }
            row.setStatusByDay(statusByDay);
            attendanceRows.add(row);

            // 4. 计算并添加概要统计
            StaffAttendanceReportVO.AttendanceSummary summary = new StaffAttendanceReportVO.AttendanceSummary();
            summary.setStaffId(staff.getId());
            summary.setStaffName(row.getStaffName());
            summary.setOnDutyDays(statusByDay.stream().filter(s -> s.equals("ON_DUTY")).count());
            summary.setOnLeaveDays(statusByDay.stream().filter(s -> s.equals("ON_LEAVE")).count());
            summary.setOffDutyDays(statusByDay.stream().filter(s -> s.equals("OFF_DUTY")).count());
            summaryRows.add(summary);
        }

        report.setAttendance(attendanceRows);
        report.setSummary(summaryRows);
        return report;
    }

    private Map<Long, Set<LocalDate>> getSchedules(List<Long> staffIds, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> scheduleRecords = medicalScheduleMapper.selectScheduleListByDateRange(startDate, endDate);
        return scheduleRecords.stream()
                .filter(record -> staffIds.contains(((Number)record.get("medical_user_id")).longValue()))
                .collect(Collectors.groupingBy(
                        record -> ((Number)record.get("medical_user_id")).longValue(),
                        Collectors.mapping(record -> ((java.sql.Date) record.get("schedule_date")).toLocalDate(), Collectors.toSet())
                ));
    }

    private Map<Long, Set<LocalDate>> getApprovedLeaves(List<Long> staffIds, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> leaveRecords = leaveRequestMapper.findApprovedByUsersAndDateRange(staffIds, startDate, endDate);
        Map<Long, Set<LocalDate>> leaveMap = new HashMap<>();
        for (LeaveRequest leave : leaveRecords) {
            Set<LocalDate> dates = leaveMap.computeIfAbsent(leave.getMedicalUserId(), k -> new HashSet<>());
            for (LocalDate date = leave.getStartDate(); !date.isAfter(leave.getEndDate()); date = date.plusDays(1)) {
                dates.add(date);
            }
        }
        return leaveMap;
    }

    // --- existing private methods for health report ---
    private MonthlyHealthReportVO.ReportSummary calculateSummary(List<HealthData> data) {
        MonthlyHealthReportVO.ReportSummary summary = new MonthlyHealthReportVO.ReportSummary();
        Function<HealthData, BigDecimal> heartRateMapper = h -> h.getHeartRate();
        data.stream().map(heartRateMapper).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().ifPresent(summary::setAvgHeartRate);
        Function<HealthData, BigDecimal> bloodSugarMapper = h -> h.getBloodSugar();
        data.stream().map(bloodSugarMapper).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().ifPresent(summary::setAvgBloodSugar);
        Function<HealthData, BigDecimal> highPressureMapper = h -> h.getBloodPressureHigh();
        data.stream().map(highPressureMapper).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).max().ifPresent(summary::setMaxBloodPressureHigh);
        Function<HealthData, BigDecimal> lowPressureMapper = h -> h.getBloodPressureLow();
        data.stream().map(lowPressureMapper).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).min().ifPresent(summary::setMinBloodPressureLow);
        Integer totalSteps = data.stream()
                .filter(h -> h.getSteps() != null)
                .collect(Collectors.groupingBy(h -> h.getMeasureTime().toLocalDate(), Collectors.maxBy(Comparator.comparing(HealthData::getSteps))))
                .values().stream()
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getSteps())
                .mapToInt(Integer::intValue)
                .sum();
        summary.setTotalSteps(totalSteps);
        return summary;
    }

    private HealthTrendVO calculateDailyTrends(List<HealthData> data, YearMonth yearMonth) {
        Map<LocalDate, List<HealthData>> dataByDay = data.stream().collect(Collectors.groupingBy(h -> h.getMeasureTime().toLocalDate()));
        HealthTrendVO trends = new HealthTrendVO();
        List<String> dateLabels = new ArrayList<>();
        List<Double> heartRates = new ArrayList<>();
        List<Double> bloodPressureHighs = new ArrayList<>();
        List<Double> bloodPressureLows = new ArrayList<>();
        List<Double> bloodSugars = new ArrayList<>();
        List<Integer> steps = new ArrayList<>();
        int daysInMonth = yearMonth.lengthOfMonth();
        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate currentDate = yearMonth.atDay(i);
            dateLabels.add(currentDate.format(DateTimeFormatter.ofPattern("dd")));
            List<HealthData> dayData = dataByDay.get(currentDate);
            if (dayData == null || dayData.isEmpty()) {
                heartRates.add(null);
                bloodPressureHighs.add(null);
                bloodPressureLows.add(null);
                bloodSugars.add(null);
                steps.add(null);
            } else {
                dayData.stream().map(HealthData::getHeartRate).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().ifPresentOrElse(heartRates::add, () -> heartRates.add(null));
                dayData.stream().map(HealthData::getBloodPressureHigh).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().ifPresentOrElse(bloodPressureHighs::add, () -> bloodPressureHighs.add(null));
                dayData.stream().map(HealthData::getBloodPressureLow).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().ifPresentOrElse(bloodPressureLows::add, () -> bloodPressureLows.add(null));
                dayData.stream().map(HealthData::getBloodSugar).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().ifPresentOrElse(bloodSugars::add, () -> bloodSugars.add(null));
                dayData.stream().map(HealthData::getSteps).filter(Objects::nonNull).max(Integer::compareTo).ifPresentOrElse(steps::add, () -> steps.add(null));
            }
        }
        trends.setDates(dateLabels);
        trends.setHeartRates(heartRates);
        trends.setBloodPressureHighs(bloodPressureHighs);
        trends.setBloodPressureLows(bloodPressureLows);
        trends.setBloodSugars(bloodSugars);
        trends.setSteps(steps);
        return trends;
    }
}
