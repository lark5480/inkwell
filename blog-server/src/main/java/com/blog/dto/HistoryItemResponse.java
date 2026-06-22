package com.blog.dto;

import java.time.LocalDateTime;

public record HistoryItemResponse(
        Long id,
        Long articleId,
        String articleTitle,
        String articleSlug,
        String coverImage,
        LocalDateTime viewedAt
) {}
