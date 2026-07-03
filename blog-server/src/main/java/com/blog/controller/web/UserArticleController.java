package com.blog.controller.web;

import java.util.Map;

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

import com.blog.common.Result;
import com.blog.dto.ArticleCreateRequest;
import com.blog.dto.ArticleDetailResponse;
import com.blog.dto.ArticleUpdateRequest;
import com.blog.dto.ArticleWebResponse;
import com.blog.dto.HistoryItemResponse;
import com.blog.dto.PageDTO;
import com.blog.service.ArticleService;
import com.blog.service.UserArticleService;
import com.blog.storage.FileStorageService;
import com.blog.storage.LocalFileStorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 用户文章控制器 —— 当前登录用户的文章 CRUD、图片上传、浏览/点赞历史
 */
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

    /**
     * 创建文章
     *
     * @param request 文章创建请求体（标题、分类、标签、正文等）
     * @return 新建文章 ID
     */
    @PostMapping("/articles")
    public Result<Long> create(@Valid @RequestBody ArticleCreateRequest request) {
        Long id = userArticleService.createArticle(getUserId(), request);
        return Result.success(id);
    }

    /**
     * 更新文章
     *
     * @param id      文章 ID
     * @param request 文章更新请求体
     */
    @PutMapping("/articles/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleUpdateRequest request) {
        userArticleService.updateArticle(getUserId(), id, request);
        return Result.success(null);
    }

    /**
     * 删除文章
     *
     * @param id 文章 ID
     */
    @DeleteMapping("/articles/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userArticleService.deleteArticle(getUserId(), id);
        return Result.success(null);
    }

    /**
     * 获取我的文章详情
     *
     * @param id 文章 ID
     * @return 文章详情（含正文、分类、标签等）
     */
    @GetMapping("/articles/{id}")
    public Result<ArticleDetailResponse> detail(@PathVariable Long id) {
        ArticleDetailResponse dto = userArticleService.getMyArticle(getUserId(), id);
        return Result.success(dto);
    }

    /**
     * 分页获取我的文章列表
     *
     * @param page     页码（默认 1）
     * @param pageSize 每页条数（默认 10）
     * @param status   按状态筛选（可选）
     * @param keyword  按关键词搜索标题（可选）
     * @return 分页文章列表
     */
    @GetMapping("/articles")
    public Result<PageDTO<ArticleWebResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        PageDTO<ArticleWebResponse> result = userArticleService.getMyArticles(getUserId(), page, pageSize, status, keyword);
        return Result.success(result);
    }

    /**
     * 上传文章封面/题图图片（通过 FileStorageService 存储，可能使用 MinIO）
     *
     * @param file 上传的图片文件
     * @return 图片访问 URL
     */
    @PostMapping("/articles/upload-image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadImage(file);
        return Result.success(Map.of("url", url));
    }

    /**
     * 文章的正文里上传图片，是上传到本地的，不通过minio
     * @param file
     * @return
     */
    @PostMapping("/articles/upload-image-content")
    public Result<Map<String, String>> uploadImageContent(@RequestParam("file") MultipartFile file) {
        LocalFileStorageService localStorage = new LocalFileStorageService();
        String url = localStorage.uploadImage(file);
        return Result.success(Map.of("url", url));
    }

    /**
     * 获取我的浏览历史（分页）
     *
     * @param page     页码（默认 1）
     * @param pageSize 每页条数（默认 10）
     * @param keyword  按关键词搜索标题（可选）
     * @return 分页浏览历史列表
     */
    @GetMapping("/history")
    public Result<PageDTO<HistoryItemResponse>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        PageDTO<HistoryItemResponse> result = articleService.getUserViewHistory(getUserId(), page, pageSize, keyword);
        return Result.success(result);
    }

    /**
     * 获取我的点赞列表（分页）
     *
     * @param page     页码（默认 1）
     * @param pageSize 每页条数（默认 10）
     * @return 分页点赞文章列表
     */
    @GetMapping("/likes")
    public Result<PageDTO<HistoryItemResponse>> likes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageDTO<HistoryItemResponse> result = articleService.getUserLikes(getUserId(), page, pageSize);
        return Result.success(result);
    }

    /**
     * 删除单条浏览历史记录
     *
     * @param id 历史记录 ID
     */
    @DeleteMapping("/history/{id}")
    public Result<Void> deleteHistory(@PathVariable Long id) {
        articleService.deleteViewHistory(id, getUserId());
        return Result.success();
    }
}
