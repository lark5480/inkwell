package com.blog.config;

import com.blog.storage.FileStorageService;
import com.blog.storage.LocalFileStorageService;
import com.blog.storage.MinioFileStorageService;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储配置。
 * 根据 blog.storage.type 属性选择 MinIO 或本地存储实现。
 * 默认使用 minio，可切换为 local。
 */
@Slf4j
@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "blog.storage.type", havingValue = "minio", matchIfMissing = true)
    public MinioClient minioClient(
            @Value("${blog.storage.minio.endpoint}") String endpoint,
            @Value("${blog.storage.minio.access-key}") String accessKey,
            @Value("${blog.storage.minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "blog.storage.type", havingValue = "minio", matchIfMissing = true)
    public FileStorageService minioFileStorageService(
            MinioClient minioClient,
            @Value("${blog.storage.minio.bucket}") String bucket,
            @Value("${blog.storage.minio.endpoint}") String endpoint,
            @Value("${blog.storage.minio.use-proxy:true}") boolean useProxy) {
        log.info("使用 MinIO 文件存储: endpoint={}, bucket={}, useProxy={}", endpoint, bucket, useProxy);
        return new MinioFileStorageService(minioClient, bucket, endpoint, useProxy);
    }

    @Bean
    @ConditionalOnProperty(name = "blog.storage.type", havingValue = "local")
    public FileStorageService localFileStorageService() {
        log.info("使用本地文件系统存储 (fallback)");
        return new LocalFileStorageService();
    }
}
