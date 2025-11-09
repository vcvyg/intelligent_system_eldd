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
}
