package org.example.persion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理员更新用户DTO
 */
@Data
public class AdminUserUpdateDTO {

    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^(ADMIN|FAMILY|MEDICAL|ELDERLY)$", message = "角色类型不正确")
    private String role;

    private Integer status; // 0-禁用 1-启用
}
