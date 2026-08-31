package com.fruitmall.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 图片存储：本地磁盘，返回 /uploads/ 访问路径。
 * C 端与管理端共用同一份校验与落盘逻辑；生产换 OSS 时只需替换本实现，控制器不动。
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("请选择图片");
        if (file.getSize() > MAX_SIZE) throw new BizException("图片不能超过 5MB");

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(ext)) throw new BizException("仅支持 jpg / png / webp 格式");

        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path dir = Paths.get(uploadDir, datePath);
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(dir.resolve(filename).toAbsolutePath());
            return "/uploads/" + datePath + "/" + filename;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("上传失败，请稍后重试");
        }
    }
}
