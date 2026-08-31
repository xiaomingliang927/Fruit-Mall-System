package com.fruitmall.admin.rbac;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端接口 → 权限点映射表。
 * 约定：未登记的接口默认放行（仅为登录校验），登记过的接口才逐点校验；
 * 这样新增页面不会「先被拦死」，等确认要控权时补一行即可。
 */
@Component
public class PermissionRegistry {

    private static final Map<String, String> RULES = new HashMap<>();

    static {
        // 数据中心
        RULES.put("GET /api/admin/stats/dashboard", "dashboard");
        RULES.put("GET /api/admin/stats/revenue", "revenue");
        RULES.put("GET /api/admin/stats/revenue/export", "revenue");
        // 商品中心
        RULES.put("GET /api/admin/products", "product:list");
        RULES.put("POST /api/admin/products", "product:create");
        RULES.put("PUT /api/admin/products/{id}", "product:update");
        RULES.put("PUT /api/admin/products/{id}/status", "product:update");
        // 订单中心
        RULES.put("GET /api/admin/orders", "order:list");
        RULES.put("GET /api/admin/orders/{id}", "order:list");
        RULES.put("GET /api/admin/orders/export", "order:list");
        RULES.put("POST /api/admin/orders/{id}/ship", "order:ship");
        RULES.put("POST /api/admin/orders/{id}/cancel", "order:cancel");
        // 售后
        RULES.put("GET /api/admin/refunds", "refund:list");
        RULES.put("POST /api/admin/refunds/{id}/approve", "refund:audit");
        RULES.put("POST /api/admin/refunds/{id}/reject", "refund:audit");
        // 评价
        RULES.put("GET /api/admin/reviews", "review:list");
        RULES.put("POST /api/admin/reviews/{id}/reply", "review:reply");
        RULES.put("PUT /api/admin/reviews/{id}/status", "review:reply");
        // 会员
        RULES.put("GET /api/admin/members", "member:list");
        RULES.put("POST /api/admin/members", "member:create");
        // 营销
        RULES.put("GET /api/admin/coupons", "coupon:list");
        RULES.put("POST /api/admin/coupons", "coupon:create");
        RULES.put("PUT /api/admin/coupons/{id}/status", "coupon:update");
        RULES.put("GET /api/admin/banners", "banner:list");
        RULES.put("POST /api/admin/banners", "banner:create");
        RULES.put("PUT /api/admin/banners/{id}", "banner:update");
        RULES.put("PUT /api/admin/banners/{id}/status", "banner:update");
        RULES.put("DELETE /api/admin/banners/{id}", "banner:delete");
        // 营销 · 秒杀
        RULES.put("GET /api/admin/seckill", "seckill:list");
        RULES.put("POST /api/admin/seckill", "seckill:create");
        RULES.put("PUT /api/admin/seckill/{id}", "seckill:update");
        RULES.put("PUT /api/admin/seckill/{id}/status", "seckill:update");
        RULES.put("DELETE /api/admin/seckill/{id}", "seckill:delete");
        // 系统 · 角色权限
        RULES.put("GET /api/admin/roles", "role:list");
        RULES.put("POST /api/admin/roles", "role:create");
        RULES.put("PUT /api/admin/roles/{id}", "role:update");
        RULES.put("DELETE /api/admin/roles/{id}", "role:delete");
        RULES.put("PUT /api/admin/roles/admins/{id}", "role:assign");
        RULES.put("GET /api/admin/admins", "role:list");
        RULES.put("GET /api/admin/permissions", "role:list");
    }

    /** 解析该请求需要的权限点；null 表示该接口不做权限点校验 */
    public String resolve(String method, String uri) {
        return RULES.get(normalize(method, uri));
    }

    /** 归一化：去 query，数字路径段统一成 {id} */
    static String normalize(String method, String uri) {
        String path = uri == null ? "" : uri;
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        StringBuilder sb = new StringBuilder();
        for (String seg : path.split("/")) {
            if (seg.isEmpty()) continue;
            sb.append('/').append(seg.matches("\\d+") ? "{id}" : seg);
        }
        return (method == null ? "GET" : method.toUpperCase()) + " " + sb;
    }
}
