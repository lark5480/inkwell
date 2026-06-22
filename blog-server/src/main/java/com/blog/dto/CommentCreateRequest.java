package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequest(
        @NotNull(message = "Article ID is required")
        Long articleId,
        @NotBlank(message = "Content is required")
        String content,
        String authorName,
        String authorEmail,
        Long parentId
) {
}
