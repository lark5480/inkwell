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
