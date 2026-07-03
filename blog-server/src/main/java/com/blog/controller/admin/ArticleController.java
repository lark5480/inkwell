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

/**
 * 文章管理控制器
 */
@RestController("adminArticleController")
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final FileStorageService fileStorageService;

    /**
     * 分页查询文章列表
     *
     * @param page     页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param keyword  搜索关键字（可选）
     * @param categoryId 分类 ID（可选）
     * @param status   文章状态（可选）
     * @return 分页文章数据
     */
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

    /**
     * 创建文章
     *
     * @param request 文章创建请求体
     * @return 新创建的文章 ID
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ArticleCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        Long id = articleService.createArticle(request, userId);
        return Result.success(id);
    }

    /**
     * 根据 ID 获取文章详情
     *
     * @param id 文章 ID
     * @return 文章详情
     */
    @GetMapping("/{id}")
    public Result<ArticleAdminResponse> getById(@PathVariable Long id) {
        ArticleAdminResponse dto = articleService.getAdminArticleById(id);
        return Result.success(dto);
    }

    /**
     * 更新文章
     *
     * @param id      文章 ID
     * @param request 文章更新请求体
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleUpdateRequest request) {
        articleService.updateArticle(id, request);
        return Result.success();
    }

    /**
     * 删除文章
     *
     * @param id 文章 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success();
    }

    /**
     * 更新文章状态（如发布、草稿等）
     *
     * @param id   文章 ID
     * @param body 请求体，包含 status 字段
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        articleService.updateArticleStatus(id, status);
        return Result.success();
    }

    /**
     * 上传图片
     *
     * @param file 上传的图片文件
     * @return 图片访问 URL
     */
    @PostMapping("/upload-image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadImage(file);
        return Result.success(Map.of("url", url));
    }
}
