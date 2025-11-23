package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 围栏警报记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("geofence_alert")
public class GeofenceAlert extends BaseEntity {

    private Long elderlyId; // 老人ID

    private Long geofenceId; // 围栏ID

    private String alertType; // 警报类型：进入/离开

    private BigDecimal longitude; // 触发时的经度

    private BigDecimal latitude; // 触发时的纬度

    private String address; // 触发时的地址

    private LocalDateTime alertTime; // 警报时间

    private Integer isRead; // 是否已读：0-未读 1-已读

    private LocalDateTime readTime; // 阅读时间

    // 排除 updateTime 字段，因为 geofence_alert 表中没有 update_time 列
    // 警报记录是历史记录，不需要更新时间
    @TableField(exist = false)
    private LocalDateTime updateTime;
}

