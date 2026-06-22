package com.blog.dto;

import java.time.LocalDateTime;

public record UserPublicResponse(
        Long id,
        String nickname,
        String avatar,
        String bio,
        LocalDateTime createTime,
        long articleCount,
        long followerCount,
        long followingCount
) {
}
