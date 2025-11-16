package org.example.persion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

/**
 * 管理员创建老人信息DTO
 */
@Data
public class AdminElderlyCreateDTO {

    @NotNull(message = "关联用户ID不能为空")
    private Long userId;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "年龄不能为空")
    private Integer age;

    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "^(男|女)$", message = "性别只能是男或女")
    private String gender;

    private LocalDate birthday;

    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$",
             message = "身份证号格式不正确")
    private String idCard;

    private String address;

    private Long roomId;  // 添加房间ID字段

    private String emergencyContact;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "紧急联系电话格式不正确")
    private String emergencyPhone;

    private String medicalHistory;

    private String allergyHistory;
}
