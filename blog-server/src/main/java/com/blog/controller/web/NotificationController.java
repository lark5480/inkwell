package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.NotificationResponse;
import com.blog.dto.PageDTO;
import com.blog.dto.UnreadCountResponse;
import com.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/web/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new com.blog.exception.BusinessException(401, "Not authenticated");
        }
        return (Long) auth.getPrincipal();
    }

    /**
     * 获取当前用户的通知列表
     *
     * @param page     页码（默认1）
     * @param pageSize 每页条数（默认20）
     * @param type     通知类型（可选，传空则查询全部）
     * @return 分页通知列表
     */
    @GetMapping
    public Result<PageDTO<NotificationResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String type) {
        PageDTO<NotificationResponse> result = notificationService.getMyNotifications(getUserId(), page, pageSize, type);
        return Result.success(result);
    }

    /**
     * 获取当前用户的未读通知数量
     *
     * @return 未读通知计数
     */
    @GetMapping("/unread-count")
    public Result<UnreadCountResponse> unreadCount() {
        UnreadCountResponse result = notificationService.getUnreadCount(getUserId());
        return Result.success(result);
    }

    /**
     * 将所有通知标记为已读
     */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(getUserId());
        return Result.success();
    }

    /**
     * 将指定通知标记为已读
     *
     * @param id 通知ID
     */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, getUserId());
        return Result.success();
    }

    /**
     * 将指定类型的通知全部标记为已读
     *
     * @param type 通知类型（如 LIKE, COMMENT 等）
     */
    @PutMapping("/mark-type-read")
    public Result<Void> markTypeRead(@RequestParam String type) {
        notificationService.markTypeAsRead(getUserId(), type.toUpperCase());
        return Result.success();
    }
}
