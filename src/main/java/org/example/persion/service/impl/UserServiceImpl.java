package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.LoginDTO;
import org.example.persion.dto.UserRegisterDTO;
import org.example.persion.entity.User;
import org.example.persion.repository.UserMapper;
import org.example.persion.security.JwtUtil;
import org.example.persion.service.EmailService;
import org.example.persion.service.UserService;
import org.example.persion.vo.LoginVO;
import org.example.persion.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Override
    public User register(UserRegisterDTO dto) {
        // 验证邮箱验证码
        if (!emailService.verifyCode(dto.getEmail(), dto.getCode())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 检查用户名是否已存在
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) != null) {
            throw new BusinessException("该用户名已被注册");
        }

        // 检查邮箱是否已存在
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())) != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        // 检查手机号是否已存在
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone())) != null) {
            throw new BusinessException("该手机号已被注册");
        }

        // 创建用户
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // 加密密码
        user.setStatus(1); // 默认启用

        userMapper.insert(user);
        return user;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 查询用户
        User user = getUserByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 构建返回数据
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);

        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        userInfoVO.setId(user.getId());
        loginVO.setUserInfo(userInfoVO);

        return loginVO;
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public boolean isUsernameExists(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean isEmailExists(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean isPhoneExists(String phone) {
        // 手机号为空时不检查
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return userMapper.selectCount(wrapper) > 0;
    }
}
