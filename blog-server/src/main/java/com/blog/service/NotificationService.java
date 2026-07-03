package com.blog.service;

import com.blog.dto.NotificationResponse;
import com.blog.dto.PageDTO;
import com.blog.dto.UnreadCountResponse;

/** 通知服务接口 */
public interface NotificationService {

    /** 评论通知：通知文章作者 + 父评论作者 */
    void notifyComment(Long articleId, Long commenterId, Long articleAuthorId, Long parentCommentAuthorId);

    /** 点赞通知：通知文章作者 */
    void notifyLike(Long articleId, Long likerId, Long articleAuthorId);

    /** 评论点赞通知 */
    void notifyCommentLike(Long commentId, Long likerId, Long commentAuthorId);

    /** 关注通知：通知被关注者 */
    void notifyFollow(Long followerId, Long followingId);

    /** 私信通知 */
    void notifyMessage(Long fromUserId, Long toUserId, String content);

    /**
     * 获取当前用户的通知列表（分页）
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @param type     通知类型（可选过滤）
     * @return 分页通知列表
     */
    PageDTO<NotificationResponse> getMyNotifications(Long userId, int page, int pageSize, String type);

    /**
     * 获取当前用户未读通知数量
     *
     * @param userId 用户 ID
     * @return 未读数量（含各类通知的分类计数）
     */
    UnreadCountResponse getUnreadCount(Long userId);

    /**
     * 将当前用户所有通知标记为已读
     *
     * @param userId 用户 ID
     */
    void markAllRead(Long userId);

    /**
     * 将指定单条通知标记为已读
     *
     * @param notificationId 通知 ID
     * @param userId         用户 ID
     */
    void markRead(Long notificationId, Long userId);

    /**
     * 将当前用户指定类型的通知全部标记为已读
     *
     * @param userId 用户 ID
     * @param type   通知类型（如 LIKE、COMMENT 等）
     */
    void markTypeAsRead(Long userId, String type);
}
