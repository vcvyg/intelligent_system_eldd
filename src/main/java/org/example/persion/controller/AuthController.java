package org.example.persion.controller;

import jakarta.validation.Valid;
import org.example.persion.common.Result;
import org.example.persion.dto.LoginDTO;
import org.example.persion.dto.SendEmailCodeDTO;
import org.example.persion.dto.UserRegisterDTO;
import org.example.persion.entity.User;
import org.example.persion.service.EmailService;
import org.example.persion.service.UserService;
import org.example.persion.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器(登录、注册)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/sendEmailCode")
    public Result<String> sendEmailCode(@Valid @RequestBody SendEmailCodeDTO dto) {
        String code = emailService.sendVerificationCode(dto.getEmail());
        // 注意: 实际项目中不应返回验证码,这里仅用于开发测试
        return Result.success("验证码已发送到邮箱: " + dto.getEmail());
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody UserRegisterDTO dto) {
        User user = userService.register(dto);
        return Result.success(user);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO loginVO = userService.login(dto);
        return Result.success(loginVO);
    }

    /**
     * 检查用户名是否已存在
     */
    @GetMapping("/checkUsername")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.isUsernameExists(username);
        // 返回true表示可用,false表示已存在
        return Result.success(!exists);
    }

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("认证接口测试成功");
    }
}
