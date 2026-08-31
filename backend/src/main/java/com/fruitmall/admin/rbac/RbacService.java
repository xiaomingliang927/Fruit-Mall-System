package com.fruitmall.admin.rbac;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.admin.AdminUser;
import com.fruitmall.admin.AdminUserMapper;
import com.fruitmall.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 后台 RBAC：权限查询（供拦截器）、角色与授权管理 */
@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AdminUserMapper adminUserMapper;

    public record PermissionVO(Long id, String name, String code, Integer type) {
    }

    public record RoleVO(Long id, String name, String code, String remark, Integer status,
                         Integer adminCount, List<String> permissions, LocalDateTime createdAt) {
    }

    public record AdminVO(Long id, String username, String realName, Integer status,
                          List<Long> roleIds, List<String> roleNames) {
    }

    public record RoleInput(String name, String code, String remark, Integer status, List<String> permissions) {
    }

    // ============ 权限查询（拦截器用） ============

    public List<Long> roleIdsOf(Long adminId) {
        return adminRoleMapper.selectList(new LambdaQueryWrapper<AdminRole>()
                        .eq(AdminRole::getAdminId, adminId))
                .stream().map(AdminRole::getRoleId).distinct().toList();
    }

    /** 管理员拥有的全部权限点编码（多角色并集） */
    public List<String> permCodesOf(Long adminId) {
        List<Long> roleIds = roleIdsOf(adminId);
        if (roleIds.isEmpty()) return List.of();
        List<Long> permIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                        .in(RolePermission::getRoleId, roleIds))
                .stream().map(RolePermission::getPermissionId).distinct().toList();
        if (permIds.isEmpty()) return List.of();
        return permissionMapper.selectBatchIds(permIds).stream()
                .map(Permission::getCode).filter(Objects::nonNull).distinct().toList();
    }

    /** 超级管理员跳过逐点校验 */
    public boolean isSuperAdmin(Long adminId) {
        List<Long> roleIds = roleIdsOf(adminId);
        if (roleIds.isEmpty()) return false;
        return roleMapper.selectBatchIds(roleIds).stream()
                .anyMatch(r -> Role.CODE_SUPER.equals(r.getCode()));
    }

    // ============ 权限点 ============

    public List<PermissionVO> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                        .orderByAsc(Permission::getId))
                .stream().map(p -> new PermissionVO(p.getId(), p.getName(), p.getCode(), p.getType()))
                .toList();
    }

    // ============ 角色 ============

    public List<RoleVO> listRoles() {
        List<RoleVO> vos = new ArrayList<>();
        for (Role r : roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId))) {
            List<Long> permIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, r.getId()))
                    .stream().map(RolePermission::getPermissionId).toList();
            List<String> codes = permIds.isEmpty() ? List.of()
                    : permissionMapper.selectBatchIds(permIds).stream()
                            .map(Permission::getCode).filter(Objects::nonNull).distinct().sorted().toList();
            long adminCount = adminRoleMapper.selectCount(new LambdaQueryWrapper<AdminRole>()
                    .eq(AdminRole::getRoleId, r.getId()));
            vos.add(new RoleVO(r.getId(), r.getName(), r.getCode(), r.getRemark(), r.getStatus(),
                    (int) adminCount, codes, r.getCreatedAt()));
        }
        return vos;
    }

    @Transactional
    public Long createRole(RoleInput input) {
        String name = requireText(input.name(), "请填写角色名");
        String code = requireText(input.code(), "请填写角色编码");
        if (Role.CODE_SUPER.equals(code)) throw new BizException("超级管理员为内置角色，不可重复创建");
        if (roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getCode, code)) > 0) {
            throw new BizException("角色编码已存在");
        }
        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setRemark(input.remark());
        role.setStatus(input.status() != null && input.status() == 0 ? 0 : 1);
        roleMapper.insert(role);
        replacePermissions(role.getId(), input.permissions());
        return role.getId();
    }

    @Transactional
    public void updateRole(Long id, RoleInput input) {
        Role role = requireRole(id);
        if (Role.CODE_SUPER.equals(role.getCode()) && !Role.CODE_SUPER.equals(input.code())) {
            throw new BizException("超级管理员角色不可修改编码");
        }
        role.setName(requireText(input.name(), "请填写角色名"));
        role.setRemark(input.remark());
        if (input.status() != null) role.setStatus(input.status() == 0 ? 0 : 1);
        roleMapper.updateById(role);
        replacePermissions(id, input.permissions());
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = requireRole(id);
        if (Role.CODE_SUPER.equals(role.getCode())) throw new BizException("超级管理员角色不可删除");
        long bound = adminRoleMapper.selectCount(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getRoleId, id));
        if (bound > 0) throw new BizException("该角色已绑定 " + bound + " 个管理员，请先解除绑定");
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    // ============ 管理员授权 ============

    public List<AdminVO> listAdmins() {
        List<AdminUser> admins = adminUserMapper.selectList(null);
        List<AdminVO> vos = new ArrayList<>();
        for (AdminUser a : admins) {
            List<Long> roleIds = roleIdsOf(a.getId());
            List<String> names = roleIds.isEmpty() ? List.of()
                    : roleMapper.selectBatchIds(roleIds).stream().map(Role::getName).toList();
            vos.add(new AdminVO(a.getId(), a.getUsername(), a.getRealName(), a.getStatus(), roleIds, names));
        }
        return vos;
    }

    @Transactional
    public void assignAdminRoles(Long adminId, List<Long> roleIds) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) throw new BizException("管理员不存在");
        adminRoleMapper.delete(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getAdminId, adminId));
        if (roleIds == null || roleIds.isEmpty()) return;
        for (Long roleId : roleIds.stream().filter(Objects::nonNull).distinct().toList()) {
            if (roleMapper.selectById(roleId) == null) throw new BizException("角色不存在：" + roleId);
            AdminRole ar = new AdminRole();
            ar.setAdminId(adminId);
            ar.setRoleId(roleId);
            adminRoleMapper.insert(ar);
        }
    }

    /** 启动兜底：没有角色的管理员自动挂上超级管理员，避免把自己锁在门外 */
    @Transactional
    public void ensureRoleBinding(Long adminId) {
        if (!roleIdsOf(adminId).isEmpty()) return;
        Role superRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, Role.CODE_SUPER));
        if (superRole == null) return;
        AdminRole ar = new AdminRole();
        ar.setAdminId(adminId);
        ar.setRoleId(superRole.getId());
        adminRoleMapper.insert(ar);
    }

    // ============ 内部 ============

    private void replacePermissions(Long roleId, List<String> codes) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (codes == null || codes.isEmpty()) return;
        for (Permission p : permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .in(Permission::getCode, codes))) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(p.getId());
            rolePermissionMapper.insert(rp);
        }
    }

    private Role requireRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw new BizException("角色不存在");
        return role;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new BizException(message);
        return value.trim();
    }
}
