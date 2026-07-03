package com.blog.service;

import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.dto.UserInfo;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求（包含用户名和密码）
     * @return 登录响应（包含 token 和用户信息）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    UserInfo getCurrentUser(Long userId);

    /**
     * 用户登出
     */
    void logout();
}
