package com.blog.dto;

import java.time.LocalDateTime;

public record FollowingResponse(
        Long userId,
        String nickname,
        String avatar,
        String bio,
        LocalDateTime followedAt
) {}
