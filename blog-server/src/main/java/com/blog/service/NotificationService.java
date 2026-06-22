package com.blog.service;

import com.blog.dto.NotificationResponse;
import com.blog.dto.PageDTO;
import com.blog.dto.UnreadCountResponse;

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

    PageDTO<NotificationResponse> getMyNotifications(Long userId, int page, int pageSize, String type);

    UnreadCountResponse getUnreadCount(Long userId);

    void markAllRead(Long userId);

    void markRead(Long notificationId, Long userId);

    void markTypeAsRead(Long userId, String type);
}
