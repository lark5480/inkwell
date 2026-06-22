package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.*;
import com.blog.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/web/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        try {
            return (Long) auth.getPrincipal();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @PostMapping("/{id}/follow")
    public Result<FollowToggleResponse> toggleFollow(@PathVariable Long id) {
        Long currentUserId = getUserId();
        if (currentUserId == null) {
            throw new com.blog.exception.BusinessException(401, "Not authenticated");
        }
        FollowToggleResponse response = followService.toggleFollow(currentUserId, id);
        return Result.success(response);
    }

    @GetMapping("/{id}/followers")
    public Result<PageDTO<FollowerResponse>> getFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageDTO<FollowerResponse> result = followService.getFollowers(id, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}/following")
    public Result<PageDTO<FollowingResponse>> getFollowing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageDTO<FollowingResponse> result = followService.getFollowing(id, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}/follow-status")
    public Result<java.util.Map<String, Boolean>> getFollowStatus(@PathVariable Long id) {
        Long currentUserId = getUserId();
        boolean following = followService.isFollowing(currentUserId, id);
        return Result.success(java.util.Map.of("following", following));
    }
}
