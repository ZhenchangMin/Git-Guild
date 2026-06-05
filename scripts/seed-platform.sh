#!/usr/bin/env bash
#
# 把平台数据库播种到「最短 MVP 演示」的起点（幂等，可反复运行；同时充当「重置」工具）：
#   1. 清空所有业务数据（quests / 接取 / 提交 / 审核 / PR / issue / 仓库 / 成长 / 通知）
#   2. upsert 三个测试用户：guild(MAINTAINER) / advent(BEGINNER) / admin(ADMIN)，密码均 admin123
#   3. 以 guild 登录，导入 Gitea 的 hello-world-mvp 并同步其 Issue
#
# 需在「后端已启动」之后运行（schema 由后端首启创建；导入走真实后端 API）。
# 用法：  bash scripts/seed-platform.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
[ -f "$ROOT/.env" ] && { set -a; . "$ROOT/.env"; set +a; }

API="${BACKEND_BASE_URL:-http://localhost:8080}/api/v1"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-gitguild-mysql}"
DB_USER="${MYSQL_USER:-gitguild}"
DB_PASS="${MYSQL_PASSWORD:-gitguild123}"
DB_NAME="${MYSQL_DATABASE:-gitguild}"
GITEA_BASE_URL="${GITEA_BASE_URL:-http://localhost:3000}"
ADMIN_USER="${GITEA_ADMIN_USERNAME:-spike-admin}"
REPO="hello-world-mvp"

db() { docker exec -i "$MYSQL_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" 2>/dev/null; }
getval() { python3 -c "
import sys, json
try: d = json.load(sys.stdin)
except Exception: print(''); sys.exit()
for k in '$1'.split('.'):
    d = d.get(k) if isinstance(d, dict) else None
    if d is None: break
print(d if d is not None else '')"; }

echo "[seed] 等待后端 ($API) ..."
for _ in $(seq 1 60); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' "$API/quests?page=1&size=1" 2>/dev/null || true)" = "200" ] && break
  sleep 2
done

echo "[seed] 清空业务数据 ..."
db <<'SQL'
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE review_records;
TRUNCATE TABLE review_items;
TRUNCATE TABLE submissions;
TRUNCATE TABLE quest_assignments;
TRUNCATE TABLE admin_review_records;
TRUNCATE TABLE quests;
TRUNCATE TABLE issues;
TRUNCATE TABLE pull_requests;
TRUNCATE TABLE repositories;
TRUNCATE TABLE contribution_records;
TRUNCATE TABLE xp_transactions;
TRUNCATE TABLE growth_profiles;
TRUNCATE TABLE notifications;
SET FOREIGN_KEY_CHECKS=1;
SQL

echo "[seed] upsert 测试用户（guild / advent / admin，密码均 admin123）..."
# 引号 heredoc，避免 bcrypt 哈希里的 $ 被 shell 展开
db <<'SQL'
INSERT INTO users (username, email, password_hash, role, status, token_version, created_at, updated_at) VALUES
  ('guild',  'guild@gg.local',  '$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC', 'MAINTAINER', 'ACTIVE', 0, NOW(6), NOW(6)),
  ('advent', 'advent@gg.local', '$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC', 'BEGINNER',   'ACTIVE', 0, NOW(6), NOW(6)),
  ('admin',  'admin@gg.local',  '$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC', 'ADMIN',      'ACTIVE', 0, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), role=VALUES(role), status=VALUES(status), updated_at=NOW(6);
SQL

echo "[seed] guild 登录并导入仓库 ..."
TOKEN=$(curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d '{"account":"guild","password":"admin123"}' | getval data.accessToken)
[ -n "$TOKEN" ] || { echo "  guild 登录失败"; exit 1; }

RID=$(curl -s -X POST "$API/repositories/import" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"sourceUrl\":\"$GITEA_BASE_URL/$ADMIN_USER/$REPO\",\"name\":\"$REPO\",\"hostType\":\"GITEA\"}" | getval data.repositoryId)
[ -n "$RID" ] || { echo "  导入仓库失败（确认 Gitea 已 setup 且后端带有效 GITEA_TOKEN）"; exit 1; }

curl -s -X POST "$API/repositories/$RID/sync" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{}' >/dev/null

echo "[seed] 核对："
curl -s "$API/quests?page=1&size=1" | python3 -c "import sys,json;print('  quests totalItems =', json.load(sys.stdin)['data'].get('totalItems'))" 2>/dev/null || true
curl -s "$API/repositories/$RID/issues" -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];print('  repo', $RID, 'issues =', [(i['issueId'], i['title'], i['status']) for i in d['items']])" 2>/dev/null || true

echo "[seed] 完成 ✓  账号：guild / advent / admin（密码均 admin123）。打开前端开始走最短 MVP 路径。"
