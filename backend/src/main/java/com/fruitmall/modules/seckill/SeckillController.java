package com.fruitmall.modules.seckill;

import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /** 进行中/即将开始的秒杀列表（匿名可看） */
    @GetMapping("/active")
    public ApiResponse<List<SeckillService.SeckillVO>> active() {
        return ApiResponse.ok(seckillService.activeList());
    }

    @GetMapping("/{id}")
    public ApiResponse<SeckillService.SeckillVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(seckillService.detail(id));
    }

    public record BuyRequest(@NotNull(message = "请选择收货地址") Long addressId, Integer quantity) {
    }

    /** 抢购：成功返回订单号（待支付状态），前端跳订单页支付 */
    @PostMapping("/{id}/buy")
    public ApiResponse<String> buy(@PathVariable Long id, @RequestBody BuyRequest request) {
        return ApiResponse.ok(seckillService.buy(UserContext.requireUserId(), id,
                request.addressId(), request.quantity()));
    }
}
