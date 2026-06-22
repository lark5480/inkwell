package com.blog.dto;

public record TagDTO(
        Long id,
        String name,
        String slug,
        Long articleCount
) {
}
