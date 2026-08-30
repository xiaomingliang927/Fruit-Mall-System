-- =====================================================================
-- V11__user_coupon_created_at.sql — 修复 user_coupon 缺失的 created_at
-- （V8 建表时遗漏；实体字段有 created_at 自动填充）
-- =====================================================================

ALTER TABLE user_coupon ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
