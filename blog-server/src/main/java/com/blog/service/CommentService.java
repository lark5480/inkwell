package com.blog.service;

import com.blog.dto.*;

import java.util.List;

public interface CommentService {

    // Web methods
    List<CommentResponse> getCommentsByArticle(Long articleId, Long currentUserId, List<Long> blockedUserIds);

    Long createComment(CommentCreateRequest request, String ip, String userAgent, Long userId);

    void deleteOwnComment(Long commentId, Long userId);

    // Admin methods
    PageDTO<CommentAdminResponse> getCommentPage(int page, int pageSize, String status);

    void updateCommentStatus(Long id, String status);

    void deleteComment(Long id);
}
