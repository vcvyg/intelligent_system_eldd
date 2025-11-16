package org.example.persion.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.persion.dto.AdminUserCreateDTO;
import org.example.persion.dto.AdminUserUpdateDTO;
import org.example.persion.entity.User;
import org.example.persion.vo.UserInfoVO;

/**
 * 管理员端-用户管理服务接口
 */
public interface AdminUserService {

    /**
     * 分页查询用户列表
     */
    Page<UserInfoVO> getUserList(Integer current, Integer size, String keyword, String role, Integer status);

    /**
     * 创建用户
     */
    User createUser(AdminUserCreateDTO dto);

    /**
     * 更新用户信息
     */
    User updateUser(Long id, AdminUserUpdateDTO dto);

    /**
     * 删除用户
     */
    void deleteUser(Long id);

    /**
     * 启用/禁用用户
     */
    void updateUserStatus(Long id, Integer status);

    /**
     * 重置用户密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 根据角色获取用户列表(不分页)
     */
    java.util.List<UserInfoVO> getUsersByRole(String role);
}