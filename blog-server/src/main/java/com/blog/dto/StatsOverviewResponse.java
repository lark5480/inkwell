package com.blog.dto;

public record StatsOverviewResponse(
        long articleCount,
        long commentCount,
        long viewCount,
        long categoryCount,
        long tagCount,
        long linkCount
) {
}
