package com.fruitmall.admin;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.order.OrderStatus;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ApiResponse<PageResult<AdminOrderService.OrderVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(adminOrderService.page(status, keyword, page, Math.min(size, 50)));
    }

    /** 订单列表导出 Excel（与列表同筛选条件） */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws java.io.IOException {
        List<OrderRow> rows = new ArrayList<>();
        for (AdminOrderService.OrderVO o : adminOrderService.listForExport(status, keyword)) {
            rows.add(new OrderRow(
                    o.orderNo(),
                    o.userNickname() == null ? "-" : o.userNickname(),
                    o.userPhone() == null ? "-" : o.userPhone(),
                    OrderStatus.textOf(o.status()),
                    fenToYuan(o.totalAmount()),
                    fenToYuan(o.payAmount()),
                    o.trackingNo() == null ? "-" : o.trackingNo(),
                    o.createdAt() == null ? "-" : o.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    o.paidAt() == null ? "-" : o.paidAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }
        AdminStatsController.setXlsxHeaders(response, "订单列表_" + System.currentTimeMillis() + ".xlsx");
        EasyExcel.write(response.getOutputStream(), OrderRow.class)
                .sheet("订单")
                .doWrite(rows);
    }

    private static String fenToYuan(Integer fen) {
        return fen == null ? "0.00" : String.format("%.2f", fen / 100.0);
    }

    public record OrderRow(
            @ExcelProperty("订单号") String orderNo,
            @ExcelProperty("买家") String nickname,
            @ExcelProperty("手机号") String phone,
            @ExcelProperty("状态") String status,
            @ExcelProperty("商品总额（元）") String totalAmount,
            @ExcelProperty("实付金额（元）") String payAmount,
            @ExcelProperty("运单号") String trackingNo,
            @ExcelProperty("下单时间") String createdAt,
            @ExcelProperty("支付时间") String paidAt) {
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
