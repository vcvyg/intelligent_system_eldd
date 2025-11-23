package org.example.persion.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.service.LocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定位数据更新定时任务
 * 定期为所有绑定定位设备的老人更新位置信息
 * 
 * 注意：为避免API调用超限，建议：
 * 1. 设置 location.task.enabled=false 禁用定时任务（如果不需要自动更新）
 * 2. 设置 location.task.interval=30 或更大值，降低调用频率
 * 3. 启用 location.api.cache.minutes 缓存，避免重复调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "location.task.enabled", havingValue = "true", matchIfMissing = false)
public class LocationUpdateTask {

    private final LocationService locationService;

    @Value("${location.task.interval:30}")
    private int intervalMinutes;

    /**
     * 定时更新定位数据
     * 默认每30分钟更新一次，可通过 location.task.interval 配置
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "${location.task.cron:0 */30 * * * ?}")
    public void updateLocations() {
        log.info("开始执行定时定位数据更新任务（间隔: {}分钟）...", intervalMinutes);
        try {
            int count = locationService.updateAllElderlyLocations();
            log.info("定时定位数据更新完成，共更新 {} 条记录", count);
        } catch (Exception e) {
            log.error("定时定位数据更新失败", e);
        }
    }

    /**
     * 系统启动后延迟执行一次（可选）
     * 默认禁用，如需启用请设置 location.task.initial.enabled=true
     */
    @ConditionalOnProperty(name = "location.task.initial.enabled", havingValue = "true", matchIfMissing = false)
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    public void initialUpdate() {
        log.info("系统启动后首次定位数据更新（延迟60秒）...");
        try {
            int count = locationService.updateAllElderlyLocations();
            log.info("首次定位数据更新完成，共更新 {} 条记录", count);
        } catch (Exception e) {
            log.error("首次定位数据更新失败", e);
        }
    }
}

