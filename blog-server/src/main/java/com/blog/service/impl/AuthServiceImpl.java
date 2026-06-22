package com.blog.service.impl;

import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.dto.UserInfo;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.repository.UserRepository;
import com.blog.security.JwtUtils;
import com.blog.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtUtils jwtUtils,
                           UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    /**
     * 登录认证：校验用户名密码，生成 JWT Token
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录 username={}", request.username());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException e) {
            log.warn("登录失败：用户名或密码错误 username={}", request.username());
            throw new BusinessException(401, "Invalid username or password");
        }

        /* 获取用户信息 */
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(401, "Invalid username or password"));

        /* 仅 ADMIN 角色可登录后台 */
        if (!"ADMIN".equals(user.getRole())) {
            log.warn("非管理员尝试登录后台 username={}", request.username());
            throw new BusinessException(403, "Access denied: admin only");
        }

        /* 生成 JWT Token */
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getRole()
        );

        log.info("登录成功 userId={}", user.getId());
        return new LoginResponse(token, userInfo);
    }

    /**
     * 获取当前用户信息
     */
    @Override
    public UserInfo getCurrentUser(Long userId) {
        log.debug("获取当前用户信息 userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "User not found"));

        return new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getRole()
        );
    }

    /**
     * 登出（JWT 无状态，服务端无操作）
     */
    @Override
    public void logout() {
        // JWT is stateless; no-op on the server side.
        log.debug("用户登出（JWT 无状态，服务端无操作）");
    }
}
