# P4-032 CI/CD 配置与运行记录

## 1. 课程要求对照

课程 P4-4.4 要求基础 CI/CD 流程在代码提交后自动完成质量检查，并至少包含：

| 要求 | 当前配置 |
| --- | --- |
| 自动安装依赖 | GitLab `frontend_build` 使用 `npm ci`；后端 Maven Wrapper 自动解析依赖 |
| 静态检查或格式检查 | `quality` 执行 `node scripts/ci-static-check.mjs`，检查冲突标记、行尾空白和乱码替换字符 |
| 自动运行单元测试 | `backend_verify` 执行 `./mvnw -B verify`，包含 Surefire 单元测试 |
| 自动运行集成测试 | `backend_verify` 同一条 `verify` 命令执行 Spring Boot 集成测试 |
| 自动构建项目 | 前端执行 `npm run build`，后端 `verify` 进入 package 阶段生成 jar |
| 验证可打包/部署 | `package_check` 检查 `frontend/dist` 和后端 jar 是否存在 |
| 保留运行结果 | GitLab artifacts 保留前端产物、后端 jar、测试报告和 JaCoCo 报告 7 天 |

## 2. 配置文件

- GitLab CI：`.gitlab-ci.yml`
- GitHub Actions 兼容配置：`.github/workflows/ci.yml`
- 静态检查脚本：`scripts/ci-static-check.mjs`

课程项目最终提交在 GitLab，因此本次补充 `.gitlab-ci.yml` 作为主要流水线配置；原 GitHub Actions 文件保留，用于 GitHub 镜像仓库或个人仓库继续自动验证。

## 3. GitLab CI 流水线设计

| Stage | Job | 作用 |
| --- | --- | --- |
| `quality` | `quality` | 使用 Node 22 执行静态/格式检查 |
| `build` | `frontend_build` | 安装前端依赖并构建 Vite 产物 |
| `verify` | `backend_verify` | 使用 JDK 17 执行 Maven `verify`，覆盖单元测试、集成测试、JaCoCo 和 jar 打包 |
| `package` | `package_check` | 检查前端 dist 与后端 jar 均已生成，作为部署前置验证 |

## 4. 本地验证记录

本次更新后在本地按 CI 关键命令复验：

```powershell
cd D:\aaaWW\SoftwareEngineII\project\Git-Guild
node scripts/ci-static-check.mjs

cd D:\aaaWW\SoftwareEngineII\project\Git-Guild\frontend
npm ci
npm run build

cd D:\aaaWW\SoftwareEngineII\project\Git-Guild\backend
.\mvnw.cmd clean verify
```

记录日期：2026-06-05。

实际结果：

- 静态检查输出 `Static/format checks passed.`
- 前端依赖安装结果：`found 0 vulnerabilities`
- 前端构建通过，生成 `frontend/dist/`
- 后端 `clean verify` 通过，生成 `backend/target/backend-0.0.1-SNAPSHOT.jar`
- 后端测试统计：`Tests run: 106, Failures: 0, Errors: 0, Skipped: 7`
- Maven 输出：`BUILD SUCCESS`

## 5. GitHub 与 GitLab CI/CD 差异说明

GitHub Actions 和 GitLab CI/CD 的核心目标一致，都是提交代码后自动执行检查、测试、构建和产物保存；差异主要在配置文件位置、语法和运行平台：

| 项目 | GitHub Actions | GitLab CI/CD |
| --- | --- | --- |
| 配置文件 | `.github/workflows/*.yml` | `.gitlab-ci.yml` |
| 任务组织 | `jobs` + `steps` | `stages` + `jobs` |
| 触发条件 | `on: push / pull_request` | `workflow: rules` 或 job `rules` |
| 执行环境 | `runs-on` + `actions/setup-*` | 每个 job 通常指定 Docker `image` |
| 产物 | `actions/upload-artifact` | job 内置 `artifacts` |
| 测试报告 | 需要额外 action 或上传文件 | 支持 `artifacts:reports:junit` |

由于课程项目提交在 GitLab，只保留 GitHub Actions 不足以证明 GitLab 仓库会自动运行 CI。本次补充 `.gitlab-ci.yml` 后，GitLab 提交即可直接触发课程要求的 CI/CD 流程。
