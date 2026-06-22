package com.blog.dto;

public record UserSearchResponse(
        Long id,
        String nickname,
        String avatar,
        String bio,
        long followerCount
) {
}
