package com.fruitmall.admin;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public ApiResponse<PageResult<AdminProductService.ProductVO>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(adminProductService.page(categoryId, keyword, page, Math.min(size, 50)));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok(adminProductService.create(request.toInput()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request) {
        adminProductService.update(id, request.toInput());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        adminProductService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    public record CreateRequest(Long categoryId, String name, String subtitle, String mainImage,
                                String origin, String unit, String tags,
                                @NotEmpty(message = "至少需要一个 SKU") List<AdminProductService.SkuInput> skus) {
        AdminProductService.ProductInput toInput() {
            return new AdminProductService.ProductInput(categoryId, name, subtitle, mainImage,
                    origin, unit, tags, skus);
        }
    }

    public record UpdateRequest(Long categoryId, String name, String subtitle, String mainImage,
                                String origin, String unit, String tags,
                                @NotEmpty(message = "至少需要一个 SKU") List<AdminProductService.SkuInput> skus) {
        AdminProductService.ProductInput toInput() {
            return new AdminProductService.ProductInput(categoryId, name, subtitle, mainImage,
                    origin, unit, tags, skus);
        }
    }

    public record StatusRequest(Integer status) {
    }
}
