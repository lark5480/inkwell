package com.blog.dto;

import java.time.LocalDateTime;

public record CommentAdminResponse(
        Long id,
        Long articleId,
        String articleTitle,
        String articleAuthorName,
        String content,
        String authorName,
        String authorEmail,
        Long userId,
        String userNickname,
        String status,
        String ip,
        LocalDateTime createdAt
) {
}
