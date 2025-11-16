package org.example.persion.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.persion.entity.Room;

import java.util.List;

/**
 * 房间管理服务接口
 */
public interface RoomService {

    /**
     * 分页查询房间列表
     */
    Page<Room> getRoomList(int current, int size, String keyword, String roomType, String status);

    /**
     * 查询所有可用房间
     */
    List<Room> getAvailableRooms();

    /**
     * 根据ID查询房间详情
     */
    Room getRoomById(Long id);

    /**
     * 根据房间号查询
     */
    Room getRoomByNumber(String roomNumber);

    /**
     * 创建房间
     */
    Room createRoom(Room room);

    /**
     * 更新房间信息
     */
    Room updateRoom(Long id, Room room);

    /**
     * 删除房间
     */
    void deleteRoom(Long id);

    /**
     * 更新房间占用情况
     * @param roomId 房间ID
     * @param increment 床位变化数量(正数表示增加占用,负数表示减少占用)
     */
    void updateOccupiedBeds(Long roomId, int increment);

    /**
     * 查询按楼层分组的房间统计
     */
    List<Room> getRoomsByFloor(Integer floor);
}
