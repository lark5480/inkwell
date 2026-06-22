package com.blog.dto;

import java.util.Set;

public record ArticleUpdateRequest(
        String title,
        String slug,
        String content,
        String summary,
        String coverImage,
        Long categoryId,
        Set<Long> tags,
        String status,
        Boolean isFeatured
) {
}
