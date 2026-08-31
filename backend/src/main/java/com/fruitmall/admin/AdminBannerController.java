package com.fruitmall.admin;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.content.BannerService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerService bannerService;

    @GetMapping
    public ApiResponse<PageResult<BannerService.BannerVO>> page(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(bannerService.adminPage(position, status, page, Math.min(size, 50)));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody BannerRequest request) {
        AdminContext.requireAdminId();
        return ApiResponse.ok(bannerService.create(request.toInput()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody BannerRequest request) {
        AdminContext.requireAdminId();
        bannerService.update(id, request.toInput());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        AdminContext.requireAdminId();
        bannerService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AdminContext.requireAdminId();
        bannerService.delete(id);
        return ApiResponse.ok();
    }

    public record BannerRequest(@NotBlank(message = "请填写标题") String title,
                                @NotBlank(message = "请上传轮播图片") String image,
                                Integer linkType, String linkValue,
                                String position, Integer sort, Integer status) {
        BannerService.BannerInput toInput() {
            return new BannerService.BannerInput(title, image, linkType, linkValue, position, sort, status);
        }
    }

    public record StatusRequest(Integer status) {
    }
}
