package com.blog.service;

import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.dto.UserInfo;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserInfo getCurrentUser(Long userId);

    void logout();
}
