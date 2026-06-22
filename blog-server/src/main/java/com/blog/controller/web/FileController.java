package com.blog.controller.web;

import com.blog.exception.BusinessException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件代理控制器。
 * 将 MinIO 存储的图片通过后端 API 提供访问（避免直接暴露 MinIO 地址，
 * 同时支持局域网访问）。
 */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final MinioClient minioClient;

    @GetMapping("/api/web/files/{bucket}/**")
    public void serveFile(
            @PathVariable String bucket,
            HttpServletResponse response) {
        // 从 request path 中提取 objectName
        String path = extractPath(bucket);

        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(path)
                        .build());
             OutputStream os = response.getOutputStream()) {

            String ext = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1).toLowerCase() : "";
            String contentType = switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "webp" -> "image/webp";
                case "svg" -> "image/svg+xml";
                case "bmp" -> "image/bmp";
                default -> "application/octet-stream";
            };
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=31536000, immutable");

            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
        } catch (MinioException e) {
            throw new BusinessException(404, "File not found");
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to serve file: " + e.getMessage());
        }
    }

    private String extractPath(String bucket) {
        // 从当前请求中提取完整路径
        var request = org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes();
        var httpRequest = ((org.springframework.web.context.request.ServletRequestAttributes) request).getRequest();
        String requestUri = httpRequest.getRequestURI();
        String prefix = "/api/web/files/" + bucket + "/";
        if (requestUri.startsWith(prefix)) {
            return requestUri.substring(prefix.length());
        }
        throw new BusinessException(400, "Invalid file path");
    }
}
