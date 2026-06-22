package com.blog.service.impl;

import com.blog.dto.NotificationResponse;
import com.blog.dto.PageDTO;
import com.blog.dto.UnreadCountResponse;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.Notification;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CommentRepository;
import com.blog.repository.NotificationRepository;
import com.blog.repository.UserRepository;
import com.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public void notifyComment(Long articleId, Long commenterId, Long articleAuthorId, Long parentCommentAuthorId) {
        User commenter = userRepository.findById(commenterId).orElse(null);
        if (commenter == null) {
            return;
        }

        /* 通知文章作者 */
        if (articleAuthorId != null && !articleAuthorId.equals(commenterId)) {
            Article article = articleRepository.findById(articleId).orElse(null);
            notificationRepository.save(Notification.builder()
                    .userId(articleAuthorId)
                    .type("COMMENT")
                    .fromUserId(commenterId)
                    .fromUserName(commenter.getNickname())
                    .fromUserAvatar(commenter.getAvatar())
                    .articleId(articleId)
                    .articleTitle(article != null ? article.getTitle() : null)
                    .articleSlug(article != null ? article.getSlug() : null)
                    .content(commenter.getNickname() + " 评论了你的文章")
                    .build());
            log.info("通知文章作者 userId={} from={}", articleAuthorId, commenter.getNickname());
        }

        /* 通知父评论作者（如果是回复评论） */
        if (parentCommentAuthorId != null
                && !parentCommentAuthorId.equals(commenterId)
                && !parentCommentAuthorId.equals(articleAuthorId)) {
            notificationRepository.save(Notification.builder()
                    .userId(parentCommentAuthorId)
                    .type("COMMENT")
                    .fromUserId(commenterId)
                    .fromUserName(commenter.getNickname())
                    .fromUserAvatar(commenter.getAvatar())
                    .articleId(articleId)
                    .articleTitle(articleRepository.findById(articleId).map(Article::getTitle).orElse(null))
                    .articleSlug(articleRepository.findById(articleId).map(Article::getSlug).orElse(null))
                    .content(commenter.getNickname() + " 回复了你的评论")
                    .build());
            log.info("通知父评论作者 userId={} from={}", parentCommentAuthorId, commenter.getNickname());
        }
    }

    @Override
    @Transactional
    public void notifyLike(Long articleId, Long likerId, Long articleAuthorId) {
        if (articleAuthorId == null || articleAuthorId.equals(likerId)) {
            return;
        }

        User liker = userRepository.findById(likerId).orElse(null);
        if (liker == null) {
            return;
        }

        Article article = articleRepository.findById(articleId).orElse(null);
        notificationRepository.save(Notification.builder()
                .userId(articleAuthorId)
                .type("LIKE")
                .fromUserId(likerId)
                .fromUserName(liker.getNickname())
                .fromUserAvatar(liker.getAvatar())
                .articleId(articleId)
                .articleTitle(article != null ? article.getTitle() : null)
                .articleSlug(article != null ? article.getSlug() : null)
                .content(liker.getNickname() + " 赞了你的文章")
                .build());
        log.info("通知文章作者 userId={} liker={}", articleAuthorId, liker.getNickname());
    }

    @Override
    @Transactional
    public void notifyMessage(Long fromUserId, Long toUserId, String content) {
        User sender = userRepository.findById(fromUserId).orElse(null);
        if (sender == null) return;

        notificationRepository.save(Notification.builder()
                .userId(toUserId)
                .type("MESSAGE")
                .fromUserId(fromUserId)
                .fromUserName(sender.getNickname())
                .fromUserAvatar(sender.getAvatar())
                .content(sender.getNickname() + ": " + content)
                .build());
        log.info("通知私信 toUserId={} from={}", toUserId, sender.getNickname());
    }

    @Override
    @Transactional
    public void notifyCommentLike(Long commentId, Long likerId, Long commentAuthorId) {
        if (commentAuthorId == null || commentAuthorId.equals(likerId)) return;

        User liker = userRepository.findById(likerId).orElse(null);
        if (liker == null) return;

        Comment comment = commentRepository.findById(commentId).orElse(null);
        String contentPreview = comment != null && comment.getContent() != null
                ? comment.getContent().substring(0, Math.min(50, comment.getContent().length()))
                : "";

        Long articleId = comment != null && comment.getArticle() != null ? comment.getArticle().getId() : null;
        String articleTitle = comment != null && comment.getArticle() != null ? comment.getArticle().getTitle() : null;
        String articleSlug = comment != null && comment.getArticle() != null ? comment.getArticle().getSlug() : null;

        notificationRepository.save(Notification.builder()
                .userId(commentAuthorId)
                .type("LIKE")
                .fromUserId(likerId)
                .fromUserName(liker.getNickname())
                .fromUserAvatar(liker.getAvatar())
                .articleId(articleId)
                .articleTitle(articleTitle)
                .articleSlug(articleSlug)
                .content(liker.getNickname() + " 赞了你的评论: " + contentPreview)
                .build());
        log.info("通知评论被点赞 userId={} liker={} article={}", commentAuthorId, liker.getNickname(), articleSlug);
    }

    @Override
    @Transactional
    public void notifyFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            return;
        }

        User follower = userRepository.findById(followerId).orElse(null);
        if (follower == null) {
            return;
        }

        notificationRepository.save(Notification.builder()
                .userId(followingId)
                .type("FOLLOW")
                .fromUserId(followerId)
                .fromUserName(follower.getNickname())
                .fromUserAvatar(follower.getAvatar())
                .content(follower.getNickname() + " 关注了你")
                .build());
        log.info("通知被关注者 userId={} follower={}", followingId, follower.getNickname());
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<NotificationResponse> getMyNotifications(Long userId, int page, int pageSize, String type) {
        Page<Notification> notificationPage;
        if (type != null && !type.isBlank()) {
            notificationPage = notificationRepository
                    .findByUserIdAndTypeOrderByCreateTimeDesc(userId, type, PageRequest.of(page - 1, pageSize));
        } else {
            notificationPage = notificationRepository
                    .findByUserIdOrderByCreateTimeDesc(userId, PageRequest.of(page - 1, pageSize));
        }

        List<NotificationResponse> records = notificationPage.getContent().stream()
                .map(n -> new NotificationResponse(
                        n.getId(), n.getType(),
                        n.getFromUserId(), n.getFromUserName(), n.getFromUserAvatar(),
                        n.getArticleId(), n.getArticleTitle(), n.getArticleSlug(),
                        n.getContent(), n.getIsRead(), n.getCreateTime()))
                .toList();

        return new PageDTO<>(records, notificationPage.getTotalElements(), page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsRead(userId, false);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
        log.info("全部标记已读 userId={}", userId);
    }

    @Override
    @Transactional
    public void markTypeAsRead(Long userId, String type) {
        notificationRepository.markTypeNotificationsAsRead(userId, type);
        log.info("标记{}类通知已读 userId={}", type, userId);
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(404, "Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(403, "Not your notification");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}
