package com.blog.service;

import com.blog.dto.ConversationResponse;
import com.blog.dto.MessageResponse;
import com.blog.dto.PageDTO;

import java.util.List;

public interface MessageService {

    MessageResponse sendMessage(Long fromUserId, Long toUserId, String content);

    PageDTO<MessageResponse> getMessages(Long currentUserId, Long otherUserId, int page, int pageSize);

    List<ConversationResponse> getConversations(Long userId);

    void markRead(Long messageId, Long userId);

    void markAllRead(Long userId, Long fromUserId);

    long getUnreadCount(Long userId);
}
