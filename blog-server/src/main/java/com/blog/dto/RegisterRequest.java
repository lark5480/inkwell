package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        String username,
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 255, message = "Password must be 6-255 characters")
        String password,
        String nickname,
        String email
) {}
