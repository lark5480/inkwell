package com.blog.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        Long fromUserId,
        String fromUserName,
        String fromUserAvatar,
        Long articleId,
        String articleTitle,
        String articleSlug,
        String content,
        boolean isRead,
        LocalDateTime createTime
) {}
