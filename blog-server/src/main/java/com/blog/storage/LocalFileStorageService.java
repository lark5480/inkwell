package com.blog.storage;

import com.blog.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 本地文件系统存储实现（fallback）。
 * 当 MinIO 不可用或未配置时使用，文件保存到 ./uploads/ 目录。
 */
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private static final String UPLOAD_DIR = "uploads";
    private static final String ALLOWED_EXTENSIONS = ".jpg.jpeg.png.gif.webp.svg.bmp";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "File size exceeds 10MB limit");
        }

        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".jpg";

        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BusinessException(400,
                    "Invalid file type. Allowed: jpg, jpeg, png, gif, webp, svg, bmp");
        }

        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        File dir = new File(System.getProperty("user.dir"), UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            File dest = new File(dir, fileName);
            file.transferTo(dest);
            String url = "/uploads/" + fileName;
            log.info("本地文件上传成功: {}", url);
            return url;
        } catch (IOException e) {
            throw new BusinessException(500, "Image upload failed: " + e.getMessage());
        }
    }
}
