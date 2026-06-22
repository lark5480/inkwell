package com.blog.common;

/**
 * 缓存名称与 key 前缀常量
 */
public final class CacheNames {

    private CacheNames() {}

    /** C端文章列表（按 page+pageSize+categoryId+tagSlug 区分） */
    public static final String ARTICLE_LIST = "article:list";

    /** C端文章详情（按 slug 区分） */
    public static final String ARTICLE_DETAIL = "article:detail";

    /** 文章上下篇（按 slug 区分） */
    public static final String ARTICLE_PREV_NEXT = "article:prev-next";

    /** 文章归档 */
    public static final String ARTICLE_ARCHIVE = "article:archive";

    /** 文章 Markdown 渲染后的 HTML（按 articleId 区分） */
    public static final String ARTICLE_HTML = "article:html";

    /** 分类全量列表 */
    public static final String CATEGORY_LIST = "category:list";

    /** 标签全量列表 */
    public static final String TAG_LIST = "tag:list";

    /** 站点信息 */
    public static final String SITE_INFO = "site:info";

    /** 精选文章列表 */
    public static final String FEATURED = "featured:articles";

    /** 友链列表 */
    public static final String LINK_LIST = "link:list";

    /** 浏览量 Redis Hash key（field=articleId, value=增量） */
    public static final String VIEW_COUNT_HASH = "blog:view-count";

    /** 点赞量 Redis Hash key（field=articleId, value=增量） */
    public static final String LIKE_COUNT_HASH = "blog:like-count";
}
