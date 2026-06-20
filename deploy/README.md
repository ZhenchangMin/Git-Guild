# GitGuild 服务器部署 Runbook（IP:端口直连，全容器化）

把整套应用部署到你已有的服务器（和 poembridge 同一台），通过 `http://服务器IP:端口` 访问。
全部容器化，一条命令拉起 MySQL + Redis + Gitea + 后端 + 前端，互不污染现有服务。

> 所有命令在**服务器**上、**仓库根目录**执行（除非特别说明）。
> 前置：服务器已装 `docker` + `docker compose` 插件、`git`，且能出网拉镜像/依赖。

---

## 第 0 步 · 前置确认

```bash
docker --version
docker compose version   # 没有则装 compose 插件
```

## 第 1 步 · 探测服务器现状（决定端口）

```bash
# 先把仓库弄到服务器（见第 2 步）再跑；或单独把 probe.sh 拷上去跑
bash deploy/probe.sh
```

看输出里 **80 / 3000 / 2222 / 8080** 是否被 poembridge 等占用：
- 空闲 → 用默认端口。
- 被占 → 记下来，第 3 步在 `.env.prod` 改对应 `*_PORT`。

> 把 probe.sh 的输出贴回来，我帮你定最终端口。

## 第 2 步 · 拉取代码

```bash
git clone <你的仓库地址> gitguild
cd gitguild
git checkout P4          # 当前要部署的分支
```

## 第 3 步 · 配置生产变量

```bash
cp deploy/.env.prod.example deploy/.env.prod
vi deploy/.env.prod
```

必填/必改：
- `SERVER_IP` → 服务器实际 IP（**不能是 localhost**，否则 Gitea clone 链接错误）。
- `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` → 强密码。
- `JWT_SECRET` → 强随机串，生成：`openssl rand -base64 48`。
- `HTTP_PORT` / `GITEA_PORT` / `GITEA_SSH_PORT` / `BACKEND_PORT` → 按第 1 步探测结果避开占用。

## 第 4 步 · 构建并启动

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml up -d --build
```

首次会构建后端（Maven）和前端（Vite），视网络几分钟到十几分钟。查看进度：

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml ps
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml logs -f backend
```

等到 `gitguild-mysql-prod`/`gitguild-gitea-prod` 为 healthy、backend 日志出现 `Started ... in N seconds`。

## 第 5 步 · 灌入管理员账号

后端用 `ddl-auto=update` 启动后会自动建好所有表。表建好后注入演示管理员：

```bash
docker exec -i gitguild-mysql-prod \
  mysql -ugitguild -p"$(grep '^MYSQL_PASSWORD=' deploy/.env.prod | cut -d= -f2)" gitguild \
  < docs/P4/database/seed-local-admin.sql
```

登录账号：`admin@gitguild.local` / `admin123`（ADMIN）。
> 生产建议登录后改密码或改用你自己的种子。

## 第 6 步 · 验证

```bash
# 后端健康（容器内）
docker exec gitguild-backend-prod sh -c 'wget -qO- http://localhost:8080/swagger-ui.html >/dev/null && echo backend-ok'
```

浏览器打开 `http://<SERVER_IP>:<HTTP_PORT>` → 用上面的管理员登录。
- Gitea Web： `http://<SERVER_IP>:<GITEA_PORT>`（spike-admin）。
- Swagger（可选）： `http://<SERVER_IP>:<BACKEND_PORT>/swagger-ui.html`。

确认服务器**防火墙/安全组**放行了 `HTTP_PORT` 和 `GITEA_PORT`（云服务器常见拦点）。

---

## Gitea token 校验 / 重签（仓库导入失败时）

若“关联/导入仓库”报 token 失效，说明 `.env.prod` 的 `GITEA_TOKEN` 与快照不符，重签一个：

```bash
docker exec -u git gitguild-gitea-prod \
  gitea admin user generate-access-token --username spike-admin \
  --token-name gitguild-prod --scopes all
# 把输出的 40 位 token 写回 deploy/.env.prod 的 GITEA_TOKEN，然后：
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml up -d backend
```

> 迁移外部公网大仓库是同步请求，可能阻塞数分钟（已放宽 nginx 超时到 600s）。
> 服务器需代理才能访问 github 时，在 `.env.prod` 设 `GITEA_PROXY_URL`。

## 日常运维

```bash
# 更新代码后重建
git pull && docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml up -d --build

# 查看日志
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml logs -f <service>

# 停止（保留数据卷）
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml down

# 彻底清空（含数据库！谨慎）
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml down -v
```

## 数据持久化

- `mysql-data` / `redis-data` / `gitea-data` → docker 命名卷。
- `./uploads` → 宿主机目录绑定挂载（用户头像等）。
- 备份示例：`docker exec gitguild-mysql-prod mysqldump -uroot -p<root_pw> gitguild > backup.sql`

## 常见问题

| 现象 | 排查 |
| --- | --- |
| backend 起不来，日志 `Communications link failure` | mysql 没 healthy；等待或看 mysql 日志 |
| 端口冲突 `address already in use` | 改 `.env.prod` 的 `*_PORT`，`up -d` 重建 |
| 页面打开空白/404 接口 | 看 frontend 容器是否起、nginx 反代是否指向 backend；确认 backend healthy |
| 外网打不开但本机 curl 通 | 云服务器安全组/防火墙没放行对应端口 |
| 仓库导入失败 token 错误 | 见上「Gitea token 校验/重签」 |
| 构建拉依赖极慢 | 配 npm/maven 国内镜像（见两个 Dockerfile 注释），或设代理 |
