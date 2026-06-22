package com.blog.dto;

public record FollowToggleResponse(
        boolean followed,
        int followerCount,
        int followingCount
) {}
