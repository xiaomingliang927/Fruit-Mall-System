package com.fruitmall.modules.product;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/categories")
    public ApiResponse<List<ProductService.CategoryNode>> categories() {
        return ApiResponse.ok(productService.categoryTree());
    }

    @GetMapping("/products")
    public ApiResponse<PageResult<ProductService.ProductListVO>> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(productService.pageProducts(categoryId, keyword, page, Math.min(size, 50)));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductService.ProductDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.detail(id));
    }
}
