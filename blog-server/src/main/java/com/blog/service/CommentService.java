package com.blog.service;

import com.blog.dto.*;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {

    // Web methods

    /**
     * 获取指定文章的所有评论
     *
     * @param articleId      文章ID
     * @param currentUserId  当前登录用户ID（用于判断是否为本人评论）
     * @param blockedUserIds 当前用户屏蔽的用户ID列表，用于过滤被屏蔽用户的评论
     * @return 评论列表
     */
    List<CommentResponse> getCommentsByArticle(Long articleId, Long currentUserId, List<Long> blockedUserIds);

    /**
     * 创建评论
     *
     * @param request  评论创建请求，包含文章ID、父评论ID、内容等
     * @param ip       评论者IP地址
     * @param userAgent 评论者浏览器 User-Agent
     * @param userId   评论者用户ID（匿名评论可为 null）
     * @return 新创建的评论ID
     */
    Long createComment(CommentCreateRequest request, String ip, String userAgent, Long userId);

    /**
     * 删除当前用户自己的评论
     *
     * @param commentId 评论ID
     * @param userId    当前用户ID
     */
    void deleteOwnComment(Long commentId, Long userId);

    // Admin methods

    /**
     * 分页查询评论列表（管理端）
     *
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数
     * @param status   评论状态筛选（如 "pending"、"approved"、"rejected"，传 null 查询所有）
     * @return 分页评论数据
     */
    PageDTO<CommentAdminResponse> getCommentPage(int page, int pageSize, String status);

    /**
     * 更新评论状态（审核通过/拒绝等）
     *
     * @param id     评论ID
     * @param status 目标状态值
     */
    void updateCommentStatus(Long id, String status);

    /**
     * 删除指定评论（管理端）
     *
     * @param id 评论ID
     */
    void deleteComment(Long id);
}
