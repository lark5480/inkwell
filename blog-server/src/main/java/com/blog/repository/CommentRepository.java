package com.blog.repository;

import com.blog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByArticleIdAndStatus(Long articleId, String status, Pageable pageable);

    List<Comment> findByArticleIdAndStatusAndParentIsNull(Long articleId, String status);

    Page<Comment> findByStatus(String status, Pageable pageable);

    int countByArticleIdAndStatus(Long articleId, String status);
}
