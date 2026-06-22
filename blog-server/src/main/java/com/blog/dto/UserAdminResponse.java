package com.blog.dto;

import java.time.LocalDateTime;

public record UserAdminResponse(
        Long id,
        String username,
        String nickname,
        String email,
        String avatar,
        String role,
        Integer status,
        LocalDateTime createTime
) {
}
