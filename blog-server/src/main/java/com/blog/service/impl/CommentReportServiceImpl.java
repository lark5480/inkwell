package com.blog.service.impl;

import com.blog.dto.PageDTO;
import com.blog.entity.CommentReport;
import com.blog.exception.BusinessException;
import com.blog.repository.CommentReportRepository;
import com.blog.repository.CommentRepository;
import com.blog.service.CommentReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CommentReportServiceImpl implements CommentReportService {

    private static final Logger log = LoggerFactory.getLogger(CommentReportServiceImpl.class);

    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;

    public CommentReportServiceImpl(CommentReportRepository commentReportRepository,
                                    CommentRepository commentRepository) {
        this.commentReportRepository = commentReportRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional
    public void report(Long commentId, Long reporterId, String reason) {
        log.debug("举报评论 commentId={} reporterId={} reason={}", commentId, reporterId, reason);

        if (!commentRepository.existsById(commentId)) {
            throw new BusinessException(404, "Comment not found");
        }

        if (commentReportRepository.existsByCommentIdAndReporterId(commentId, reporterId)) {
            throw new BusinessException(400, "Already reported");
        }

        CommentReport report = CommentReport.builder()
                .commentId(commentId)
                .reporterId(reporterId)
                .reason(reason)
                .status("PENDING")
                .build();
        commentReportRepository.save(report);
        log.info("举报提交成功 commentId={} reportId={}", commentId, report.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<CommentReport> getReportPage(int page, int pageSize, String status) {
        log.debug("后台查询举报列表 page={} status={}", page, status);
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<CommentReport> reportPage;
        if (status != null && !status.isBlank()) {
            reportPage = commentReportRepository.findByStatus(status, pageable);
        } else {
            reportPage = commentReportRepository.findAllByOrderByCreateTimeDesc(pageable);
        }

        return new PageDTO<>(reportPage.getContent(), reportPage.getTotalElements(), page, pageSize);
    }

    @Override
    @Transactional
    public void updateReportStatus(Long id, String status) {
        log.info("处理举报 id={} status={}", id, status);
        CommentReport report = commentReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Report not found"));

        report.setStatus(status);
        report.setResolveTime(LocalDateTime.now());
        commentReportRepository.save(report);
    }
}
