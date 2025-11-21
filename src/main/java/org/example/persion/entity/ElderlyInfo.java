package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * 老人信息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("elderly_info")
public class ElderlyInfo extends BaseEntity {

    private String name;

    private Integer age;

    private String gender;

    private LocalDate birthday;

    private String idCard;

    private String address;

    private String emergencyContact; // 紧急联系人

    private String emergencyPhone; // 紧急联系电话

    private String medicalHistory; // 病史

    private String allergyHistory; // 过敏史

    private Long roomId; // 房间ID

    // 健康数据阈值
    private Integer heartRateHigh; // 心率过高阈值
    private Integer heartRateLow; // 心率过低阈值
    private Integer systolicPressureHigh; // 收缩压过高阈值
    private Integer systolicPressureLow; // 收缩压过低阈值
    private Integer diastolicPressureHigh; // 舒张压过高阈值
    private Integer diastolicPressureLow; // 舒张压过低阈值
    private Double temperatureHigh; // 体温过高阈值
    private Double temperatureLow; // 体温过低阈值
}
