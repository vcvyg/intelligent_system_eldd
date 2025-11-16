package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.AdminUserCreateDTO;
import org.example.persion.dto.AdminUserUpdateDTO;
import org.example.persion.entity.User;
import org.example.persion.repository.UserMapper;
import org.example.persion.service.AdminUserService;
import org.example.persion.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理员端-用户管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserInfoVO> getUserList(Integer current, Integer size, String keyword, String role, Integer status) {
        Page<User> page = new Page<>(current, size);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), User::getUsername, keyword)
               .or()
               .like(StringUtils.hasText(keyword), User::getRealName, keyword)
               .or()
               .like(StringUtils.hasText(keyword), User::getPhone, keyword);

        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }

        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreateTime);

        Page<User> userPage = userMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<UserInfoVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(user -> {
            UserInfoVO vo = new UserInfoVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).toList());

        return voPage;
    }

    @Override
    public User createUser(AdminUserCreateDTO dto) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查手机号是否已存在
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("手机号已被注册");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(1); // 默认启用

        userMapper.insert(user);
        return user;
    }

    @Override
    public User updateUser(Long id, AdminUserUpdateDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查手机号是否被其他用户使用
        if (StringUtils.hasText(dto.getPhone()) && !dto.getPhone().equals(user.getPhone())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, dto.getPhone())
                   .ne(User::getId, id);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("手机号已被其他用户使用");
            }
        }

        BeanUtils.copyProperties(dto, user, "id", "username", "password");
        userMapper.updateById(user);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        userMapper.deleteById(id);
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public java.util.List<UserInfoVO> getUsersByRole(String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, role)
               .eq(User::getStatus, 1) // 只返回启用的用户
               .orderByDesc(User::getCreateTime);

        return userMapper.selectList(wrapper).stream().map(user -> {
            UserInfoVO vo = new UserInfoVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).toList();
    }
}
