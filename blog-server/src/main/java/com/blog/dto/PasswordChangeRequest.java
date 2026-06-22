package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "Current password is required")
        String oldPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 100, message = "Password must be 6-100 characters")
        String newPassword
) {}
