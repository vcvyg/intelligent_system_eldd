package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 电子围栏实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("geofence")
public class Geofence extends BaseEntity {

    private Long elderlyId; // 老人ID

    private String name; // 围栏名称

    private BigDecimal longitude; // 中心点经度

    private BigDecimal latitude; // 中心点纬度

    private Integer radius; // 半径（米）

    private String status; // 状态：启用/禁用

    private Long createUserId; // 创建人ID
}

