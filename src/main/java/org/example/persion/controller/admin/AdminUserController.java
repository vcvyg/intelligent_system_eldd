package org.example.persion.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.AdminUserCreateDTO;
import org.example.persion.dto.AdminUserUpdateDTO;
import org.example.persion.entity.User;
import org.example.persion.service.AdminUserService;
import org.example.persion.vo.UserInfoVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    public Result<Page<UserInfoVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        Page<UserInfoVO> page = adminUserService.getUserList(current, size, keyword, role, status);
        return Result.success(page);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<User> createUser(@Valid @RequestBody AdminUserCreateDTO dto) {
        User user = adminUserService.createUser(dto);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateDTO dto) {
        User user = adminUserService.updateUser(id, dto);
        return Result.success(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return Result.success();
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminUserService.updateUserStatus(id, status);
        return Result.success();
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        adminUserService.resetPassword(id, newPassword);
        return Result.success();
    }
}
