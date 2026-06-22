package com.blog.dto;

public record SettingDTO(
        Long id,
        String settingKey,
        String settingValue,
        String description
) {
}
