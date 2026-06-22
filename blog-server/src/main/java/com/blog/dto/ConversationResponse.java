package com.blog.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long userId,
        String userName,
        String userAvatar,
        String lastMessage,
        LocalDateTime lastMessageTime,
        long unreadCount
) {
}
