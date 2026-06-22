package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.ConversationResponse;
import com.blog.dto.MessageResponse;
import com.blog.dto.PageDTO;
import com.blog.dto.SendMessageRequest;
import com.blog.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/web/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversations")
    public Result<List<ConversationResponse>> getConversations() {
        Long userId = getCurrentUserId();
        List<ConversationResponse> list = messageService.getConversations(userId);
        return Result.success(list);
    }

    @GetMapping
    public Result<PageDTO<MessageResponse>> getMessages(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long currentUserId = getCurrentUserId();
        PageDTO<MessageResponse> result = messageService.getMessages(currentUserId, userId, page, pageSize);
        return Result.success(result);
    }

    @PostMapping
    public Result<MessageResponse> send(@Valid @RequestBody SendMessageRequest request) {
        Long fromUserId = getCurrentUserId();
        MessageResponse msg = messageService.sendMessage(fromUserId, request.toUserId(), request.content());
        return Result.success(msg);
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        messageService.markRead(id, userId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestParam Long userId) {
        Long currentUserId = getCurrentUserId();
        messageService.markAllRead(currentUserId, userId);
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = getCurrentUserId();
        long count = messageService.getUnreadCount(userId);
        return Result.success(count);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        throw new com.blog.exception.BusinessException(401, "Authentication required");
    }
}
