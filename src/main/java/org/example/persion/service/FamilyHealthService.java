package org.example.persion.service;

import org.example.persion.vo.HealthDataVO;

import java.util.List;
import java.util.Map;

/**
 * 子女端 - 健康数据服务接口
 */
public interface FamilyHealthService {

    /**
     * 获取子女端仪表盘数据（统计信息和老人列表）
     */
    Map<String, Object> getDashboardData();

    /**
     * 获取指定老人的最新健康数据
     */
    HealthDataVO getLatestHealthData(Long elderlyId);

    /**
     * 获取指定老人的健康数据列表
     */
    List<HealthDataVO> getHealthDataList(Long elderlyId, Integer days);
}

