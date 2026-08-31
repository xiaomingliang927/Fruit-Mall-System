package com.fruitmall.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fruitmall.admin.rbac.PermissionRegistry;
import com.fruitmall.admin.rbac.RbacService;
import com.fruitmall.auth.JwtUtil;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 管理端鉴权：① 只认 role=admin 的 token；② 按 RBAC 权限点校验接口。
 * 未登记在 PermissionRegistry 的接口只做登录校验（默认放行）。
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private static final int FORBIDDEN = 403;

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RbacService rbacService;
    private final PermissionRegistry permissionRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        Long adminId = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            Claims claims = jwtUtil.parseClaims(auth.substring(7));
            if (claims != null && "admin".equals(claims.get("role", String.class))) {
                adminId = Long.valueOf(claims.getSubject());
            }
        }
        if (adminId == null) {
            return reject(response, ErrorCode.UNAUTHORIZED.getCode(), "请先登录管理后台");
        }

        String required = permissionRegistry.resolve(request.getMethod(), request.getRequestURI());
        if (required != null && !rbacService.isSuperAdmin(adminId)) {
            List<String> owned = rbacService.permCodesOf(adminId);
            if (!owned.contains(required)) {
                return reject(response, FORBIDDEN, "当前账号无此操作权限（需 " + required + "）");
            }
        }

        AdminContext.set(adminId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AdminContext.clear();
    }

    private boolean reject(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
        return false;
    }
}
