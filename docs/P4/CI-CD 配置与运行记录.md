# CI/CD 配置与运行记录

## 1. 配置文件

- GitLab CI：`.gitlab-ci.yml`
- GitHub Actions 兼容配置：`.github/workflows/ci.yml`
- 静态检查脚本：`scripts/ci-static-check.mjs`

课程项目最终提交在 GitLab，因此本次补充 `.gitlab-ci.yml` 作为主要流水线配置；原 GitHub Actions 文件保留，用于 GitHub 镜像仓库或个人仓库继续自动验证。

## 2. GitLab CI 流水线设计

| Stage | Job | 作用 |
| --- | --- | --- |
| `quality` | `quality` | 使用 Node 22 执行静态/格式检查 |
| `build` | `frontend_build` | 安装前端依赖并构建 Vite 产物 |
| `verify` | `backend_verify` | 使用 JDK 17 执行 Maven `verify`，覆盖单元测试、集成测试、JaCoCo 和 jar 打包 |
| `package` | `package_check` | 检查前端 dist 与后端 jar 均已生成，作为部署前置验证 |

## 3. 本地验证记录

### 3.1 最近一次运行记录

本次在准备演示版本前，按 CI 关键命令重新复验：

```powershell
cd D:\aaaWW\SoftwareEngineII\project\Git-Guild
node scripts/ci-static-check.mjs

cd D:\aaaWW\SoftwareEngineII\project\Git-Guild\frontend
npm ci
npm run build

cd D:\aaaWW\SoftwareEngineII\project\Git-Guild\backend
.\mvnw.cmd -B verify
```

记录日期：2026-06-07。

本地环境：

| 项目 | 版本或环境 |
| --- | --- |
| 操作系统 | Windows 11 |
| Shell | Windows PowerShell |
| Node.js | `v22.15.1` |
| npm | `10.9.2` |
| JDK | Eclipse Temurin `17.0.19` |
| Maven Wrapper | Apache Maven `3.6.3` |

实际结果：

| CI 阶段 | 本地命令 | 运行结果 | 产物或关键输出 |
| --- | --- | --- | --- |
| 静态检查 | `node scripts/ci-static-check.mjs` | 通过 | `Static/format checks passed.` |
| 前端依赖安装 | `npm ci` | 通过 | `added 38 packages`，`found 0 vulnerabilities` |
| 前端构建 | `npm run build` | 通过 | Vite `built in 853ms`，生成 `frontend/dist/index.html` 和 `frontend/dist/assets/` |
| 后端测试与打包 | `.\mvnw.cmd -B verify` | 通过 | `Tests run: 140, Failures: 0, Errors: 0, Skipped: 7`，`BUILD SUCCESS` |
| 后端产物检查 | 文件检查 | 通过 | 生成 `backend/target/backend-0.0.1-SNAPSHOT.jar` |
| 测试报告检查 | 文件检查 | 通过 | 生成 `backend/target/surefire-reports/TEST-*.xml` 共 28 个 |
| 覆盖率报告检查 | 文件检查 | 通过 | 生成 `backend/target/site/jacoco/index.html` |

### 3.2 Docker 演示环境检查记录

为了配合演示版本，本次还对基础服务进行了干净恢复和状态检查：

```powershell
cd D:\aaaWW\SoftwareEngineII\project\Git-Guild
docker compose down -v
docker compose up -d
docker compose ps
```

运行结果：

| 服务 | 状态 | 端口 |
| --- | --- | --- |
| `gitguild-mysql` | `healthy` | `localhost:3307 -> 3306` |
| `gitguild-redis` | `healthy` | `localhost:6379 -> 6379` |
| `gitguild-gitea` | `healthy` | `localhost:3000 -> 3000`，`localhost:2222 -> 22` |

干净快照恢复结果：

| 数据项 | 结果 |
| --- | --- |
| Git Guild 用户 | `admin`、`guild`、`advent` |
| 预置仓库 | `hello-world-mvp` |
| 预置 Issue | `#1 Add hello world file` |
| 初始任务数 | `quests=0` |
| Gitea 管理员 | `spike-admin` |
