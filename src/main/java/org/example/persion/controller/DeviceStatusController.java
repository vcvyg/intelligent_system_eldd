package org.example.persion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.DeviceStatusReportDTO;
import org.example.persion.service.DeviceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接收设备状态上报的控制器
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceStatusController {

    private final DeviceService deviceService;

    /**
     * 接收来自IoT设备的状态报告
     * @param report 包含设备ID和事件类型的数据
     * @return 操作结果
     */
    @PostMapping("/report-status")
    public Result<?> reportStatus(@Valid @RequestBody DeviceStatusReportDTO report) {
        deviceService.handleDeviceStatusReport(report);
        return Result.success("状态已接收");
    }
}
