package com.blog.service;

import com.blog.dto.ConversationResponse;
import com.blog.dto.MessageResponse;
import com.blog.dto.PageDTO;

import java.util.List;

/**
 * 消息服务接口，提供私信发送、查询、已读标记等业务功能。
 */
public interface MessageService {

    /**
     * 发送私信消息。
     *
     * @param fromUserId 发送方用户ID
     * @param toUserId   接收方用户ID
     * @param content    消息文本内容
     * @return 已保存的消息响应对象
     */
    MessageResponse sendMessage(Long fromUserId, Long toUserId, String content);

    /**
     * 分页查询当前用户与另一个用户的聊天消息列表。
     *
     * @param currentUserId 当前登录用户ID
     * @param otherUserId   对话的对方用户ID
     * @param page          页码（从 1 开始）
     * @param pageSize      每页条数
     * @return 分页消息响应列表
     */
    PageDTO<MessageResponse> getMessages(Long currentUserId, Long otherUserId, int page, int pageSize);

    /**
     * 获取指定用户的会话列表（按最后消息时间倒序）。
     *
     * @param userId 用户ID
     * @return 会话响应列表
     */
    List<ConversationResponse> getConversations(Long userId);

    /**
     * 将单条消息标记为已读。
     *
     * @param messageId 消息ID
     * @param userId    当前用户ID（用于鉴权，只能标记自己接收的消息）
     */
    void markRead(Long messageId, Long userId);

    /**
     * 将某用户发来的所有未读消息标记为已读。
     *
     * @param userId     当前用户ID
     * @param fromUserId 发消息方的用户ID（将该对话中所有消息标为已读）
     */
    void markAllRead(Long userId, Long fromUserId);

    /**
     * 获取当前用户的未读消息总数。
     *
     * @param userId 用户ID
     * @return 未读消息数量
     */
    long getUnreadCount(Long userId);
}
