package com.blog.dto;

public record CategoryDTO(
        Long id,
        String name,
        String slug,
        String description,
        Integer sort,
        Long articleCount
) {
}
