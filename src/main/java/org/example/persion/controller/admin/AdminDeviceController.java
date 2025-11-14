package org.example.persion.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.DeviceCreateDTO;
import org.example.persion.dto.DeviceUpdateDTO;
import org.example.persion.service.DeviceService;
import org.example.persion.vo.DeviceInfoVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备管理控制器
 */
@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeviceController {

    private final DeviceService deviceService;

    /**
     * 分页查询设备列表
     */
    @GetMapping
    public Result<Page<DeviceInfoVO>> getDeviceList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String status
    ) {
        Page<DeviceInfoVO> page = deviceService.getDeviceList(current, size, keyword, deviceType, status);
        return Result.success(page);
    }

    /**
     * 根据ID查询设备详情
     */
    @GetMapping("/{id}")
    public Result<DeviceInfoVO> getDeviceById(@PathVariable Long id) {
        DeviceInfoVO device = deviceService.getDeviceById(id);
        return Result.success(device);
    }

    /**
     * 根据设备编号查询
     */
    @GetMapping("/code/{deviceCode}")
    public Result<DeviceInfoVO> getDeviceByCode(@PathVariable String deviceCode) {
        DeviceInfoVO device = deviceService.getDeviceByCode(deviceCode);
        return Result.success(device);
    }

    /**
     * 根据老人ID查询设备列表
     */
    @GetMapping("/elderly/{elderlyId}")
    public Result<List<DeviceInfoVO>> getDevicesByElderlyId(@PathVariable Long elderlyId) {
        List<DeviceInfoVO> devices = deviceService.getDevicesByElderlyId(elderlyId);
        return Result.success(devices);
    }

    /**
     * 创建设备
     */
    @PostMapping
    public Result<Long> createDevice(@Valid @RequestBody DeviceCreateDTO dto) {
        Long deviceId = deviceService.createDevice(dto);
        return Result.success(deviceId, "设备创建成功");
    }

    /**
     * 更新设备
     */
    @PutMapping
    public Result<Void> updateDevice(@Valid @RequestBody DeviceUpdateDTO dto) {
        deviceService.updateDevice(dto);
        return Result.success(null, "设备更新成功");
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return Result.success(null, "设备删除成功");
    }

    /**
     * 绑定设备到老人
     */
    @PutMapping("/{deviceId}/bind/{elderlyId}")
    public Result<Void> bindDeviceToElderly(
            @PathVariable Long deviceId,
            @PathVariable Long elderlyId
    ) {
        deviceService.bindDeviceToElderly(deviceId, elderlyId);
        return Result.success(null, "设备绑定成功");
    }

    /**
     * 解绑设备
     */
    @PutMapping("/{deviceId}/unbind")
    public Result<Void> unbindDevice(@PathVariable Long deviceId) {
        deviceService.unbindDevice(deviceId);
        return Result.success(null, "设备解绑成功");
    }

    /**
     * 更新设备状态
     */
    @PutMapping("/{deviceId}/status")
    public Result<Void> updateDeviceStatus(
            @PathVariable Long deviceId,
            @RequestParam String status
    ) {
        deviceService.updateDeviceStatus(deviceId, status);
        return Result.success(null, "设备状态更新成功");
    }

    /**
     * 获取设备统计信息
     */
    @GetMapping("/statistics")
    public Result<Object> getDeviceStatistics() {
        Object statistics = deviceService.getDeviceStatistics();
        return Result.success(statistics);
    }
}
