package com.fruitmall.rbac;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fruitmall.admin.AdminUser;
import com.fruitmall.admin.AdminUserMapper;
import com.fruitmall.admin.rbac.RbacService;
import com.fruitmall.auth.JwtUtil;
import com.fruitmall.common.BizException;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** RBAC：权限点种子、角色授权、接口拦截（超管放行 / 只读账号写操作被拦） */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    RbacService rbacService;
    @Autowired
    AdminUserMapper adminUserMapper;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    ObjectMapper objectMapper;

    private Long newAdmin(String username) {
        AdminUser u = new AdminUser();
        u.setUsername(username);
        u.setRealName("测试账号");
        u.setStatus(1);
        u.setSalt("testsalt");
        u.setPasswordHash("testhash");
        adminUserMapper.insert(u);
        return u.getId();
    }

    private String authHeader(Long adminId) {
        return "Bearer " + jwtUtil.generateAdmin(adminId);
    }

    private int codeOf(String json) {
        return JsonPath.read(json, "$.code");
    }

    private RbacService.RoleVO roleByCode(String code) {
        return rbacService.listRoles().stream()
                .filter(r -> code.equals(r.code()))
                .findFirst().orElseThrow(() -> new IllegalStateException("缺少角色 " + code));
    }

    // ==================== 种子数据 ====================

    @Test
    @DisplayName("V13 种子：权限点齐备，超管全量、运营不含角色管理、只读仅菜单权限")
    void seedData() {
        List<RbacService.PermissionVO> perms = rbacService.listPermissions();
        assertThat(perms).extracting(RbacService.PermissionVO::code)
                .contains("dashboard", "banner:list", "banner:create", "role:assign");
        assertThat(perms).isNotEmpty();

        assertThat(roleByCode("super").permissions()).hasSize(perms.size());
        assertThat(roleByCode("operator").permissions())
                .contains("banner:create").doesNotContain("role:list", "role:assign");
        assertThat(roleByCode("viewer").permissions())
                .contains("banner:list").doesNotContain("banner:create");
    }

    // ==================== 授权模型 ====================

    @Test
    @DisplayName("账号权限 = 角色并集；改角色授权后立即生效；解绑后权限清空")
    void permissionUnionAndUpdates() {
        Long adminId = newAdmin("ops_" + System.nanoTime());
        String code = "role_" + System.nanoTime();
        Long roleId = rbacService.createRole(new RbacService.RoleInput(
                "测试运营", code, "临时角色", 1, List.of("banner:list", "product:list")));
        rbacService.assignAdminRoles(adminId, List.of(roleId));

        assertThat(rbacService.isSuperAdmin(adminId)).isFalse();
        assertThat(rbacService.permCodesOf(adminId))
                .containsExactlyInAnyOrder("banner:list", "product:list");

        rbacService.updateRole(roleId, new RbacService.RoleInput(
                "测试运营", code, "临时角色", 1, List.of("banner:list", "banner:create")));
        assertThat(rbacService.permCodesOf(adminId)).contains("banner:create");

        // 已绑定账号的角色不能直接删，解绑后可以
        assertThatThrownBy(() -> rbacService.deleteRole(roleId))
                .isInstanceOf(BizException.class).hasMessageContaining("已绑定");
        rbacService.assignAdminRoles(adminId, List.of());
        rbacService.deleteRole(roleId);
        assertThat(rbacService.permCodesOf(adminId)).isEmpty();
    }

    @Test
    @DisplayName("内置超级管理员角色受保护：不可新建同名、不可删除")
    void superRoleProtected() {
        assertThatThrownBy(() -> rbacService.createRole(
                new RbacService.RoleInput("冒牌超管", "super", "", 1, List.of("dashboard"))))
                .isInstanceOf(BizException.class).hasMessageContaining("内置角色");
        assertThatThrownBy(() -> rbacService.deleteRole(roleByCode("super").id()))
                .isInstanceOf(BizException.class).hasMessageContaining("不可删除");
    }

    // ==================== 接口拦截 ====================

    @Test
    @DisplayName("接口拦截：只读账号可看不可写，超管全部放行，未登录 401")
    void interceptorEnforcesPermissions() throws Exception {
        Long viewerId = newAdmin("viewer_" + System.nanoTime());
        rbacService.assignAdminRoles(viewerId, List.of(roleByCode("viewer").id()));
        Long superId = newAdmin("super_" + System.nanoTime());
        rbacService.assignAdminRoles(superId, List.of(roleByCode("super").id()));

        // 只读账号：查看放行
        String listJson = mockMvc.perform(get("/api/admin/banners").header("Authorization", authHeader(viewerId)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(codeOf(listJson)).isEqualTo(0);

        // 只读账号：写操作被拦（403 + 中文提示）
        String denied = mockMvc.perform(post("/api/admin/banners")
                        .header("Authorization", authHeader(viewerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "无权限测试", "image", "/uploads/x.jpg",
                                "linkType", 0, "position", "home", "sort", 99, "status", 1))))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(codeOf(denied)).isEqualTo(403);
        assertThat((String) JsonPath.read(denied, "$.message")).contains("无此操作权限");

        // 超管：写操作放行
        String created = mockMvc.perform(post("/api/admin/banners")
                        .header("Authorization", authHeader(superId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "超管测试轮播", "image", "/uploads/y.jpg",
                                "linkType", 0, "position", "home", "sort", 98, "status", 0))))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(codeOf(created)).isEqualTo(0);
        Long bannerId = Long.valueOf(JsonPath.read(created, "$.data").toString());
        mockMvc.perform(delete("/api/admin/banners/" + bannerId)
                .header("Authorization", authHeader(superId)));

        // 未登录：401
        String anonymous = mockMvc.perform(get("/api/admin/banners"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(codeOf(anonymous)).isEqualTo(401);
    }
}
