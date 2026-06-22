package com.blog.service.impl;

import com.blog.dto.VoteResult;
import com.blog.entity.Comment;
import com.blog.entity.CommentVote;
import com.blog.exception.BusinessException;
import com.blog.repository.CommentRepository;
import com.blog.repository.CommentVoteRepository;
import com.blog.service.CommentVoteService;
import com.blog.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CommentVoteServiceImpl implements CommentVoteService {

    private static final Logger log = LoggerFactory.getLogger(CommentVoteServiceImpl.class);

    private final CommentVoteRepository commentVoteRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    public CommentVoteServiceImpl(CommentVoteRepository commentVoteRepository,
                                  CommentRepository commentRepository,
                                  NotificationService notificationService) {
        this.commentVoteRepository = commentVoteRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public VoteResult vote(Long commentId, Long userId, String voteType) {
        log.debug("投票 commentId={} userId={} voteType={}", commentId, userId, voteType);

        if (!commentRepository.existsById(commentId)) {
            throw new BusinessException(404, "Comment not found");
        }

        boolean isNewLike = false;
        Optional<CommentVote> existing = commentVoteRepository.findByCommentIdAndUserId(commentId, userId);

        if (voteType == null || voteType.isBlank()) {
            existing.ifPresent(commentVoteRepository::delete);
            log.debug("取消投票 commentId={} userId={}", commentId, userId);
        } else if (existing.isPresent()) {
            CommentVote vote = existing.get();
            if (vote.getVoteType().equals(voteType)) {
                commentVoteRepository.delete(vote);
                log.debug("取消{} commentId={} userId={}", voteType, commentId, userId);
            } else {
                vote.setVoteType(voteType);
                commentVoteRepository.save(vote);
                log.debug("切换为{} commentId={} userId={}", voteType, commentId, userId);
            }
        } else {
            // 首次投票
            CommentVote newVote = CommentVote.builder()
                    .commentId(commentId)
                    .userId(userId)
                    .voteType(voteType)
                    .build();
            commentVoteRepository.save(newVote);
            isNewLike = "LIKE".equals(voteType);
            log.debug("新增{} commentId={} userId={}", voteType, commentId, userId);
        }

        // 新点赞时通知评论作者
        if (isNewLike) {
            Comment comment = commentRepository.findById(commentId).orElse(null);
            Long commentAuthorId = comment != null && comment.getUser() != null ? comment.getUser().getId() : null;
            if (commentAuthorId != null) {
                notificationService.notifyCommentLike(commentId, userId, commentAuthorId);
            }
        }

        int likeCount = commentVoteRepository.countByCommentIdAndVoteType(commentId, "LIKE");
        int dislikeCount = commentVoteRepository.countByCommentIdAndVoteType(commentId, "DISLIKE");

        Optional<CommentVote> current = commentVoteRepository.findByCommentIdAndUserId(commentId, userId);
        return new VoteResult(
                likeCount,
                dislikeCount,
                current.isPresent() && "LIKE".equals(current.get().getVoteType()),
                current.isPresent() && "DISLIKE".equals(current.get().getVoteType())
        );
    }
}
