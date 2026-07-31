package com.blog.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.blog.dto.CommentAdminResponse;
import com.blog.dto.CommentCreateRequest;
import com.blog.dto.CommentResponse;
import com.blog.dto.PageDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.exception.BusinessException;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CommentRepository;
import com.blog.repository.CommentVoteRepository;
import com.blog.service.CommentService;
import com.blog.service.NotificationService;
import com.blog.util.SensitiveWordFilter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final EntityManager entityManager;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final NotificationService notificationService;
    private final CommentVoteRepository commentVoteRepository;

    public CommentServiceImpl(CommentRepository commentRepository,
                              ArticleRepository articleRepository,
                              EntityManager entityManager,
                              SensitiveWordFilter sensitiveWordFilter,
                              NotificationService notificationService,
                              CommentVoteRepository commentVoteRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.entityManager = entityManager;
        this.sensitiveWordFilter = sensitiveWordFilter;
        this.notificationService = notificationService;
        this.commentVoteRepository = commentVoteRepository;
    }

    // ========== Web Methods ==========

    /**
     * C端：获取文章评论列表（只返回已审核的顶级评论及其回复）
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByArticle(Long articleId, Long currentUserId, List<Long> blockedUserIds) {
        log.debug("C端获取文章评论 articleId={} currentUserId={}", articleId, currentUserId);
        /* 验证文章存在 */
        if (!articleRepository.existsById(articleId)) {
            throw new BusinessException(404, "Article not found");
        }

        /* 获取已审核通过的顶级评论（parent 为 null） */
        List<Comment> rootComments = commentRepository.findByArticleIdAndStatusAndParentIsNull(articleId, "APPROVED");

        return rootComments.stream()
                .filter(c -> blockedUserIds == null || blockedUserIds.isEmpty() || !isUserBlocked(c, blockedUserIds))
                .map(c -> toCommentResponse(c, currentUserId))
                .toList();
    }

    /**
     * C端：创建评论（含敏感词检查、关联登录用户、更新文章评论数）
     */
    @Override
    @Transactional
    public Long createComment(CommentCreateRequest request, String ip, String userAgent, Long userId) {
        log.info("创建评论 articleId={} userId={}", request.articleId(), userId);
        Article article = articleRepository.findById(request.articleId())
                .orElseThrow(() -> new BusinessException(404, "Article not found"));

        Comment comment = new Comment();
        comment.setContent(request.content());
        comment.setAuthorName(request.authorName());
        comment.setAuthorEmail(request.authorEmail());
        comment.setIp(ip);
        comment.setUserAgent(userAgent);

        /* 敏感词检查：无敏感词则自动通过，否则标记待审核 */
        if (sensitiveWordFilter.containsSensitiveWords(request.content())
                || sensitiveWordFilter.containsSensitiveWords(request.authorName())) {
            comment.setStatus("PENDING");
        } else {
            comment.setStatus("APPROVED");
        }

        comment.setArticle(article);

        /* 设置父评论（仅支持一级嵌套：不能回复子评论） */
        if (request.parentId() != null) {
            Comment parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(404, "Parent comment not found"));
            if (parent.getParent() != null) {
                throw new BusinessException(400, "评论仅支持一级嵌套，不能回复子评论");
            }
            comment.setParent(parent);
        }

        /* 关联登录用户 */
        if (userId != null) {
            com.blog.entity.User user = entityManager.getReference(com.blog.entity.User.class, userId);
            comment.setUser(user);
            if (!StringUtils.hasText(request.authorName())) {
                comment.setAuthorName(user.getNickname());
            }
            if (!StringUtils.hasText(request.authorEmail())) {
                comment.setAuthorEmail(user.getEmail());
            }
        }

        commentRepository.save(comment);

        /* 更新文章评论数 */
        int count = commentRepository.countByArticleIdAndStatus(request.articleId(), "APPROVED");
        articleRepository.updateCommentCount(request.articleId(), count);

        /* 触发通知：通知文章作者 + 父评论作者 */
        Long articleAuthorId = article.getUser() != null ? article.getUser().getId() : null;
        Long parentCommentAuthorId = null;
        if (comment.getParent() != null && comment.getParent().getUser() != null) {
            parentCommentAuthorId = comment.getParent().getUser().getId();
        }
        if (articleAuthorId != null) {
            notificationService.notifyComment(article.getId(), userId, articleAuthorId, parentCommentAuthorId);
        }

        log.info("评论创建成功 id={} status={}", comment.getId(), comment.getStatus());
        return comment.getId();
    }

    // ========== Admin Methods ==========

    /**
     * 后台：分页查询评论列表（可按状态筛选）
     */
    @Override
    @Transactional(readOnly = true)
    public PageDTO<CommentAdminResponse> getCommentPage(int page, int pageSize, String status) {
        log.debug("后台查询评论列表 page={} status={}", page, status);
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Comment> cq = cb.createQuery(Comment.class);
        Root<Comment> root = cq.from(Comment.class);
        root.fetch("article", JoinType.LEFT);
        root.fetch("user", JoinType.LEFT);

        List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("isDeleted"), false));
        if (status != null && !status.isBlank()) {
            predicates.add(cb.equal(root.get("status"), status));
        }

        cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        cq.orderBy(cb.desc(root.get("createTime")));

        // Count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Comment> countRoot = countQuery.from(Comment.class);
        List<jakarta.persistence.criteria.Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countRoot.get("isDeleted"), false));
        if (status != null && !status.isBlank()) {
            countPredicates.add(cb.equal(countRoot.get("status"), status));
        }
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new jakarta.persistence.criteria.Predicate[0]));

        long total = entityManager.createQuery(countQuery).getSingleResult();

        TypedQuery<Comment> typedQuery = entityManager.createQuery(cq);
        typedQuery.setFirstResult((page - 1) * pageSize);
        typedQuery.setMaxResults(pageSize);
        List<Comment> comments = typedQuery.getResultList();

        List<CommentAdminResponse> records = comments.stream()
                .map(this::toAdminResponse)
                .toList();

        return new PageDTO<>(records, total, page, pageSize);
    }

    /**
     * 后台：审核评论（通过/标记垃圾），并更新文章评论数
     */
    @Override
    @Transactional
    public void updateCommentStatus(Long id, String status) {
        log.info("审核评论 id={} status={}", id, status);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Comment not found"));
        comment.setStatus(status);
        commentRepository.save(comment);

        /* 审核后更新文章评论数 */
        if (comment.getArticle() != null) {
            int count = commentRepository.countByArticleIdAndStatus(comment.getArticle().getId(), "APPROVED");
            articleRepository.updateCommentCount(comment.getArticle().getId(), count);
        }
    }

    /**
     * 后台：删除评论并更新文章评论数
     */
    @Override
    @Transactional
    public void deleteComment(Long id) {
        log.warn("删除评论 id={}", id);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Comment not found"));
        commentRepository.delete(comment);

        /* 删除后更新文章评论数 */
        if (comment.getArticle() != null) {
            int count = commentRepository.countByArticleIdAndStatus(comment.getArticle().getId(), "APPROVED");
            articleRepository.updateCommentCount(comment.getArticle().getId(), count);
        }
    }

    /**
     * C端：用户删除自己的评论（软删除）
     */
    @Override
    @Transactional
    public void deleteOwnComment(Long commentId, Long userId) {
        log.info("用户删除自己的评论 id={} userId={}", commentId, userId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(404, "Comment not found"));

        /* 验证是否是自己的评论 */
        if (comment.getUser() == null || !userId.equals(comment.getUser().getId())) {
            throw new BusinessException(403, "Not allowed to delete this comment");
        }

        commentRepository.delete(comment);

        /* 更新文章评论数 */
        if (comment.getArticle() != null) {
            int count = commentRepository.countByArticleIdAndStatus(comment.getArticle().getId(), "APPROVED");
            articleRepository.updateCommentCount(comment.getArticle().getId(), count);
        }
    }

    // ========== Helper Methods ==========

    private CommentResponse toCommentResponse(Comment comment, Long currentUserId) {
        Long userId = comment.getUser() != null ? comment.getUser().getId() : null;
        String userNickname = comment.getUser() != null ? comment.getUser().getNickname() : null;
        String userAvatar = comment.getUser() != null ? comment.getUser().getAvatar() : null;

        /* 赞/踩统计 */
        int likeCount = commentVoteRepository.countByCommentIdAndVoteType(comment.getId(), "LIKE");
        int dislikeCount = commentVoteRepository.countByCommentIdAndVoteType(comment.getId(), "DISLIKE");

        /* 当前用户的投票状态 */
        boolean liked = false;
        boolean disliked = false;
        if (currentUserId != null) {
            var vote = commentVoteRepository.findByCommentIdAndUserId(comment.getId(), currentUserId);
            if (vote.isPresent()) {
                liked = "LIKE".equals(vote.get().getVoteType());
                disliked = "DISLIKE".equals(vote.get().getVoteType());
            }
        }

        List<CommentResponse> replies = Collections.emptyList();
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            replies = comment.getReplies().stream()
                    .filter(r -> "APPROVED".equals(r.getStatus()))
                    .map(r -> toCommentResponse(r, currentUserId))
                    .toList();
        }

        return new CommentResponse(
                comment.getId(),
                comment.getArticle() != null ? comment.getArticle().getId() : null,
                comment.getContent(),
                comment.getAuthorName(),
                comment.getAuthorEmail(),
                userId,
                userNickname,
                userAvatar,
                comment.getCreateTime(),
                replies,
                likeCount,
                dislikeCount,
                liked,
                disliked
        );
    }

    private CommentAdminResponse toAdminResponse(Comment comment) {
        Long userId = comment.getUser() != null ? comment.getUser().getId() : null;
        String userNickname = comment.getUser() != null ? comment.getUser().getNickname() : null;
        String articleTitle = comment.getArticle() != null ? comment.getArticle().getTitle() : null;
        Long articleId = comment.getArticle() != null ? comment.getArticle().getId() : null;
        String articleAuthorName = null;
        if (comment.getArticle() != null && comment.getArticle().getUser() != null) {
            articleAuthorName = comment.getArticle().getUser().getNickname();
        }

        return new CommentAdminResponse(
                comment.getId(),
                articleId,
                articleTitle,
                articleAuthorName,
                comment.getContent(),
                comment.getAuthorName(),
                comment.getAuthorEmail(),
                userId,
                userNickname,
                comment.getStatus(),
                comment.getIp(),
                comment.getCreateTime()
        );
    }

    /**
     * 检查评论作者是否在屏蔽列表中
     */
    private boolean isUserBlocked(Comment comment, List<Long> blockedUserIds) {
        if (comment.getUser() == null || blockedUserIds == null || blockedUserIds.isEmpty()) {
            return false;
        }
        return blockedUserIds.contains(comment.getUser().getId());
    }
}
