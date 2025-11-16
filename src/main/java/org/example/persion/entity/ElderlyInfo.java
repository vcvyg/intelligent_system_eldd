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

    private Long userId; // 关联用户表

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
}
