-- =====================================================================
-- V8__m2_m3_schema.sql — M2/M3 功能表结构（评价/优惠券/售后/秒杀/内容/RBAC/日志）
-- 设计文档：docs/数据库设计.md
-- 兼容 MySQL 8 与 H2（MySQL 模式）；JSON 类字段统一用 TEXT 存储
-- =====================================================================

-- ----------------------------
-- M2 · 商品评价
-- ----------------------------
CREATE TABLE review (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    order_id      BIGINT       NOT NULL COMMENT '订单ID',
    order_no      VARCHAR(32)  NOT NULL COMMENT '订单号',
    order_item_id BIGINT       NOT NULL COMMENT '订单明细ID（每明细一条评价）',
    user_id       BIGINT       NOT NULL COMMENT '评价人',
    product_id    BIGINT       NOT NULL COMMENT '商品ID（冗余，便于按商品查评价）',
    rating        TINYINT      NOT NULL DEFAULT 5 COMMENT '评分 1~5',
    content       VARCHAR(500) NULL COMMENT '评价内容',
    images        TEXT         NULL COMMENT '晒图 URL JSON 数组',
    is_anonymous  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否匿名',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0待审核 1通过 2隐藏',
    admin_reply   VARCHAR(500) NULL COMMENT '商家回复',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item (order_item_id)
) COMMENT ='商品评价';
CREATE INDEX idx_review_product ON review (product_id, status);
CREATE INDEX idx_review_user ON review (user_id);

-- ----------------------------
-- M2 · 优惠券模板 + 用户券
-- ----------------------------
CREATE TABLE coupon (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    name             VARCHAR(64) NOT NULL COMMENT '券名，如 新人立减10元',
    type             TINYINT     NOT NULL DEFAULT 1 COMMENT '1满减 2折扣 3无门槛',
    threshold_amount INT         NOT NULL DEFAULT 0 COMMENT '使用门槛，分（满X可用，0=无门槛）',
    discount_amount  INT         NOT NULL DEFAULT 0 COMMENT '优惠金额，分（满减/无门槛用）',
    discount_percent INT         NULL COMMENT '折扣率（折扣券用，如 90 = 9 折）',
    total_count      INT         NOT NULL DEFAULT 0 COMMENT '发放总量',
    issued_count     INT         NOT NULL DEFAULT 0 COMMENT '已发放数量',
    per_user_limit   INT         NOT NULL DEFAULT 1 COMMENT '每人限领',
    start_time       DATETIME    NOT NULL COMMENT '可领取开始时间',
    end_time         DATETIME    NOT NULL COMMENT '可领取结束时间',
    valid_days       INT         NOT NULL DEFAULT 30 COMMENT '领取后有效天数',
    status           TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT ='优惠券模板';

CREATE TABLE user_coupon (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    coupon_id   BIGINT      NOT NULL COMMENT '券模板ID',
    user_id     BIGINT      NOT NULL COMMENT '持有用户',
    status      TINYINT     NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
    order_no    VARCHAR(32) NULL COMMENT '核销订单号',
    received_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    expire_at   DATETIME    NOT NULL COMMENT '过期时间 = 领取时间 + valid_days',
    used_at     DATETIME    NULL,
    PRIMARY KEY (id),
    KEY idx_uc_user (user_id, status),
    KEY idx_uc_coupon (coupon_id)
) COMMENT ='用户优惠券';

-- ----------------------------
-- M2 · 售后退款单
-- ----------------------------
CREATE TABLE refund (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    refund_no     VARCHAR(32)  NOT NULL COMMENT '售后单号 RF+时间+随机',
    order_no      VARCHAR(32)  NOT NULL COMMENT '订单号',
    user_id       BIGINT       NOT NULL,
    type          TINYINT      NOT NULL DEFAULT 1 COMMENT '1仅退款 2退货退款',
    reason        VARCHAR(255) NOT NULL COMMENT '申请原因',
    images        TEXT         NULL COMMENT '凭证图 URL JSON 数组',
    amount        INT          NOT NULL COMMENT '申请退款金额，分',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1已同意待退款 2已拒绝 3待用户寄回 4已退款',
    audit_by      BIGINT       NULL COMMENT '审核管理员ID',
    audit_time    DATETIME     NULL,
    audit_remark  VARCHAR(255) NULL COMMENT '审核备注/拒绝原因',
    refund_time   DATETIME     NULL COMMENT '退款到账时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no)
) COMMENT ='售后退款单';
CREATE INDEX idx_refund_order ON refund (order_no);
CREATE INDEX idx_refund_user ON refund (user_id, status);

-- ----------------------------
-- M3 · 秒杀活动
-- ----------------------------
CREATE TABLE seckill_activity (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    product_id      BIGINT      NOT NULL COMMENT '商品ID',
    sku_id          BIGINT      NOT NULL COMMENT '秒杀规格ID',
    seckill_price   INT         NOT NULL COMMENT '秒杀价，分',
    total_stock     INT         NOT NULL COMMENT '秒杀总量',
    available_stock INT         NOT NULL COMMENT '剩余可秒量',
    limit_per_user  INT         NOT NULL DEFAULT 1 COMMENT '每人限购',
    start_time      DATETIME    NOT NULL,
    end_time        DATETIME    NOT NULL,
    status          TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_seckill_time (start_time, end_time, status)
) COMMENT ='秒杀活动';

-- ----------------------------
-- 内容管理：轮播图/推荐位 + 公告
-- ----------------------------
CREATE TABLE banner (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(64)  NOT NULL COMMENT '标题（后台标识用）',
    image      VARCHAR(255) NOT NULL COMMENT '图片地址',
    link_type  TINYINT      NOT NULL DEFAULT 0 COMMENT '0无跳转 1商品 2页面路径',
    link_value VARCHAR(255) NULL COMMENT '商品ID或页面路径',
    position   VARCHAR(20)  NOT NULL DEFAULT 'home' COMMENT '位置：home 首页轮播',
    sort       INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT ='轮播图/推荐位';

CREATE TABLE notice (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(64)  NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT ='公告';

-- ----------------------------
-- M3 · RBAC 权限（角色/权限点/关联）
-- ----------------------------
CREATE TABLE role (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(32) NOT NULL COMMENT '角色名，如 商品运营',
    code       VARCHAR(32) NOT NULL COMMENT '角色编码，如 product_operator',
    remark     VARCHAR(128) NULL,
    status     TINYINT     NOT NULL DEFAULT 1,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) COMMENT ='后台角色';

CREATE TABLE permission (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限点（菜单分组），0=顶级',
    name      VARCHAR(32)  NOT NULL COMMENT '权限名，如 会员列表',
    code      VARCHAR(64)  NOT NULL COMMENT '权限编码，与前端菜单 perm 对齐，如 member:list',
    type      TINYINT      NOT NULL DEFAULT 1 COMMENT '1菜单 2按钮/接口',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (code)
) COMMENT ='后台权限点';

CREATE TABLE admin_role (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_role (admin_id, role_id)
) COMMENT ='管理员-角色关联';

CREATE TABLE role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) COMMENT ='角色-权限关联';

-- ----------------------------
-- M3 · 后台操作日志（审计）
-- ----------------------------
CREATE TABLE operation_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    admin_id   BIGINT       NOT NULL,
    admin_name VARCHAR(32)  NOT NULL,
    module     VARCHAR(32)  NOT NULL COMMENT '模块：order/product/member...',
    action     VARCHAR(32)  NOT NULL COMMENT '动作：create/update/ship/audit...',
    detail     TEXT         NULL COMMENT '变更详情 JSON',
    ip         VARCHAR(45)  NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_oplog_admin (admin_id, created_at)
) COMMENT ='后台操作日志';
