package org.example.persion.service;

import org.example.persion.vo.MonthlyHealthReportVO;

/**
 * 管理端 - 数据报告服务接口
 */
public interface AdminReportService {

    /**
     * 生成月度健康报告
     * @param month "YYYY-MM"格式的月份字符串
     * @param elderlyId 老人ID (可选)
     * @return 月度健康报告VO
     */
    MonthlyHealthReportVO generateMonthlyHealthReport(String month, Long elderlyId);

    /**
     * 生成医护人员月度考勤报告
     * @param month "YYYY-MM"格式的月份字符串
     * @param staffId 医护人员ID (可选)
     * @return 考勤报告VO
     */
    org.example.persion.vo.StaffAttendanceReportVO generateStaffAttendanceReport(String month, Long staffId);
}
