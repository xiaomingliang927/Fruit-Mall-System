-- =====================================================================
-- demo-records.sql — 演示数据（会员/订单/支付/优惠券/评价/售后/轮播）
-- 适用：已执行 V1~V11 的 fruit 库（商品 ID 11~20）
-- 本地库使用时把 USE fruit 改为 USE fruit_mall
-- 可重复执行性：订单号/售后号/手机号固定，重复执行会因唯一键报错，属正常
-- =====================================================================
USE fruit;

-- ---------- 会员（4 人：黄金/白银临期/普通/禁用） ----------
INSERT INTO `user` (phone, nickname, level, status, member_expire_at, created_at) VALUES
('13900001001', '张小果', 3, 1, DATE_ADD(NOW(), INTERVAL 200 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
('13900001002', '李橙子', 2, 1, DATE_ADD(NOW(), INTERVAL 5 DAY),  DATE_SUB(NOW(), INTERVAL 60 DAY)),
('13900001003', '王莓莓', 1, 1, NULL,                             DATE_SUB(NOW(), INTERVAL 30 DAY)),
('13900001004', '刘蕉蕉', 1, 0, NULL,                             DATE_SUB(NOW(), INTERVAL 15 DAY));
SET @u1 = (SELECT id FROM `user` WHERE phone = '13900001001');
SET @u2 = (SELECT id FROM `user` WHERE phone = '13900001002');
SET @u3 = (SELECT id FROM `user` WHERE phone = '13900001003');

-- ---------- 收货地址 ----------
INSERT INTO user_address (user_id, receiver, phone, province, city, district, detail, is_default) VALUES
(@u1, '张小果', '13900001001', '广东省', '深圳市', '南山区', '科技园南路88号A栋1201', 1),
(@u2, '李橙子', '13900001002', '浙江省', '杭州市', '西湖区', '文三路200号华星时代广场', 1),
(@u3, '王莓莓', '13900001003', '上海市', '上海市', '浦东新区', '张江高科技园区博云路2号', 1);

-- ---------- 优惠券模板（3 种类型） ----------
INSERT INTO coupon (name, type, threshold_amount, discount_amount, discount_percent, total_count, issued_count, per_user_limit, start_time, end_time, valid_days, status) VALUES
('新人专享 满99减20', 1, 9900, 2000, NULL, 500, 120, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 30, 1),
('会员日 整单9折',    2, 19900, 0, 90,    200, 45,  1, DATE_SUB(NOW(), INTERVAL 5 DAY),  DATE_ADD(NOW(), INTERVAL 30 DAY), 15, 1),
('无门槛立减5元',     3, 0,     500, NULL, 1000, 300, 2, DATE_SUB(NOW(), INTERVAL 3 DAY),  DATE_ADD(NOW(), INTERVAL 90 DAY), 30, 1);
SET @cp1 = (SELECT id FROM coupon WHERE name = '新人专享 满99减20');
SET @cp2 = (SELECT id FROM coupon WHERE name = '会员日 整单9折');
SET @cp3 = (SELECT id FROM coupon WHERE name = '无门槛立减5元');

-- 用户持券：未使用 / 已使用 / 已过期 各有覆盖
INSERT INTO user_coupon (coupon_id, user_id, status, order_no, received_at, expire_at, used_at) VALUES
(@cp1, @u1, 0, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 28 DAY), NULL),
(@cp3, @u1, 0, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY),  DATE_ADD(NOW(), INTERVAL 29 DAY), NULL),
(@cp2, @u2, 2, NULL, DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL),
(@cp3, @u2, 1, 'FMDEMO0101', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY));

-- ---------- 订单 A：已完成 + 已评价 + 用券（张小果，6 天前） ----------
INSERT INTO `order` (order_no, user_id, status, total_amount, pay_amount, freight, address_snapshot, delivery_type, tracking_no, user_coupon_id, remark, paid_at, shipped_at, completed_at, created_at)
VALUES ('FMDEMO0101', @u1, 40, 11980, 11480, 0,
        '{"receiver":"张小果","phone":"13900001001","province":"广东省","city":"深圳市","district":"南山区","detail":"科技园南路88号A栋1201"}',
        1, 'SF0001111001', NULL, '尽快发货', DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 10 HOUR,
        DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 15 HOUR, DATE_SUB(NOW(), INTERVAL 4 DAY),
        DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 9 HOUR);
SET @oA = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@oA, 16, 16, '丹东草莓', '250g/盒', '/images/products/fruit-strawberry.jpg', 1990, 2, 3980),
(@oA, 18, 18, '云南蓝莓', '125g/盒', '/images/products/fruit-blueberry.jpg', 1590, 5, 7950);
SET @oiA1 = (SELECT id FROM order_item WHERE order_id = @oA AND product_id = 16);
SET @oiA2 = (SELECT id FROM order_item WHERE order_id = @oA AND product_id = 18);
INSERT INTO payment (order_no, transaction_id, channel, amount, status, paid_at) VALUES
('FMDEMO0101', 'DEMO-TX-0101', 'MOCK', 11480, 1, DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 10 HOUR);

-- ---------- 订单 B：待发货 + 售后审核中（张小果，今天） ----------
INSERT INTO `order` (order_no, user_id, status, total_amount, pay_amount, freight, address_snapshot, delivery_type, remark, paid_at, created_at)
VALUES ('FMDEMO0102', @u1, 60, 12900, 12900, 0,
        '{"receiver":"张小果","phone":"13900001001","province":"广东省","city":"深圳市","district":"南山区","detail":"科技园南路88号A栋1201"}',
        1, '有坏果，申请退款', DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR));
SET @oB = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@oB, 11, 11, '麒麟西瓜', '约4kg/个', '/images/products/fruit-watermelon.jpg', 2990, 1, 2990),
(@oB, 17, 17, '金煌芒果', '500g/份', '/images/products/fruit-mango.jpg', 1390, 1, 1390);
INSERT INTO payment (order_no, transaction_id, channel, amount, status, paid_at) VALUES
('FMDEMO0102', 'DEMO-TX-0102', 'MOCK', 12900, 1, DATE_SUB(NOW(), INTERVAL 4 HOUR));
INSERT INTO refund (refund_no, order_no, user_id, type, reason, amount, status, prev_status, audit_remark, created_at) VALUES
('RFDEMO0001', 'FMDEMO0102', @u1, 1, '西瓜切开是生的，附照片凭证，申请坏果包赔', 12900, 0, 20, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- ---------- 订单 C：待收货（李橙子，2 天前发货） ----------
INSERT INTO `order` (order_no, user_id, status, total_amount, pay_amount, freight, address_snapshot, delivery_type, tracking_no, remark, paid_at, shipped_at, created_at)
VALUES ('FMDEMO0103', @u2, 30, 29900, 26910, 0,
        '{"receiver":"李橙子","phone":"13900001002","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文三路200号华星时代广场"}',
        1, 'SF0001111002', '', DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 11 HOUR,
        DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 9 HOUR, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 10 HOUR);
SET @oC = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@oC, 11, 11, '麒麟西瓜', '约4kg/个', '/images/products/fruit-watermelon.jpg', 2990, 6, 17940);
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@oC, 20, 20, '都乐菠萝', '约1kg/个', '/images/products/fruit-pineapple.jpg', 1290, 1, 1290);
INSERT INTO payment (order_no, transaction_id, channel, amount, status, paid_at) VALUES
('FMDEMO0103', 'DEMO-TX-0103', 'MOCK', 26910, 1, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 11 HOUR);

-- ---------- 订单 D：待支付（李橙子，1 小时前） ----------
INSERT INTO `order` (order_no, user_id, status, total_amount, pay_amount, freight, address_snapshot, delivery_type, created_at)
VALUES ('FMDEMO0104', @u2, 10, 7980, 7980, 0,
        '{"receiver":"李橙子","phone":"13900001002","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文三路200号华星时代广场"}',
        1, DATE_SUB(NOW(), INTERVAL 1 HOUR));
SET @oD = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@oD, 13, 13, '海南香蕉', '500g/份', '/images/products/fruit-banana.jpg', 590, 2, 1180),
(@oD, 15, 15, '巨峰葡萄', '500g/份', '/images/products/fruit-grape.jpg', 1280, 1, 1280),
(@oD, 12, 12, '红富士苹果', '500g/份', '/images/products/fruit-apple.jpg', 880, 3, 2640);

-- ---------- 订单 E：已取消（王莓莓，3 天前） ----------
INSERT INTO `order` (order_no, user_id, status, total_amount, pay_amount, freight, address_snapshot, delivery_type, created_at)
VALUES ('FMDEMO0105', @u3, 50, 9900, 0, 0,
        '{"receiver":"王莓莓","phone":"13900001003","province":"上海市","city":"上海市","district":"浦东新区","detail":"张江高科技园区博云路2号"}',
        1, DATE_SUB(NOW(), INTERVAL 3 DAY));
SET @oE = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@oE, 14, 14, '赣南脐橙', '500g/份', '/images/products/fruit-orange.jpg', 990, 1, 990);

-- ---------- 订单 F：已完成 + 匿名评价（王莓莓，5 天前） ----------
INSERT INTO `order` (order_no, user_id, status, total_amount, pay_amount, freight, address_snapshot, delivery_type, tracking_no, paid_at, shipped_at, completed_at, created_at)
VALUES ('FMDEMO0106', @u3, 40, 25880, 25880, 0,
        '{"receiver":"王莓莓","phone":"13900001003","province":"上海市","city":"上海市","district":"浦东新区","detail":"张江高科技园区博云路2号"}',
        1, 'SF0001111003', DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 14 HOUR,
        DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 18 HOUR, DATE_SUB(NOW(), INTERVAL 3 DAY),
        DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 13 HOUR);
SET @oF = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, sku_id, product_name, sku_spec, image, price, quantity, subtotal) VALUES
(@oF, 19, 19, '红心火龙果', '约450g/个', '/images/products/fruit-dragonfruit.jpg', 790, 2, 1580),
(@oF, 12, 12, '红富士苹果', '500g/份', '/images/products/fruit-apple.jpg', 880, 3, 2640);
INSERT INTO payment (order_no, transaction_id, channel, amount, status, paid_at) VALUES
('FMDEMO0106', 'DEMO-TX-0106', 'MOCK', 25880, 1, DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 14 HOUR);

-- ---------- 评价（5 星带商家回复 / 4 星匿名 / 3 星） ----------
INSERT INTO review (order_id, order_no, order_item_id, user_id, product_id, rating, content, is_anonymous, status, admin_reply, created_at) VALUES
(@oA, 'FMDEMO0101', @oiA1, @u1, 16, 5, '草莓个头大、奶油味很浓，冰袋都没化，五星！', 0, 1, '感谢认可，坏的随时拍照找客服，秒赔～', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(@oA, 'FMDEMO0101', @oiA2, @u1, 18, 4, '蓝莓果粉厚，就是有两颗压软了，整体不错。', 0, 1, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(@oF, 'FMDEMO0106', (SELECT id FROM order_item WHERE order_id = @oF AND product_id = 19), @u3, 19, 5, '红心很甜，花青素满满，会回购。', 1, 1, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ---------- 轮播图（首页） ----------
INSERT INTO banner (title, image, link_type, link_value, position, sort, status) VALUES
('当季草莓 新品尝鲜', '/images/products/fruit-strawberry.jpg', 1, '16', 'home', 1, 1),
('进口车厘子整箱直降', '/images/products/fruit-grape.jpg', 1, '15', 'home', 2, 1),
('热带风味周 芒果榴莲', '/images/products/fruit-mango.jpg', 0, NULL, 'home', 3, 1);

-- 汇总
SELECT CONCAT('会员: ', (SELECT COUNT(*) FROM `user`), ' | 订单: ', (SELECT COUNT(*) FROM `order`),
       ' | 支付流水: ', (SELECT COUNT(*) FROM payment), ' | 评价: ', (SELECT COUNT(*) FROM review),
       ' | 售后: ', (SELECT COUNT(*) FROM refund), ' | 券模板: ', (SELECT COUNT(*) FROM coupon),
       ' | 用户券: ', (SELECT COUNT(*) FROM user_coupon), ' | 轮播: ', (SELECT COUNT(*) FROM banner)) AS demo_summary;
