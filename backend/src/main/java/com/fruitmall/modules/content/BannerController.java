package com.fruitmall.modules.content;

import com.fruitmall.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** C 端轮播图：匿名可读（已在 WebMvcConfig 放行 /api/v1/banners） */
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    public ApiResponse<List<BannerService.BannerVO>> list(
            @RequestParam(defaultValue = "home") String position) {
        return ApiResponse.ok(bannerService.listByPosition(position));
    }
}
