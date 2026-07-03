package com.blog.controller.web;

import com.blog.dto.PageDTO;
import com.blog.common.Result;
import com.blog.dto.ArticleDetailResponse;
import com.blog.dto.ArticleWebResponse;
import com.blog.dto.LikeToggleResponse;
import com.blog.dto.PrevNextDTO;
import com.blog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章控制器（C端）- 提供公开的文章列表、详情、点赞等接口
 */
@RestController("webArticleController")
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 分页查询已发布的文章列表
     *
     * @param page     页码，从 1 开始（默认 1）
     * @param pageSize 每页条数（默认 10）
     * @param categoryId 可选，按分类筛选
     * @param tagSlug    可选，按标签 slug 筛选
     * @return 分页文章列表
     */
    @GetMapping("/articles")
    public Result<PageDTO<ArticleWebResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tagSlug) {
        PageDTO<ArticleWebResponse> result = articleService.getPublishedArticles(page, pageSize, categoryId, tagSlug);
        return Result.success(result);
    }

    /**
     * 获取推荐（置顶/精选）文章列表
     *
     * @return 推荐文章列表
     */
    @GetMapping("/articles/featured")
    public Result<List<ArticleWebResponse>> featured() {
        return Result.success(articleService.getFeaturedArticles());
    }

    /**
     * 根据 slug 获取文章详情
     *
     * @param slug 文章 slug（URL 友好标识）
     * @return 文章详情
     */
    @GetMapping("/articles/{slug}")
    public Result<ArticleDetailResponse> detail(@PathVariable String slug) {
        ArticleDetailResponse dto = articleService.getArticleBySlug(slug);
        return Result.success(dto);
    }

    /**
     * 获取某篇文章的上一篇和下一篇
     *
     * @param slug 当前文章 slug
     * @return 上一篇和下一篇的标题与 slug
     */
    @GetMapping("/articles/{slug}/prev-next")
    public Result<PrevNextDTO> prevNext(@PathVariable String slug) {
        PrevNextDTO dto = articleService.getPrevNextArticle(slug);
        return Result.success(dto);
    }

    /**
     * 点赞/取消点赞文章（切换操作）
     *
     * @param slug 文章 slug
     * @return 点赞状态（liked: 是否已点赞, count: 点赞数）
     */
    @PostMapping("/articles/{slug}/like")
    public Result<LikeToggleResponse> like(@PathVariable String slug) {
        LikeToggleResponse response = articleService.likeArticle(slug);
        return Result.success(response);
    }
}
