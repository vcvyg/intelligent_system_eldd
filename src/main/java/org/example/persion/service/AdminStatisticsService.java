package org.example.persion.service;

import org.example.persion.vo.SystemStatisticsVO;

/**
 * 管理员端-系统统计服务接口
 */
public interface AdminStatisticsService {

    /**
     * 获取系统统计数据
     */
    SystemStatisticsVO getSystemStatistics();
}
