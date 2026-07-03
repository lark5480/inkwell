package com.blog.service;

import com.blog.dto.*;

import java.util.List;

public interface ArticleService {

    // Admin methods
    /**
     * 分页查询管理端文章列表
     *
     * @param page     页号
     * @param pageSize 每页大小
     * @param keyword  搜索关键词（可选）
     * @param categoryId 分类ID（可选）
     * @param status   文章状态（可选）
     * @return 管理端文章分页数据
     */
    PageDTO<ArticleAdminResponse> getAdminArticlePage(int page, int pageSize, String keyword, Long categoryId, String status);

    /**
     * 根据ID获取管理端文章详情
     *
     * @param id 文章ID
     * @return 管理端文章详情
     */
    ArticleAdminResponse getAdminArticleById(Long id);

    /**
     * 创建文章
     *
     * @param request 创建文章请求体
     * @param userId  作者用户ID
     * @return 新建文章ID
     */
    Long createArticle(ArticleCreateRequest request, Long userId);

    /**
     * 更新文章
     *
     * @param id      文章ID
     * @param request 更新文章请求体
     */
    void updateArticle(Long id, ArticleUpdateRequest request);

    /**
     * 删除文章
     *
     * @param id 文章ID
     */
    void deleteArticle(Long id);

    /**
     * 更新文章状态（发布/草稿/下架）
     *
     * @param id     文章ID
     * @param status 目标状态
     */
    void updateArticleStatus(Long id, String status);

    // Web methods
    /**
     * 分页查询已发布的文章
     *
     * @param page       页号
     * @param pageSize   每页大小
     * @param categoryId 分类ID（可选）
     * @param tagSlug    标签slug（可选）
     * @return 已发布文章分页数据
     */
    PageDTO<ArticleWebResponse> getPublishedArticles(int page, int pageSize, Long categoryId, String tagSlug);

    /**
     * 获取推荐文章列表
     *
     * @return 推荐文章列表
     */
    List<ArticleWebResponse> getFeaturedArticles();

    /**
     * 根据slug获取文章详情（前端展示用）
     *
     * @param slug 文章slug
     * @return 文章详情（含正文、分类、标签、作者等信息）
     */
    ArticleDetailResponse getArticleBySlug(String slug);

    /**
     * 获取上一篇和下一篇文章
     *
     * @param slug 当前文章slug
     * @return 上一篇和下一篇文章信息
     */
    PrevNextDTO getPrevNextArticle(String slug);

    /**
     * 点赞/取消点赞文章
     *
     * @param slug 文章slug
     * @return 点赞结果（当前点赞状态及点赞数）
     */
    LikeToggleResponse likeArticle(String slug);

    /**
     * 搜索文章
     *
     * @param keyword  搜索关键词
     * @param page     页号
     * @param pageSize 每页大小
     * @return 搜索结果分页数据
     */
    PageDTO<ArticleWebResponse> searchArticles(String keyword, int page, int pageSize);

    /**
     * 获取文章归档（按年月分组）
     *
     * @return 归档列表
     */
    List<ArchiveItem> getArchives();

    /**
     * 生成RSS订阅内容
     *
     * @return RSS XML字符串
     */
    String generateRss();

    /**
     * 生成站点地图
     *
     * @return Sitemap XML字符串
     */
    String generateSitemap();

    /**
     * 分页查询用户浏览历史
     *
     * @param userId   用户ID
     * @param page     页号
     * @param pageSize 每页大小
     * @param keyword  搜索关键词（可选）
     * @return 浏览历史分页数据
     */
    PageDTO<HistoryItemResponse> getUserViewHistory(Long userId, int page, int pageSize, String keyword);

    /**
     * 分页查询用户点赞列表
     *
     * @param userId   用户ID
     * @param page     页号
     * @param pageSize 每页大小
     * @return 点赞列表分页数据
     */
    PageDTO<HistoryItemResponse> getUserLikes(Long userId, int page, int pageSize);

    /**
     * 检查用户是否已点赞文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return true 已点赞，false 未点赞
     */
    boolean hasUserLiked(Long userId, Long articleId);

    /**
     * 删除浏览历史记录
     *
     * @param id     浏览历史记录ID
     * @param userId 用户ID（用于权限校验）
     */
    void deleteViewHistory(Long id, Long userId);
}
