package com.blog.service;

import com.blog.dto.*;

import java.util.List;

public interface ArticleService {

    // Admin methods
    PageDTO<ArticleAdminResponse> getAdminArticlePage(int page, int pageSize, String keyword, Long categoryId, String status);

    ArticleAdminResponse getAdminArticleById(Long id);

    Long createArticle(ArticleCreateRequest request, Long userId);

    void updateArticle(Long id, ArticleUpdateRequest request);

    void deleteArticle(Long id);

    void updateArticleStatus(Long id, String status);

    // Web methods
    PageDTO<ArticleWebResponse> getPublishedArticles(int page, int pageSize, Long categoryId, String tagSlug);

    List<ArticleWebResponse> getFeaturedArticles();

    ArticleDetailResponse getArticleBySlug(String slug);

    PrevNextDTO getPrevNextArticle(String slug);

    LikeToggleResponse likeArticle(String slug);

    PageDTO<ArticleWebResponse> searchArticles(String keyword, int page, int pageSize);

    List<ArchiveItem> getArchives();

    String generateRss();

    String generateSitemap();

    PageDTO<HistoryItemResponse> getUserViewHistory(Long userId, int page, int pageSize, String keyword);

    PageDTO<HistoryItemResponse> getUserLikes(Long userId, int page, int pageSize);

    boolean hasUserLiked(Long userId, Long articleId);

    void deleteViewHistory(Long id, Long userId);
}
