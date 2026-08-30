package com.fruitmall.modules.coupon;

import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** C 端优惠券：列表/领取/我的券/下单可用券 */
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /** 可领券列表（匿名可见） */
    @GetMapping("/list")
    public ApiResponse<List<CouponService.CouponVO>> receivable() {
        return ApiResponse.ok(couponService.listReceivable());
    }

    public record ReceiveRequest(@NotNull Long couponId) {
    }

    /** 领取 */
    @PostMapping("/receive")
    public ApiResponse<CouponService.MyCouponVO> receive(@Valid @RequestBody ReceiveRequest request) {
        return ApiResponse.ok(couponService.receive(UserContext.requireUserId(), request.couponId()));
    }

    /** 我的优惠券（0未使用 1已使用 2已过期，空=未使用） */
    @GetMapping("/mine")
    public ApiResponse<List<CouponService.MyCouponVO>> mine(
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(couponService.myCoupons(UserContext.requireUserId(), status));
    }

    /** 结算页：当前金额下可用的券（含可抵扣金额，由前端算实际抵扣） */
    @GetMapping("/usable")
    public ApiResponse<List<CouponService.MyCouponVO>> usable(@RequestParam int amount) {
        return ApiResponse.ok(couponService.usableForOrder(UserContext.requireUserId(), amount));
    }

    // ============ 管理端 ============

    @RestController
    @RequestMapping("/api/admin/coupons")
    @RequiredArgsConstructor
    public static class AdminCouponController {

        private final CouponService couponService;

        @GetMapping
        public ApiResponse<PageResult<CouponService.CouponVO>> page(
                @RequestParam(required = false) String keyword,
                @RequestParam(defaultValue = "1") long page,
                @RequestParam(defaultValue = "10") long size) {
            return ApiResponse.ok(couponService.adminPage(keyword, page, Math.min(size, 50)));
        }

        public record CreateRequest(
                @NotBlank String name,
                @NotNull Integer type,
                Integer thresholdAmount,
                Integer discountAmount,
                Integer discountPercent,
                @NotNull Integer totalCount,
                Integer perUserLimit,
                @NotNull String startTime,
                @NotNull String endTime,
                Integer validDays) {
        }

        /** 创建券模板 */
        @PostMapping
        public ApiResponse<Long> create(@Valid @RequestBody CreateRequest request) {
            return ApiResponse.ok(couponService.create(new CouponService.CouponInput(
                    request.name(), request.type(),
                    request.thresholdAmount() == null ? 0 : request.thresholdAmount(),
                    request.discountAmount(), request.discountPercent(),
                    request.totalCount(), request.perUserLimit(),
                    LocalDateTime.parse(request.startTime()), LocalDateTime.parse(request.endTime()),
                    request.validDays())));
        }

        /** 启用/停用 */
        @PutMapping("/{id}/status")
        public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
            couponService.updateStatus(id, status);
            return ApiResponse.ok();
        }
    }
}
