-- =====================================================================
-- V1__init_schema.sql — 全量表结构
-- 兼容 MySQL 8 与 H2（MySQL 模式）：不使用 ENGINE/表级 COMMENT/内联 INDEX
-- 金额字段单位一律为「分」（INT）；created_at/updated_at 由应用层自动填充
-- =====================================================================

CREATE TABLE `user` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    openid      VARCHAR(64)  NULL COMMENT '微信 openid，网站端登录为空',
    phone       VARCHAR(20)  NOT NULL COMMENT '手机号',
    nickname    VARCHAR(64)  NULL COMMENT '昵称',
    avatar      VARCHAR(255) NULL COMMENT '头像',
    level       TINYINT      NOT NULL DEFAULT 1 COMMENT '会员等级',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_openid (openid)
) COMMENT ='会员表';

CREATE TABLE user_address (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    receiver    VARCHAR(32)  NOT NULL COMMENT '收货人',
    phone       VARCHAR(20)  NOT NULL COMMENT '收货手机号',
    province    VARCHAR(32)  NOT NULL COMMENT '省',
    city        VARCHAR(32)  NOT NULL COMMENT '市',
    district    VARCHAR(32)  NULL COMMENT '区/县',
    detail      VARCHAR(255) NOT NULL COMMENT '详细地址',
    is_default  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT ='收货地址表';
CREATE INDEX idx_addr_user ON user_address (user_id);

CREATE TABLE category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID，顶级为0',
    name        VARCHAR(32)  NOT NULL COMMENT '分类名',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    icon        VARCHAR(255) NULL COMMENT '分类图标',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT ='商品分类表';

CREATE TABLE product (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    category_id BIGINT       NOT NULL COMMENT '分类ID',
    name        VARCHAR(128) NOT NULL COMMENT '商品名',
    subtitle    VARCHAR(255) NULL COMMENT '副标题（卖点）',
    main_image  VARCHAR(255) NULL COMMENT '主图，生产环境替换为 OSS/CDN 地址',
    origin      VARCHAR(64)  NULL COMMENT '产地',
    unit        VARCHAR(16)  NULL COMMENT '计量单位：斤/箱/盒/个',
    tags        VARCHAR(128) NULL COMMENT '标签，逗号分隔',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    sales       INT          NOT NULL DEFAULT 0 COMMENT '累计销量',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT ='商品 SPU 表';
CREATE INDEX idx_product_category ON product (category_id);

CREATE TABLE product_sku (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    product_id  BIGINT       NOT NULL COMMENT '商品ID',
    spec        VARCHAR(64)  NOT NULL COMMENT '规格：5斤装 / 单果80-90mm',
    price       INT          NOT NULL COMMENT '售价，单位：分',
    stock       INT          NOT NULL DEFAULT 0 COMMENT '库存',
    warn_stock  INT          NOT NULL DEFAULT 10 COMMENT '库存预警阈值',
    image       VARCHAR(255) NULL COMMENT '规格图',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT ='商品 SKU 表';
CREATE INDEX idx_sku_product ON product_sku (product_id);

CREATE TABLE cart_item (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    sku_id      BIGINT   NOT NULL,
    quantity    INT      NOT NULL DEFAULT 1 COMMENT '数量',
    checked     TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否勾选结算',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_sku (user_id, sku_id)
) COMMENT ='购物车表';

CREATE TABLE `order` (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    order_no         VARCHAR(32)  NOT NULL COMMENT '业务订单号 FM+yyMMddHHmmss+4位随机',
    user_id          BIGINT       NOT NULL,
    status           TINYINT      NOT NULL DEFAULT 10 COMMENT '10待支付 20待发货 30待收货 40已完成 50已取消 60售后中 70已退款',
    total_amount     INT          NOT NULL DEFAULT 0 COMMENT '商品总额，分',
    pay_amount       INT          NOT NULL DEFAULT 0 COMMENT '实付金额，分',
    freight          INT          NOT NULL DEFAULT 0 COMMENT '运费，分',
    address_snapshot TEXT         NULL COMMENT '收货地址 JSON 快照',
    delivery_type    TINYINT      NOT NULL DEFAULT 1 COMMENT '1快递 2同城 3自提',
    tracking_no      VARCHAR(64)  NULL COMMENT '快递运单号',
    remark           VARCHAR(255) NULL COMMENT '买家留言',
    paid_at          DATETIME     NULL,
    shipped_at       DATETIME     NULL,
    completed_at     DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no)
) COMMENT ='订单主表';
CREATE INDEX idx_order_user ON `order` (user_id);
CREATE INDEX idx_order_status ON `order` (status);

CREATE TABLE order_item (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    order_id     BIGINT       NOT NULL,
    product_id   BIGINT       NOT NULL,
    sku_id       BIGINT       NOT NULL,
    product_name VARCHAR(128) NOT NULL COMMENT '商品名快照',
    sku_spec     VARCHAR(64)  NULL COMMENT '规格快照',
    image        VARCHAR(255) NULL COMMENT '图片快照',
    price        INT          NOT NULL COMMENT '成交单价，分',
    quantity     INT          NOT NULL,
    subtotal     INT          NOT NULL COMMENT '小计，分',
    PRIMARY KEY (id)
) COMMENT ='订单明细表';
CREATE INDEX idx_item_order ON order_item (order_id);

CREATE TABLE payment (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(32) NOT NULL,
    transaction_id VARCHAR(64) NULL COMMENT '渠道交易号（微信 transaction_id）',
    channel        VARCHAR(20) NOT NULL COMMENT 'MOCK / WECHAT_JSAPI / WECHAT_NATIVE',
    amount         INT         NOT NULL COMMENT '实付金额，分',
    status         TINYINT     NOT NULL DEFAULT 1 COMMENT '1支付成功',
    paid_at        DATETIME    NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT ='支付流水表';
CREATE INDEX idx_pay_order ON payment (order_no);

CREATE TABLE admin_user (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    username      VARCHAR(32) NOT NULL COMMENT '登录名',
    password_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256(salt+密码)',
    salt          VARCHAR(32) NOT NULL,
    real_name     VARCHAR(32) NULL,
    status        TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) COMMENT ='管理后台账号表';
