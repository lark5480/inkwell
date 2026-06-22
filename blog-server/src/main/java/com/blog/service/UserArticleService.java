package com.blog.service;

import com.blog.dto.ArticleCreateRequest;
import com.blog.dto.ArticleDetailResponse;
import com.blog.dto.ArticleUpdateRequest;
import com.blog.dto.ArticleWebResponse;
import com.blog.dto.PageDTO;

public interface UserArticleService {

    Long createArticle(Long userId, ArticleCreateRequest request);

    void updateArticle(Long userId, Long articleId, ArticleUpdateRequest request);

    void deleteArticle(Long userId, Long articleId);

    ArticleDetailResponse getMyArticle(Long userId, Long articleId);

    PageDTO<ArticleWebResponse> getMyArticles(Long userId, int page, int pageSize, String status, String keyword);
}
