-- =====================================================================
-- V4__product_photo_jpg.sql — 商品主图从 SVG 插画切换为真实照片
-- 照片文件见 backend/src/main/resources/static/images/products/*.jpg
-- 版权：Unsplash / Pexels 免费许可（仅演示用途）；
-- 商用上线请替换为自有拍摄或已购商业图库照片
-- =====================================================================

UPDATE product SET main_image = REPLACE(main_image, '.svg', '.jpg') WHERE main_image LIKE '%.svg';
