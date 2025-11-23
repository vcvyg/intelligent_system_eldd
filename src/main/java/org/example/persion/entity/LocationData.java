package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定位数据实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("location_data")
public class LocationData extends BaseEntity {

    private Long elderlyId; // 老人ID

    private BigDecimal longitude; // 经度

    private BigDecimal latitude; // 纬度

    private String address; // 地址（通过逆地理编码获取）

    private BigDecimal accuracy; // 定位精度（米）

    private BigDecimal altitude; // 海拔（米）

    private BigDecimal speed; // 速度（米/秒）

    private BigDecimal heading; // 方向角（度）

    private String deviceId; // 设备ID

    private String deviceType; // 设备类型：定位手环/GPS设备/手机

    private String deviceStatus; // 设备状态：在线/离线

    private LocalDateTime locationTime; // 定位时间

    private LocalDateTime syncTime; // 同步到服务器的时间
}

