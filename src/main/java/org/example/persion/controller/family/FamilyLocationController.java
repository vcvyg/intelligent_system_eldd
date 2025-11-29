package org.example.persion.controller.family;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.entity.Geofence;
import org.example.persion.entity.GeofenceAlert;
import org.example.persion.entity.LocationData;
import org.example.persion.entity.DeviceInfo;
import org.example.persion.repository.DeviceInfoMapper;
import org.example.persion.repository.GeofenceAlertMapper;
import org.example.persion.repository.GeofenceMapper;
import org.example.persion.repository.LocationDataMapper;
import org.example.persion.security.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 子女端 - GPS定位控制器
 */
@RestController
@RequestMapping("/api/family/location")
@RequiredArgsConstructor
public class FamilyLocationController {

    private final LocationDataMapper locationDataMapper;
    private final GeofenceMapper geofenceMapper;
    private final GeofenceAlertMapper geofenceAlertMapper;
    private final org.example.persion.service.LocationService locationService;
    private final DeviceInfoMapper deviceInfoMapper;
    private final org.example.persion.service.ApiCallLimitService apiCallLimitService;

    /**
     * 获取指定老人的当前位置
     */
    @GetMapping("/{elderlyId}")
    public Result<LocationDataVO> getLocation(@PathVariable Long elderlyId) {
        // 从数据库获取最新的定位数据
        LocationData locationData = locationDataMapper.selectLatestByElderlyId(elderlyId);
        
        // 如果没有定位数据，检查是否有绑定设备，如果有则自动获取一次
        if (locationData == null) {
            // 检查是否有绑定定位设备
            DeviceInfo device = deviceInfoMapper.selectOne(
                new LambdaQueryWrapper<DeviceInfo>()
                    .eq(DeviceInfo::getElderlyId, elderlyId)
                    .eq(DeviceInfo::getDeviceType, "定位设备")
                    .eq(DeviceInfo::getDeleted, 0)
                    .orderByDesc(DeviceInfo::getId)
            );
            
            if (device != null) {
                // 有绑定设备，尝试自动获取定位数据
                String deviceCode = device.getDeviceCode();
                boolean updated = locationService.updateLocationForElderly(elderlyId, deviceCode);
                
                if (updated) {
                    // 重新查询定位数据
                    locationData = locationDataMapper.selectLatestByElderlyId(elderlyId);
                } else {
                    // 获取失败，返回详细错误提示
                    // 注意：IP定位在某些网络环境下（内网、VPN等）无法获取位置是正常现象
                    String errorMsg = "⚠️ 无法通过IP定位获取位置信息\n\n" +
                        "可能的原因：\n" +
                        "1. 服务器在内网环境（192.168.x.x, 10.x.x.x等）- 这是正常现象\n" +
                        "2. 使用了VPN或代理服务器\n" +
                        "3. 高德地图API Key未配置或配置错误\n" +
                        "4. API Key无效或已过期\n" +
                        "5. 网络连接问题\n\n" +
                        "解决方案：\n" +
                        "• 如果服务器在内网，IP定位无法获取位置是正常的\n" +
                        "• 建议使用GPS定位设备或手动上传位置信息\n" +
                        "• 可以使用测试接口手动上传位置：POST /api/family/location/test/upload\n" +
                        "• 检查 application.properties 中的 amap.api.key 配置\n" +
                        "• 查看服务器日志获取详细错误信息";
                    return Result.error(errorMsg);
                }
            } else {
                // 没有绑定设备
                return Result.error("该老人未绑定定位设备，请先在设备管理中绑定定位设备");
            }
        }
        
        // 如果还是没有数据，返回错误
        if (locationData == null) {
            return Result.error("暂无定位数据，请确保定位设备已连接并上传位置信息");
        }
        
        LocationDataVO location = new LocationDataVO();
        location.setLongitude(locationData.getLongitude() != null ? locationData.getLongitude().doubleValue() : null);
        location.setLatitude(locationData.getLatitude() != null ? locationData.getLatitude().doubleValue() : null);
        location.setAddress(locationData.getAddress());
        location.setUpdateTime(locationData.getLocationTime());
        location.setDeviceStatus(locationData.getDeviceStatus());
        location.setDeviceType(locationData.getDeviceType());
        location.setLastSync(locationData.getSyncTime());
        
        return Result.success(location);
    }

    /**
     * 获取指定老人的电子围栏列表
     */
    @GetMapping("/geofence/{elderlyId}")
    public Result<List<GeofenceVO>> getGeofenceList(@PathVariable Long elderlyId) {
        // 从数据库获取围栏数据
        LambdaQueryWrapper<Geofence> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Geofence::getElderlyId, elderlyId)
                    .eq(Geofence::getDeleted, 0)
                    .orderByDesc(Geofence::getCreateTime);
        
        List<Geofence> geofences = geofenceMapper.selectList(queryWrapper);
        
        List<GeofenceVO> result = geofences.stream().map(g -> {
            GeofenceVO vo = new GeofenceVO();
            vo.setId(g.getId());
            vo.setName(g.getName());
            vo.setLatitude(g.getLatitude() != null ? g.getLatitude().doubleValue() : null);
            vo.setLongitude(g.getLongitude() != null ? g.getLongitude().doubleValue() : null);
            vo.setRadius(g.getRadius());
            vo.setStatus(g.getStatus());
            return vo;
        }).collect(Collectors.toList());
        
        return Result.success(result);
    }

    /**
     * 添加电子围栏
     */
    @PostMapping("/geofence")
    public Result<Void> addGeofence(@RequestBody GeofenceCreateDTO dto) {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        // 验证数据有效性
        if (dto.getLatitude() < -90 || dto.getLatitude() > 90) {
            return Result.error("纬度范围应在 -90 到 90 之间");
        }
        if (dto.getLongitude() < -180 || dto.getLongitude() > 180) {
            return Result.error("经度范围应在 -180 到 180 之间");
        }
        if (dto.getRadius() < 10 || dto.getRadius() > 10000) {
            return Result.error("围栏半径应在 10 到 10000 米之间");
        }
        
        // 保存围栏到数据库
        Geofence geofence = new Geofence();
        geofence.setElderlyId(dto.getElderlyId());
        geofence.setName(dto.getName());
        geofence.setLatitude(BigDecimal.valueOf(dto.getLatitude()));
        geofence.setLongitude(BigDecimal.valueOf(dto.getLongitude()));
        geofence.setRadius(dto.getRadius());
        geofence.setStatus("启用");
        geofence.setCreateUserId(userId);
        
        int result = geofenceMapper.insert(geofence);
        if (result > 0) {
            return Result.success(null);
        } else {
            return Result.error("保存围栏失败");
        }
    }

    /**
     * 更新电子围栏
     */
    @PutMapping("/geofence/{id}")
    public Result<Void> updateGeofence(@PathVariable Long id, @RequestBody GeofenceUpdateDTO dto) {
        // 验证数据有效性
        if (dto.getLatitude() != null && (dto.getLatitude() < -90 || dto.getLatitude() > 90)) {
            return Result.error("纬度范围应在 -90 到 90 之间");
        }
        if (dto.getLongitude() != null && (dto.getLongitude() < -180 || dto.getLongitude() > 180)) {
            return Result.error("经度范围应在 -180 到 180 之间");
        }
        if (dto.getRadius() != null && (dto.getRadius() < 10 || dto.getRadius() > 10000)) {
            return Result.error("围栏半径应在 10 到 10000 米之间");
        }
        
        // 查询围栏是否存在
        Geofence geofence = geofenceMapper.selectById(id);
        if (geofence == null) {
            return Result.error("围栏不存在");
        }
        
        // 更新围栏信息
        if (dto.getName() != null) {
            geofence.setName(dto.getName());
        }
        if (dto.getLatitude() != null) {
            geofence.setLatitude(BigDecimal.valueOf(dto.getLatitude()));
        }
        if (dto.getLongitude() != null) {
            geofence.setLongitude(BigDecimal.valueOf(dto.getLongitude()));
        }
        if (dto.getRadius() != null) {
            geofence.setRadius(dto.getRadius());
        }
        if (dto.getStatus() != null) {
            geofence.setStatus(dto.getStatus());
        }
        
        int result = geofenceMapper.updateById(geofence);
        if (result > 0) {
            return Result.success(null);
        } else {
            return Result.error("更新围栏失败");
        }
    }

    /**
     * 删除电子围栏
     */
    @DeleteMapping("/geofence/{id}")
    public Result<Void> deleteGeofence(@PathVariable Long id) {
        // 逻辑删除围栏
        Geofence geofence = geofenceMapper.selectById(id);
        if (geofence == null) {
            return Result.error("围栏不存在");
        }
        
        geofence.setDeleted(1);
        int result = geofenceMapper.updateById(geofence);
        if (result > 0) {
            return Result.success(null);
        } else {
            return Result.error("删除围栏失败");
        }
    }

    /**
     * 获取围栏警报历史
     */
    @GetMapping("/alerts/{elderlyId}")
    public Result<List<AlertVO>> getAlertHistory(@PathVariable Long elderlyId) {
        // 从数据库获取警报数据（最多50条）
        LambdaQueryWrapper<GeofenceAlert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GeofenceAlert::getElderlyId, elderlyId)
                    .eq(GeofenceAlert::getDeleted, 0)
                    .orderByDesc(GeofenceAlert::getAlertTime);
        
        List<GeofenceAlert> allAlerts = geofenceAlertMapper.selectList(queryWrapper);
        // 限制返回最近50条
        List<GeofenceAlert> alerts = allAlerts.stream()
                .limit(50)
                .collect(Collectors.toList());
        
        // 查询围栏名称并构建警报内容
        List<AlertVO> result = new ArrayList<>();
        for (GeofenceAlert alert : alerts) {
            Geofence geofence = geofenceMapper.selectById(alert.getGeofenceId());
            String geofenceName = geofence != null ? geofence.getName() : "未知围栏";
            
            AlertVO vo = new AlertVO();
            vo.setTime(alert.getAlertTime());
            if ("进入".equals(alert.getAlertType())) {
                vo.setContent("老人进入了\"" + geofenceName + "\"围栏范围");
            } else if ("离开".equals(alert.getAlertType())) {
                vo.setContent("老人离开了\"" + geofenceName + "\"围栏范围");
            } else {
                vo.setContent("围栏警报：" + alert.getAlertType());
            }
            result.add(vo);
        }
        
        return Result.success(result);
    }

    /**
     * 手动触发位置更新（使用真实定位API）
     * 为指定老人更新真实位置信息
     */
    @PostMapping("/update/{elderlyId}")
    public Result<Void> updateLocation(@PathVariable Long elderlyId) {
        boolean success = locationService.updateLocationForElderly(elderlyId, null);
        if (success) {
            return Result.success(null);
        } else {
            return Result.error("更新位置失败。如果服务器在内网环境，IP定位无法获取位置是正常现象。建议使用GPS定位设备或手动上传位置信息。");
        }
    }

    /**
     * 手动触发所有老人的位置更新
     */
    @PostMapping("/update/all")
    public Result<Integer> updateAllLocations() {
        int count = locationService.updateAllElderlyLocations();
        return Result.success(count);
    }

    /**
     * 测试高德地图API配置
     * 用于验证API Key是否有效
     */
    @GetMapping("/test/api")
    public Result<String> testAmapApi() {
        try {
            LocationData location = locationService.getRealLocation();
            if (location != null) {
                return Result.success(String.format(
                    "API配置正常！\n" +
                    "经度: %s\n" +
                    "纬度: %s\n" +
                    "地址: %s",
                    location.getLongitude(),
                    location.getLatitude(),
                    location.getAddress() != null ? location.getAddress() : "未知"
                ));
            } else {
                return Result.error("API调用失败，请查看服务器日志获取详细错误信息");
            }
        } catch (Exception e) {
            return Result.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取API调用统计信息
     */
    @GetMapping("/api/stats")
    public Result<String> getApiStats() {
        try {
            String stats = apiCallLimitService.getCallStats();
            int remainingDaily = apiCallLimitService.getRemainingDailyCalls();
            int remainingHourly = apiCallLimitService.getRemainingHourlyCalls();
            
            String result = String.format("%s\n剩余调用次数 - 今日：%d, 本小时：%d", 
                stats, remainingDaily, remainingHourly);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 测试接口：上传定位数据（用于测试）
     * 实际生产环境中，这个接口应该由定位设备或手机APP调用
     */
    @PostMapping("/test/upload")
    public Result<Void> uploadLocation(@RequestBody LocationUploadDTO dto) {
        // 验证数据
        if (dto.getElderlyId() == null) {
            return Result.error("老人ID不能为空");
        }
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            return Result.error("经纬度不能为空");
        }
        if (dto.getLatitude() < -90 || dto.getLatitude() > 90) {
            return Result.error("纬度范围应在 -90 到 90 之间");
        }
        if (dto.getLongitude() < -180 || dto.getLongitude() > 180) {
            return Result.error("经度范围应在 -180 到 180 之间");
        }
        
        // 保存定位数据
        LocationData locationData = new LocationData();
        locationData.setElderlyId(dto.getElderlyId());
        locationData.setLatitude(BigDecimal.valueOf(dto.getLatitude()));
        locationData.setLongitude(BigDecimal.valueOf(dto.getLongitude()));
        locationData.setAddress(dto.getAddress());
        locationData.setDeviceId(dto.getDeviceId());
        locationData.setDeviceType(dto.getDeviceType() != null ? dto.getDeviceType() : "定位手环");
        locationData.setDeviceStatus(dto.getDeviceStatus() != null ? dto.getDeviceStatus() : "在线");
        locationData.setLocationTime(dto.getLocationTime() != null ? dto.getLocationTime() : LocalDateTime.now());
        locationData.setSyncTime(LocalDateTime.now());
        
        int result = locationDataMapper.insert(locationData);
        if (result > 0) {
            return Result.success(null);
        } else {
            return Result.error("保存定位数据失败");
        }
    }

    @Data
    public static class LocationDataVO {
        private Double longitude;
        private Double latitude;
        private String address;
        private LocalDateTime updateTime;
        private String deviceStatus;
        private String deviceType;
        private LocalDateTime lastSync;
    }

    @Data
    public static class GeofenceVO {
        private Long id;
        private String name;
        private Double latitude;
        private Double longitude;
        private Integer radius;
        private String status;
    }

    @Data
    public static class GeofenceCreateDTO {
        private Long elderlyId;
        private String name;
        private Double latitude;
        private Double longitude;
        private Integer radius;
    }

    @Data
    public static class GeofenceUpdateDTO {
        private String name;
        private Double latitude;
        private Double longitude;
        private Integer radius;
        private String status;
    }

    @Data
    public static class AlertVO {
        private LocalDateTime time;
        private String content;
    }

    @Data
    public static class LocationUploadDTO {
        private Long elderlyId;
        private Double latitude;
        private Double longitude;
        private String address;
        private String deviceId;
        private String deviceType;
        private String deviceStatus;
        private LocalDateTime locationTime;
    }
}

