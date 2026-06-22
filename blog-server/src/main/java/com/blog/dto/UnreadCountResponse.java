package com.blog.dto;

public record UnreadCountResponse(
        long count,
        long comment,
        long like,
        long follow
) {}
