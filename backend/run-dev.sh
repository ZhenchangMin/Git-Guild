#!/usr/bin/env bash
# 本地开发启动后端：先从仓库根的 .env 载入环境变量（GITEA_TOKEN / MYSQL_* / JWT_SECRET 等），
# 再以 dev profile 运行。Spring Boot 不会自动读取 .env，缺了 GITEA_TOKEN 会导致所有 Gitea
# 写操作返回 401（“代码托管平台请求失败”），所以务必用本脚本启动。
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
env_file="$here/../.env"

if [ -f "$env_file" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$env_file"
  set +a
  echo "[run-dev] 已载入 $env_file（GITEA_TOKEN 前缀: ${GITEA_TOKEN:0:8}…）"
else
  echo "[run-dev] 警告：未找到 $env_file，GITEA_TOKEN 可能为空，Gitea 写操作会 401。"
fi

chmod +x "$here/mvnw" 2>/dev/null || true
exec "$here/mvnw" -q spring-boot:run
