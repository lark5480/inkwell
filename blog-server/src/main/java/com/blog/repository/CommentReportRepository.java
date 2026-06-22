package com.blog.repository;

import com.blog.entity.CommentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    Page<CommentReport> findByStatus(String status, Pageable pageable);

    Page<CommentReport> findAllByOrderByCreateTimeDesc(Pageable pageable);

    boolean existsByCommentIdAndReporterId(Long commentId, Long reporterId);
}
