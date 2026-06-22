package com.blog.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatar,
        String bio,
        String email,
        LocalDateTime createTime
) {
}
