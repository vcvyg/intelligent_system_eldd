package org.example.persion.vo;

import lombok.Data;
import java.util.List;

/**
 * 医护人员考勤报告VO
 */
@Data
public class StaffAttendanceReportVO {

    /**
     * 月份中的日期列表 (e.g., ["01", "02", ...])
     */
    private List<String> daysInMonth;

    /**
     * 考勤数据行
     */
    private List<StaffAttendanceRow> attendance;

    /**
     * 考勤概要统计
     */
    private List<AttendanceSummary> summary;

    /**
     * 内部类，代表一个员工的考勤数据
     */
    @Data
    public static class StaffAttendanceRow {
        private Long staffId;
        private String staffName;
        /**
         * 每日的状态列表, 与 daysInMonth 对应
         * e.g., ["ON_DUTY", "ON_LEAVE", "OFF_DUTY"]
         */
        private List<String> statusByDay;
    }

    /**
     * 内部类，代表一个员工的考勤概要
     */
    @Data
    public static class AttendanceSummary {
        private Long staffId;
        private String staffName;
        private long onDutyDays;
        private long onLeaveDays;
        private long offDutyDays;
    }
}
