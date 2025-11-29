package org.example.persion.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.entity.Room;
import org.example.persion.repository.RoomMapper;
import org.example.persion.service.CacheService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 缓存服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
    
    private final RoomMapper roomMapper;
    
    @Override
    @Cacheable(value = "rooms", key = "'available'")
    public List<Room> getAvailableRooms() {
        log.debug("从数据库加载可用房间列表");
        return roomMapper.selectList(null);
    }
    
    @Override
    @Cacheable(value = "rooms", key = "'map'")
    public Map<Long, Room> getRoomMap() {
        log.debug("从数据库加载房间映射");
        List<Room> rooms = roomMapper.selectList(null);
        return rooms.stream().collect(Collectors.toMap(Room::getId, room -> room));
    }
    
    @Override
    @CacheEvict(value = "rooms", allEntries = true)
    public void clearRoomCache() {
        log.debug("清除房间缓存");
    }
    
    @Override
    @CacheEvict(value = "rooms", allEntries = true)
    public void refreshRoomCache() {
        log.debug("刷新房间缓存");
        // 缓存会在下次访问时自动重新加载
    }
}