package com.blog.service;

import com.blog.dto.VoteResult;

public interface CommentVoteService {

    /**
     * 点赞/点踩/取消投票。voteType 为 null 时取消当前投票
     */
    VoteResult vote(Long commentId, Long userId, String voteType);
}
