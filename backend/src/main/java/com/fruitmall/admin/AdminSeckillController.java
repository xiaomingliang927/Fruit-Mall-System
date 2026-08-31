package com.fruitmall.admin;

import com.fruitmall.admin.AdminContext;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.seckill.SeckillService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/seckill")
@RequiredArgsConstructor
public class AdminSeckillController {

    private final SeckillService seckillService;

    @GetMapping
    public ApiResponse<PageResult<SeckillService.SeckillVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(seckillService.adminPage(status, page, size));
    }

    public record SeckillRequest(@NotNull(message = "请选择秒杀规格") Long skuId,
                                 @NotNull(message = "请填写秒杀价") Integer seckillPrice,
                                 @NotNull(message = "请填写秒杀总量") Integer totalStock,
                                 Integer limitPerUser,
                                 @NotNull(message = "请选择开始时间") LocalDateTime startTime,
                                 @NotNull(message = "请选择结束时间") LocalDateTime endTime,
                                 Integer status) {
        SeckillService.SeckillInput toInput() {
            return new SeckillService.SeckillInput(skuId, seckillPrice, totalStock, limitPerUser,
                    startTime, endTime, status);
        }
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody SeckillRequest request) {
        AdminContext.requireAdminId();
        return ApiResponse.ok(seckillService.create(request.toInput()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody SeckillRequest request) {
        AdminContext.requireAdminId();
        seckillService.update(id, request.toInput());
        return ApiResponse.ok();
    }

    public record StatusRequest(Integer status) {
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        AdminContext.requireAdminId();
        seckillService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AdminContext.requireAdminId();
        seckillService.delete(id);
        return ApiResponse.ok();
    }
}
