package org.example.persion.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.entity.Room;
import org.example.persion.service.RoomService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房间管理控制器
 */
@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final RoomService roomService;

    /**
     * 分页查询房间列表
     */
    @GetMapping
    public Result<Page<Room>> getRoomList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String status
    ) {
        Page<Room> page = roomService.getRoomList(current, size, keyword, roomType, status);
        return Result.success(page);
    }

    /**
     * 查询所有可用房间
     */
    @GetMapping("/available")
    public Result<List<Room>> getAvailableRooms() {
        List<Room> rooms = roomService.getAvailableRooms();
        return Result.success(rooms);
    }

    /**
     * 根据ID查询房间详情
     */
    @GetMapping("/{id}")
    public Result<Room> getRoomById(@PathVariable Long id) {
        Room room = roomService.getRoomById(id);
        return Result.success(room);
    }

    /**
     * 根据楼层查询房间
     */
    @GetMapping("/floor/{floor}")
    public Result<List<Room>> getRoomsByFloor(@PathVariable Integer floor) {
        List<Room> rooms = roomService.getRoomsByFloor(floor);
        return Result.success(rooms);
    }

    /**
     * 创建房间
     */
    @PostMapping
    public Result<Room> createRoom(@RequestBody Room room) {
        Room createdRoom = roomService.createRoom(room);
        return Result.success(createdRoom);
    }

    /**
     * 更新房间信息
     */
    @PutMapping("/{id}")
    public Result<Room> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        Room updatedRoom = roomService.updateRoom(id, room);
        return Result.success(updatedRoom);
    }

    /**
     * 删除房间
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return Result.success();
    }
}
