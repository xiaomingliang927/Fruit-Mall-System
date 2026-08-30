# 水果商城（网站 + 微信小程序 + 管理后台）

> 三端一体的生鲜水果电商系统：三端共用一套后端 API，数据实时同步。
> 设计文档：[docs/水果商城系统设计文档.md](docs/水果商城系统设计文档.md)
> 系统架构图：[架构图/fruit-mall-architecture.html](架构图/fruit-mall-architecture.html)（浏览器打开查看，Ctrl+P 可导出 PDF）

## 当前进度（M1 基础交易闭环 · 已完成并通过联调验收）

- [x] 系统设计文档 + 可视化架构图
- [x] 后端：登录 / 商品 / 购物车 / 订单 / 支付(mock) / **管理端接口（登录·商品·订单·统计）**
- [x] **用户端网站**（Vue3 + Vite）：首页 / 搜索 / 详情选规格 / 购物车 / 结算下单 / 订单支付·取消·确认收货
- [x] **管理后台**（Vue3 + Element Plus + ECharts）：数据看板（销售趋势/分类占比/状态分布图表）· 商品管理（增改·上下架·SKU 库存）· 订单管理（发货·取消·详情）
- [x] 数据库脚本 + 7 款水果种子数据 + 矢量插画商品图
- [x] **微信小程序**（uni-app + Vue3）：首页/详情/购物车/结算/订单/微信登录，已通过 mp-weixin 构建
- [x] **第一阶段工程地基**：Git 分支模型与规范提交 · 配置外部化（密钥走环境变量）· Flyway 版本化迁移 · 核心链路集成测试（6 个全绿）· Docker 化 · GitHub Actions CI
- [ ] 真实微信支付 + 订阅消息 + 手机号绑定 + 超时自动关单（Redis 延迟队列）
- [ ] M2：优惠券、评价、售后退款（坏果包赔）、物流跟踪
- [ ] 第二阶段（企业级）：微信支付真实接入与对账 · Redis 缓存/延迟队列 · Spring Security + RBAC · 日志监控告警 · 压测

## 目录结构

```
fruit/
├── backend/                     # 后端（Spring Boot 3，模块化单体，端口 8080）
│   ├── pom.xml
│   ├── Dockerfile               # 后端镜像（多阶段构建）
│   ├── settings-aliyun.xml      # 国内 Maven 镜像（构建时 -s 指定）
│   └── src/main/
│       ├── java/com/fruitmall/
│       │   ├── common/          # 统一响应 / 异常 / 错误码 / 分页
│       │   ├── auth/            # C 端 JWT（拦截器 / 用户上下文）
│       │   ├── admin/           # 管理端：登录 / 商品 / 订单 / 统计（独立 JWT，role=admin）
│       │   ├── config/          # MyBatis-Plus / 双拦截器注册
│       │   └── modules/
│       │       ├── auth/ user/ product/ cart/ order/ pay/   # C 端业务模块
│       ├── resources/
│       │   ├── application.yml  # 配置（密钥走环境变量）
│       │   ├── application-prod.yml  # 生产 profile（无默认值，fail fast）
│       │   ├── db/migration/    # Flyway 版本化迁移（V1 建表 / V2 种子）
│       │   └── static/images/products/   # 7 张矢量插画商品图（生产换 OSS+CDN）
│       └── src/test/            # 核心链路集成测试（H2 + Flyway）
├── docker-compose.yml           # MySQL + 后端一键编排
├── .github/workflows/ci.yml     # CI：后端测试 + 三端构建
├── docs/
│   ├── 水果商城系统设计文档.md
│   ├── 工程规范.md              # 分支模型 / 提交规范 / 质量门禁
│   └── sql/demo-data.sql        # 近 7 日演示订单（仪表板图表展示用，可选）
├── web/                         # 用户端网站（Vue3 + Vite，端口 5173）
│   └── src/{views,components}   # 首页/登录/详情/购物车/结算/订单 + 导航
├── admin-web/                   # 管理后台（Vue3 + Element Plus + ECharts，端口 5174）
│   └── src/{views,layout}       # 登录/看板(趋势·占比·状态图表)/商品管理/订单管理
├── miniapp/                     # 微信小程序（uni-app + Vue3）
│   └── src/pages/{index,detail,cart,checkout,orders,login}
├── 架构图/fruit-mall-architecture.html
└── tools/apache-maven-3.9.9/    # 项目自带 Maven（系统未装 mvn 时使用；不入库）
├── 架构图/fruit-mall-architecture.html
└── tools/apache-maven-3.9.9/    # 项目自带 Maven（系统未装 mvn 时使用）
```

## 技术栈

| 层 | 选型 |
|----|------|
| 后端 | Java 17 + Spring Boot 3.4 + MyBatis-Plus 3.5 + MySQL 8，JWT 鉴权（C 端与管理端 token 隔离） |
| 用户网站 | Vue 3 + Vite + vue-router（生产 SEO 场景可平滑迁移 Nuxt） |
| 管理后台 | Vue 3 + Element Plus + vue-router |
| 数据 | MySQL 8（utf8mb4），金额一律按「分」存储 |

## 快速开始

### 1. 初始化数据库

无需手动建库建表——后端启动时 **Flyway 自动执行迁移**（`backend/src/main/resources/db/migration/`）：
- 全新数据库：自动建库（JDBC `createDatabaseIfNotExist`）+ 建表（V1）+ 种子数据（V2）；
- 已有表结构的老库：自动打基线（`baseline-version=2`）跳过历史脚本，数据不受影响；
- 后续表结构变更一律新增迁移脚本 `V3__xxx.sql`，禁止修改已发布的迁移。

可选：导入近 7 日演示订单，让仪表板图表有数据可看：

```bash
mysql -u root -p --default-character-set=utf8mb4 < docs/sql/demo-data.sql
```

### 2. 配置并启动后端（端口 8080）

敏感配置全部走环境变量（本地开发有默认值，生产用 `--spring.profiles.active=prod`，缺失即启动失败）：

| 环境变量 | 说明 | 默认（仅开发） |
|---------|------|--------------|
| `FRUIT_DB_HOST/PORT/NAME/USER/PASSWORD` | 数据库连接 | localhost/3306/fruit_mall/root/123456 |
| `FRUIT_JWT_SECRET` | JWT 签名密钥（≥32 字符） | dev 占位值 |
| `FRUIT_WX_APPID/FRUIT_WX_SECRET` | 微信小程序 | 空（开发模式登录） |

```bash
cd backend
../tools/apache-maven-3.9.9/bin/mvn -s settings-aliyun.xml spring-boot:run
```

首次启动自动创建默认管理员 **admin / admin123**（SHA-256 加盐存储）。

### 2.1 运行测试（核心链路集成测试）

```bash
cd backend
../tools/apache-maven-3.9.9/bin/mvn -s settings-aliyun.xml test
```

H2 内存库 + Flyway 自动建库，无需本地 MySQL，覆盖：下单锁库存、支付回调幂等、取消回补库存、防超卖回滚、短信注册、微信登录幂等。

### 2.2 Docker 一键启动（需安装 Docker）

```bash
cp .env.example .env   # 按需修改密钥
docker compose up -d --build
```

### 3. 启动用户网站（端口 5173）

```bash
cd web
npm install   # 已配置 npmmirror 镜像
npm run dev
```

浏览器打开 http://localhost:5173 。登录：任意 11 位手机号 + 验证码 `123456`（开发模式，自动注册）。

### 4. 启动管理后台（端口 5174）

```bash
cd admin-web
npm install
npm run dev
```

浏览器打开 http://localhost:5174 ，使用 admin / admin123 登录。

### 5. 运行微信小程序（uni-app）

```bash
cd miniapp
npm install        # 已配置 npmmirror 镜像
npm run dev:mp-weixin   # 开发模式（ watch，产物在 dist/dev/mp-weixin）
```

1. 打开「微信开发者工具」→ 导入项目 → 目录选择 `miniapp/dist/dev/mp-weixin`，AppID 可用测试号；
2. 在开发者工具「详情 → 本地设置」勾选 **不校验合法域名**（后端是 http）；
3. 编译即可看到小程序。登录支持「微信一键登录」（开发模式自动派生 openid）与手机号验证码两种方式；
4. 正式构建：`npm run build:mp-weixin`（产物在 `dist/build/mp-weixin`）。

> 真机预览时把 `miniapp/src/api.js` 里的 `BASE_URL` 改成电脑局域网 IP。
> 生产上线前在 `backend/application.yml` 配置 `app.wechat.appid/secret`（配置后走真实 code2Session）。

> 开发期 `/api` 与 `/images` 由 Vite 代理到 8080，无需处理跨域。

## 演示流程（已验收 ✅）

1. 网站：浏览商品 → 选规格 → 立即购买/加购 → 登录（13800001111 / 123456）→ 选地址提交订单 → 模拟支付 → 订单转「待发货」
2. 管理后台：看板查看今日销售额/热销榜 → 商品管理新增/编辑/上下架 → 订单管理对待发货订单「发货」填运单号
3. 网站：订单页出现「确认收货」→ 确认后订单完成
4. 防超卖：下单乐观锁扣库存；取消订单（用户/管理员）自动回补库存；重复支付被状态机拦截（幂等）

## 接口约定

- 统一响应：`{ "code": 0, "message": "ok", "data": ... }`，业务失败 code 非 0
- C 端：`/api/v1/**`（JWT：`Authorization: Bearer <token>`；分类/商品浏览免登录）
- 管理端：`/api/admin/**`（独立 JWT，token 中带 `role=admin`，与 C 端不通用）

## 后续迭代

- **M1 收尾**：微信小程序端、微信支付真实接入、Redis 延迟队列超时关单
- **M2**：优惠券、评价、售后退款（坏果包赔）、订阅消息、物流跟踪
- **M3**：秒杀、拼团、同城配送、RBAC 完整权限、数据报表

详见设计文档第 9 节《开发迭代计划》。
