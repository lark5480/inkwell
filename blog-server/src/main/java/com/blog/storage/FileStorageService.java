package com.blog.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口。
 * 实现类可以是 MinIO (S3兼容) 或本地文件系统。
 */
public interface FileStorageService {

    /**
     * 上传图片文件
     *
     * @param file 上传的文件
     * @return 可访问的文件 URL（MinIO 返回完整 URL，本地返回相对路径）
     */
    String uploadImage(MultipartFile file);
}
