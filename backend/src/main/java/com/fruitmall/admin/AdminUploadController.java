package com.fruitmall.admin;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 管理端图片上传（轮播图/商品图）：/api/admin/** 已由 AdminInterceptor 鉴权 */
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/image")
    public ApiResponse<Map<String, String>> image(@RequestParam("file") MultipartFile file) {
        AdminContext.requireAdminId();
        return ApiResponse.ok(Map.of("url", fileStorageService.storeImage(file)));
    }
}
