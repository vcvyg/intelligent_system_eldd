package org.example.persion.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 老人信息VO
 */
@Data
public class ElderlyInfoVO {

    private Long id;

    private Long userId;

    private String name;

    private Integer age;

    private String gender;

    private LocalDate birthday;

    private String idCard;

    private String address;

    private Long roomId;  // 添加房间ID字段！！！

    private String emergencyContact;

    private String emergencyPhone;

    private String medicalHistory;

    private String allergyHistory;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
