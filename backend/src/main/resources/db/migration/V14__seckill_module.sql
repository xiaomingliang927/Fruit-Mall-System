-- =====================================================================
-- V14__seckill_module.sql — M3 秒杀参与记录 + 秒杀权限点
-- seckill_activity 建于 V8；本表记录「谁参与了哪场秒杀」，
-- uk_seckill_user 唯一键在数据库层面保证每人一场一单（防并发重复参与）。
-- =====================================================================

CREATE TABLE seckill_order (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    seckill_id     BIGINT       NOT NULL COMMENT '秒杀活动ID',
    user_id        BIGINT       NOT NULL COMMENT '参与用户',
    order_no       VARCHAR(32)  NULL COMMENT '生成的订单号（下单成功后回填）',
    quantity       INT          NOT NULL DEFAULT 1 COMMENT '秒杀件数',
    seckill_price  INT          NOT NULL COMMENT '成交秒杀价，分',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seckill_user (seckill_id, user_id),
    KEY idx_so_order (order_no)
) COMMENT ='秒杀参与记录';

-- 秒杀权限点（与 admin-web 菜单 perm、PermissionRegistry 三方对齐）
INSERT INTO permission (parent_id, name, code, type) VALUES
(0, '秒杀管理',     'seckill:list',   1),
(0, '新建秒杀',     'seckill:create', 2),
(0, '编辑秒杀',     'seckill:update', 2),
(0, '删除秒杀',     'seckill:delete', 2);

-- 超级管理员：全量权限点（含新加的秒杀）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'super' AND p.code IN ('seckill:list', 'seckill:create', 'seckill:update', 'seckill:delete');

-- 运营专员：秒杀读写
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'operator' AND p.code IN ('seckill:list', 'seckill:create', 'seckill:update', 'seckill:delete');
