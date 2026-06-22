package com.blog.controller.admin;

import com.blog.dto.PageDTO;
import com.blog.common.Result;
import com.blog.dto.ArticleAdminResponse;
import com.blog.dto.ArticleCreateRequest;
import com.blog.dto.ArticleUpdateRequest;
import com.blog.service.ArticleService;
import com.blog.storage.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController("adminArticleController")
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public Result<PageDTO<ArticleAdminResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status) {
        PageDTO<ArticleAdminResponse> result = articleService.getAdminArticlePage(page, pageSize, keyword, categoryId, status);
        return Result.success(result);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ArticleCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        Long id = articleService.createArticle(request, userId);
        return Result.success(id);
    }

    @GetMapping("/{id}")
    public Result<ArticleAdminResponse> getById(@PathVariable Long id) {
        ArticleAdminResponse dto = articleService.getAdminArticleById(id);
        return Result.success(dto);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleUpdateRequest request) {
        articleService.updateArticle(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        articleService.updateArticleStatus(id, status);
        return Result.success();
    }

    @PostMapping("/upload-image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadImage(file);
        return Result.success(Map.of("url", url));
    }
}
