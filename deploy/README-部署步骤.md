# 水果商城 · 宝塔部署指南（116.62.60.53）

> 架构：Nginx 静态站（5173 网站 / 5174 管理后台）→ 反向代理 → Spring Boot jar（8080）→ MySQL 8
> 前端走相对路径 `/api`，由 Nginx 代理到 8080，无跨域、无硬编码 IP，以后加域名不用改代码。

```
浏览器 ──► :5173 (web dist)      ┐
浏览器 ──► :5174 (admin dist)    ┼── Nginx ──► /api,/uploads,/images ──► 127.0.0.1:8080 (jar)
                                 ┘                                    │
                                                                   MySQL 8 (3306)
```

---

## 一、服务器环境准备（宝塔软件商店安装）

1. **MySQL 8.0**（宝塔 → 软件商店 → MySQL 8.0，安装后记住 root 密码）
2. **Nginx**（宝塔默认自带）
3. **JDK 17**（宝塔 → 软件商店 → Java 项目管理器 / 或 yum install java-17-openjdk）

## 二、初始化数据库

宝塔 → 数据库 → phpMyAdmin，执行（或命令行）：

```sql
CREATE DATABASE fruit_mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 业务表不用建！后端首次启动 Flyway 自动执行 V1~V11 全部迁移
```

## 三、上传与摆放文件（部署包 deploy/ 已 scp 到服务器）

```bash
# 假设部署包已解压到 /www/fruitmall
mkdir -p /www/fruitmall && cd /www/fruitmall
# 把 backend jar 放好、前端 dist 解压
mv fruit-mall-backend-0.1.0-SNAPSHOT.jar /www/fruitmall/backend/app.jar
mkdir -p /www/fruitmall/web  && unzip -q web-dist.zip  -d /www/fruitmall/web
mkdir -p /www/fruitmall/admin && unzip -q admin-dist.zip -d /www/fruitmall/admin
chmod +x /www/fruitmall/backend/start.sh
```

## 四、启动后端（8080）

```bash
# 先改 start.sh 里的数据库密码！
vim /www/fruitmall/backend/start.sh
bash /www/fruitmall/backend/start.sh        # 前台试跑，看到 Started 即成功
# 首次启动自动创建管理员 admin/admin123，并执行全部 Flyway 迁移
```

验证：`curl http://127.0.0.1:8080/api/v1/products?size=1`

常驻运行二选一：
- **宝塔 Java 项目管理器**：添加项目 → jar 模式 → 选 `/www/fruitmall/backend/app.jar`，JDK17，端口 8080
- 或 systemd：`cp fruit-backend.service /etc/systemd/system/ && systemctl daemon-reload && systemctl enable --now fruit-backend`

## 五、宝塔建两个静态站点（Nginx）

宝塔 → 网站 → 添加站点（PHP 版本选“纯静态”）：

| 域名填 | 根目录 |
|--------|--------|
| `116.62.60.53:5173` | `/www/fruitmall/web` |
| `116.62.60.53:5174` | `/www/fruitmall/admin` |

> 宝塔若不允许纯 IP:端口建站：站点名随便填（如 `web.local`），然后在配置文件里手动改 listen 端口。

每个站点的【配置文件】里，把 `server{}` 内替换为 `nginx/` 目录下对应 conf 的内容（核心是 3 个代理 location）：

```nginx
location /api/     { proxy_pass http://127.0.0.1:8080; proxy_set_header Host $host; }
location /uploads/ { proxy_pass http://127.0.0.1:8080; }
location /images/  { proxy_pass http://127.0.0.1:8080; }
```

另加一条 SPA 路由回退（history 模式刷新 404 时需要）：

```nginx
location / { try_files $uri $uri/ /index.html; }
```

## 六、放行端口

宝塔 → 安全：放行 `5173`、`5174`、`8080`（8080 也可不放行、仅内网代理，更安全）
阿里云控制台 → 安全组：同样放行 `5173`、`5174`（8080 可不放）。

## 七、验收

- 网站：http://116.62.60.53:5173 （登录 13800001111 / 123456）
- 管理后台：http://116.62.60.53:5174 （admin / admin123，**登录后立即改密**）

## 八、安全清单（上线前必做）

- [ ] 改掉 MySQL root/业务密码，`start.sh` 中同步
- [ ] 改后台 admin 密码
- [ ] `FRUIT_JWT_SECRET` 换成随机 64 位字符串
- [ ] 8080 端口不开公网（仅 Nginx 代理）
- [ ] MySQL 3306 不开公网
