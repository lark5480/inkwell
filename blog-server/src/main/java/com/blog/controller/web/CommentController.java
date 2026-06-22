package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.BlockedUserResponse;
import com.blog.dto.CommentCreateRequest;
import com.blog.dto.CommentResponse;
import com.blog.dto.ReportRequest;
import com.blog.dto.VoteRequest;
import com.blog.dto.VoteResult;
import com.blog.service.CommentReportService;
import com.blog.service.CommentService;
import com.blog.service.CommentVoteService;
import com.blog.service.UserBlockService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController("webCommentController")
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentVoteService commentVoteService;
    private final CommentReportService commentReportService;
    private final UserBlockService userBlockService;

    @GetMapping("/comments")
    public Result<List<CommentResponse>> getComments(@RequestParam Long articleId) {
        Long currentUserId = getCurrentUserId();
        List<Long> blockedUserIds = currentUserId != null
                ? userBlockService.getBlockedUserIds(currentUserId)
                : Collections.emptyList();
        List<CommentResponse> list = commentService.getCommentsByArticle(articleId, currentUserId, blockedUserIds);
        return Result.success(list);
    }

    @GetMapping("/user/blocks")
    public Result<List<BlockedUserResponse>> getBlockedUsers() {
        Long userId = requireCurrentUserId();
        List<BlockedUserResponse> list = userBlockService.getBlockedUsers(userId);
        return Result.success(list);
    }

    @PostMapping("/comments")
    public Result<Long> create(@Valid @RequestBody CommentCreateRequest request,
                               HttpServletRequest servletRequest) {
        String ip = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        Long userId = getCurrentUserId();
        Long id = commentService.createComment(request, ip, userAgent, userId);
        return Result.success(id);
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteOwn(@PathVariable Long id) {
        Long userId = requireCurrentUserId();
        commentService.deleteOwnComment(id, userId);
        return Result.success();
    }

    @PostMapping("/comments/{id}/vote")
    public Result<VoteResult> vote(@PathVariable Long id, @RequestBody VoteRequest request) {
        Long userId = requireCurrentUserId();
        VoteResult result = commentVoteService.vote(id, userId, request.voteType());
        return Result.success(result);
    }

    @PostMapping("/comments/{id}/report")
    public Result<Void> report(@PathVariable Long id, @RequestBody ReportRequest request) {
        Long userId = requireCurrentUserId();
        commentReportService.report(id, userId, request.reason());
        return Result.success();
    }

    @PostMapping("/users/{id}/block")
    public Result<Void> block(@PathVariable("id") Long blockedId) {
        Long blockerId = requireCurrentUserId();
        userBlockService.block(blockerId, blockedId);
        return Result.success();
    }

    @DeleteMapping("/users/{id}/block")
    public Result<Void> unblock(@PathVariable("id") Long blockedId) {
        Long blockerId = requireCurrentUserId();
        userBlockService.unblock(blockerId, blockedId);
        return Result.success();
    }

    // ========== Helpers ==========

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    private Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new com.blog.exception.BusinessException(401, "Authentication required");
        }
        return userId;
    }
}
