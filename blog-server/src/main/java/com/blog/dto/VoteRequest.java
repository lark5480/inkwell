package com.blog.dto;

public record VoteRequest(
        String voteType  // "LIKE" / "DISLIKE" / null (取消)
) {
}
