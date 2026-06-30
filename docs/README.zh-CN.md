# Git Guild

<div align="center">

Git Guild 是 Software Engineering II 课程项目。项目把本地 Gitea 代码协作流程与“冒险者公会”式委托系统结合起来：委托人把真实工程任务发布为委托，冒险家通过分支和 Pull Request 完成委托，平台记录审核、XP、成长档案、通知和排行榜。

当前项目处于 P4 集成阶段。主要流程已经接入真实后端接口，数据落在 MySQL 中，并与本地种子 Gitea 实例集成，不再只是前端 mock 。

## 技术栈


| 层级     | 技术                                            |
| ---------- | ------------------------------------------------- |
| 前端     | Vue 3、Vite、Vue Router                         |
| 后端     | Spring Boot 3、Spring Security、Spring Data JPA |
| 数据库   | 本地 MySQL 8、后端测试 H2                       |
| 代码托管 | 本地 Gitea 1.22 容器                            |
| 基础设施 | Docker Compose、Redis、GitHub Actions           |
| 测试     | JUnit 5、Mockito、Spring MVC 测试、集成测试     |

## 项目结构

```text
Git-Guild/
├── README.md                    # 英文 README
├── docs/                        # 课程交付物与设计文档
│   ├── README.zh-CN.md          # 中文 README
│   ├── P0/ … P4/                # 各阶段交付物（章程、ADR、设计、反思日志等）
│   ├── hci/                     # 人机交互 / 交互设计说明
│   └── 演示数据/                # 演示数据说明
├── .github/workflows/           # GitHub Actions CI（ci.yml）
├── .gitlab-ci.yml               # GitLab CI 镜像
├── docker-compose.yml           # 本地开发
├── docker-compose.prod.yml      # 生产环境 compose
├── .env.example                 # 可选本地环境变量覆盖
├── deploy/                      # Dockerfile、nginx.conf、probe.sh
├── scripts/                     # CI 静态检查 + Python 演示数据脚本
├── seed/                        # MySQL 种子数据 + Gitea 快照
├── backend/                     # Spring Boot 3 后端（含独立 README）
└── frontend/                    # Vue 3 + Vite 前端（含独立 README）
```

## 环境要求

- Docker Desktop 或带 Compose v2 的 Docker Engine；旧版 `docker-compose` v1 也可使用。
- JDK 17。
- 推荐 Node.js 22，与 CI 保持一致。
- npm。

## 本地快速启动

在项目根目录启动基础设施：

```bash
docker compose up -d
```

该命令会启动：


| 服务  | 地址 / 端口           | 说明                       |
| ------- | ----------------------- | ---------------------------- |
| Gitea | http://localhost:3000 | 已带种子数据的本地代码托管 |
| MySQL | localhost:3307        | 数据库`gitguild`           |
| Redis | localhost:6379        | 通知与会话辅助服务         |

启动后端：

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

启动前端：

```bash
cd frontend
npm ci
npm run dev
```

打开前端页面：

```text
http://localhost:5173
```

后端启动后可访问 API 文档：

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/api-docs
```

## 种子演示账号

所有种子账号密码均为 `admin123`。


| 角色   | 用户名   | 用途                                  |
| -------- | ---------- | --------------------------------------- |
| 管理员 | `admin`  | 审核委托上架，维护平台配置            |
| 委托人 | `guild`  | 导入仓库，发布委托，审核提交，合并 PR |
| 冒险家 | `advent` | 接取委托，推送代码，提交成果，获得 XP |

## 推荐演示路径

1. 使用 `guild` 登录。
2. 进入委托人工作台，如需要可导入或同步仓库。
3. 基于已有仓库 Issue 发布委托。
4. 使用 `admin` 登录，在管理员审核页批准委托上架。
5. 使用 `advent` 登录，在委托板接取委托。
6. 进入工作台，准备 task branch，克隆仓库，提交并推送代码。
7. 进入提交柜台并提交成果。后端会自动创建或复用 Pull Request。
8. 切回 `guild`，进入审核台审核成果，再单独执行合并 PR。
9. 查看冒险家的成长档案、通知和排行榜变化。

委托人也可以通过“完成工作台”进入同一套冒险家式流程，接取并完成委托。

## 重置本地演示数据

本地演示数据来自仓库内提交的种子快照。删除 Docker 卷后重新启动即可恢复干净状态：

```bash
docker compose down -v
docker compose up -d
```

`down -v` 会删除本地 MySQL、Redis、Gitea 数据卷；下一次 `up` 会重新载入种子数据。

## 验证命令

以下命令与 CI 的核心检查保持一致：

```bash
node scripts/ci-static-check.mjs
```

```bash
cd frontend
npm ci
npm run build
```

```bash
cd backend
./mvnw -B verify
```

Windows PowerShell 后端验证：

```powershell
cd backend
.\mvnw.cmd -B verify
```

## CI/CD

GitHub Actions 会在推送到 `main`、`P4`、`front`、`feat/**`、`feature/**`，以及向 `main` 或 `P4` 发起 Pull Request 时运行。

流水线包括：

- 通过 `scripts/ci-static-check.mjs` 执行静态与格式检查。
- 安装前端依赖并执行生产构建。
- 执行后端 Maven verify 与打包。
- 上传前端 `dist/` 构建产物。
- 上传后端 JAR 与 JaCoCo 覆盖率报告。

## 常见问题

### localhost 或代理问题

如果 Gitea、后端或 Vite 无法通过 `localhost` 连接，检查系统代理是否拦截本地流量。建议把以下地址加入代理绕过列表：

```text
localhost,127.0.0.1,::1
```

WSL 用户也可以启用 mirrored networking：

```text
[wsl2]
networkingMode=mirrored
```

然后在 PowerShell 中执行 `wsl --shutdown`，再重新打开 WSL。

### Gitea Token

后端默认配置了与种子 Gitea 快照匹配的本地演示 token。它只用于本地 demo。如果切换到自己的 Gitea 实例，请通过 `GITEA_TOKEN` 覆盖。

### 端口占用

默认端口如下：

- 前端开发服务：`5173`
- 前端预览服务：`4173`
- 后端：`8080`
- Gitea：`3000`
- MySQL：`3307`
- Redis：`6379`

如果端口被占用，请关闭冲突进程或调整本地服务端口。

## 团队


| 成员   | 职责 |
| -------- | ------ |
| 闵振昌 | 需求 |
| 阳旅帆 | 架构 |
| 王炜   | 开发 |
| 谭咏俊 | 测试 |

---

每位专家都曾是初学者。今天就开始你的委托吧。
