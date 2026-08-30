package com.fruitmall.admin;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.modules.order.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ApiResponse<com.fruitmall.common.PageResult<AdminOrderService.OrderVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(adminOrderService.page(status, keyword, page, Math.min(size, 50)));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminOrderService.OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminOrderService.detail(id));
    }

    public record ShipRequest(@NotBlank(message = "运单号不能为空") String trackingNo) {
    }

    /** 发货：待发货 → 待收货 */
    @PostMapping("/{id}/ship")
    public ApiResponse<Void> ship(@PathVariable Long id, @Valid @RequestBody ShipRequest request) {
        adminOrderService.ship(id, request.trackingNo());
        return ApiResponse.ok();
    }

    /** 取消订单：仅限未支付（回补库存） */
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        adminOrderService.cancel(id);
        return ApiResponse.ok();
    }
}
