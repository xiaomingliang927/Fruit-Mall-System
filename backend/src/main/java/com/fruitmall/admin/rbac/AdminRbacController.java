package com.fruitmall.admin.rbac;

import com.fruitmall.admin.AdminContext;
import com.fruitmall.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 管理端 RBAC 接口：权限点列表、角色增删改、管理员授权 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRbacController {

    private final RbacService rbacService;

    @GetMapping("/permissions")
    public ApiResponse<List<RbacService.PermissionVO>> permissions() {
        AdminContext.requireAdminId();
        return ApiResponse.ok(rbacService.listPermissions());
    }

    @GetMapping("/roles")
    public ApiResponse<List<RbacService.RoleVO>> roles() {
        AdminContext.requireAdminId();
        return ApiResponse.ok(rbacService.listRoles());
    }

    @PostMapping("/roles")
    public ApiResponse<Long> createRole(@RequestBody RoleRequest request) {
        AdminContext.requireAdminId();
        return ApiResponse.ok(rbacService.createRole(request.toInput()));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<Void> updateRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        AdminContext.requireAdminId();
        rbacService.updateRole(id, request.toInput());
        return ApiResponse.ok();
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        AdminContext.requireAdminId();
        rbacService.deleteRole(id);
        return ApiResponse.ok();
    }

    @GetMapping("/admins")
    public ApiResponse<List<RbacService.AdminVO>> admins() {
        AdminContext.requireAdminId();
        return ApiResponse.ok(rbacService.listAdmins());
    }

    @PutMapping("/roles/admins/{adminId}")
    public ApiResponse<Void> assignRoles(@PathVariable Long adminId, @RequestBody AssignRequest request) {
        AdminContext.requireAdminId();
        rbacService.assignAdminRoles(adminId, request.roleIds());
        return ApiResponse.ok();
    }

    public record RoleRequest(@NotBlank(message = "请填写角色名") String name,
                              @NotBlank(message = "请填写角色编码") String code,
                              String remark, Integer status,
                              @NotEmpty(message = "请至少选择一个权限点") List<String> permissions) {
        RbacService.RoleInput toInput() {
            return new RbacService.RoleInput(name, code, remark, status, permissions);
        }
    }

    public record AssignRequest(List<Long> roleIds) {
    }
}
