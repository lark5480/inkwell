package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.*;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.repository.ArticleRepository;
import com.blog.repository.UserRepository;
import com.blog.storage.FileStorageService;
import com.blog.service.UserArticleService;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户个人资料控制器
 */
@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final UserArticleService userArticleService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException(401, "Not authenticated");
        }
        return (Long) auth.getPrincipal();
    }

    // ========== Authenticated: own profile ==========

    /**
     * 获取当前登录用户的个人资料
     *
     * @return 当前用户的昵称、头像、简介、邮箱等信息
     */
    @GetMapping("/api/web/user/profile")
    public Result<UserProfileResponse> getMyProfile() {
        User user = userRepository.findById(getUserId())
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        return Result.success(new UserProfileResponse(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getBio(), user.getEmail(),
                user.getCreateTime()
        ));
    }

    /**
     * 更新当前登录用户的个人资料
     *
     * @param request 包含昵称、头像、简介等可更新字段
     * @return 更新后的用户个人资料
     */
    @PutMapping("/api/web/user/profile")
    public Result<UserProfileResponse> updateMyProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        User user = userRepository.findById(getUserId())
                .orElseThrow(() -> new BusinessException(404, "User not found"));

        if (request.nickname() != null) user.setNickname(request.nickname());
        if (request.avatar() != null) user.setAvatar(request.avatar());
        if (request.bio() != null) user.setBio(request.bio());

        userRepository.save(user);
        return Result.success(new UserProfileResponse(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getBio(), user.getEmail(),
                user.getCreateTime()
        ));
    }

    // ========== Avatar Upload ==========

    /**
     * 上传用户头像
     *
     * @param file 上传的头像文件
     * @return 头像图片 URL
     */
    @PostMapping("/api/web/user/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        User user = userRepository.findById(getUserId())
                .orElseThrow(() -> new BusinessException(404, "User not found"));

        String avatarUrl = fileStorageService.uploadImage(file);
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        return Result.success(avatarUrl);
    }

    // ========== Password Change ==========

    /**
     * 修改当前登录用户的密码
     *
     * @param request 包含旧密码和新密码
     * @return 修改成功返回空响应
     */
    @PutMapping("/api/web/user/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        User user = userRepository.findById(getUserId())
                .orElseThrow(() -> new BusinessException(404, "User not found"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(400, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return Result.success();
    }

    // ========== Public: user search ==========

    /**
     * 搜索用户（按昵称或用户名模糊匹配）
     *
     * @param q 搜索关键词
     * @return 匹配的用户列表（最多 20 条）
     */
    @GetMapping("/api/web/users/search")
    public Result<List<UserSearchResponse>> searchUsers(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return Result.success(List.of());
        }
        List<User> users = userRepository.findByNicknameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                q.trim(), q.trim(), PageRequest.of(0, 20));
        List<UserSearchResponse> result = users.stream()
                .map(u -> new UserSearchResponse(
                        u.getId(), u.getNickname(), u.getAvatar(),
                        u.getBio(), u.getFollowerCount() != null ? u.getFollowerCount() : 0))
                .toList();
        return Result.success(result);
    }

    // ========== Public: user profiles ==========

    /**
     * 获取指定用户的公开资料
     *
     * @param userId 目标用户 ID
     * @return 用户的昵称、头像、简介、文章数等公开信息
     */
    @GetMapping("/api/web/users/{userId}")
    public Result<UserPublicResponse> getPublicProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "User not found"));

        long articleCount = articleRepository.countByUserIdAndStatus(userId, "PUBLISHED");

        return Result.success(new UserPublicResponse(
                user.getId(), user.getNickname(), user.getAvatar(),
                user.getBio(), user.getCreateTime(), articleCount,
                user.getFollowerCount(), user.getFollowingCount()
        ));
    }

    /**
     * 获取指定用户的公开文章列表，支持分页查询
     *
     * @param userId   目标用户 ID
     * @param page     页码（从 1 开始，默认 1）
     * @param pageSize 每页条数（默认 10）
     * @return 文章分页列表（仅包含已发布的文章）
     */
    @GetMapping("/api/web/users/{userId}/articles")
    public Result<PageDTO<ArticleWebResponse>> getUserArticles(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        // Only published articles for public view
        PageDTO<ArticleWebResponse> result = userArticleService.getMyArticles(userId, page, pageSize, "PUBLISHED", null);
        return Result.success(result);
    }
}
