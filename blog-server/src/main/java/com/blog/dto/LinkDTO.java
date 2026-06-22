package com.blog.dto;

public record LinkDTO(
        Long id,
        String name,
        String url,
        String avatar,
        String description,
        Integer sort,
        Integer status
) {
}
