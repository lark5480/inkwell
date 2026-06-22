package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ArticleCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be less than 200 characters")
        String title,
        String slug,
        @NotBlank(message = "Content is required")
        String content,
        String summary,
        String coverImage,
        Long categoryId,
        Set<Long> tags,
        String status,
        Boolean isFeatured
) {
}
