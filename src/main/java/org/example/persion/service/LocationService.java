package org.example.persion.service;

import org.example.persion.entity.LocationData;

/**
 * 定位服务接口
 */
public interface LocationService {
    
    /**
     * 获取真实定位信息（基于IP或高德地图API）
     * 
     * @return 定位数据，如果获取失败返回null
     */
    LocationData getRealLocation();
    
    /**
     * 为指定老人更新定位数据
     * 
     * @param elderlyId 老人ID
     * @param deviceId 设备ID（可选）
     * @return 是否更新成功
     */
    boolean updateLocationForElderly(Long elderlyId, String deviceId);
    
    /**
     * 为所有绑定定位设备的老人更新位置
     * 
     * @return 更新的数量
     */
    int updateAllElderlyLocations();
}

