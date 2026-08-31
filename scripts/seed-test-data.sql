-- =====================================================================
-- scripts/seed-test-data.sql — 后台「数据中心 / 订单中心 / 优惠券」演示测试数据
-- 幂等：以业务键（order_no / refund_no / 券名）为准，重复执行先删后插
-- 金额单位一律为「分」（INT）
-- 使用：mysql -uroot -p -h127.0.0.1 --default-character-set=utf8mb4 fruit_mall < scripts/seed-test-data.sql
-- =====================================================================

-- ---------- 1. 清理（幂等：按券名/业务键，不依赖自增 id） ----------
DELETE FROM order_item WHERE order_id IN (SELECT id FROM `order` WHERE order_no IN ('FM2608310935123401','FM2608311030123402','FM2608311130123403'));
DELETE FROM payment    WHERE order_no IN ('FM2608310935123401','FM2608311030123402','FM2608311130123403');
DELETE FROM refund     WHERE refund_no = 'RF2608310935128888';
DELETE FROM `order`    WHERE order_no IN ('FM2608310935123401','FM2608311030123402','FM2608311130123403');
DELETE FROM user_coupon WHERE order_no = 'FM2608311030123402'
    OR coupon_id IN (SELECT id FROM coupon WHERE name IN ('新人满99减20','整单9折','新人立减5元'));
DELETE FROM coupon WHERE name IN ('新人满99减20','整单9折','新人立减5元');

-- ---------- 2. 优惠券模板：满减 / 折扣 / 无门槛 三种类型齐 ----------
INSERT INTO coupon (name, type, threshold_amount, discount_amount, discount_percent,
                    total_count, issued_count, per_user_limit, start_time, end_time, valid_days, status)
VALUES ('新人满99减20', 1, 9900, 2000, NULL,
        50, 0, 1, '2026-08-01 00:00:00', '2026-12-31 23:59:59', 30, 1);
SET @c1 = LAST_INSERT_ID();
INSERT INTO coupon (name, type, threshold_amount, discount_amount, discount_percent,
                    total_count, issued_count, per_user_limit, start_time, end_time, valid_days, status)
VALUES ('整单9折', 2, 9900, 0, 90,
        30, 0, 1, '2026-08-01 00:00:00', '2026-12-31 23:59:59', 30, 1);
SET @c2 = LAST_INSERT_ID();
INSERT INTO coupon (name, type, threshold_amount, discount_amount, discount_percent,
                    total_count, issued_count, per_user_limit, start_time, end_time, valid_days, status)
VALUES ('新人立减5元', 3, 0, 500, NULL,
        100, 0, 1, '2026-08-01 00:00:00', '2026-12-31 23:59:59', 30, 1);
SET @c3 = LAST_INSERT_ID();

-- ---------- 3. 订单 A：user4 售后中（60）→ 联动 1 条待审核退款单 ----------
INSERT INTO `order` (order_no, user_id, status, total_amount, pay_amount, freight,
                     address_snapshot, delivery_type, tracking_no, remark,
                     paid_at, shipped_at, completed_at, created_at, updated_at)
VALUES ('FM2608310935123401', 4, 60, 4570, 4570, 0,
        '{"receiver":"李雷","phone":"13900002222","province":"广东省","city":"广州市","district":"天河区","detail":"体育西路189号"}',
        1, NULL, '芒果有碰伤，申请售后',
        '2026-08-31 09:35:30', NULL, NULL, '2026-08-31 09:35:12', '2026-08-31 09:35:12');
SET @oa = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@oa, 17, 17, '金煌芒果', '500g/份', '/images/products/fruit-mango.jpg',      1390, 1, 1390),
(@oa, 18, 18, '云南蓝莓', '125g/盒', '/images/products/fruit-blueberry.jpg',  1590, 2, 3180);
INSERT INTO payment (order_no, transaction_id, channel, amount, status, paid_at) VALUES
('FM2608310935123401', 'MOCK-FM2608310935123401', 'MOCK', 4570, 1, '2026-08-31 09:35:30');
INSERT INTO refund (refund_no, order_no, user_id, type, reason, images, amount, status,
                    audit_by, audit_time, audit_remark, refund_time, created_at, updated_at)
VALUES ('RF2608310935128888', 'FM2608310935123401', 4, 1, '芒果收到有碰伤，申请退款', NULL, 1390, 0,
        NULL, NULL, NULL, NULL, '2026-08-31 09:40:00', '2026-08-31 09:40:00');

-- ---------- 4. 订单 B：user2 待发货（20）+ 核销无门槛券（减 5 元） ----------
INSERT INTO `order` (order_no, user_id, status, total_amount, pay_amount, freight,
                     address_snapshot, delivery_type, tracking_no, remark,
                     paid_at, shipped_at, completed_at, created_at, updated_at)
VALUES ('FM2608311030123402', 2, 20, 4750, 4250, 0,
        '{"receiver":"王芳","phone":"13700003333","province":"广东省","city":"深圳市","district":"福田区","detail":"福华一路88号"}',
        1, NULL, '请尽快发货',
        '2026-08-31 10:30:20', NULL, NULL, '2026-08-31 10:30:12', '2026-08-31 10:30:12');
SET @ob = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@ob, 11, 11, '麒麟西瓜',   '约4kg/个', '/images/products/fruit-watermelon.jpg', 2990, 1, 2990),
(@ob, 12, 12, '红富士苹果', '500g/份', '/images/products/fruit-apple.jpg',       880, 2, 1760);
INSERT INTO payment (order_no, transaction_id, channel, amount, status, paid_at) VALUES
('FM2608311030123402', 'MOCK-FM2608311030123402', 'MOCK', 4250, 1, '2026-08-31 10:30:20');

-- ---------- 5. 订单 C：user2 已完成（40）+ 全流程时间线 ----------
INSERT INTO `order` (order_no, user_id, status, total_amount, pay_amount, freight,
                     address_snapshot, delivery_type, tracking_no, remark,
                     paid_at, shipped_at, completed_at, created_at, updated_at)
VALUES ('FM2608311130123403', 2, 40, 3370, 3370, 0,
        '{"receiver":"王芳","phone":"13700003333","province":"广东省","city":"深圳市","district":"福田区","detail":"福华一路88号"}',
        1, 'SF1389998888777', '第二次回购，果很新鲜',
        '2026-08-31 11:32:20', '2026-08-31 11:45:00', '2026-08-31 15:20:00', '2026-08-31 11:30:12', '2026-08-31 15:20:00');
SET @oc = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@oc, 20, 20, '都乐菠萝',   '约1kg/个', '/images/products/fruit-pineapple.jpg',     1290, 2, 2580),
(@oc, 19, 19, '红心火龙果', '约450g/个', '/images/products/fruit-dragonfruit.jpg',   790, 1, 790);
INSERT INTO payment (order_no, transaction_id, channel, amount, status, paid_at) VALUES
('FM2608311130123403', 'MOCK-FM2608311130123403', 'MOCK', 3370, 1, '2026-08-31 11:32:20');

-- ---------- 6. 用户领券记录：覆盖「未使用 / 已使用」状态 ----------
INSERT INTO user_coupon (coupon_id, user_id, status, order_no, received_at, expire_at, used_at) VALUES
(@c1, 2, 0, NULL, '2026-08-29 10:00:00', '2026-09-28 10:00:00', NULL),      -- 券1 满99减20 · user2 未使用
(@c2, 4, 0, NULL, '2026-08-30 11:00:00', '2026-09-29 11:00:00', NULL),      -- 券2 整单9折  · user4 未使用
(@c3, 2, 1, 'FM2608311030123402', '2026-08-31 09:00:00', '2026-09-30 09:00:00', '2026-08-31 10:30:00'); -- 券3 立减5元 · user2 已核销订单B

-- ---------- 7. 按实际领取数重算 issued_count（幂等收敛） ----------
UPDATE coupon c
SET issued_count = (SELECT COUNT(*) FROM user_coupon uc WHERE uc.coupon_id = c.id);
