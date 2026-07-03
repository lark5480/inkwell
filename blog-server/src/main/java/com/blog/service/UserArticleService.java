package com.blog.service;

import com.blog.dto.ArticleCreateRequest;
import com.blog.dto.ArticleDetailResponse;
import com.blog.dto.ArticleUpdateRequest;
import com.blog.dto.ArticleWebResponse;
import com.blog.dto.PageDTO;

public interface UserArticleService {

    /**
     * 创建新文章
     *
     * @param userId  用户ID
     * @param request 创建请求，包含标题、正文、标签等
     * @return 新创建的文章ID
     */
    Long createArticle(Long userId, ArticleCreateRequest request);

    /**
     * 更新文章内容
     *
     * @param userId    用户ID（校验文章归属）
     * @param articleId 文章ID
     * @param request   更新请求，包含标题、正文、标签等
     */
    void updateArticle(Long userId, Long articleId, ArticleUpdateRequest request);

    /**
     * 删除文章（软删除）
     *
     * @param userId    用户ID（校验文章归属）
     * @param articleId 文章ID
     */
    void deleteArticle(Long userId, Long articleId);

    /**
     * 获取当前用户的某篇文章详情
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 文章详情（包含正文、标签、分类等）
     */
    ArticleDetailResponse getMyArticle(Long userId, Long articleId);

    /**
     * 分页查询当前用户的文章列表
     *
     * @param userId   用户ID
     * @param page     页码（从 0 开始）
     * @param pageSize 每页条数
     * @param status   文章状态筛选（可选：draft/published）
     * @param keyword  关键字搜索（可选，匹配标题）
     * @return 分页结果
     */
    PageDTO<ArticleWebResponse> getMyArticles(Long userId, int page, int pageSize, String status, String keyword);
}
