package com.blog.service.impl;

import com.blog.dto.ConversationResponse;
import com.blog.dto.MessageResponse;
import com.blog.dto.PageDTO;
import com.blog.entity.Message;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.repository.MessageRepository;
import com.blog.repository.NotificationRepository;
import com.blog.repository.UserRepository;
import com.blog.service.MessageService;
import com.blog.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public MessageServiceImpl(MessageRepository messageRepository,
                              UserRepository userRepository,
                              NotificationRepository notificationRepository,
                              NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long fromUserId, Long toUserId, String content) {
        log.info("发送私信 from={} to={}", fromUserId, toUserId);

        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(400, "Cannot send message to yourself");
        }

        if (!userRepository.existsById(toUserId)) {
            throw new BusinessException(404, "User not found");
        }

        User sender = userRepository.findById(fromUserId)
                .orElseThrow(() -> new BusinessException(404, "Sender not found"));

        Message message = Message.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .content(content)
                .isRead(false)
                .build();
        messageRepository.save(message);

        // 触发通知
        notificationService.notifyMessage(fromUserId, toUserId, content);

        return toMessageResponse(message, sender.getNickname(), sender.getAvatar());
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<MessageResponse> getMessages(Long currentUserId, Long otherUserId, int page, int pageSize) {
        log.debug("查询私信历史 currentUserId={} otherUserId={}", currentUserId, otherUserId);
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<Message> messagePage = messageRepository
                .findByFromUserIdAndToUserIdOrFromUserIdAndToUserIdOrderByCreateTimeDesc(
                        currentUserId, otherUserId, otherUserId, currentUserId, pageable);

        // 批量查询用户信息
        List<Long> userIds = List.of(currentUserId, otherUserId);
        var userMap = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        List<MessageResponse> records = messagePage.getContent().stream()
                .map(m -> {
                    User u = userMap.get(m.getFromUserId());
                    return toMessageResponse(m, u != null ? u.getNickname() : null, u != null ? u.getAvatar() : null);
                })
                .toList();

        return new PageDTO<>(records, messagePage.getTotalElements(), page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long userId) {
        log.debug("查询会话列表 userId={}", userId);

        List<Long> conversationUserIds = messageRepository.findConversationUserIds(userId);
        if (conversationUserIds.isEmpty()) {
            return List.of();
        }

        var userMap = userRepository.findAllById(conversationUserIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        List<ConversationResponse> result = new ArrayList<>();
        for (Long otherId : conversationUserIds) {
            User other = userMap.get(otherId);
            if (other == null) continue;

            // 最后一条消息
            Pageable top = PageRequest.of(0, 1);
            List<Message> lastMsg = messageRepository.findLastMessageBetween(userId, otherId, top);
            String lastContent = lastMsg.isEmpty() ? "" : lastMsg.get(0).getContent();
            var lastTime = lastMsg.isEmpty() ? null : lastMsg.get(0).getCreateTime();

            // 未读数
            long unread = messageRepository.countByToUserIdAndFromUserIdAndIsRead(userId, otherId, false);

            result.add(new ConversationResponse(
                    other.getId(), other.getNickname(), other.getAvatar(),
                    lastContent, lastTime, unread));
        }

        // 按最后消息时间倒序
        result.sort((a, b) -> {
            if (a.lastMessageTime() == null && b.lastMessageTime() == null) return 0;
            if (a.lastMessageTime() == null) return 1;
            if (b.lastMessageTime() == null) return -1;
            return b.lastMessageTime().compareTo(a.lastMessageTime());
        });

        return result;
    }

    @Override
    @Transactional
    public void markRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(404, "Message not found"));
        if (!message.getToUserId().equals(userId)) {
            throw new BusinessException(403, "Not your message");
        }
        message.setIsRead(true);
        messageRepository.save(message);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId, Long fromUserId) {
        messageRepository.markAllRead(userId, fromUserId);
        notificationRepository.markMessageNotificationsAsRead(userId, fromUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return messageRepository.countByToUserIdAndIsRead(userId, false);
    }

    private MessageResponse toMessageResponse(Message m, String userName, String userAvatar) {
        return new MessageResponse(
                m.getId(), m.getFromUserId(), userName, userAvatar,
                m.getToUserId(), m.getContent(), m.getIsRead(), m.getCreateTime());
    }
}
