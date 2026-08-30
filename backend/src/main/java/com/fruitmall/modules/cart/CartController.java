package com.fruitmall.modules.cart;

import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartService.CartSummary> myCart() {
        return ApiResponse.ok(cartService.myCart(UserContext.requireUserId()));
    }

    public record AddCartRequest(
            @NotNull(message = "skuId 不能为空") Long skuId,
            @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量至少为 1") @Max(value = 99, message = "数量最多为 99") Integer quantity
    ) {
    }

    @PostMapping("/items")
    public ApiResponse<Void> addItem(@Valid @RequestBody AddCartRequest request) {
        cartService.addItem(UserContext.requireUserId(), request.skuId(), request.quantity());
        return ApiResponse.ok();
    }

    public record UpdateCartRequest(
            @Min(value = 1, message = "数量至少为 1") @Max(value = 99, message = "数量最多为 99") Integer quantity,
            Boolean checked
    ) {
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<Void> updateItem(@PathVariable Long itemId,
                                        @Valid @RequestBody UpdateCartRequest request) {
        cartService.updateItem(UserContext.requireUserId(), itemId, request.quantity(), request.checked());
        return ApiResponse.ok();
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> removeItem(@PathVariable Long itemId) {
        cartService.removeItem(UserContext.requireUserId(), itemId);
        return ApiResponse.ok();
    }

    /** 清空已勾选商品（下单成功后调用） */
    @DeleteMapping("/items/checked")
    public ApiResponse<Void> clearChecked() {
        cartService.clearChecked(UserContext.requireUserId());
        return ApiResponse.ok();
    }
}
