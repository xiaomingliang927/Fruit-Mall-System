-- =====================================================================
-- V12__banner_seed.sql — 首页轮播默认数据
-- 后台「营销中心 → 轮播图」可增删改；此处仅保证首屏开箱有内容，
-- 图片沿用本地静态图（/images/products/*），生产替换为运营banner图。
-- =====================================================================

INSERT INTO banner (title, image, link_type, link_value, position, sort, status) VALUES
('当季麒麟西瓜 · 产地直发', '/images/products/fruit-watermelon.jpg', 1, '11', 'home', 10, 1),
('烟台红富士 · 脆甜多汁',   '/images/products/fruit-apple.jpg',      1, '12', 'home', 20, 1),
('坏果包赔 · 拍照极速退款', '/images/products/fruit-banner.jpg',     2, '/pages/index/index', 'home', 30, 1);
