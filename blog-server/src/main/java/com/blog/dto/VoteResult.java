package com.blog.dto;

public record VoteResult(
        int likeCount,
        int dislikeCount,
        boolean likedByCurrentUser,
        boolean dislikedByCurrentUser
) {
}
