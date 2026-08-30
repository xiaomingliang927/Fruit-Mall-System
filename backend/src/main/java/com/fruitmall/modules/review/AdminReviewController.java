package com.fruitmall.modules.review;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<PageResult<ReviewService.AdminReviewVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(reviewService.adminPage(status, productId, keyword, page, Math.min(size, 50)));
    }

    public record ReplyRequest(@NotBlank String reply) {
    }

    /** 商家回复 */
    @PostMapping("/{id}/reply")
    public ApiResponse<Void> reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest request) {
        reviewService.adminReply(com.fruitmall.admin.AdminContext.requireAdminId(), id, request.reply());
        return ApiResponse.ok();
    }

    /** 显示 / 隐藏 */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> setStatus(@PathVariable Long id, @RequestParam Integer status) {
        reviewService.adminSetStatus(com.fruitmall.admin.AdminContext.requireAdminId(), id, status);
        return ApiResponse.ok();
    }

}
