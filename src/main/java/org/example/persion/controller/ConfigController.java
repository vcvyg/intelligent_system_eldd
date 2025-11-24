package org.example.persion.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置控制器 - 提供前端需要的配置信息
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    @Value("${amap.api.key:YOUR_AMAP_KEY}")
    private String amapApiKey;

    /**
     * 获取高德地图API Key
     */
    @GetMapping("/amap-key")
    public Result<MapConfigVO> getAmapKey() {
        MapConfigVO config = new MapConfigVO();
        config.setApiKey(amapApiKey);
        return Result.success(config);
    }

    @Data
    public static class MapConfigVO {
        private String apiKey;
    }
}

