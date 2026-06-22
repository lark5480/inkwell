package com.blog.dto;

public record LoginResponse(
        String token,
        UserInfo user
) {
}
