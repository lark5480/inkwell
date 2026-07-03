package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.*;
import com.blog.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/** 用户关注关系控制器 */
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

    /**
     * 关注或取消关注指定用户
     *
     * @param id 目标用户 ID
     * @return 关注操作结果（包含是否已关注状态）
     */
    @PostMapping("/{id}/follow")
    public Result<FollowToggleResponse> toggleFollow(@PathVariable Long id) {
        Long currentUserId = getUserId();
        if (currentUserId == null) {
            throw new com.blog.exception.BusinessException(401, "Not authenticated");
        }
        FollowToggleResponse response = followService.toggleFollow(currentUserId, id);
        return Result.success(response);
    }

    /**
     * 获取指定用户的粉丝列表，支持分页查询
     *
     * @param id       目标用户 ID
     * @param page     页码（从 1 开始，默认 1）
     * @param pageSize 每页条数（默认 20）
     * @return 粉丝分页列表
     */
    @GetMapping("/{id}/followers")
    public Result<PageDTO<FollowerResponse>> getFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageDTO<FollowerResponse> result = followService.getFollowers(id, page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取指定用户的关注列表，支持分页查询
     *
     * @param id       目标用户 ID
     * @param page     页码（从 1 开始，默认 1）
     * @param pageSize 每页条数（默认 20）
     * @return 关注分页列表
     */
    @GetMapping("/{id}/following")
    public Result<PageDTO<FollowingResponse>> getFollowing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageDTO<FollowingResponse> result = followService.getFollowing(id, page, pageSize);
        return Result.success(result);
    }

    /**
     * 检查当前登录用户是否已关注指定用户
     *
     * @param id 目标用户 ID
     * @return 包含 "following" 键的布尔值映射
     */
    @GetMapping("/{id}/follow-status")
    public Result<java.util.Map<String, Boolean>> getFollowStatus(@PathVariable Long id) {
        Long currentUserId = getUserId();
        boolean following = followService.isFollowing(currentUserId, id);
        return Result.success(java.util.Map.of("following", following));
    }
}
