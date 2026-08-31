-- 部署前执行一次（只需执行；业务表由后端 Flyway 自动创建）
CREATE DATABASE IF NOT EXISTS fruit_mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
