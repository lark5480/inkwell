package com.blog.controller.web;

import com.blog.exception.BusinessException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 文件代理控制器。
 * 将 MinIO 存储的图片通过后端 API 提供访问（避免直接暴露 MinIO 地址，
 * 同时支持局域网访问）。
 *
 * 注意：只有 blog.storage.type=minio 时才存在 MinioClient bean。
 * 这里用 ObjectProvider 延迟获取，避免 storage.type=local（如 application-prod.yml 默认值）
 * 时因为缺少 MinioClient 而让整个应用启动失败——本地存储的图片走 /uploads/** 静态映射。
 */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final ObjectProvider<MinioClient> minioClientProvider;

    /**
     * 代理获取 MinIO 中的文件
     */
    @GetMapping("/api/web/files/{bucket}/**")
    public ResponseEntity<InputStreamResource> serveFile(
            @PathVariable String bucket,
            HttpServletRequest request) {
        MinioClient minioClient = minioClientProvider.getIfAvailable();
        if (minioClient == null) {
            // 当前是本地文件存储模式，没有 MinIO 代理，按“文件不存在”处理
            throw new BusinessException(404, "File not found");
        }

        String objectName = extractPath(request, bucket);

        try {
            var response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());

            String ext = objectName.contains(".")
                    ? objectName.substring(objectName.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            MediaType mediaType = switch (ext) {
                case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                case "png" -> MediaType.IMAGE_PNG;
                case "gif" -> MediaType.IMAGE_GIF;
                case "webp" -> MediaType.valueOf("image/webp");
                case "svg" -> MediaType.valueOf("image/svg+xml");
                case "bmp" -> MediaType.valueOf("image/bmp");
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                    .body(new InputStreamResource(response));
        } catch (MinioException e) {
            throw new BusinessException(404, "File not found");
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to serve file: " + e.getMessage());
        }
    }

    private String extractPath(HttpServletRequest request, String bucket) {
        String requestUri = request.getRequestURI();
        String prefix = "/api/web/files/" + bucket + "/";
        if (requestUri.startsWith(prefix)) {
            return requestUri.substring(prefix.length());
        }
        throw new BusinessException(400, "Invalid file path");
    }
}
