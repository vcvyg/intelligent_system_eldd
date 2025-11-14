package org.example.persion.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.persion.dto.DeviceCreateDTO;
import org.example.persion.dto.DeviceUpdateDTO;
import org.example.persion.vo.DeviceInfoVO;

import java.util.List;

/**
 * 设备管理服务接口
 */
public interface DeviceService {

    /**
     * 分页查询设备列表
     */
    Page<DeviceInfoVO> getDeviceList(int current, int size, String keyword, String deviceType, String status);

    /**
     * 根据ID查询设备详情
     */
    DeviceInfoVO getDeviceById(Long id);

    /**
     * 根据设备编号查询
     */
    DeviceInfoVO getDeviceByCode(String deviceCode);

    /**
     * 根据老人ID查询设备列表
     */
    List<DeviceInfoVO> getDevicesByElderlyId(Long elderlyId);

    /**
     * 创建设备
     */
    Long createDevice(DeviceCreateDTO dto);

    /**
     * 更新设备
     */
    void updateDevice(DeviceUpdateDTO dto);

    /**
     * 删除设备(逻辑删除)
     */
    void deleteDevice(Long id);

    /**
     * 绑定设备到老人
     */
    void bindDeviceToElderly(Long deviceId, Long elderlyId);

    /**
     * 解绑设备
     */
    void unbindDevice(Long deviceId);

    /**
     * 更新设备状态
     */
    void updateDeviceStatus(Long deviceId, String status);

    /**
     * 统计各类型设备数量
     */
    Object getDeviceStatistics();
}
