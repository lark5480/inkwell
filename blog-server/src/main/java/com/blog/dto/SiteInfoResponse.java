package com.blog.dto;

public record SiteInfoResponse(
        String siteTitle,
        String siteDescription,
        long articleCount,
        long categoryCount,
        long tagCount,
        String aboutContent
) {
}
