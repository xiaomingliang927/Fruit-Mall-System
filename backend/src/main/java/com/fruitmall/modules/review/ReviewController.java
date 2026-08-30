package com.fruitmall.modules.review;

import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    public record CreateRequest(
            @NotBlank String orderNo,
            @NotNull Long orderItemId,
            @NotNull @Min(1) @Max(5) Integer rating,
            @NotBlank String content,
            Boolean isAnonymous
    ) {
    }

    /** 评价（订单完成后，一明细一评） */
    @PostMapping
    public ApiResponse<ReviewService.ReviewVO> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok(reviewService.create(UserContext.requireUserId(), request.orderNo(),
                request.orderItemId(), request.rating(), request.content(), request.isAnonymous()));
    }

    /** 我的订单评价进度（每明细是否已评） */
    @GetMapping("/summary")
    public ApiResponse<List<ReviewService.OrderReviewItemVO>> summary(@RequestParam String orderNo) {
        return ApiResponse.ok(reviewService.orderReviewSummary(UserContext.requireUserId(), orderNo));
    }

    /** 商品评价列表（公开）——挂在 products 路径下走匿名放行 */
    @RestController
    @RequestMapping("/api/v1/products/{productId}/reviews")
    @RequiredArgsConstructor
    public static class ProductReviewController {

        private final ReviewService reviewService;

        @GetMapping
        public ApiResponse<PageResult<ReviewService.ReviewVO>> page(
                @PathVariable Long productId,
                @RequestParam(defaultValue = "1") long page,
                @RequestParam(defaultValue = "5") long size) {
            return ApiResponse.ok(reviewService.productReviews(productId, page, Math.min(size, 20)));
        }
    }
}
