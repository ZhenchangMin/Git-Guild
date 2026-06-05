#!/usr/bin/env bash
#
# 引导本地 Gitea，使其进入「最短 MVP 演示」的起点（幂等，可反复运行）：
#   1. 等待 Gitea 健康
#   2. 确保管理员 spike-admin 存在
#   3. 复用 .env 里有效的 GITEA_TOKEN；没有则生成一个并写回 .env
#   4. 重建干净的 demo 仓库 spike-admin/hello-world-mvp（README + 一个 open Issue）
#
# 需在「启动后端之前」运行（后端要读 .env 里的 GITEA_TOKEN 才能代理 Gitea 写操作）。
# 用法：  bash scripts/setup-gitea.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
ENV_FILE="$ROOT/.env"
[ -f "$ENV_FILE" ] || { echo "缺少 $ENV_FILE —— 请先 cp .env.example .env"; exit 1; }
set -a; . "$ENV_FILE"; set +a

GITEA_BASE_URL="${GITEA_BASE_URL:-http://localhost:3000}"
GITEA_CONTAINER="${GITEA_CONTAINER:-gitguild-gitea}"
ADMIN_USER="${GITEA_ADMIN_USERNAME:-spike-admin}"
ADMIN_PASSWORD="${GITEA_ADMIN_PASSWORD:-Gitguild-demo-2026}"
ADMIN_EMAIL="${GITEA_ADMIN_EMAIL:-spike@gitguild.local}"
REPO="hello-world-mvp"

echo "[setup-gitea] 等待 Gitea 健康 ($GITEA_BASE_URL) ..."
for _ in $(seq 1 60); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' "$GITEA_BASE_URL/api/v1/healthz" 2>/dev/null || true)" = "200" ] && break
  sleep 2
done

echo "[setup-gitea] 确保管理员 $ADMIN_USER ..."
docker exec -u git "$GITEA_CONTAINER" gitea admin user create \
  --admin --username "$ADMIN_USER" --email "$ADMIN_EMAIL" \
  --password "$ADMIN_PASSWORD" --must-change-password=false >/dev/null 2>&1 \
  && echo "  已创建" || echo "  已存在（跳过）"

token_valid() {
  [ -n "${1:-}" ] || return 1
  [ "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: token $1" "$GITEA_BASE_URL/api/v1/user" 2>/dev/null || true)" = "200" ]
}

if token_valid "${GITEA_TOKEN:-}"; then
  echo "[setup-gitea] 复用 .env 中已有的有效 GITEA_TOKEN"
else
  echo "[setup-gitea] 生成新的 admin token ..."
  NEW_TOKEN=$(curl -s -u "$ADMIN_USER:$ADMIN_PASSWORD" \
    -X POST "$GITEA_BASE_URL/api/v1/users/$ADMIN_USER/tokens" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"gitguild-demo-$(date +%s)\",\"scopes\":[\"all\"]}" \
    | python3 -c "import sys,json;print(json.load(sys.stdin).get('sha1',''))" 2>/dev/null || true)
  [ -n "$NEW_TOKEN" ] || { echo "  生成 token 失败：请确认 $ADMIN_USER 的密码为 $ADMIN_PASSWORD（或设 GITEA_ADMIN_PASSWORD）"; exit 1; }
  GITEA_TOKEN="$NEW_TOKEN"
  if grep -q '^GITEA_TOKEN=' "$ENV_FILE"; then
    python3 - "$ENV_FILE" "$GITEA_TOKEN" <<'PY'
import sys, re
path, tok = sys.argv[1], sys.argv[2]
s = open(path, encoding='utf-8').read()
s = re.sub(r'(?m)^GITEA_TOKEN=.*$', 'GITEA_TOKEN=' + tok, s)
open(path, 'w', encoding='utf-8').write(s)
PY
  else
    printf '\nGITEA_TOKEN=%s\n' "$GITEA_TOKEN" >> "$ENV_FILE"
  fi
  echo "  已写入 .env（GITEA_TOKEN 前缀 ${GITEA_TOKEN:0:8}…）"
fi

auth=(-H "Authorization: token $GITEA_TOKEN")
repo_api="$GITEA_BASE_URL/api/v1/repos/$ADMIN_USER/$REPO"

if [ "$(curl -s -o /dev/null -w '%{http_code}' "${auth[@]}" "$repo_api" 2>/dev/null || true)" = "200" ]; then
  echo "[setup-gitea] 删除旧的 $ADMIN_USER/$REPO（重建干净起点）"
  curl -s -o /dev/null -X DELETE "${auth[@]}" "$repo_api"
fi

echo "[setup-gitea] 创建 $ADMIN_USER/$REPO（auto_init，含 README）"
curl -s -o /dev/null -X POST "${auth[@]}" -H "Content-Type: application/json" \
  "$GITEA_BASE_URL/api/v1/user/repos" \
  -d "{\"name\":\"$REPO\",\"description\":\"Git-Guild 最短 MVP 演示仓库\",\"private\":false,\"auto_init\":true,\"default_branch\":\"main\"}"

echo "[setup-gitea] 创建 Issue #1"
curl -s -o /dev/null -X POST "${auth[@]}" -H "Content-Type: application/json" \
  "$repo_api/issues" \
  -d '{"title":"Add hello world file","body":"在仓库新增一个 hello world 文件，作为最短 MVP 演示任务。"}'

echo "[setup-gitea] 完成 ✓  仓库 $ADMIN_USER/$REPO 已就绪（README + Issue#1 open）。"
echo "[setup-gitea] 下一步：cd backend && bash run-dev.sh   然后另开终端 bash scripts/seed-platform.sh"
