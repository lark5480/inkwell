package com.blog.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ArticleDetailResponse(
        Long id,
        String title,
        String slug,
        String content,
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
        LocalDateTime createTime,
        LocalDateTime updateTime,
        Long authorId,
        String authorName,
        String authorAvatar,
        Boolean isLiked,
        String status
) {
}
