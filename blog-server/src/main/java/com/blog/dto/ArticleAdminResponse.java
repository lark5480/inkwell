package com.blog.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ArticleAdminResponse(
        Long id,
        String title,
        String slug,
        String content,
        String summary,
        String coverImage,
        String status,
        Long viewCount,
        Integer likeCount,
        Integer commentCount,
        Boolean isTop,
        Boolean isFeatured,
        String categoryName,
        Long categoryId,
        Set<TagDTO> tags,
        Long userId,
        String authorName,
        LocalDateTime publishedAt,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
