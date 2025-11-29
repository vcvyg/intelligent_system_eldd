package org.example.persion.service;

import org.example.persion.entity.Room;
import java.util.List;
import java.util.Map;

/**
 * 缓存服务接口
 */
public interface CacheService {
    
    /**
     * 获取所有可用房间列表（带缓存）
     */
    List<Room> getAvailableRooms();
    
    /**
     * 获取房间信息映射（带缓存）
     */
    Map<Long, Room> getRoomMap();
    
    /**
     * 清除房间缓存
     */
    void clearRoomCache();
    
    /**
     * 刷新房间缓存
     */
    void refreshRoomCache();
    
    /**
     * 获取医护人员列表（带缓存）
     */
    List<org.example.persion.entity.User> getMedicalUsers();
    
    /**
     * 清除用户缓存
     */
    void clearUserCache();
}