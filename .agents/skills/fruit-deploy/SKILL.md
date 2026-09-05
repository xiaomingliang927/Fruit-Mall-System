---
name: fruit-deploy
description: 部署/发布水果商城到宝塔服务器 116.62.60.53（网站 5173、管理后台 5174、后端 8080）。只要用户提到"部署/发版/更新到服务器"、"上线"、"发布到 116.62.60.53 / 宝塔"，或要求重启线上后端、更新线上前端页面，就使用本技能——即使没有明说"部署"两个字。
---

# 水果商城自动部署

将本项目（后端 Spring Boot jar + 用户网站 web + 管理后台 admin-web）构建并发布到阿里云服务器
`root@116.62.60.53`（宝塔面板）。小程序（miniapp/miniapp-hbx）**不在本技能范围**——那是微信开发者工具流程。

## 快速执行

优先使用打包好的脚本（在仓库根目录的 Git Bash 中运行）：

```bash
bash .agents/skills/fruit-deploy/scripts/deploy.sh                  # 全量：后端+两端前端
bash .agents/skills/fruit-deploy/scripts/deploy.sh --frontend-only  # 只发 web + admin 静态页
bash .agents/skills/fruit-deploy/scripts/deploy.sh --backend-only   # 只发后端 jar 并重启
```

脚本自动完成：本地构建 → scp 上传 → 服务器端解压/重启 → 健康检查。若脚本失败，按下方
「手工流程」逐步执行，并先读「血泪坑」一节确认没踩已知雷。

## 服务器档案（勿凭记忆改，以此为准）

| 项 | 值 |
|----|-----|
| SSH | `root@116.62.60.53`（已配免密钥；阿里云 ECS，宝塔面板） |
| 后端 | `/www/fruitmall/backend/app.jar`，systemd 单元 `fruit-backend`（start.sh 注入环境变量） |
| 前端 | `/www/fruitmall/web`（5173）、`/www/fruitmall/admin`（5174），Nginx 纯静态 |
| Nginx 站点配置 | `/www/server/panel/vhost/nginx/fruit-web-5173.conf`、`fruit-admin-5174.conf` |
| Nginx 代理规则 | `/api/`、`/uploads/`、`/images/` → `127.0.0.1:8080`；`location /` SPA 回退 index.html |
| MySQL | 宝塔版 5.7（`/www/server/mysql`），库名 `fruit`，用户 `fruit`（密码在宝塔/服务器 `.db_password`） |
| 上传目录 | `/www/fruitmall/uploads` |
| 密钥文件 | `/www/fruitmall/backend/.db_password`（DB root 密码），start.sh 内有 JWT 密钥 |

前端代码全部用相对路径（`/api`、`/images`、`/uploads`），由 Nginx 代理到 8080——
**永远不要**在前端代码里硬编码 `http://116.62.60.53:8080` 之类的绝对 API 地址。

## 手工流程（脚本失败时逐条排查）

1. **本地构建**：
   - 后端：`cd backend && ../tools/apache-maven-3.9.9/bin/mvn -s settings-aliyun.xml -DskipTests package`
     （产物 `target/fruit-mall-backend-0.1.0-SNAPSHOT.jar`，约 58MB）
   - 前端：`cd web && npm run build`；`cd admin-web && npm run build`
2. **上传**：`scp -r deploy/backend deploy/web-dist deploy/admin-dist root@116.62.60.53:/www/fruitmall/`
3. **服务器端**：
   ```bash
   ssh root@116.62.60.53
   cp /www/fruitmall/backend/app.jar 备份后替换；cp -r dist 内容到 web/ admin/
   systemctl restart fruit-backend && sleep 15 && systemctl is-active fruit-backend
   ```
4. **验证**（缺一不可）：
   - 服务器内：`curl 127.0.0.1:8080/api/v1/products?size=1` → 200
   - 服务器内：`curl 127.0.0.1:5174/api/admin/auth/login -X POST -d '...'` → 返回 token
   - 本机（公网）：`curl http://116.62.60.53:5173/` 与 `:5174/` → 200

## 数据库迁移

后端启动时 Flyway 自动执行 `backend/src/main/resources/db/migration/` 下未应用的迁移。
**发新版只需把新迁移文件放进源码再构建 jar，服务器无需手工跑 SQL。**

## 血泪坑（每一条都真实踩过，必读）

1. **Flyway 迁移文件发布后一个字都不能改**（包括注释、品牌名替换）。
   改了 → 启动报 `Migration checksum mismatch` 直接起不来。
   修复：启动失败日志里会打印 `Resolved locally: <数字>`，把该值
   `UPDATE flyway_schema_history SET checksum=<数字> WHERE version='<版本>';` 即可（等于官方 repair）。
   顺带：全仓品牌替换/格式化必须排除 `db/migration/` 目录。
2. **本地起后端前先杀 8080 僵尸进程**：Windows 下 `mvn spring-boot:run` 停止时 java 进程常残留，
   导致新实例端口冲突失败、旧代码继续服务（表现为"明明改了代码线上却没变化"）。
   ```bash
   for pid in $(netstat -ano | grep ":8080.*LISTENING" | awk '{print $NF}' | sort -u); do taskkill //F //PID $pid; done
   ```
3. **CORS 白名单**：后端按 `FRUIT_CORS_ORIGINS` 环境变量放行来源（逗号分隔），默认只含
   localhost 三端 + 线上 IP 两站点。浏览器请求自动带 `Origin` 头，不在白名单 → 403
   `Invalid CORS request`；curl 不带 Origin 所以测不出来。新增前端来源（如新域名）时：
   改服务器 `/www/fruitmall/backend/start.sh` 里的 `FRUIT_CORS_ORIGINS` 并重启。
4. **Windows Git Bash 的 curl 中文乱码**：`-d '{"name":"草莓"}'` 会被按 GBK 编码发送，
   后端报 `Invalid UTF-8 start byte`。正确姿势：先用 node 写 UTF-8 JSON 文件再
   `--data-binary @file.json`；或把中文放在 URL 参数里。
5. **服务器日志文件是二进制混合编码**：grep 服务器日志加 `-a`，否则 "binary file matches" 无输出。
6. **「网络异常/请求失败(502)」的快速定位**：
   - 页面能开、API 全 502/504 → 后端挂了：`ssh root@116.62.60.53 'systemctl status fruit-backend; tail -30 /www/fruitmall/backend/app.log'`
   - 403 `Invalid CORS request` → 见坑 3
   - 401 → token 过期（7 天），重新登录
7. **外网不通先分两层查**：服务器内 `curl 127.0.0.1:<端口>` 通而外网超时 → 阿里云**安全组**
   或宝塔防火墙没放行（安全组只能用户在阿里云控制台加，SSH 做不了）。
8. **宝塔 MySQL root 密码与面板记录可能不同步**：以服务器
   `/www/fruitmall/backend/.db_password` 和用户自己改的为准；应用库账号是
   `fruit`（宝塔建），后端连 `fruit` 库。
9. **"改了没生效"先查浏览器缓存**：Nginx 若不给 index.html 发 `Cache-Control`，
   浏览器会启发式缓存旧入口页，新构建上线的功能（如移动端抽屉遮罩）用户端
   永远看不到，且桌面 curl 测不出来。已在 `deploy/nginx/*.conf` 加：HTML
   `no-cache`、`/assets/`（带哈希文件名）`max-age=31536000, immutable`。
   改 Nginx 配置后记得 `nginx -t && nginx -s reload`，并让用户下拉刷新一次。

## 部署后变更记录习惯

每次部署在 commit message 里注明发了什么（例：`deploy: 发布评价系统到线上`），
并把 `deploy/admin-dist`、`deploy/web-dist` 的更新一并提交，保持仓库与线上一致。
