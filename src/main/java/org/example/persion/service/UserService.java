package org.example.persion.service;

import org.example.persion.dto.LoginDTO;
import org.example.persion.dto.UserRegisterDTO;
import org.example.persion.entity.User;
import org.example.persion.vo.LoginVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    User register(UserRegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 根据用户名查询用户
     */
    User getUserByUsername(String username);

    /**
     * 根据ID查询用户
     */
    User getUserById(Long id);

    /**
     * 检查用户名是否已存在
     */
    boolean isUsernameExists(String username);

    /**
     * 检查邮箱是否已存在
     */
    boolean isEmailExists(String email);

    /**
     * 检查手机号是否已存在
     */
    boolean isPhoneExists(String phone);

    /**
     * 重置密码
     * @param email 邮箱
     * @param code 验证码
     * @param newPassword 新密码
     */
    void resetPassword(String email, String code, String newPassword);
}
