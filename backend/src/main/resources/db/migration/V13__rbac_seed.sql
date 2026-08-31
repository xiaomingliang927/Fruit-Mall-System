-- =====================================================================
-- V13__rbac_seed.sql — RBAC 权限点与内置角色
-- permission.code 与 admin-web/menu.js 的 perm、PermissionRegistry 的映射三者对齐；
-- role.code = 'super' 为内置超级管理员（跳过逐点校验、不可删除）。
-- =====================================================================

INSERT INTO permission (parent_id, name, code, type) VALUES
(0, '经营看板',     'dashboard',      1),
(0, '营业统计',     'revenue',        1),
(0, '商品管理',     'product:list',   1),
(0, '新建商品',     'product:create', 2),
(0, '编辑商品',     'product:update', 2),
(0, '订单管理',     'order:list',     1),
(0, '订单发货',     'order:ship',     2),
(0, '订单取消',     'order:cancel',   2),
(0, '售后管理',     'refund:list',    1),
(0, '售后审核',     'refund:audit',   2),
(0, '评价管理',     'review:list',    1),
(0, '评价回复',     'review:reply',   2),
(0, '会员管理',     'member:list',    1),
(0, '会员开卡',     'member:create',  2),
(0, '优惠券管理',   'coupon:list',    1),
(0, '新建优惠券',   'coupon:create',  2),
(0, '优惠券上下架', 'coupon:update',  2),
(0, '轮播图管理',   'banner:list',    1),
(0, '新建轮播图',   'banner:create',  2),
(0, '编辑轮播图',   'banner:update',  2),
(0, '删除轮播图',   'banner:delete',  2),
(0, '角色管理',     'role:list',      1),
(0, '新建角色',     'role:create',    2),
(0, '编辑角色',     'role:update',    2),
(0, '删除角色',     'role:delete',    2),
(0, '账号授权',     'role:assign',    2);

INSERT INTO role (name, code, remark, status) VALUES
('超级管理员',   'super',    '内置角色：拥有全部权限，跳过逐点校验，不可删除', 1),
('运营专员',     'operator', '日常运营：商品/订单/售后/评价/会员/营销读写，不含账号与角色管理', 1),
('只读观察员',   'viewer',   '仅可查看，不可执行任何写操作', 1);

-- 超级管理员：全量权限点
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p WHERE r.code = 'super';

-- 运营专员：除角色/账号管理外的全部读写权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'operator' AND p.code IN (
    'dashboard', 'revenue',
    'product:list', 'product:create', 'product:update',
    'order:list', 'order:ship', 'order:cancel',
    'refund:list', 'refund:audit',
    'review:list', 'review:reply',
    'member:list', 'member:create',
    'coupon:list', 'coupon:create', 'coupon:update',
    'banner:list', 'banner:create', 'banner:update', 'banner:delete'
);

-- 只读观察员：仅菜单级（查看）权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'viewer' AND p.type = 1;
