package com.blog.service;

import com.blog.dto.PageDTO;
import com.blog.entity.CommentReport;

public interface CommentReportService {

    /**
     * 用户举报评论
     */
    void report(Long commentId, Long reporterId, String reason);

    /**
     * 后台：分页查询举报列表
     */
    PageDTO<CommentReport> getReportPage(int page, int pageSize, String status);

    /**
     * 后台：处理举报（RESOLVED / DISMISSED）
     */
    void updateReportStatus(Long id, String status);
}
