package com.blog.dto;

public record UserInfo(
        Long id,
        String username,
        String nickname,
        String avatar,
        String role
) {
}
