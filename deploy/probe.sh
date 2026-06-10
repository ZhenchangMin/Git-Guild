#!/usr/bin/env bash
# GitGuild 服务器环境探测脚本（部署第 1 步）
# 在目标服务器上执行：bash deploy/probe.sh
# 目的：摸清 docker / 端口占用 / nginx / 资源，决定端口映射与复用策略。
set -u

line() { printf '\n========== %s ==========\n' "$1"; }

line "OS / 内核"
( . /etc/os-release 2>/dev/null && echo "$PRETTY_NAME" ) || uname -a
uname -m

line "Docker"
if command -v docker >/dev/null 2>&1; then
  docker --version
  docker compose version 2>/dev/null || docker-compose --version 2>/dev/null || echo "!! 未检测到 docker compose 插件"
  echo "-- 正在运行的容器 --"
  docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}' 2>/dev/null
else
  echo "!! 未安装 docker。需先安装 docker + compose 插件。"
fi

line "目标端口占用情况 (80 / 3000 / 2222 / 8080 / 3306 / 6379)"
for p in 80 3000 2222 8080 3306 6379; do
  if command -v ss >/dev/null 2>&1; then
    hit=$(ss -tlnp 2>/dev/null | grep -E "[:.]$p\b")
  else
    hit=$(netstat -tlnp 2>/dev/null | grep -E "[:.]$p\b")
  fi
  if [ -n "$hit" ]; then
    echo "端口 $p ：已占用 → $hit"
  else
    echo "端口 $p ：空闲"
  fi
done

line "nginx（是否已有反代在跑）"
if command -v nginx >/dev/null 2>&1; then
  nginx -v 2>&1
  systemctl is-active nginx 2>/dev/null || echo "（非 systemd 或未托管）"
else
  echo "未安装宿主机 nginx（本方案自带前端 nginx 容器，可无需宿主 nginx）"
fi

line "磁盘 / 内存"
df -h / 2>/dev/null | tail -n +1
free -h 2>/dev/null || echo "(free 不可用)"

line "出网连通性（构建需拉 maven / npm / docker 镜像）"
for url in https://repo1.maven.org https://registry.npmjs.org https://github.com; do
  if command -v curl >/dev/null 2>&1; then
    code=$(curl -m 8 -s -o /dev/null -w '%{http_code}' "$url" 2>/dev/null)
    echo "$url → HTTP $code"
  fi
done

line "结论提示"
cat <<'EOF'
- 端口若被占用：改 deploy/.env.prod 里对应的 *_PORT，避开冲突即可。
- 无 docker：先装 docker 与 compose 插件再继续。
- 出网为空/超时：构建会很慢或失败 → 配 npm/maven 国内镜像，或设 GITEA_PROXY_URL。
- 把本段输出贴回给我，我据此定最终端口与步骤。
EOF
