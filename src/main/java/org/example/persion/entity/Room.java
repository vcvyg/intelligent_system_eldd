package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房间信息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room")
public class Room extends BaseEntity {

    private String roomNumber; // 房间号

    private Integer floor; // 楼层

    private String roomType; // 房间类型: 单人间/双人间/三人间/VIP房

    private Integer bedCount; // 床位数

    private Integer occupiedBeds; // 已占用床位数

    private String status; // 状态: 可用/已满/维护中

    private String facilities; // 设施说明

    private String remark; // 备注
}