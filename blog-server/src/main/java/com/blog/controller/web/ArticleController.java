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

@RestController("webArticleController")
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping("/articles")
    public Result<PageDTO<ArticleWebResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tagSlug) {
        PageDTO<ArticleWebResponse> result = articleService.getPublishedArticles(page, pageSize, categoryId, tagSlug);
        return Result.success(result);
    }

    @GetMapping("/articles/featured")
    public Result<List<ArticleWebResponse>> featured() {
        return Result.success(articleService.getFeaturedArticles());
    }

    @GetMapping("/articles/{slug}")
    public Result<ArticleDetailResponse> detail(@PathVariable String slug) {
        ArticleDetailResponse dto = articleService.getArticleBySlug(slug);
        return Result.success(dto);
    }

    @GetMapping("/articles/{slug}/prev-next")
    public Result<PrevNextDTO> prevNext(@PathVariable String slug) {
        PrevNextDTO dto = articleService.getPrevNextArticle(slug);
        return Result.success(dto);
    }

    @PostMapping("/articles/{slug}/like")
    public Result<LikeToggleResponse> like(@PathVariable String slug) {
        LikeToggleResponse response = articleService.likeArticle(slug);
        return Result.success(response);
    }
}
