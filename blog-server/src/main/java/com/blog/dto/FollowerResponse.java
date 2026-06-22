package com.blog.dto;

import java.time.LocalDateTime;

public record FollowerResponse(
        Long userId,
        String nickname,
        String avatar,
        String bio,
        LocalDateTime followedAt
) {}
