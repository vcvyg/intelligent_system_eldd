package org.example.persion.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.persion.dto.AlertCreateDTO;
import org.example.persion.dto.AlertHandleDTO;
import org.example.persion.vo.AlertRecordVO;

import java.util.List;
import java.util.Map;

/**
 * 预警管理服务接口
 */
public interface AlertService {

    /**
     * 分页查询预警列表
     */
    Page<AlertRecordVO> getAlertList(int current, int size, String alertType, String alertLevel, String status, String elderlyName);

    /**
     * 查询全部预警（包含老人、设备等详情）
     */
    List<AlertRecordVO> getAllAlertsWithDetails();

    /**
     * 根据ID查询预警详情
     */
    AlertRecordVO getAlertById(Long id);

    /**
     * 根据老人ID查询预警列表
     */
    List<AlertRecordVO> getAlertsByElderlyId(Long elderlyId);

    /**
     * 创建预警记录
     */
    Long createAlert(AlertCreateDTO dto);

    /**
     * 分配医护人员
     */
    void assignMedical(Long alertId, Long medicalId);

    /**
     * 处理预警
     */
    void handleAlert(AlertHandleDTO dto);

    /**
     * 开始处理预警(状态变为处理中)
     * @param alertId 预警ID
     */
    void processAlert(Long alertId);

    /**
     * 忽略预警
     */
    void ignoreAlert(Long alertId);

    /**
     * 删除预警记录
     */
    void deleteAlert(Long id);

    /**
     * 获取预警统计数据
     */
    Map<String, Object> getAlertStatistics();

    /**
     * 获取今日预警列表
     */
    List<AlertRecordVO> getTodayAlerts();
}
