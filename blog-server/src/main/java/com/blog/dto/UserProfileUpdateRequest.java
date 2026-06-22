package com.blog.dto;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 50, message = "Nickname max 50 characters")
        String nickname,
        @Size(max = 500, message = "Avatar URL max 500 characters")
        String avatar,
        @Size(max = 1000, message = "Bio max 1000 characters")
        String bio
) {
}
