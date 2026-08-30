package com.fruitmall.modules.order;

import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.order.dto.CreateOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 提交订单（锁定库存），返回订单号 */
    @PostMapping
    public ApiResponse<String> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.createOrder(UserContext.requireUserId(), request));
    }

    /** 我的订单列表，可按状态筛选 */
    @GetMapping
    public ApiResponse<PageResult<OrderService.OrderListVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(orderService.pageMyOrders(UserContext.requireUserId(), status, page, Math.min(size, 50)));
    }

    /** 订单详情（含商品明细快照与地址快照） */
    @GetMapping("/{orderNo}")
    public ApiResponse<OrderService.OrderDetailVO> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.orderDetail(UserContext.requireUserId(), orderNo));
    }

    /** 取消订单（仅待支付状态，取消后释放库存） */
    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String orderNo) {
        orderService.cancelOrder(UserContext.requireUserId(), orderNo);
        return ApiResponse.ok();
    }

    /** 确认收货（待收货 → 已完成） */
    @PostMapping("/{orderNo}/confirm")
    public ApiResponse<Void> confirm(@PathVariable String orderNo) {
        orderService.confirmOrder(UserContext.requireUserId(), orderNo);
        return ApiResponse.ok();
    }
}
