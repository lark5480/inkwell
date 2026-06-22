package com.blog.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long fromUserId,
        String fromUserName,
        String fromUserAvatar,
        Long toUserId,
        String content,
        boolean isRead,
        LocalDateTime createTime
) {
}
