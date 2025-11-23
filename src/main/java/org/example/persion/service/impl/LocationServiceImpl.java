package org.example.persion.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.entity.DeviceInfo;
import org.example.persion.entity.LocationData;
import org.example.persion.repository.DeviceInfoMapper;
import org.example.persion.repository.LocationDataMapper;
import org.example.persion.service.LocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.alibaba.fastjson2.JSONArray;

/**
 * 定位服务实现类
 * 使用高德地图IP定位API获取真实位置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationDataMapper locationDataMapper;
    private final DeviceInfoMapper deviceInfoMapper;
    private final RestTemplate restTemplate;

    @Value("${amap.api.key:}")
    private String amapApiKey;
    
    @Value("${location.api.cache.minutes:60}")
    private int cacheMinutes;
    
    // 缓存最近一次获取的定位数据，避免频繁调用API
    private LocationData cachedLocation = null;
    private LocalDateTime cacheTime = null;
    
    // API调用统计
    private int apiCallCount = 0;
    private LocalDateTime lastApiCallTime = null;

    /**
     * 获取真实定位信息（基于高德地图IP定位API）
     * 带缓存机制，避免频繁调用API导致超限
     */
    @Override
    public LocationData getRealLocation() {
        // 检查API Key配置
        if (amapApiKey == null || amapApiKey.isEmpty()) {
            log.warn("高德地图API Key未配置，无法获取真实定位");
            return null;
        }
        
        // 检查缓存，如果缓存有效则直接返回
        if (cachedLocation != null && cacheTime != null) {
            long minutesSinceCache = java.time.Duration.between(cacheTime, LocalDateTime.now()).toMinutes();
            if (minutesSinceCache < cacheMinutes) {
                log.debug("使用缓存的定位数据（缓存时间: {}分钟前，有效期: {}分钟）", 
                    minutesSinceCache, cacheMinutes);
                return createCachedLocationCopy(cachedLocation);
            } else {
                log.debug("缓存已过期（{}分钟），需要重新调用API", minutesSinceCache);
            }
        }
        
        // 记录API Key的前几位（用于调试，不暴露完整key）
        String keyPrefix = amapApiKey.length() > 8 ? amapApiKey.substring(0, 8) + "..." : "***";
        log.debug("使用高德地图API Key: {}", keyPrefix);
        
        // 记录API调用
        apiCallCount++;
        lastApiCallTime = LocalDateTime.now();
        log.info("调用高德地图API（第{}次调用，上次调用: {}）", 
            apiCallCount, 
            lastApiCallTime != null ? lastApiCallTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME) : "首次");

        try {
            // 使用高德地图IP定位API
            // 文档：https://lbs.amap.com/api/webservice/guide/api/ipconfig
            String url = String.format(
                "https://restapi.amap.com/v3/ip?key=%s",
                amapApiKey
            );

            log.debug("调用高德地图API: {}", url.replace(amapApiKey, keyPrefix));
            
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.error("高德地图API返回空响应");
                return null;
            }

            log.debug("高德地图API响应: {}", response);
            
            JSONObject json = JSON.parseObject(response);
            String status = json.getString("status");
            if (status == null || !"1".equals(status)) {
                Object infoObj = json.get("info");
                String info = infoObj != null ? infoObj.toString() : "未知错误";
                String infocode = json.getString("infocode");
                
                // 针对常见错误提供详细提示
                String errorDetail = "";
                if ("USERKEY_PLAT_NOMATCH".equals(info) || "10009".equals(infocode)) {
                    errorDetail = "\n⚠️ 错误原因：API Key平台类型不匹配！\n" +
                        "当前使用的是后端服务API，但您的Key可能是\"Web端(JS API)\"类型。\n" +
                        "解决方法：\n" +
                        "1. 访问 https://console.amap.com/dev/key/app\n" +
                        "2. 在您的应用下，点击「添加」按钮\n" +
                        "3. 服务平台选择「Web服务」（不是\"Web端(JS API)\"）\n" +
                        "4. 获取新的Key并更新到 application.properties\n" +
                        "5. 重启应用";
                } else if ("INVALID_USER_KEY".equals(info) || "10001".equals(infocode)) {
                    errorDetail = "\n⚠️ 错误原因：API Key无效！\n" +
                        "请检查 application.properties 中的 amap.api.key 配置是否正确";
                } else if ("DAILY_QUERY_OVER_LIMIT".equals(info) || "10003".equals(infocode)) {
                    errorDetail = "\n⚠️ 错误原因：API调用次数超限！\n" +
                        "请检查高德地图控制台中的API使用量";
                }
                
                log.error("高德地图API调用失败 - status: {}, info: {}, infocode: {}, 完整响应: {}{}", 
                    status, info, infocode, response, errorDetail);
                return null;
            }

            // 解析返回的经纬度
            // rectangle可能是字符串或数组，需要分别处理
            Object rectangleObj = json.get("rectangle");
            String locationStr = null;
            
            if (rectangleObj == null) {
                log.warn("高德地图API未返回rectangle字段，完整响应: {}", response);
                return null;
            }
            
            // 判断是数组还是字符串
            if (rectangleObj instanceof com.alibaba.fastjson2.JSONArray) {
                com.alibaba.fastjson2.JSONArray rectangleArray = (com.alibaba.fastjson2.JSONArray) rectangleObj;
                if (rectangleArray.isEmpty()) {
                    // 检查是否所有字段都为空
                    String province = parseStringOrArray(json.get("province"));
                    String city = parseStringOrArray(json.get("city"));
                    String adcode = parseStringOrArray(json.get("adcode"));
                    
                    log.warn("高德地图IP定位API返回空数据，这是正常现象（在某些网络环境下）：\n" +
                        "可能原因：\n" +
                        "1. 服务器在内网环境（如：192.168.x.x, 10.x.x.x, 172.16.x.x等）\n" +
                        "2. 使用了VPN或代理服务器\n" +
                        "3. 服务器IP地址无法被高德地图识别\n" +
                        "4. 高德地图IP定位服务暂时无法获取该IP的位置信息\n\n" +
                        "解决方案：\n" +
                        "1. 如果服务器在内网，IP定位无法获取位置是正常的\n" +
                        "2. 建议使用GPS定位设备或手动设置位置\n" +
                        "3. 或者将服务器部署到公网环境\n" +
                        "4. 检查是否使用了VPN，可以尝试关闭VPN后重试\n\n" +
                        "当前返回数据: province={}, city={}, adcode={}, rectangle=[]\n" +
                        "完整响应: {}", province, city, adcode, response);
                    return null;
                }
                // 如果数组不为空，取第一个元素
                locationStr = rectangleArray.getString(0);
            } else if (rectangleObj instanceof String) {
                locationStr = (String) rectangleObj;
            } else {
                locationStr = rectangleObj.toString();
            }
            
            if (locationStr == null || locationStr.isEmpty() || "[]".equals(locationStr)) {
                log.warn("高德地图API未返回有效的位置信息，完整响应: {}", response);
                return null;
            }

            // rectangle格式: "116.397128,39.916527;116.397128,39.916527"
            // 取第一个坐标点
            String[] locations = locationStr.split(";");
            if (locations.length == 0) {
                log.error("无法解析位置坐标，rectangle格式错误: {}", locationStr);
                return null;
            }

            String[] coords = locations[0].split(",");
            if (coords.length < 2) {
                log.error("无法解析经纬度，坐标格式错误: {}", locations[0]);
                return null;
            }

            double longitude = Double.parseDouble(coords[0]);
            double latitude = Double.parseDouble(coords[1]);
            
            // 解析省市区信息（也可能是数组）
            String province = parseStringOrArray(json.get("province"));
            String city = parseStringOrArray(json.get("city"));
            String address = (province != null ? province : "") + (city != null ? city : "");

            // 创建定位数据对象
            LocationData locationData = new LocationData();
            locationData.setLongitude(BigDecimal.valueOf(longitude));
            locationData.setLatitude(BigDecimal.valueOf(latitude));
            locationData.setAddress(address);
            locationData.setDeviceType("定位设备");
            locationData.setDeviceStatus("在线");
            locationData.setLocationTime(LocalDateTime.now());
            locationData.setSyncTime(LocalDateTime.now());

            // 更新缓存
            cachedLocation = createCachedLocationCopy(locationData);
            cacheTime = LocalDateTime.now();
            
            log.info("成功获取真实定位: 经度={}, 纬度={}, 地址={}（已缓存，有效期{}分钟）", 
                longitude, latitude, address, cacheMinutes);
            return locationData;

        } catch (org.springframework.web.client.RestClientException e) {
            log.error("调用高德地图API网络异常: {}", e.getMessage(), e);
            return null;
        } catch (NumberFormatException e) {
            log.error("解析高德地图API返回的坐标格式错误: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("获取真实定位失败，异常类型: {}, 异常信息: {}", 
                e.getClass().getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 为指定老人更新定位数据
     */
    @Override
    public boolean updateLocationForElderly(Long elderlyId, String deviceId) {
        // 获取真实定位
        LocationData realLocation = getRealLocation();
        if (realLocation == null) {
            log.warn("无法获取真实定位，跳过更新");
            return false;
        }

        // 设置老人ID和设备ID
        realLocation.setElderlyId(elderlyId);
        if (deviceId != null) {
            realLocation.setDeviceId(deviceId);
        }

        // 保存到数据库
        try {
            locationDataMapper.insert(realLocation);
            log.info("成功为老人ID {} 更新定位数据", elderlyId);
            return true;
        } catch (Exception e) {
            log.error("保存定位数据失败", e);
            return false;
        }
    }

    /**
     * 为所有绑定定位设备的老人更新位置
     */
    @Override
    public int updateAllElderlyLocations() {
        // 查询所有绑定定位设备的老人
        List<DeviceInfo> devices = deviceInfoMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeviceInfo>()
                .eq(DeviceInfo::getDeviceType, "定位设备")
                .isNotNull(DeviceInfo::getElderlyId)
                .eq(DeviceInfo::getDeleted, 0)
        );

        if (devices.isEmpty()) {
            log.info("未找到绑定定位设备的老人");
            return 0;
        }

        // 获取一次真实定位（所有设备共享同一个位置，实际场景中每个设备应该有自己的位置）
        LocationData realLocation = getRealLocation();
        if (realLocation == null) {
            log.warn("无法获取真实定位，跳过批量更新");
            return 0;
        }

        int successCount = 0;
        for (DeviceInfo device : devices) {
            try {
                LocationData locationData = new LocationData();
                locationData.setElderlyId(device.getElderlyId());
                locationData.setDeviceId(device.getDeviceCode());
                locationData.setLongitude(realLocation.getLongitude());
                locationData.setLatitude(realLocation.getLatitude());
                locationData.setAddress(realLocation.getAddress());
                locationData.setDeviceType(device.getDeviceName());
                locationData.setDeviceStatus(device.getStatus());
                locationData.setLocationTime(LocalDateTime.now());
                locationData.setSyncTime(LocalDateTime.now());

                locationDataMapper.insert(locationData);
                
                // 更新设备最后同步时间
                device.setLastSyncTime(LocalDateTime.now());
                deviceInfoMapper.updateById(device);
                
                successCount++;
                log.info("成功为老人ID {} 更新定位数据", device.getElderlyId());
            } catch (Exception e) {
                log.error("为老人ID {} 更新定位数据失败", device.getElderlyId(), e);
            }
        }

            log.info("批量更新定位数据完成，成功: {}/{}", successCount, devices.size());
        return successCount;
    }
    
    /**
     * 解析可能是字符串或数组的字段
     * @param obj 可能是String或JSONArray的对象
     * @return 字符串值，如果是空数组则返回null
     */
    private String parseStringOrArray(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof JSONArray) {
            JSONArray array = (JSONArray) obj;
            if (array.isEmpty()) {
                return null;
            }
            // 取第一个元素
            return array.getString(0);
        } else if (obj instanceof String) {
            String str = (String) obj;
            return str.isEmpty() || "[]".equals(str) ? null : str;
        } else {
            String str = obj.toString();
            return str.isEmpty() || "[]".equals(str) ? null : str;
        }
    }
    
    /**
     * 创建定位数据的副本（用于缓存）
     */
    private LocationData createCachedLocationCopy(LocationData original) {
        LocationData copy = new LocationData();
        copy.setLongitude(original.getLongitude());
        copy.setLatitude(original.getLatitude());
        copy.setAddress(original.getAddress());
        copy.setDeviceType(original.getDeviceType());
        copy.setDeviceStatus(original.getDeviceStatus());
        copy.setLocationTime(original.getLocationTime());
        copy.setSyncTime(original.getSyncTime());
        return copy;
    }
    
    /**
     * 获取API调用统计信息（用于监控）
     */
    public String getApiCallStats() {
        return String.format("API调用统计: 总次数=%d, 上次调用=%s, 缓存有效期=%d分钟", 
            apiCallCount,
            lastApiCallTime != null ? lastApiCallTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME) : "无",
            cacheMinutes);
    }
}

