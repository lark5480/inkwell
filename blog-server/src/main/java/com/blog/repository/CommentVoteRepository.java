package com.blog.repository;

import com.blog.entity.CommentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {

    Optional<CommentVote> findByCommentIdAndUserId(Long commentId, Long userId);

    int countByCommentIdAndVoteType(Long commentId, String voteType);

    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}
