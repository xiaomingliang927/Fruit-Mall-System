package com.fruitmall.admin;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.order.Order;
import com.fruitmall.modules.order.OrderMapper;
import com.fruitmall.modules.order.OrderStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        data.put("categoryGmv", statsMapper.categoryGmv());
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

    /** 营业额统计：区间汇总 + 每日明细（缺天补零） */
    @GetMapping("/revenue")
    public ApiResponse<Map<String, Object>> revenue(@RequestParam String from, @RequestParam String to) {
        Range range = parseRange(from, to);
        Map<String, Object> total = statsMapper.revenueTotal(range.from(), range.to());
        long orders = ((Number) total.getOrDefault("orders", 0)).longValue();
        long gmv = ((Number) total.getOrDefault("gmv", 0)).longValue();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", range.from());
        data.put("to", range.to());
        data.put("totalGmv", gmv);
        data.put("totalOrders", orders);
        data.put("avgOrderAmount", orders == 0 ? 0 : gmv / orders);
        data.put("days", fillRevenueGaps(range, statsMapper.revenueDaily(range.from(), range.to())));
        return ApiResponse.ok(data);
    }

    /** 营业额统计导出 Excel：每日明细 + 合计行 */
    @GetMapping("/revenue/export")
    public void exportRevenue(@RequestParam String from, @RequestParam String to,
                              HttpServletResponse response) throws java.io.IOException {
        Range range = parseRange(from, to);
        Map<String, Object> total = statsMapper.revenueTotal(range.from(), range.to());
        long orders = ((Number) total.getOrDefault("orders", 0)).longValue();
        long gmv = ((Number) total.getOrDefault("gmv", 0)).longValue();

        List<RevenueRow> rows = new ArrayList<>();
        for (Map<String, Object> day : fillRevenueGaps(range, statsMapper.revenueDaily(range.from(), range.to()))) {
            rows.add(new RevenueRow(String.valueOf(day.get("day")),
                    ((Number) day.get("orders")).longValue(),
                    yuanText(((Number) day.get("gmv")).longValue())));
        }
        rows.add(new RevenueRow("合计", orders, yuanText(gmv)));

        setXlsxHeaders(response, "营业统计_" + range.from() + "_" + range.to() + ".xlsx");
        EasyExcel.write(response.getOutputStream(), RevenueRow.class)
                .sheet("每日营业额")
                .doWrite(rows);
    }

    // ===== 内部工具 =====

    private record Range(String from, String to) {
    }

    private record RevenueRow(
            @ExcelProperty("日期") String day,
            @ExcelProperty("支付订单数") Long orders,
            @ExcelProperty("营业额（元）") String gmvYuan) {
    }

    private Range parseRange(String from, String to) {
        try {
            LocalDate f = LocalDate.parse(from);
            LocalDate t = LocalDate.parse(to);
            if (f.isAfter(t)) {
                throw new IllegalArgumentException();
            }
            if (f.plusYears(1).isBefore(t)) {
                throw new com.fruitmall.common.BizException("查询区间不能超过 1 年");
            }
            return new Range(from, to);
        } catch (java.time.format.DateTimeParseException | IllegalArgumentException e) {
            throw new com.fruitmall.common.BizException("日期格式应为 yyyy-MM-dd 且开始不晚于结束");
        }
    }

    private List<Map<String, Object>> fillRevenueGaps(Range range, List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byDay.put(String.valueOf(row.get("day")), row);
        }
        List<Map<String, Object>> filled = new ArrayList<>();
        LocalDate cur = LocalDate.parse(range.from());
        LocalDate end = LocalDate.parse(range.to());
        while (!cur.isAfter(end)) {
            String key = cur.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Map<String, Object> row = byDay.get(key);
            if (row == null) {
                row = new LinkedHashMap<>();
                row.put("day", key);
                row.put("orders", 0);
                row.put("gmv", 0);
            }
            filled.add(row);
            cur = cur.plusDays(1);
        }
        return filled;
    }

    /** 近 7 日缺天补零，保证前端折线图 x 轴连续 */
    private List<Map<String, Object>> fillTrendGaps(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byDay.put(String.valueOf(row.get("day")), row);
        }
        List<Map<String, Object>> filled = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
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

    private String yuanText(long fen) {
        return String.format("%.2f", fen / 100.0);
    }

    static void setXlsxHeaders(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }
}
