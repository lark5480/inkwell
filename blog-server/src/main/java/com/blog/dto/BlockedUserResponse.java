package com.blog.dto;

public record BlockedUserResponse(
        Long userId,
        String nickname,
        String avatar,
        String bio
) {}
