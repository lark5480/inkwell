package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.dto.RegisterRequest;
import com.blog.dto.UserInfo;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.repository.UserRepository;
import com.blog.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController("webAuthController")
@RequestMapping("/api/web/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return Result.error(400, "Username already exists");
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname() != null ? request.nickname() : request.username())
                .email(request.email())
                .role("AUTHOR")
                .status(1)
                .build();
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole());

        return Result.success(new LoginResponse(token, userInfo));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException e) {
            return Result.error(401, "Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(401, "Invalid username or password"));

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole());

        return Result.success(new LoginResponse(token, userInfo));
    }

    @GetMapping("/me")
    public Result<UserInfo> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return Result.error(401, "Not authenticated");
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole());
        return Result.success(userInfo);
    }
}
