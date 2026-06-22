package com.blog.service.impl;

import com.blog.common.CacheNames;
import com.blog.dto.*;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CategoryRepository;
import com.blog.repository.TagRepository;
import com.blog.repository.UserRepository;
import com.blog.service.MarkdownRenderer;
import com.blog.service.UserArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserArticleServiceImpl implements UserArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final MarkdownRenderer markdownRenderer;

    @Override
    @Transactional
    @CacheEvict(value = {CacheNames.ARTICLE_LIST, CacheNames.ARTICLE_ARCHIVE, CacheNames.FEATURED, CacheNames.SITE_INFO}, allEntries = true)
    public Long createArticle(Long userId, ArticleCreateRequest request) {
        log.info("用户创建文章 title={} userId={}", request.title(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "User not found"));

        Article article = new Article();
        article.setTitle(request.title());
        article.setContent(request.content());
        article.setSummary(request.summary());
        article.setCoverImage(request.coverImage());
        article.setStatus(request.status() != null ? request.status() : "DRAFT");
        article.setViewCount(0L);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setIsTop(false);
        article.setIsFeatured(false);
        article.setUser(user);

        if (request.slug() != null && !request.slug().isBlank()) {
            String slug = request.slug();
            // 如果 slug 重复，追加随机后缀
            while (articleRepository.existsBySlug(slug)) {
                slug = request.slug() + "-" + UUID.randomUUID().toString().substring(0, 4);
            }
            article.setSlug(slug);
        } else {
            article.setSlug(UUID.randomUUID().toString().substring(0, 8));
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(404, "Category not found"));
            article.setCategory(category);
        }

        if (request.tags() != null && !request.tags().isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.tags()));
            article.setTags(tags);
        }

        if ("PUBLISHED".equals(article.getStatus())) {
            article.setPublishedAt(LocalDateTime.now());
        }

        articleRepository.save(article);
        log.info("用户文章创建成功 id={} userId={}", article.getId(), userId);
        return article.getId();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ARTICLE_HTML, key = "#articleId"),
            @CacheEvict(value = CacheNames.ARTICLE_DETAIL, key = "#articleId"),
            @CacheEvict(value = CacheNames.ARTICLE_PREV_NEXT, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_ARCHIVE, allEntries = true),
            @CacheEvict(value = CacheNames.FEATURED, allEntries = true),
            @CacheEvict(value = CacheNames.SITE_INFO, allEntries = true)
    })
    public void updateArticle(Long userId, Long articleId, ArticleUpdateRequest request) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(404, "Article not found"));

        if (!article.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "You can only edit your own articles");
        }

        if (request.title() != null) {
            article.setTitle(request.title());
        }
        if (request.content() != null) {
            article.setContent(request.content());
        }
        if (request.summary() != null) {
            article.setSummary(request.summary());
        }
        if (request.coverImage() != null) {
            article.setCoverImage(request.coverImage());
        }
        if (request.status() != null) {
            String oldStatus = article.getStatus();
            article.setStatus(request.status());
            if ("PUBLISHED".equals(request.status()) && !"PUBLISHED".equals(oldStatus)) {
                article.setPublishedAt(LocalDateTime.now());
            }
        }
        if (request.slug() != null && !request.slug().isBlank()) {
            if (!request.slug().equals(article.getSlug()) && articleRepository.existsBySlug(request.slug())) {
                throw new BusinessException(400, "Slug already exists");
            }
            article.setSlug(request.slug());
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(404, "Category not found"));
            article.setCategory(category);
        }

        if (request.tags() != null) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.tags()));
            article.setTags(tags);
        }

        articleRepository.save(article);
        log.info("用户文章更新完成 id={} userId={}", articleId, userId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ARTICLE_HTML, key = "#articleId"),
            @CacheEvict(value = CacheNames.ARTICLE_DETAIL, key = "#articleId"),
            @CacheEvict(value = CacheNames.ARTICLE_PREV_NEXT, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_ARCHIVE, allEntries = true),
            @CacheEvict(value = CacheNames.FEATURED, allEntries = true),
            @CacheEvict(value = CacheNames.SITE_INFO, allEntries = true)
    })
    public void deleteArticle(Long userId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(404, "Article not found"));

        if (!article.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "You can only delete your own articles");
        }

        article.setIsDeleted(true);
        articleRepository.save(article);
        log.info("用户文章删除 id={} userId={}", articleId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleDetailResponse getMyArticle(Long userId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BusinessException(404, "Article not found"));

        if (!article.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "You can only view your own articles");
        }

        String renderedHtml = markdownRenderer.render(article.getContent());
        return toDetailResponse(article, renderedHtml);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ArticleWebResponse> getMyArticles(Long userId, int page, int pageSize, String status, String keyword) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Article> articlePage;

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasStatus = status != null && !status.isBlank();

        if (hasStatus && hasKeyword) {
            articlePage = articleRepository.findByUserIdAndStatusAndKeyword(userId, status, keyword, pageRequest);
        } else if (hasKeyword) {
            articlePage = articleRepository.findByUserIdAndKeyword(userId, keyword, pageRequest);
        } else if (hasStatus) {
            articlePage = articleRepository.findByUserIdAndStatus(userId, status, pageRequest);
        } else {
            articlePage = articleRepository.findByUserId(userId, pageRequest);
        }

        List<ArticleWebResponse> records = articlePage.getContent().stream()
                .map(this::toWebResponse)
                .collect(Collectors.toList());

        return new PageDTO<>(records, articlePage.getTotalElements(), page, pageSize);
    }

    // ========== Helper Methods ==========

    private ArticleWebResponse toWebResponse(Article article) {
        String categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
        Set<TagDTO> tagDTOs = article.getTags() != null
                ? article.getTags().stream().map(t -> new TagDTO(t.getId(), t.getName(), t.getSlug(), 0L)).collect(Collectors.toSet())
                : Set.of();

        Long authorId = article.getUser() != null ? article.getUser().getId() : null;
        String authorName = article.getUser() != null ? article.getUser().getNickname() : null;
        String authorAvatar = article.getUser() != null ? article.getUser().getAvatar() : null;

        return new ArticleWebResponse(
                article.getId(), article.getTitle(), article.getSlug(), article.getSummary(),
                article.getCoverImage(), categoryName, tagDTOs,
                article.getViewCount(), article.getLikeCount(), article.getCommentCount(),
                article.getPublishedAt(), article.getIsTop(), article.getIsFeatured(),
                authorId, authorName, authorAvatar, article.getStatus()
        );
    }

    private ArticleDetailResponse toDetailResponse(Article article, String renderedContent) {
        String categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
        Set<TagDTO> tagDTOs = article.getTags() != null
                ? article.getTags().stream().map(t -> new TagDTO(t.getId(), t.getName(), t.getSlug(), 0L)).collect(Collectors.toSet())
                : Set.of();

        Long authorId = article.getUser() != null ? article.getUser().getId() : null;
        String authorName = article.getUser() != null ? article.getUser().getNickname() : null;
        String authorAvatar = article.getUser() != null ? article.getUser().getAvatar() : null;

        return new ArticleDetailResponse(
                article.getId(), article.getTitle(), article.getSlug(), renderedContent,
                article.getSummary(), article.getCoverImage(), categoryName, tagDTOs,
                article.getViewCount(), article.getLikeCount(), article.getCommentCount(),
                article.getPublishedAt(), article.getIsTop(), article.getIsFeatured(),
                article.getCreateTime(), article.getUpdateTime(),
                authorId, authorName, authorAvatar, false, article.getStatus()
        );
    }
}
