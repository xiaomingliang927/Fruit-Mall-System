#!/usr/bin/env bash
# =============================================================
# 水果商城一键部署脚本（宝塔服务器 root@116.62.60.53）
# 用法：
#   bash .agents/skills/fruit-deploy/scripts/deploy.sh                 # 全量（后端+前端）
#   bash .agents/skills/fruit-deploy/scripts/deploy.sh --frontend-only # 仅前端静态页
#   bash .agents/skills/fruit-deploy/scripts/deploy.sh --backend-only  # 仅后端 jar + 重启
# 依赖：Git Bash（Windows）或 Linux/macOS；ssh/scp 免密已配置
# =============================================================
set -euo pipefail

SERVER="root@116.62.60.53"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../" && pwd)"   # 仓库根目录（scripts→技能→skills→.agents→根）
# 某些终端环境 HOME 乱码导致读不到 known_hosts，accept-new 保证仍可连接
# 部分终端 HOME 会被 GBK 破坏导致 ssh 找不到 ~/.ssh 密钥——显式指定密钥路径免疫此问题
USER_HOME="$(dirname "$(dirname "$ROOT")")"           # → C:/Users/<用户名>
SSH_KEY="$USER_HOME/.ssh/id_ed25519"
SSH_KNOWN="$USER_HOME/.ssh/known_hosts"
SSH_OPTS="-i $SSH_KEY -o UserKnownHostsFile=$SSH_KNOWN -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15"
REMOTE_DIR="/www/fruitmall"

# 兼容部分终端 HOME 变量被 GBK 破坏的情况（否则 ssh 找不到 ~/.ssh 密钥，报 Permission denied）
export HOME="$USER_HOME"

MODE="all"
[ "${1:-}" = "--frontend-only" ] && MODE="fe"
[ "${1:-}" = "--backend-only"  ] && MODE="be"

echo "════════════════════════════════════════"
echo " 水果商城部署 → $SERVER  (模式: $MODE)"
echo "════════════════════════════════════════"

# ---------- 0. 预检：SSH 连通 ----------
ssh -o BatchMode=yes $SSH_OPTS "$SERVER" "echo ok" >/dev/null 2>&1 \
  || { echo "✗ SSH 连不上 $SERVER（检查网络/密钥）"; exit 1; }
echo "✓ SSH 连通"

# ---------- 1. 本地构建 ----------
if [ "$MODE" != "fe" ]; then
  echo "── 构建后端 jar ..."
  (cd "$ROOT/backend" && ../tools/apache-maven-3.9.9/bin/mvn -s settings-aliyun.xml -B -DskipTests package 2>&1 | grep -E "BUILD|ERROR" | tail -2) \
    | grep -q "BUILD SUCCESS" || { echo "✗ 后端构建失败"; exit 1; }
  mkdir -p "$ROOT/deploy/backend"
  cp "$ROOT/backend/target/fruit-mall-backend-0.1.0-SNAPSHOT.jar" "$ROOT/deploy/backend/app.jar"
  echo "✓ 后端 jar 就绪 ($(du -h "$ROOT/deploy/backend/app.jar" | cut -f1))"
fi

if [ "$MODE" != "be" ]; then
  echo "── 构建前端 dist ..."
  (cd "$ROOT/web" && npm run build --silent 2>&1 | grep -E "✓|error" | tail -1)
  (cd "$ROOT/admin-web" && npm run build --silent 2>&1 | grep -E "✓|error" | tail -1)
  rm -rf "$ROOT/deploy/web-dist" "$ROOT/deploy/admin-dist"
  mkdir -p "$ROOT/deploy/web-dist" "$ROOT/deploy/admin-dist"
  cp -r "$ROOT/web/dist/."        "$ROOT/deploy/web-dist/"
  cp -r "$ROOT/admin-web/dist/."  "$ROOT/deploy/admin-dist/"
  echo "✓ 前端 dist 就绪"
fi

# ---------- 2. 上传 ----------
echo "── scp 上传 ..."
if [ "$MODE" != "fe" ]; then
  scp -q $SSH_OPTS "$ROOT/deploy/backend/app.jar" "$SERVER:$REMOTE_DIR/backend/app.jar"
fi
if [ "$MODE" != "be" ]; then
  ssh $SSH_OPTS "$SERVER" "rm -rf $REMOTE_DIR/web-dist $REMOTE_DIR/admin-dist"
  scp -q -r $SSH_OPTS "$ROOT/deploy/web-dist"   "$SERVER:$REMOTE_DIR/web-dist"
  scp -q -r $SSH_OPTS "$ROOT/deploy/admin-dist" "$SERVER:$REMOTE_DIR/admin-dist"
fi
echo "✓ 上传完成"

# ---------- 3. 服务器端生效 ----------
echo "── 服务器端生效 ..."
ssh $SSH_OPTS "$SERVER" "set -e
  rm -rf $REMOTE_DIR/web $REMOTE_DIR/admin
  mkdir -p $REMOTE_DIR/web $REMOTE_DIR/admin
  cp -r $REMOTE_DIR/web-dist/.  $REMOTE_DIR/web/
  cp -r $REMOTE_DIR/admin-dist/. $REMOTE_DIR/admin/
  mkdir -p $REMOTE_DIR/uploads
"
if [ "$MODE" != "fe" ]; then
  ssh $SSH_OPTS "$SERVER" "systemctl restart fruit-backend"
  echo "⏳ 等待后端启动 ..."
  ok=""
  for i in $(seq 1 20); do
    sleep 3
    code=$(ssh $SSH_OPTS "$SERVER" "curl -s -o /dev/null -w '%{http_code}' --max-time 3 'http://127.0.0.1:8080/api/v1/products?size=1'" 2>/dev/null || echo 000)
    [ "$code" = "200" ] && { ok=1; break; }
  done
  [ "$ok" ] || { echo "✗ 后端启动失败，查看：ssh $SERVER 'journalctl -u fruit-backend -n 50'"; exit 1; }
  echo "✓ 后端已重启并健康 (fruit-backend)"
fi

# ---------- 4. 验证 ----------
echo "── 验证 ..."
fail=0
for check in "5173 /" "5174 /" "5173 /api/v1/products?size=1" "5174 /api/admin/auth/login"; do
  port=${check%% *}; path=${check#* }
  if [ "$path" = "/api/admin/auth/login" ]; then
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 -X POST "http://116.62.60.53:$port$path" -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}')
  else
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "http://116.62.60.53:$port$path")
  fi
  if [ "$code" = "200" ] || [ "$code" = "400" ]; then echo "  ✓ $port$path → $code"; else echo "  ✗ $port$path → $code"; fail=1; fi
done

[ "$fail" = "0" ] && echo "════════ 🎉 部署成功 http://116.62.60.53:5173 ｜ http://116.62.60.53:5174 ════════" \
                  || { echo "════════ ✗ 部署完成但存在验证失败项，按 SKILL.md『血泪坑』排查 ════════"; exit 1; }
