package com.blog.service.impl;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blog.common.CacheNames;
import com.blog.dto.ArchiveArticle;
import com.blog.dto.ArchiveItem;
import com.blog.dto.ArticleAdminResponse;
import com.blog.dto.ArticleCreateRequest;
import com.blog.dto.ArticleDetailResponse;
import com.blog.dto.ArticleUpdateRequest;
import com.blog.dto.ArticleWebResponse;
import com.blog.dto.HistoryItemResponse;
import com.blog.dto.LikeToggleResponse;
import com.blog.dto.PageDTO;
import com.blog.dto.PrevNextDTO;
import com.blog.dto.PrevNextItem;
import com.blog.dto.TagDTO;
import com.blog.entity.Article;
import com.blog.entity.ArticleLike;
import com.blog.entity.ArticleViewHistory;
import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.exception.BusinessException;
import com.blog.repository.ArticleLikeRepository;
import com.blog.repository.ArticleRepository;
import com.blog.repository.ArticleViewHistoryRepository;
import com.blog.repository.CategoryRepository;
import com.blog.repository.TagRepository;
import com.blog.repository.UserRepository;
import com.blog.service.ArticleService;
import com.blog.service.MarkdownRenderer;
import com.blog.service.NotificationService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
public class ArticleServiceImpl implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final ArticleViewHistoryRepository viewHistoryRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final EntityManager entityManager;
    private final MarkdownRenderer markdownRenderer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;

    public ArticleServiceImpl(ArticleRepository articleRepository,
                              CategoryRepository categoryRepository,
                              TagRepository tagRepository,
                              UserRepository userRepository,
                              ArticleViewHistoryRepository viewHistoryRepository,
                              ArticleLikeRepository articleLikeRepository,
                              EntityManager entityManager,
                              MarkdownRenderer markdownRenderer,
                              RedisTemplate<String, Object> redisTemplate,
                              NotificationService notificationService) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.viewHistoryRepository = viewHistoryRepository;
        this.articleLikeRepository = articleLikeRepository;
        this.entityManager = entityManager;
        this.markdownRenderer = markdownRenderer;
        this.redisTemplate = redisTemplate;
        this.notificationService = notificationService;
    }

    // ========== Admin Methods ==========

    /**
     * 后台：分页查询文章列表（支持关键字/分类/状态筛选）
     */
    @Override
    public PageDTO<ArticleAdminResponse> getAdminArticlePage(int page, int pageSize, String keyword, Long categoryId, String status) {
        log.info("后台查询文章列表 page={} keyword={} categoryId={} status={}", page, keyword, categoryId, status);
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Article> cq = cb.createQuery(Article.class);
        Root<Article> root = cq.from(Article.class);
        root.fetch("category", JoinType.LEFT);
        root.fetch("tags", JoinType.LEFT);
        root.fetch("user", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("isDeleted"), false));

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim() + "%";
            Predicate titleLike = cb.like(root.get("title"), pattern);
            Predicate contentLike = cb.like(root.get("content"), pattern);
            predicates.add(cb.or(titleLike, contentLike));
        }
        if (categoryId != null) {
            predicates.add(cb.equal(root.get("category").get("id"), categoryId));
        }
        if (status != null && !status.isBlank()) {
            predicates.add(cb.equal(root.get("status"), status));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.distinct(true);
        cq.orderBy(cb.desc(root.get("createTime")));

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Article> countRoot = countQuery.from(Article.class);
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countRoot.get("isDeleted"), false));
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim() + "%";
            Predicate titleLike = cb.like(countRoot.get("title"), pattern);
            Predicate contentLike = cb.like(countRoot.get("content"), pattern);
            countPredicates.add(cb.or(titleLike, contentLike));
        }
        if (categoryId != null) {
            countPredicates.add(cb.equal(countRoot.get("category").get("id"), categoryId));
        }
        if (status != null && !status.isBlank()) {
            countPredicates.add(cb.equal(countRoot.get("status"), status));
        }
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        long total = entityManager.createQuery(countQuery).getSingleResult();

        TypedQuery<Article> typedQuery = entityManager.createQuery(cq);
        typedQuery.setFirstResult((page - 1) * pageSize);
        typedQuery.setMaxResults(pageSize);
        List<Article> articles = typedQuery.getResultList();

        List<ArticleAdminResponse> records = articles.stream()
                .map(this::toAdminResponse)
                .toList();

        log.info("查询结果 total={} records={}", total, records.size());
        return new PageDTO<>(records, total, page, pageSize);
    }

    /**
     * 后台：根据 ID 获取文章详情
     */
    @Override
    @Transactional(readOnly = true)
    public ArticleAdminResponse getAdminArticleById(Long id) {
        Article article = findArticleById(id);
        log.debug("获取文章详情 id={}", id);
        return toAdminResponse(article);
    }

    /**
     * 后台：创建新文章
     */
    @Override
    @Transactional
    @CacheEvict(value = {CacheNames.ARTICLE_LIST, CacheNames.ARTICLE_ARCHIVE, CacheNames.FEATURED}, allEntries = true)
    public Long createArticle(ArticleCreateRequest request, Long userId) {
        log.info("创建文章 title={} userId={}", request.title(), userId);

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
        article.setIsFeatured(request.isFeatured() != null ? request.isFeatured() : false);

        /* 生成 Slug：优先使用请求中指定的，否则自动生成 */
        if (request.slug() != null && !request.slug().isBlank()) {
            String slug = request.slug();
            while (articleRepository.existsBySlug(slug)) {
                slug = request.slug() + "-" + UUID.randomUUID().toString().substring(0, 4);
            }
            article.setSlug(slug);
        } else {
            article.setSlug(generateSlug());
        }

        /* 设置作者 */
        article.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "User not found")));

        /* 设置分类 */
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(404, "Category not found"));
            article.setCategory(category);
        }

        /* 设置标签 */
        if (request.tags() != null && !request.tags().isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.tags()));
            article.setTags(tags);
        }

        /* 如果是发布状态，设置发布时间 */
        if ("PUBLISHED".equals(article.getStatus())) {
            article.setPublishedAt(LocalDateTime.now());
        }

        articleRepository.save(article);
        log.info("文章创建成功 id={}", article.getId());
        return article.getId();
    }

    /**
     * 后台：更新文章
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ARTICLE_HTML, key = "#id"),
            @CacheEvict(value = CacheNames.ARTICLE_DETAIL, key = "#id"),
            @CacheEvict(value = CacheNames.ARTICLE_PREV_NEXT, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_ARCHIVE, allEntries = true),
            @CacheEvict(value = CacheNames.FEATURED, allEntries = true),
            @CacheEvict(value = CacheNames.SITE_INFO, allEntries = true)
    })
    public void updateArticle(Long id, ArticleUpdateRequest request) {
        log.info("更新文章 id={}", id);
        Article article = findArticleById(id);

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
            /* 从非发布转为发布时，设置发布时间 */
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

        if (request.isFeatured() != null) {
            article.setIsFeatured(request.isFeatured());
        }

        articleRepository.save(article);
        log.info("文章更新完成 id={}", id);
    }

    /**
     * 后台：删除文章
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ARTICLE_HTML, key = "#id"),
            @CacheEvict(value = CacheNames.ARTICLE_DETAIL, key = "#id"),
            @CacheEvict(value = CacheNames.ARTICLE_PREV_NEXT, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_ARCHIVE, allEntries = true),
            @CacheEvict(value = CacheNames.FEATURED, allEntries = true),
            @CacheEvict(value = CacheNames.SITE_INFO, allEntries = true)
    })
    public void deleteArticle(Long id) {
        log.warn("软删除文章 id={}", id);
        Article article = findArticleById(id);
        article.setIsDeleted(true);
        articleRepository.save(article);
    }

    /**
     * 后台：更新文章状态（发布/隐藏切换）
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ARTICLE_HTML, key = "#id"),
            @CacheEvict(value = CacheNames.ARTICLE_DETAIL, key = "#id"),
            @CacheEvict(value = CacheNames.ARTICLE_PREV_NEXT, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.ARTICLE_ARCHIVE, allEntries = true),
            @CacheEvict(value = CacheNames.FEATURED, allEntries = true),
            @CacheEvict(value = CacheNames.SITE_INFO, allEntries = true)
    })
    public void updateArticleStatus(Long id, String status) {
        log.info("更新文章状态 id={} status={}", id, status);
        Article article = findArticleById(id);
        article.setStatus(status);
        if ("PUBLISHED".equals(status)) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleRepository.save(article);
    }

    // ========== Web Methods ==========

    /**
     * C端：分页查询已发布的公开文章
     */
    @Override
    @Cacheable(value = CacheNames.ARTICLE_LIST,
            key = "#page + ':' + #pageSize + ':' + (#categoryId == null ? 'all' : #categoryId) + ':' + (#tagSlug == null ? 'all' : #tagSlug)")
    public PageDTO<ArticleWebResponse> getPublishedArticles(int page, int pageSize, Long categoryId, String tagSlug) {
        log.info("C端查询文章列表 page={} categoryId={} tagSlug={}", page, categoryId, tagSlug);
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "isTop", "publishedAt"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Article> cq = cb.createQuery(Article.class);
        Root<Article> root = cq.from(Article.class);
        root.fetch("user", JoinType.LEFT);
        root.fetch("category", JoinType.LEFT);
        root.fetch("tags", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("isDeleted"), false));
        predicates.add(cb.equal(root.get("status"), "PUBLISHED"));

        if (categoryId != null) {
            predicates.add(cb.equal(root.get("category").get("id"), categoryId));
        }
        if (tagSlug != null && !tagSlug.isBlank()) {
            Join<Article, Tag> tagJoin = root.join("tags");
            predicates.add(cb.equal(tagJoin.get("slug"), tagSlug));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.distinct(true);
        cq.orderBy(cb.desc(root.get("isTop")), cb.desc(root.get("publishedAt")));

        // Count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Article> countRoot = countQuery.from(Article.class);
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countRoot.get("isDeleted"), false));
        countPredicates.add(cb.equal(countRoot.get("status"), "PUBLISHED"));
        if (categoryId != null) {
            countPredicates.add(cb.equal(countRoot.get("category").get("id"), categoryId));
        }
        if (tagSlug != null && !tagSlug.isBlank()) {
            Join<Article, Tag> countTagJoin = countRoot.join("tags");
            countPredicates.add(cb.equal(countTagJoin.get("slug"), tagSlug));
        }
        countQuery.select(cb.countDistinct(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        long total = entityManager.createQuery(countQuery).getSingleResult();

        TypedQuery<Article> typedQuery = entityManager.createQuery(cq);
        typedQuery.setFirstResult((page - 1) * pageSize);
        typedQuery.setMaxResults(pageSize);
        List<Article> articles = typedQuery.getResultList();

        List<ArticleWebResponse> records = articles.stream()
                .map(this::toWebResponse)
                .toList();

        return new PageDTO<>(records, total, page, pageSize);
    }

    /**
     * C端：获取精选文章列表
     * 优先返回手动精选（isFeatured = true），不足 6 篇则按热度补充。
     * 热度公式：viewCount + likeCount * 3 + commentCount * 5
     * 缓存 1 天，手动精选变更时通过 @CacheEvict 即时失效。
     */
    @Override
    @Cacheable(value = CacheNames.FEATURED)
    public List<ArticleWebResponse> getFeaturedArticles() {
        log.info("获取精选文章列表");
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. 手动精选：isFeatured = true
        CriteriaQuery<Article> featuredQuery = cb.createQuery(Article.class);
        Root<Article> featuredRoot = featuredQuery.from(Article.class);
        featuredRoot.fetch("user", JoinType.LEFT);
        featuredRoot.fetch("category", JoinType.LEFT);
        featuredRoot.fetch("tags", JoinType.LEFT);
        featuredQuery.where(
                cb.equal(featuredRoot.get("isDeleted"), false),
                cb.equal(featuredRoot.get("status"), "PUBLISHED"),
                cb.equal(featuredRoot.get("isFeatured"), true)
        );
        featuredQuery.distinct(true);
        featuredQuery.orderBy(cb.desc(featuredRoot.get("publishedAt")));

        List<Article> manual = entityManager.createQuery(featuredQuery)
                .setMaxResults(6)
                .getResultList();

        List<Article> result = new ArrayList<>(manual);

        // 2. 手动精选不足 6 篇，按热度补充
        if (result.size() < 6) {
            Set<Long> excludedIds = result.stream().map(Article::getId).collect(Collectors.toSet());

            CriteriaQuery<Article> popularQuery = cb.createQuery(Article.class);
            Root<Article> popularRoot = popularQuery.from(Article.class);
            popularRoot.fetch("user", JoinType.LEFT);
            popularRoot.fetch("category", JoinType.LEFT);
            popularRoot.fetch("tags", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(popularRoot.get("isDeleted"), false));
            predicates.add(cb.equal(popularRoot.get("status"), "PUBLISHED"));
            if (!excludedIds.isEmpty()) {
                predicates.add(cb.not(popularRoot.get("id").in(excludedIds)));
            }

            popularQuery.where(predicates.toArray(new Predicate[0]));
            popularQuery.distinct(true);
            // 热度排序：按 viewCount 降序为主，likeCount、commentCount 为辅
            // popularQuery.orderBy(
            //         cb.desc(popularRoot.get("viewCount")),
            //         cb.desc(popularRoot.get("likeCount")),
            //         cb.desc(popularRoot.get("commentCount"))
            // );

            // 热度排序：viewCount + likeCount * 3 + commentCount * 5
            // 写法一
            // Expression<Integer> score = cb.prod(popularRoot.get("likeCount"), cb.literal(3));
            // score = cb.sum(score, cb.prod(popularRoot.get("commentCount"), cb.literal(5)));
            // score = cb.sum(popularRoot.get("viewCount"), score);
            // popularQuery.orderBy(cb.desc(score));
            
            // 写法二
            Expression<Integer> score = cb.sum(
                popularRoot.get("viewCount"),
                cb.sum(
                    cb.prod(popularRoot.get("likeCount"), cb.literal(3)),
                    cb.prod(popularRoot.get("commentCount"), cb.literal(5))
                )
            );
            popularQuery.orderBy(cb.desc(score));

            List<Article> popular = entityManager.createQuery(popularQuery)
                    .setMaxResults(6 - result.size())
                    .getResultList();

            result.addAll(popular);
        }

        return result.stream()
                .map(this::toWebResponse)
                .toList();
    }

    /**
     * C端：根据 Slug 获取文章详情（同时增加浏览量）
     * 浏览量改为 Redis INCR，定时回写 MySQL，避免每次访问都写库。
     * 详情缓存按 articleId 维度，浏览量在缓存命中时从 Redis 实时读取合并。
     */
    @Override
    @Transactional
    public ArticleDetailResponse getArticleBySlug(String slug) {
        log.debug("访问文章 slug={}", slug);
        Article article = articleRepository.findBySlugAndIsDeleted(slug, false)
                .orElseThrow(() -> new BusinessException(404, "Article not found"));

        /* 浏览量自增到 Redis Hash，定时回写 */
        Long pendingIncr = redisTemplate.opsForHash().increment(CacheNames.VIEW_COUNT_HASH, String.valueOf(article.getId()), 1);
        /* 合并 DB 中的基础浏览量 + Redis 待回写增量，作为实时展示值 */
        long realtimeViewCount = article.getViewCount() + pendingIncr;

        /* 记录登录用户的浏览历史 */
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            viewHistoryRepository.save(ArticleViewHistory.builder()
                    .userId(currentUserId)
                    .articleId(article.getId())
                    .articleTitle(article.getTitle())
                    .articleSlug(article.getSlug())
                    .build());
        }

        /* 用缓存渲染 HTML（文章更新时主动失效） */
        String renderedContent = markdownRenderer.renderForArticle(article.getId(), article.getContent());

        boolean liked = hasUserLiked(currentUserId, article.getId());
        return toDetailResponse(article, renderedContent, realtimeViewCount, liked);
    }

    /**
     * C端：获取上一篇/下一篇文章
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.ARTICLE_PREV_NEXT, key = "#slug")
    public PrevNextDTO getPrevNextArticle(String slug) {
        Article current = articleRepository.findBySlugAndIsDeleted(slug, false)
                .orElseThrow(() -> new BusinessException(404, "Article not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        /* 查找上一篇文章（发布时间更早） */
        CriteriaQuery<Article> prevQuery = cb.createQuery(Article.class);
        Root<Article> prevRoot = prevQuery.from(Article.class);
        prevQuery.select(prevRoot);
        prevQuery.where(
                cb.equal(prevRoot.get("isDeleted"), false),
                cb.equal(prevRoot.get("status"), "PUBLISHED"),
                cb.lessThan(prevRoot.get("publishedAt"), current.getPublishedAt())
        );
        prevQuery.orderBy(cb.desc(prevRoot.get("publishedAt")));

        List<Article> prevResults = entityManager.createQuery(prevQuery)
                .setMaxResults(1)
                .getResultList();
        PrevNextItem prev = prevResults.isEmpty() ? null : toPrevNextItem(prevResults.get(0));

        /* 查找下一篇文章（发布时间更晚） */
        CriteriaQuery<Article> nextQuery = cb.createQuery(Article.class);
        Root<Article> nextRoot = nextQuery.from(Article.class);
        nextQuery.select(nextRoot);
        nextQuery.where(
                cb.equal(nextRoot.get("isDeleted"), false),
                cb.equal(nextRoot.get("status"), "PUBLISHED"),
                cb.greaterThan(nextRoot.get("publishedAt"), current.getPublishedAt())
        );
        nextQuery.orderBy(cb.asc(nextRoot.get("publishedAt")));

        List<Article> nextResults = entityManager.createQuery(nextQuery)
                .setMaxResults(1)
                .getResultList();
        PrevNextItem next = nextResults.isEmpty() ? null : toPrevNextItem(nextResults.get(0));

        return new PrevNextDTO(prev, next);
    }

    /**
     * C端：点赞文章（toggle）
     * 登录用户：每人每篇只能赞一次，再次点击取消
     * 匿名用户：保持旧行为（Redis 计数）
     */
    @Override
    @Transactional
    public LikeToggleResponse likeArticle(String slug) {
        Article article = articleRepository.findBySlugAndIsDeleted(slug, false)
                .orElseThrow(() -> new BusinessException(404, "Article not found"));

        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            // 登录用户：toggle 行为
            boolean alreadyLiked = articleLikeRepository.existsByUserIdAndArticleId(currentUserId, article.getId());
            if (alreadyLiked) {
                articleLikeRepository.deleteByUserIdAndArticleId(currentUserId, article.getId());
                article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
                articleRepository.save(article);
                log.info("取消点赞 userId={} articleId={}", currentUserId, article.getId());
                return new LikeToggleResponse(false, article.getLikeCount());
            } else {
                articleLikeRepository.save(ArticleLike.builder()
                        .userId(currentUserId)
                        .articleId(article.getId())
                        .articleTitle(article.getTitle())
                        .articleSlug(article.getSlug())
                        .build());
                article.setLikeCount(article.getLikeCount() + 1);
                articleRepository.save(article);

                /* 触发通知：通知文章作者 */
                Long articleAuthorId = article.getUser() != null ? article.getUser().getId() : null;
                if (articleAuthorId != null) {
                    notificationService.notifyLike(article.getId(), currentUserId, articleAuthorId);
                }

                log.info("点赞 userId={} articleId={}", currentUserId, article.getId());
                return new LikeToggleResponse(true, article.getLikeCount());
            }
        } else {
            // 匿名用户：保持旧行为
            Long pendingIncr = redisTemplate.opsForHash().increment(CacheNames.LIKE_COUNT_HASH, String.valueOf(article.getId()), 1);
            int newCount = article.getLikeCount() + pendingIncr.intValue();
            log.info("匿名点赞 slug={} likeCount={}", slug, newCount);
            return new LikeToggleResponse(true, newCount);
        }
    }

    /**
     * C端：搜索文章（标题/内容/摘要匹配关键字）
     */
    @Override
    public PageDTO<ArticleWebResponse> searchArticles(String keyword, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "publishedAt"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Article> cq = cb.createQuery(Article.class);
        Root<Article> root = cq.from(Article.class);
        root.fetch("user", JoinType.LEFT);
        root.fetch("category", JoinType.LEFT);
        root.fetch("tags", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("isDeleted"), false));
        predicates.add(cb.equal(root.get("status"), "PUBLISHED"));

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim() + "%";
            predicates.add(cb.or(
                    cb.like(root.get("title"), pattern),
                    cb.like(root.get("content"), pattern),
                    cb.like(root.get("summary"), pattern)
            ));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.distinct(true);
        cq.orderBy(cb.desc(root.get("publishedAt")));

        // Count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Article> countRoot = countQuery.from(Article.class);
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countRoot.get("isDeleted"), false));
        countPredicates.add(cb.equal(countRoot.get("status"), "PUBLISHED"));
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim() + "%";
            countPredicates.add(cb.or(
                    cb.like(countRoot.get("title"), pattern),
                    cb.like(countRoot.get("content"), pattern),
                    cb.like(countRoot.get("summary"), pattern)
            ));
        }
        countQuery.select(cb.countDistinct(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        long total = entityManager.createQuery(countQuery).getSingleResult();

        TypedQuery<Article> typedQuery = entityManager.createQuery(cq);
        typedQuery.setFirstResult((page - 1) * pageSize);
        typedQuery.setMaxResults(pageSize);
        List<Article> articles = typedQuery.getResultList();

        List<ArticleWebResponse> records = articles.stream()
                .map(this::toWebResponse)
                .toList();

        return new PageDTO<>(records, total, page, pageSize);
    }

    /**
     * C端：获取文章归档（按月分组）
     */
    @Override
    @Cacheable(value = CacheNames.ARTICLE_ARCHIVE)
    public List<ArchiveItem> getArchives() {
        log.debug("获取文章归档");
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Article> cq = cb.createQuery(Article.class);
        Root<Article> root = cq.from(Article.class);
        root.fetch("category", JoinType.LEFT);
        root.fetch("tags", JoinType.LEFT);

        cq.where(
                cb.equal(root.get("isDeleted"), false),
                cb.equal(root.get("status"), "PUBLISHED"),
                cb.isNotNull(root.get("publishedAt"))
        );
        cq.orderBy(cb.desc(root.get("publishedAt")));

        List<Article> articles = entityManager.createQuery(cq).getResultList();

        Map<String, List<ArchiveArticle>> grouped = articles.stream()
                .collect(Collectors.groupingBy(
                        a -> YearMonth.from(a.getPublishedAt()).format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        LinkedHashMap::new,
                        Collectors.mapping(this::toArchiveArticle, Collectors.toList())
                ));

        return grouped.entrySet().stream()
                .map(entry -> new ArchiveItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 生成 RSS Feed XML（取最近 20 篇已发布文章）
     */
    @Override
    public String generateRss() {
        List<Article> articles = articleRepository.findByStatusAndIsDeleted("PUBLISHED", false,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "publishedAt")))
                .getContent();

        String siteTitle = "Blog";
        String siteDescription = "Personal Blog";
        String siteUrl = "https://blog.example.com";

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>").append(escapeXml(siteTitle)).append("</title>\n");
        xml.append("    <link>").append(escapeXml(siteUrl)).append("</link>\n");
        xml.append("    <description>").append(escapeXml(siteDescription)).append("</description>\n");
        xml.append("    <language>zh-CN</language>\n");
        xml.append("    <atom:link href=\"").append(escapeXml(siteUrl)).append("/rss.xml\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        /* 遍历文章生成 RSS 条目 */
        for (Article article : articles) {
            xml.append("    <item>\n");
            xml.append("      <title>").append(escapeXml(article.getTitle())).append("</title>\n");
            xml.append("      <link>").append(escapeXml(siteUrl)).append("/article/").append(escapeXml(article.getSlug())).append("</link>\n");
            xml.append("      <guid>").append(escapeXml(siteUrl)).append("/article/").append(escapeXml(article.getSlug())).append("</guid>\n");
            if (article.getSummary() != null) {
                xml.append("      <description>").append(escapeXml(article.getSummary())).append("</description>\n");
            }
            if (article.getPublishedAt() != null) {
                xml.append("      <pubDate>").append(article.getPublishedAt().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("</pubDate>\n");
            }
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>\n");
        return xml.toString();
    }

    @Override
    public String generateSitemap() {
        String siteUrl = "https://blog.example.com";
        List<Article> articles = articleRepository.findByStatusAndIsDeleted("PUBLISHED", false,
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "publishedAt"))).getContent();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Homepage
        xml.append("  <url>\n");
        xml.append("    <loc>").append(siteUrl).append("</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // Static pages
        String[][] staticPages = {
                {"/archive", "weekly", "0.8"},
                {"/tags", "weekly", "0.7"},
                {"/about", "monthly", "0.6"},
                {"/links", "monthly", "0.5"},
        };
        for (String[] p : staticPages) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(siteUrl).append(p[0]).append("</loc>\n");
            xml.append("    <changefreq>").append(p[1]).append("</changefreq>\n");
            xml.append("    <priority>").append(p[2]).append("</priority>\n");
            xml.append("  </url>\n");
        }

        // Articles
        for (Article article : articles) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(siteUrl).append("/article/").append(escapeXml(article.getSlug())).append("</loc>\n");
            LocalDateTime lastMod = article.getUpdateTime() != null ? article.getUpdateTime()
                    : (article.getPublishedAt() != null ? article.getPublishedAt() : article.getCreateTime());
            if (lastMod != null) {
                xml.append("    <lastmod>").append(lastMod.toLocalDate()).append("</lastmod>\n");
            }
            xml.append("    <changefreq>monthly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    // ========== Helper Methods ==========

    /**
     * 根据 ID 查找文章，不存在则抛出异常
     */
    private Article findArticleById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Article not found"));
    }

    /**
     * 生成随机 8 位 Slug
     */
    private String generateSlug() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** 转换为后台管理响应 DTO */
    private ArticleAdminResponse toAdminResponse(Article article) {
        String categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
        Long categoryId = article.getCategory() != null ? article.getCategory().getId() : null;
        String authorName = article.getUser() != null ? article.getUser().getNickname() : null;
        Long userId = article.getUser() != null ? article.getUser().getId() : null;
        Set<TagDTO> tagDTOs = article.getTags() != null
                ? article.getTags().stream().map(t -> new TagDTO(t.getId(), t.getName(), t.getSlug(), 0L)).collect(Collectors.toSet())
                : Collections.emptySet();

        return new ArticleAdminResponse(
                article.getId(), article.getTitle(), article.getSlug(), article.getContent(),
                article.getSummary(), article.getCoverImage(), article.getStatus(),
                article.getViewCount(), article.getLikeCount(), article.getCommentCount(),
                article.getIsTop(), article.getIsFeatured(), categoryName, categoryId, tagDTOs, userId, authorName,
                article.getPublishedAt(), article.getCreateTime(), article.getUpdateTime()
        );
    }

    /** 转换为 C端列表响应 DTO */
    private ArticleWebResponse toWebResponse(Article article) {
        String categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
        Set<TagDTO> tagDTOs = article.getTags() != null
                ? article.getTags().stream().map(t -> new TagDTO(t.getId(), t.getName(), t.getSlug(), 0L)).collect(Collectors.toSet())
                : Collections.emptySet();

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

    /** 转换为 C端详情响应 DTO（接受预渲染的 HTML、实时浏览量、当前用户点赞状态） */
    private ArticleDetailResponse toDetailResponse(Article article, String renderedContent, long realtimeViewCount, boolean isLiked) {
        String categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
        Set<TagDTO> tagDTOs = article.getTags() != null
                ? article.getTags().stream().map(t -> new TagDTO(t.getId(), t.getName(), t.getSlug(), 0L)).collect(Collectors.toSet())
                : Collections.emptySet();

        Long authorId = article.getUser() != null ? article.getUser().getId() : null;
        String authorName = article.getUser() != null ? article.getUser().getNickname() : null;
        String authorAvatar = article.getUser() != null ? article.getUser().getAvatar() : null;

        return new ArticleDetailResponse(
                article.getId(), article.getTitle(), article.getSlug(), renderedContent,
                article.getSummary(), article.getCoverImage(), categoryName, tagDTOs,
                realtimeViewCount, article.getLikeCount(), article.getCommentCount(),
                article.getPublishedAt(), article.getIsTop(), article.getIsFeatured(),
                article.getCreateTime(), article.getUpdateTime(),
                authorId, authorName, authorAvatar, isLiked, article.getStatus()
        );
    }

    /** 转换为归档 DTO */
    private ArchiveArticle toArchiveArticle(Article article) {
        return new ArchiveArticle(article.getId(), article.getTitle(), article.getSlug(), article.getPublishedAt());
    }

    /** 转换为上下篇 DTO */
    private PrevNextItem toPrevNextItem(Article article) {
        return new PrevNextItem(article.getId(), article.getTitle(), article.getSlug());
    }

    /** XML 转义 */
    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /** 获取当前登录用户 ID，未登录返回 null */
    private Long getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        try {
            return (Long) auth.getPrincipal();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public PageDTO<HistoryItemResponse> getUserViewHistory(Long userId, int page, int pageSize, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<ArticleViewHistory> historyPage = viewHistoryRepository.findByUserIdAndKeyword(userId, keyword, pageable);

        List<HistoryItemResponse> records = historyPage.getContent().stream()
                .map(h -> new HistoryItemResponse(
                        h.getId(),
                        h.getArticleId(),
                        h.getArticleTitle(),
                        h.getArticleSlug(),
                        null,
                        h.getCreateTime()
                ))
                .toList();

        return new PageDTO<>(records, historyPage.getTotalElements(), page, pageSize);
    }

    @Override
    @Transactional
    public void deleteViewHistory(Long id, Long userId) {
        viewHistoryRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public PageDTO<HistoryItemResponse> getUserLikes(Long userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<ArticleLike> likesPage = articleLikeRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);

        List<HistoryItemResponse> records = likesPage.getContent().stream()
                .map(l -> new HistoryItemResponse(
                        l.getId(),
                        l.getArticleId(),
                        l.getArticleTitle(),
                        l.getArticleSlug(),
                        null,
                        l.getCreateTime()
                ))
                .toList();

        return new PageDTO<>(records, likesPage.getTotalElements(), page, pageSize);
    }

    @Override
    public boolean hasUserLiked(Long userId, Long articleId) {
        if (userId == null || articleId == null) {
            return false;
        }
        return articleLikeRepository.existsByUserIdAndArticleId(userId, articleId);
    }
}
