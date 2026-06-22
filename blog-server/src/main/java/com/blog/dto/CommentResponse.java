package com.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        Long articleId,
        String content,
        String authorName,
        String authorEmail,
        Long userId,
        String userNickname,
        String userAvatar,
        LocalDateTime createdAt,
        List<CommentResponse> replies,
        int likeCount,
        int dislikeCount,
        boolean likedByCurrentUser,
        boolean dislikedByCurrentUser
) {
}
