package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.Room;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.RoomMapper;
import org.example.persion.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 房间管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomMapper roomMapper;
    private final ElderlyInfoMapper elderlyInfoMapper;

    @Override
    public Page<Room> getRoomList(int current, int size, String keyword, String roomType, String status) {
        Page<Room> page = new Page<>(current, size);
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索(房间号或设施)
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Room::getRoomNumber, keyword)
                    .or().like(Room::getFacilities, keyword));
        }

        // 房间类型筛选
        if (StringUtils.hasText(roomType)) {
            wrapper.eq(Room::getRoomType, roomType);
        }

        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(Room::getStatus, status);
        }

        // 按楼层、房间号排序
        wrapper.orderByAsc(Room::getFloor, Room::getRoomNumber);

        Page<Room> roomPage = roomMapper.selectPage(page, wrapper);

        // 动态计算入住人数
        if (!roomPage.getRecords().isEmpty()) {
            List<Long> roomIds = roomPage.getRecords().stream().map(Room::getId).collect(Collectors.toList());
            Map<Long, Long> occupancyMap = getRoomOccupancyMap(roomIds);
            roomPage.getRecords().forEach(room -> room.setOccupiedBeds(occupancyMap.getOrDefault(room.getId(), 0L).intValue()));
        }

        return roomPage;
    }

    @Override
    public List<Room> getAvailableRooms() {
        // 1. 获取所有房间
        List<Room> allRooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().orderByAsc(Room::getFloor, Room::getRoomNumber));

        // 2. 动态计算所有房间的入住人数
        if (!allRooms.isEmpty()) {
            List<Long> roomIds = allRooms.stream().map(Room::getId).collect(Collectors.toList());
            Map<Long, Long> occupancyMap = getRoomOccupancyMap(roomIds);
            allRooms.forEach(room -> room.setOccupiedBeds(occupancyMap.getOrDefault(room.getId(), 0L).intValue()));
        }

        return allRooms;
    }

    /**
     * 根据房间ID列表获取入住人数映射
     */
    private Map<Long, Long> getRoomOccupancyMap(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        LambdaQueryWrapper<ElderlyInfo> elderlyWrapper = new LambdaQueryWrapper<>();
        elderlyWrapper.in(ElderlyInfo::getRoomId, roomIds);
        List<ElderlyInfo> elderlyList = elderlyInfoMapper.selectList(elderlyWrapper);

        return elderlyList.stream()
                .collect(Collectors.groupingBy(ElderlyInfo::getRoomId, Collectors.counting()));
    }

    @Override
    public Room getRoomById(Long id) {
        return roomMapper.selectById(id);
    }

    @Override
    public Room getRoomByNumber(String roomNumber) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Room::getRoomNumber, roomNumber);
        return roomMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Room createRoom(Room room) {
        // 检查房间号是否已存在
        Room existingRoom = getRoomByNumber(room.getRoomNumber());
        if (existingRoom != null) {
            throw new RuntimeException("房间号已存在: " + room.getRoomNumber());
        }

        // 设置默认值
        if (room.getOccupiedBeds() == null) {
            room.setOccupiedBeds(0);
        }
        if (room.getStatus() == null) {
            room.setStatus("可用");
        }

        roomMapper.insert(room);
        log.info("创建房间成功: {}", room.getRoomNumber());
        return room;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Room updateRoom(Long id, Room room) {
        Room existingRoom = getRoomById(id);
        if (existingRoom == null) {
            throw new RuntimeException("房间不存在: " + id);
        }

        room.setId(id);
        roomMapper.updateById(room);
        log.info("更新房间成功: {}", id);
        return getRoomById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        if (room == null) {
            throw new RuntimeException("房间不存在: " + id);
        }

        if (room.getOccupiedBeds() > 0) {
            throw new RuntimeException("房间还有老人居住，无法删除");
        }

        roomMapper.deleteById(id);
        log.info("删除房间成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOccupiedBeds(Long roomId, int increment) {
        Room room = getRoomById(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在: " + roomId);
        }

        int newOccupiedBeds = room.getOccupiedBeds() + increment;

        // 检查床位数是否合法
        if (newOccupiedBeds < 0) {
            throw new RuntimeException("占用床位数不能为负数");
        }
        if (newOccupiedBeds > room.getBedCount()) {
            throw new RuntimeException("占用床位数超过总床位数");
        }

        room.setOccupiedBeds(newOccupiedBeds);

        // 自动更新房间状态
        if (newOccupiedBeds >= room.getBedCount()) {
            room.setStatus("已满");
        } else if ("已满".equals(room.getStatus())) {
            room.setStatus("可用");
        }

        roomMapper.updateById(room);
        log.info("更新房间{}占用床位: {} -> {}", roomId, room.getOccupiedBeds() - increment, newOccupiedBeds);
    }

    @Override
    public List<Room> getRoomsByFloor(Integer floor) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Room::getFloor, floor)
                .orderByAsc(Room::getRoomNumber);
        return roomMapper.selectList(wrapper);
    }
}
