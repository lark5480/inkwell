package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.*;
import com.blog.service.ArticleService;
import com.blog.service.UserArticleService;
import com.blog.storage.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/web/user")
@RequiredArgsConstructor
public class UserArticleController {

    private final UserArticleService userArticleService;
    private final ArticleService articleService;
    private final FileStorageService fileStorageService;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new com.blog.exception.BusinessException(401, "Not authenticated");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping("/articles")
    public Result<Long> create(@Valid @RequestBody ArticleCreateRequest request) {
        Long id = userArticleService.createArticle(getUserId(), request);
        return Result.success(id);
    }

    @PutMapping("/articles/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleUpdateRequest request) {
        userArticleService.updateArticle(getUserId(), id, request);
        return Result.success(null);
    }

    @DeleteMapping("/articles/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userArticleService.deleteArticle(getUserId(), id);
        return Result.success(null);
    }

    @GetMapping("/articles/{id}")
    public Result<ArticleDetailResponse> detail(@PathVariable Long id) {
        ArticleDetailResponse dto = userArticleService.getMyArticle(getUserId(), id);
        return Result.success(dto);
    }

    @GetMapping("/articles")
    public Result<PageDTO<ArticleWebResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        PageDTO<ArticleWebResponse> result = userArticleService.getMyArticles(getUserId(), page, pageSize, status, keyword);
        return Result.success(result);
    }

    @PostMapping("/articles/upload-image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadImage(file);
        return Result.success(Map.of("url", url));
    }

    @GetMapping("/history")
    public Result<PageDTO<HistoryItemResponse>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        PageDTO<HistoryItemResponse> result = articleService.getUserViewHistory(getUserId(), page, pageSize, keyword);
        return Result.success(result);
    }

    @GetMapping("/likes")
    public Result<PageDTO<HistoryItemResponse>> likes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageDTO<HistoryItemResponse> result = articleService.getUserLikes(getUserId(), page, pageSize);
        return Result.success(result);
    }

    @DeleteMapping("/history/{id}")
    public Result<Void> deleteHistory(@PathVariable Long id) {
        articleService.deleteViewHistory(id, getUserId());
        return Result.success();
    }
}
