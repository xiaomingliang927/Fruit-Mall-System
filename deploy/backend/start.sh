#!/bin/bash
# 水果商城后端启动脚本（宝塔/systemd 通用）
# 用前必改：FRUIT_DB_PASSWORD、FRUIT_JWT_SECRET
export FRUIT_DB_HOST=127.0.0.1
export FRUIT_DB_PORT=3306
export FRUIT_DB_NAME=fruit_mall
export FRUIT_DB_USER=root
export FRUIT_DB_PASSWORD=CHANGE_ME_MYSQL_PASSWORD
export FRUIT_JWT_SECRET=CHANGE_ME_random_64char_secret_0123456789abcdef
export FRUIT_WX_APPID=wxa0baa0a3ac856f2d
export FRUIT_WX_SECRET=
export FRUIT_UPLOAD_DIR=/www/fruitmall/uploads
export TZ=Asia/Shanghai

mkdir -p "$FRUIT_UPLOAD_DIR"
exec java -jar -Xms256m -Xmx512m /www/fruitmall/backend/app.jar --spring.profiles.active=prod
