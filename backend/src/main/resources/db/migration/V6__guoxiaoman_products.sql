-- =====================================================================
-- V6__guoxiaoman_products.sql — 录入「鲜果集」10 款水果（图片已本地化）
-- 与既有 7 款商品并存，ID 从 11 起
-- =====================================================================

INSERT INTO product (id, category_id, name, subtitle, main_image, origin, unit, tags, status, sales) VALUES
(11, 1, '麒麟西瓜', '沙瓤多汁，当季现摘', '/images/products/fruit-watermelon.jpg', '海南', '个', '热卖', 1, 1200),
(12, 2, '红富士苹果', '脆甜多汁，个大匀称', '/images/products/fruit-apple.jpg', '山东烟台', '份', '', 1, 2300),
(13, 2, '海南香蕉', '自然熟，软糯香甜', '/images/products/fruit-banana.jpg', '海南', '份', '', 1, 3100),
(14, 1, '赣南脐橙', '皮薄多汁，手剥橙香浓郁', '/images/products/fruit-orange.jpg', '江西赣州', '份', '特价', 1, 1800),
(15, 1, '巨峰葡萄', '果粒饱满，酸甜适口', '/images/products/fruit-grape.jpg', '新疆', '份', '', 1, 960),
(16, 2, '丹东草莓', '奶油甜，99红颜品种', '/images/products/fruit-strawberry.jpg', '辽宁丹东', '盒', '新品', 1, 780),
(17, 3, '金煌芒果', '核薄肉厚，香甜细腻', '/images/products/fruit-mango.jpg', '海南', '份', '', 1, 1500),
(18, 2, '云南蓝莓', '当季，果粉厚实脆甜', '/images/products/fruit-blueberry.jpg', '云南', '盒', '', 1, 640),
(19, 3, '红心火龙果', '红心甜润，富含花青素', '/images/products/fruit-dragonfruit.jpg', '越南', '个', '', 1, 1100),
(20, 3, '都乐菠萝', '无需泡盐水，直接吃', '/images/products/fruit-pineapple.jpg', '菲律宾', '个', '', 1, 880);

INSERT INTO product_sku (product_id, spec, price, stock, warn_stock, status) VALUES
(11, '约4kg/个', 2990, 500, 50, 1),
(12, '500g/份',   880, 500, 50, 1),
(13, '500g/份',   590, 500, 50, 1),
(14, '500g/份',   990, 400, 40, 1),
(15, '500g/份',  1280, 300, 30, 1),
(16, '250g/盒',  1990, 260, 26, 1),
(17, '500g/份',  1390, 320, 32, 1),
(18, '125g/盒',  1590, 280, 28, 1),
(19, '约450g/个', 790, 350, 35, 1),
(20, '约1kg/个', 1290, 300, 30, 1);
