#!/usr/bin/env bash
#
# 一键把「最短 MVP 演示」的真实数据快照拉起来（供队友 clone 后直接测试）。
#
# 与 setup-gitea.sh + seed-platform.sh 的区别：
#   这里不是「重新生成」数据，而是直接还原本仓库随附的字节级快照——
#     · seed/gitea-data.tar.gz  Gitea 整卷（spike-admin / hello-world-mvp / Issue#1 / 同一个 token）
#     · seed/mysql-seed.sql      平台库（guild/advent/admin 三用户 + 仓库 + Issue#1，0 quests）
#     · seed/demo.env            与快照内 token 一致的环境变量
#   所以每位队友拿到的是完全相同、token 对得上的 MVP 起点，无需任何运行时生成。
#
# 用法：  bash scripts/bootstrap.sh
#        然后另起两个终端： (A) cd backend && bash run-dev.sh   (B) cd frontend && npm install && npm run dev
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
cd "$ROOT"

PROJECT="${COMPOSE_PROJECT_NAME:-gitguild}"
GITEA_VOL="${PROJECT}_gitea-data"

# 1. 写 .env（不覆盖已有的）
if [ -f .env ]; then
  echo "[bootstrap] 已存在 .env，保留不动（如需重置请手动 cp seed/demo.env .env）"
else
  cp seed/demo.env .env
  echo "[bootstrap] 已从 seed/demo.env 写入 .env"
fi

# 2. 把 Gitea 快照灌进卷（必须在 gitea 容器首次启动前完成）
if docker volume inspect "$GITEA_VOL" >/dev/null 2>&1 \
   && docker run --rm -v "$GITEA_VOL":/data alpine sh -c 'test -f /data/gitea/gitea.db' 2>/dev/null; then
  echo "[bootstrap] Gitea 卷 $GITEA_VOL 已有数据，跳过解包"
  echo "            （要从头还原快照：docker compose down -v 后重跑本脚本）"
else
  echo "[bootstrap] 创建并填充 Gitea 卷 $GITEA_VOL ..."
  docker volume create "$GITEA_VOL" >/dev/null
  docker run --rm -v "$GITEA_VOL":/data -v "$ROOT/seed":/seed:ro alpine \
    sh -c 'tar xzf /seed/gitea-data.tar.gz -C /data && chown -R 1000:1000 /data'
  echo "  已还原 Gitea 快照（spike-admin / hello-world-mvp / Issue#1）"
fi

# 3. 起基础设施（MySQL 空卷会自动执行 seed/mysql-seed.sql；Gitea 读已填充的卷）
echo "[bootstrap] docker compose up -d ..."
docker compose up -d

# 4. 等健康
echo "[bootstrap] 等待 Gitea 健康 ..."
for _ in $(seq 1 60); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/api/healthz 2>/dev/null || true)" = "200" ] && break
  sleep 2
done

echo ""
echo "[bootstrap] 完成 ✓"
echo "  Gitea : http://localhost:3000  （仓库 spike-admin/hello-world-mvp，Issue#1 open）"
echo "  账号  : guild / advent / admin  （密码均 admin123）"
echo ""
echo "  下一步："
echo "    终端 A:  cd backend  && bash run-dev.sh"
echo "    终端 B:  cd frontend && npm install && npm run dev   # 打开 http://localhost:5173"
