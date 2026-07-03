package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.dto.UserInfo;
import com.blog.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
/**
 * 管理员认证控制器
 */
public class AuthController {

    private final AuthService authService;

    /**
     * 管理员登录
     *
     * @param request 登录请求（用户名和密码）
     * @return 登录结果，包含 token 和用户信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 管理员登出
     *
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    /**
     * 获取当前登录管理员信息
     *
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public Result<UserInfo> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        UserInfo userInfo = authService.getCurrentUser(userId);
        return Result.success(userInfo);
    }
}
