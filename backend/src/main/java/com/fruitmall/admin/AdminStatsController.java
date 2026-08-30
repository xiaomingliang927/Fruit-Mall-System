package com.fruitmall.admin;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsMapper statsMapper;
    private final OrderMapper orderMapper;

    /** 数据看板：核心指标 + 图表数据 + 热销榜 + 最近订单 */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayGmv", statsMapper.todayGmv());
        data.put("todayPaidOrders", statsMapper.todayPaidOrders());
        data.put("userCount", statsMapper.userCount());
        data.put("onSaleProductCount", statsMapper.onSaleProductCount());
        data.put("pendingShipCount", statsMapper.pendingShipCount());
        data.put("topProducts", statsMapper.topProducts());
        data.put("categorySales", statsMapper.categorySales());
        data.put("statusDistribution", statsMapper.statusDistribution());
        data.put("salesTrend", fillTrendGaps(statsMapper.salesTrend()));
        Page<Order> recent = orderMapper.selectPage(new Page<>(1, 8),
                new LambdaQueryWrapper<Order>().orderByDesc(Order::getId));
        data.put("recentOrders", PageResult.of(recent, o -> new AdminOrderService.OrderVO(
                o.getId(), o.getOrderNo(), o.getUserId(), null, null,
                o.getStatus(), OrderStatus.textOf(o.getStatus()),
                o.getTotalAmount(), o.getPayAmount(), o.getTrackingNo(),
                o.getCreatedAt(), o.getPaidAt())));
        return ApiResponse.ok(data);
    }

    /** 近 7 日缺天补零，保证前端折线图 x 轴连续 */
    private List<Map<String, Object>> fillTrendGaps(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byDay.put(String.valueOf(row.get("day")), row);
        }
        List<Map<String, Object>> filled = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            String day = today.minusDays(i).format(fmt);
            Map<String, Object> row = byDay.get(day);
            if (row == null) {
                row = new LinkedHashMap<>();
                row.put("day", day);
                row.put("gmv", 0);
                row.put("orders", 0);
            }
            filled.add(row);
        }
        return filled;
    }
}
