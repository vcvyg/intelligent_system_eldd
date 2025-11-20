package org.example.persion.service;

import org.example.persion.vo.HealthTrendVO;

import java.time.LocalDate;

/**
 * 管理端 - 健康数据服务接口
 */
public interface AdminHealthService {

    /**
     * 获取健康数据趋势
     * @param days 天数
     * @param elderlyId 老人ID (可选, 为null则查询所有人)
     * @return 趋势数据VO
     */
    HealthTrendVO getHealthTrend(Integer days, Long elderlyId);

    /**
     * 获取单日健康数据
     * @param date 日期
     * @param elderlyId 老人ID (可选, 为null则查询所有人)
     * @return 单日数据VO
     */
    HealthTrendVO getDailyHealthData(LocalDate date, Long elderlyId);

    /**
     * 检查健康数据并生成告警
     * @param elderlyId 老人ID
     */
    void checkAndGenerateHealthAlerts(Long elderlyId);
}
