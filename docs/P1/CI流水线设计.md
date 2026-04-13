# ADR 002: 持续集成流水线设计（前后端分离构建）

## 状态
已接受（Accepted）

## 背景
初始 CI 配置（直接复制开源模板）失败：
- 错误：`package-lock.json` 在根目录未找到
- 原因：项目结构为前后端分离（`frontend/` + `backend/`），非单一 Node.js 项目
- 影响：阻塞团队代码合并，P0 交付物验收受阻

## 决策
采用 **分离式构建策略**，GitHub Actions 工作流分三个并行 Job：

### 1. Frontend Job
- **触发**：`push` 到任意分支
- **环境**：Ubuntu + Node.js 20
- **工作目录**：`frontend/`（关键配置 `working-directory`）
- **缓存**：`cache-dependency-path: frontend/package-lock.json`
- **步骤**：`npm ci` → `npm run lint`（可选）→ `npm run build`

### 2. Backend Job
- **触发**：`push` 到任意分支
- **环境**：Ubuntu + JDK 17 (Eclipse Temurin)
- **工作目录**：`backend/`
- **缓存**：Maven 依赖（`~/.m2`）
- **步骤**：`mvn clean` → `mvn package` → `mvn test`

### 3. Security Job
- **触发**：`pull_request` 到 main 分支
- **步骤**：`npm audit --audit-level=high`（仅检查前端高危漏洞）
