# P4-Adapter Gitea 接入优化设计

> 覆盖 GitHub Issues #9–#17，在 `feature/P4-Adapter` 分支上按依赖链顺序实现。
> 每阶段实现后回填对应的"实现状态"小节。

---

## 1. 背景：模型变更

### 1.1 CONTEXT.md 变更

| 维度 | 旧模型（P4-016） | 新模型（2026-06-02） |
| --- | --- | --- |
| 操作模式 | 用户自操作：Adventurer 用自己的 Gitea 账号做 commit/PR | 平台代理受限写操作：Git-Guild 工作台代理创建分支、上传文件、创建 PR |
| PR 归属 | `code_host_account_bindings` 校验"PR 属于该 Adventurer" | PR 主链路不再要求 Adventurer 跳出 Git-Guild 手动创建 PR |
| GiteaAdapter 范围 | 只读 | 读 + 平台写操作（创建仓库、创建分支、上传文件、创建 PR） |

### 1.2 对已交付代码的影响

| 已写模块 | 在新模型下 |
| --- | --- |
| `GiteaAdapter`（只读） | 加写操作：`createRepository`、`createBranch`、`uploadFile`、`createPullRequest` |
| `CodePullRequestSyncService`（从 Gitea 拉 PR upsert） | #14 创建 PR 时直接写本地表，不再需要"同步"语义 |
| `submission-draft`（懒同步返回候选 PR） | 变为返回系统代理创建的 PR，不再列 Gitea 全量 |
| `CodeHostAccountBinding`（校验 PR 归属） | 降级：PR 是系统代理创建的，不需校验"这个 PR 是不是你的" |
| P4-022 成长 / P4-025 推荐 | **不受影响**——下游消费，上游 PR 来源变化不关它们的事 |

---

## 2. 架构决策（ADRs）

### 决策 1 — 仓库 Gitea 侧 owner = admin 用户（MVP）

Gitea 侧仓库建在 admin 用户下（`POST /api/v1/user/repos`），`CodeRepository.owner` 指向
Git-Guild 的 Guild Master。区分"代码托管所有权"和"业务所有权"。

- **选定（甲）**：admin 用户下建仓库。零额外依赖，Guild Master 不需 Gitea 账号。
- **后续升级（乙）**：仓库建到 Guild Master 的 Gitea 账号下，需 binding 前置。仅需改
  `GiteaAdapter.createRepo` 的目标用户参数，其余不变。

### 决策 2 — 平台凭据 = 系统 admin token（不变）

沿用现有 `GiteaProperties`（`base-url` + `token`），不把 access token 存入业务库（P2 ADR-002）。
写操作使用同一套 token。

### 决策 3 — CodeRepository 补 description 列

对齐 Gitea API 的 `POST /api/v1/user/repos` 入参。`@Column(length=512)` 可空。

---

## 3. 任务依赖链与完成状态

```
#9 仓库接入 ─► #11 绑定仓库+Issue ─► #12 创建 task branch ─► #13 提交文件 commit
    ─► #14 创建 PR + 回填 ◄─────────────────────────────────────┘
    ─► #15 Submission Draft + API 接入 ─► #16 Maintainer 审核闭环 ─► #17 E2E 演示
#10 Quest 状态修正 ──────────────────────► feeds into #16
```

| Issue | 标题 | 状态 | 实现状态 |
| --- | --- | --- | --- |
| #9 | 本地 Gitea 仓库接入 MVP | `ready-for-agent` | 🔨 实现中 |
| #10 | 修正 Submission Quest 状态流转 | `ready-for-agent` | 已实现（2026-06-03） |
| #11 | Quest 绑定本地仓库与 Issue | `ready-for-agent` | 已实现（2026-06-03） |
| #12 | 接取 Quest 后创建 task branch | `ready-for-agent` | 已实现（2026-06-03） |
| #13 | 工作台提交文件并生成 commit | `ready-for-human` | ⏳ |
| #14 | 工作台创建 PR 并回填 | `ready-for-agent` | ⏳ |
| #15 | Submission Draft + API 接入 | `ready-for-agent` | ⏳ |
| #16 | Maintainer 审核闭环 | `ready-for-agent` | ⏳ |
| #17 | E2E 演示与回归 | `ready-for-agent` | ⏳ |

---

## 4. 各 Issue 设计详情

---

### #9 — 本地 Gitea 仓库接入 MVP

**端点：**

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/v1/repositories` | Guild Master 创建新仓库（Gitea 侧建 + 本地存） |
| `GET` | `/api/v1/repositories` | 列出当前用户有权限的仓库（owner 或 Collaborator） |

**POST 流程：**
1. 校验调用者角色为 `MAINTAINER` 或 `ADMIN`
2. `POST /api/v1/user/repos` 在 admin 用户下创建仓库
3. 解析 Gitea 响应得到 id、full_name、html_url、default_branch
4. 存 `CodeRepository`（owner=Git-Guild Guild Master, hostType=GITEA, 拼接 sourceUrl）
5. 返回 `repositoryId`

**改动：**
- `GiteaProperties`：补 `adminUsername`（拼接 sourceUrl 用）
- `GiteaAdapter(+Impl)`：新增 `RepositoryInfo createRepository(name, description)`
- `CodeRepository`：补 `description` 列（`@Column(length=512)` 可空）+ setter
- `RepositoryController`（新）：`POST /repositories` + `GET /repositories`
- `RepositoryService(+Impl)`（新）：协调 Gitea API + 本地持久化

**实现状态：** 🔨 实现中

---

### #10 — Quest 状态流转修正

**问题：** `SubmissionServiceImpl.createSubmission()` 在创建 Submission 后调用 `quest.markInReview()`，将 Quest 从 `IN_PROGRESS` 变为 `IN_REVIEW`。这与 CONTEXT.md 冲突——`IN_REVIEW` 是保留状态，不作为默认主流程节点。

**修正内容：**

| # | 文件 | 改动 |
|---|------|------|
| 1 | `SubmissionServiceImpl.java:76` | 守卫条件移除 `IN_REVIEW`，只允许 `IN_PROGRESS` 时创建 Submission |
| 2 | `SubmissionServiceImpl.java:94-95` | 删除 `quest.markInReview()` + `questRepository.save(quest)` |
| 3 | `QuestServiceImpl.java:57` | `ACTIVE_ISSUE_QUEST_STATUSES` 移除 `IN_REVIEW` |
| 4 | `Quest.java:171-173` | `markInReview()` 加 Javadoc 标注"保留状态" |
| 5 | 3 个测试文件 | `IN_REVIEW` 断言改为 `IN_PROGRESS` |
| 6 | `ReviewServiceImplTest.java` | 新增回归测试：接取 → 提交 → 验证 IN_PROGRESS → 审核通过 → 验证 COMPLETED |

**修正后状态流转：**

- 创建 Submission：Quest 保持 `IN_PROGRESS`（不变）
- Guild Master APPROVED：Quest `IN_PROGRESS` → `COMPLETED`
- Guild Master CHANGES_REQUESTED / REJECTED：Quest 保持 `IN_PROGRESS`（不变）
- `IN_REVIEW` 枚举保留但不被任何主流程代码路径触发

---

### #11 — Quest 绑定仓库与 Issue

**目标：** Guild Master 发布 Quest 时，将 Quest 绑定到本地 Gitea 仓库，并创建/同步对应的 Gitea Issue。

**两种绑定方式：**

| 方式 | 入参 | 行为 |
|------|------|------|
| 创建新 Issue | `giteaIssueTitle` (+ 可选 `giteaIssueBody`) | 后端在 Gitea 上创建 Issue → 同步到本地 → 绑定 |
| 关联已有 Issue | `issueId` | 前端选定已有本地 Issue → 直接绑定（已有路径，不变） |

**改动：**

| # | 文件 | 改动 |
|---|------|------|
| 1 | `IssueInfo.java` | 补 `body` 字段 |
| 2 | `GiteaAdapter.java` / `GiteaAdapterImpl.java` | 新增 `createIssue(owner, repo, title, body)` |
| 3 | `CodeIssue.java` | 补 `setTitle/setStatus/setExternalUrl/setBody/setSyncedAt` 等 setter/getter |
| 4 | `CodeIssueRepository.java` | 新增 `findByRepositoryRepositoryIdAndExternalIssueId` |
| 5 | `CodeIssueService.java` + `CodeIssueServiceImpl.java`（新） | 从 sourceUrl 解析 owner/repo → 调用 Gitea → 幂等 upsert |
| 6 | `CreateQuestRequest.java` | `issueId` 改为可选；新增 `giteaIssueTitle` / `giteaIssueBody` |
| 7 | `QuestResponses.java` | `RepositoryBrief` 补 `defaultBranch`；`IssueBrief` 补 `externalUrl`；`CreateQuestResponse` 补 `externalIssueId/externalIssueUrl/defaultBranch` |
| 8 | `QuestServiceImpl.java` | 新增 `CodeIssueService` 依赖；`createQuest` 按入参选择创建或关联路径 |
| 9 | 2 个测试文件 | 适配新 record 字段和新增依赖 |

---

### #12 — 接取后创建 task branch

**目标：** 冒险家接取 Quest 后，由 Git-Guild 在本地 Gitea 基于 Quest 仓库默认分支创建 task branch，作为后续工作台 commit（#13）与创建 PR（#14）的基础。

**分支命名：** `task/quest-{questId}-assignment-{assignmentId}-{username}`，含 Quest、Assignment、Adventurer 三要素（username 经 git refname 合法化）。

**幂等策略：**
- Assignment 已记录 `taskBranch` → 直接返回，不再调 Gitea；
- Gitea 侧分支已存在（409 → `CODE_HOST_RESOURCE_CONFLICT`）→ 视为成功，回填确定性分支名。

**失败处理：** 接取时尽力创建（best-effort，与通知模式一致）。Gitea 不可达不阻塞接取，`taskBranch` 留空；提供显式重试端点 `POST /api/v1/quests/{questId}/task-branch`（幂等）。

**改动：**

| # | 文件 | 改动 |
|---|------|------|
| 1 | `GiteaRepoCoordinates.java`（新） | 抽取 sourceUrl→owner/repo 解析为共享工具，消除 3 处重复 |
| 2 | `CodeIssueServiceImpl` / `CodePullRequestSyncServiceImpl` | 改用共享 `GiteaRepoCoordinates.parse`，删除各自私有副本 |
| 3 | `GiteaAdapter` / `GiteaAdapterImpl` | 新增 `createBranch(owner, repo, newBranch, oldBranch)`；`execute` 补 409→`CODE_HOST_RESOURCE_CONFLICT`、`IllegalArgumentException`（baseUrl 未配置）→`CODE_HOST_UNAVAILABLE` |
| 4 | `QuestAssignment.java` | 补 `task_branch` 列 + getter/setter |
| 5 | `QuestTaskBranchService` / `Impl`（新） | 幂等 ensureTaskBranch（不加 @Transactional，运行在调用方事务内） |
| 6 | `QuestResponses.AssignmentResponse` | 补 `taskBranch` 字段 |
| 7 | `QuestService` / `QuestServiceImpl` | acceptQuest 尽力创建 task branch；新增 `ensureTaskBranch(questId, userId)` 重试方法 |
| 8 | `QuestController.java` | 新增 `POST /{questId}/task-branch` 端点 |
| 9 | 测试 | 新增 `QuestTaskBranchServiceImplTest`（幂等/409/失败）；更新 Quest 服务与控制器测试 |

---

### #13 — 工作台提交文件 commit

*(待设计)*

---

### #14 — 工作台创建 PR

*(待设计)*

---

### #15 — Submission Draft API 接入

*(待设计)*

---

### #16 — Maintainer 审核闭环

*(待设计)*

---

### #17 — E2E 演示

*(待设计)*
