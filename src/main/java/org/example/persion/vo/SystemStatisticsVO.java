package org.example.persion.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统统计数据VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatisticsVO {

    private Long totalUsers; // 总用户数

    private Long totalElderly; // 总老人数

    private Long activeUsers; // 活跃用户数

    private Long disabledUsers; // 禁用用户数

    private Long adminCount; // 管理员数量

    private Long familyCount; // 子女用户数量

    private Long medicalCount; // 医护人员数量

    private Long elderlyCount; // 老人用户数量

    private Long todayHealthRecords; // 今日健康数据记录数
}
