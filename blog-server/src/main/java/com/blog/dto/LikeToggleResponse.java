package com.blog.dto;

public record LikeToggleResponse(
        boolean liked,
        int likeCount
) {}
