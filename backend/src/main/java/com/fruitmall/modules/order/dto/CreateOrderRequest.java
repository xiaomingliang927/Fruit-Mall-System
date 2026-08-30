package com.fruitmall.modules.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "收货地址不能为空") Long addressId,
        @NotEmpty(message = "订单商品不能为空") List<Item> items,
        String remark
) {

    public record Item(
            @NotNull(message = "skuId 不能为空") Long skuId,
            @NotNull(message = "数量不能为空")
            @Min(value = 1, message = "数量至少为 1")
            @Max(value = 99, message = "数量最多为 99") Integer quantity
    ) {
    }
}
