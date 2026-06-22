package com.blog.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ArticleWebResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String coverImage,
        String categoryName,
        Set<TagDTO> tags,
        Long viewCount,
        Integer likeCount,
        Integer commentCount,
        LocalDateTime publishedAt,
        Boolean isTop,
        Boolean isFeatured,
        Long authorId,
        String authorName,
        String authorAvatar,
        String status
) {
}
