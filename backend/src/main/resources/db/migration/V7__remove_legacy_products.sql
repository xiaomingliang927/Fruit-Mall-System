-- =====================================================================
-- V7__remove_legacy_products.sql — 下架最初的 7 款旧商品（ID 1~7）
-- 统一为「鲜果集」10 款水果（ID 11~20，真实摄影图）
-- 历史订单不受影响（order_item 为快照），购物车/收藏中的旧引用一并清理
-- =====================================================================

DELETE FROM cart_item WHERE sku_id IN (SELECT id FROM product_sku WHERE product_id <= 7);
DELETE FROM favorite WHERE product_id <= 7;
DELETE FROM product_sku WHERE product_id <= 7;
DELETE FROM product WHERE id <= 7;
