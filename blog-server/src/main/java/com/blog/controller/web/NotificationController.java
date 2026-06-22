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

    @GetMapping
    public Result<PageDTO<NotificationResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String type) {
        PageDTO<NotificationResponse> result = notificationService.getMyNotifications(getUserId(), page, pageSize, type);
        return Result.success(result);
    }

    @GetMapping("/unread-count")
    public Result<UnreadCountResponse> unreadCount() {
        UnreadCountResponse result = notificationService.getUnreadCount(getUserId());
        return Result.success(result);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(getUserId());
        return Result.success();
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, getUserId());
        return Result.success();
    }

    @PutMapping("/mark-type-read")
    public Result<Void> markTypeRead(@RequestParam String type) {
        notificationService.markTypeAsRead(getUserId(), type.toUpperCase());
        return Result.success();
    }
}
