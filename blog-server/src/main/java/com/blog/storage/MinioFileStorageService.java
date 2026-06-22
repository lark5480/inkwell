package com.blog.storage;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.blog.exception.BusinessException;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * MinIO 文件存储实现。
 * 上传文件到 MinIO bucket，返回预签名 URL（有效期 7 天）。
 */
@Slf4j
public class MinioFileStorageService implements FileStorageService {
    
    private final MinioClient minioClient;
    private final String bucket;
    private final String endpoint;

    private static final String ALLOWED_EXTENSIONS = ".jpg.jpeg.png.gif.webp.svg.bmp";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public MinioFileStorageService(String endpoint, String accessKey, String secretKey, String bucket) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
                /* 设置 bucket 为公开读取（允许匿名 GET） */
                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [{
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                          }]
                        }
                        """.formatted(bucket);
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
                log.info("MinIO bucket '{}' 创建成功，已设置公开读取策略", bucket);
            } else {
                log.info("MinIO bucket '{}' 已存在", bucket);
            }
        } catch (Exception e) {
            log.error("MinIO bucket 初始化失败", e);
            throw new RuntimeException("MinIO bucket initialization failed", e);
        }
    }

    @Override
    public String uploadImage(MultipartFile file) {
        validateFile(file);

        String objectName = generateObjectName(file);

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            /* 返回公开访问 URL（bucket 已设公开读取策略） */
            String url = endpoint + "/" + bucket + "/" + objectName;
            log.info("图片上传成功: {}", url);
            return url;

        } catch (Exception e) {
            log.error("MinIO 上传失败", e);
            throw new BusinessException(500, "Image upload failed: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "File size exceeds 10MB limit");
        }
        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : "";
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BusinessException(400,
                    "Invalid file type. Allowed: jpg, jpeg, png, gif, webp, svg, bmp");
        }
    }

    private String generateObjectName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".jpg";
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        /* 按年月分目录，便于管理 */
        String yearMonth = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM"));
        return yearMonth + "/" + fileName;
    }
}
