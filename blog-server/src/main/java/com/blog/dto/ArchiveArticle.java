package com.blog.dto;

import java.time.LocalDateTime;

public record ArchiveArticle(
        Long id,
        String title,
        String slug,
        LocalDateTime publishedAt
) {
}
