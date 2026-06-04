# Git Guild P4 Bug 修复日志

> 最后更新：2026-06-04

## 1. 文档目的

本文档用于记录 P4 编码、联调、测试和课堂演示准备阶段发现并修复的关键 Bug。每条记录必须能支持后续复盘、AI 调试对决、最终回归。

P4-035 的交付分两步：

- 模板建立：在第一批并行任务中完成，保证后续 Bug 有统一记录格式。
- 持续维护：从 P4-029 第一轮端到端联调开始，所有阻塞核心链路或影响演示的 Bug 都必须登记。

## 2. 记录范围

必须记录：

- 影响核心演示链路的 Bug：登录、仓库导入、Issue 同步、任务发布、管理员上架审核、任务接取、提交成果、审核、成长反馈。
- 影响 Gitea 集成的 Bug：仓库读取、分支创建、上传 commit、创建 PR、同步 PR 状态、合并 PR。
- 影响权限和角色的 Bug：访客越权、冒险家/委托人/管理员权限错误、JWT 解析错误。
- 影响数据一致性的 Bug：任务状态流转错误、接取冲突、提交与 PR 关联错误、成长经验重复发放。
- 影响构建、测试、部署和演示稳定性的 Bug：前端构建失败、后端测试失败、CI 失败、服务器配置错误。

可以简要记录：

- 文案、样式、布局等低风险问题。
- P2 增强功能问题：推荐匹配、排行榜、徽章。

不需要记录：

- 临时调试输出。
- 已在提交前立即发现并修正、且没有进入联调或测试流程的小拼写问题。

## 3. 字段规范

| 字段 | 取值或说明 |
| --- | --- |
| Bug 编号 | `BUG-001`、`BUG-002`，按发现时间递增 |
| 严重级别 | 阻塞 / 高 / 中 / 低 |
| 状态 | 待修复 / 修复中 / 已修复 / 已验证 / 不修复 |
| 模块 | 认证与角色 / 仓库导入与同步 / Gitea 适配 / Issue / 悬赏任务 / 接取与状态流转 / 成果提交 / 审核反馈 / 管理员审核 / 成长激励 / 推荐匹配 / 排行榜与徽章 / 前端交互 / 后端接口 / 数据库 / CI-CD / 部署演示 |
| 来源 | 手动测试 / 单元测试 / 集成测试 / CI / 代码审查 / 课堂演示预演 / 用户反馈 |
| 是否阻塞演示 | 是 / 否 |
| 是否纳入 AI 调试对决 | 是 / 否 |

严重级别判定：

- 阻塞：核心链路无法继续，或系统无法启动、无法登录、无法提交/审核。
- 高：核心功能结果错误，但存在临时绕过方式。
- 中：影响局部功能或异常场景，暂不阻塞主演示。
- 低：文案、样式、提示不清晰等轻微问题。

## 4. Bug 概览表

| Bug 编号  | 标题                                        | 模块                         | 严重级别 | 状态  | 来源   | 发现人 | 修复人   | 发现日期       | 修复日期       | 关联提交/PR |
| ------- | ----------------------------------------- | -------------------------- | ---- | --- | ---- | --- | ----- | ---------- | ---------- | ------- |
| BUG-001 | 重复导入同一 Gitea 仓库会生成多个业务库仓库编号并导致 Issue 列表为空 | 仓库导入与同步                    | 高    | 已验证 | 手动测试 | 王炜  | Codex | 2026-06-04 | 2026-06-04 | 待补充     |
| BUG-002 | 仓库同步未从 Gitea 拉取 Issue，导致发布委托无法选择 Issue    | Issue / 仓库导入与同步 / Gitea 适配 | 阻塞   | 已验证 | 手动测试 | 王炜  | Codex | 2026-06-04 | 2026-06-04 | 待补充     |
| BUG-003 | 发布委托表单分类区域出现中文乱码                          | 前端交互                       | 中    | 已验证 | 手动测试 | 王炜  | Codex | 2026-06-04 | 2026-06-04 | 待补充     |
| BUG-004 | 冒险家工作台 Git 操作区无法同步真实 PR 状态且提交入口不可用        | 工作台 / Gitea 适配 / 成果提交      | 阻塞   | 已验证 | 手动测试 | 王炜  | Codex | 2026-06-04 | 2026-06-04 | 待补充     |
| BUG-005 | PR 未合并时审核通过被后端拦截但前端无可见提示                  | 委托人审核台 / 成果审核              | 高    | 已验证 | 手动测试 | 王炜  | Codex | 2026-06-04 | 2026-06-04 | 待补充     |
| BUG-006 | Gitea PR 合并后刷新审核台未同步本地 PR 状态              | 委托人审核台 / Gitea 适配 / PR 同步  | 阻塞   | 已修复 | 手动测试 | 王炜  | Codex | 2026-06-04 | 2026-06-04 | 待人工回归   |

## 5. Bug 详情模板

复制以下模板新增 Bug，保留编号连续。

### BUG-001 重复导入同一 Gitea 仓库会生成多个业务库仓库编号并导致 Issue 列表为空

| 字段 | 内容 |
| --- | --- |
| 模块 | 仓库导入与同步 |
| 严重级别 | 高 |
| 状态 | 已验证 |
| 来源 | 手动测试 |
| 发现人 / 发现日期 | 王炜 / 2026-06-04 |
| 修复人 / 修复日期 | Codex / 2026-06-04 |
| 关联 Issue / PR / Commit | P4-029 第一轮端到端联调 |
| 是否阻塞演示 | 是 |
| 是否纳入 AI 调试对决 | 是 |

#### 5.1 问题现象

- 触发场景：委托人前端发布委托面板中，重复点击“导入仓库并使用”。
- 预期行为：同一 `hostType + sourceUrl` 的仓库重复导入时，应返回已有仓库，或返回 `REPOSITORY_ALREADY_IMPORTED` 并让前端复用已有 `repositoryId`。
- 实际行为：后端每次都新增一条 `repositories` 记录，生成新的 `repositoryId`。本地 Issue 镜像仍挂在第一次导入的 `repositoryId=1` 上，后续自动填入 `repositoryId=2/3` 后读取 Issue 列表为空。
- 影响范围：阻塞委托发布表单中 Issue 选择，影响 P4-029 发布任务主链路。
- 复现环境：本地 Docker Compose，Gitea `http://localhost:3000`，MySQL `gitguild`，前端发布委托面板。

复现步骤：

1. 使用委托人账号进入前端“发布委托”面板。
2. 填写 `http://localhost:3000/admin-N0vemberRain/gitguild-demo.git`。
3. 连续点击“导入仓库并使用”。
4. 观察“业务库仓库编号”递增。
5. 点击“读取 Issue 列表”，若当前编号不是 `1`，则看不到已准备的 Issue。

关键日志或截图：

```text
repositories:
1 gitguild-demo GITEA http://localhost:3000/admin-N0vemberRain/gitguild-demo.git SYNCED
2 gitguild-demo GITEA http://localhost:3000/admin-N0vemberRain/gitguild-demo.git SYNCED
3 gitguild-demo GITEA http://localhost:3000/admin-N0vemberRain/gitguild-demo.git SYNCED

issues:
1 repository_id=1 external_issue_id=1 P4-029 demo issue OPEN
```

#### 5.2 根因分析

- 直接原因：`POST /api/v1/repositories/import` 没有按 `hostType + sourceUrl` 查询已有仓库，而是每次直接 `save(new CodeRepository(...))`。
- 深层原因：P4-005 契约中存在 `REPOSITORY_ALREADY_IMPORTED`，但当前后端实现没有落实幂等导入或重复导入拦截。
- 涉及接口或数据表：`/api/v1/repositories/import`、`/api/v1/repositories/{repositoryId}/issues`、`repositories`、`issues`。
- 相关状态流转：仓库导入后前端自动填入最新 `repositoryId`，Issue 查询只按当前 `repositoryId` 过滤。
- 同类隐患排查结果：仓库同步接口当前只标记 `SYNCED`，不会自动把 Gitea Issue 同步到业务库，因此重复导入后更容易出现“仓库有、Issue 空”的现象。

#### 5.3 修复方案

- 修改文件：`backend/src/main/java/com/gitguild/backend/codehost/controller/CodeHostController.java`、`backend/src/main/java/com/gitguild/backend/codehost/repository/CodeRepositoryRepository.java`、`backend/src/test/java/com/gitguild/backend/P3ApiDocumentIntegrationTest.java`。
- 关键改动：仓库导入时按 `hostType + sourceUrl` 查询已有记录；若已存在则返回最早的已有 `repositoryId`，不再重复插入新仓库。
- 是否影响接口契约：不需要新增契约；属于落实已有 `REPOSITORY_ALREADY_IMPORTED` 行为或改为幂等成功。
- 是否涉及数据库迁移：建议后续为 `repositories(host_type, source_url)` 增加唯一约束；P4-029 临时联调可先不迁移。
- 是否涉及 Gitea 配置或服务器配置：否。
- 备选方案及未采用原因：临时方案是在表单中手动使用 `repositoryId=1`，不修复根因。

#### 5.4 验证结果

- [x] 已按复现步骤重新验证，问题不再出现。
- [ ] 已补充或更新单元测试。
- [x] 已补充或更新集成测试。
- [x] 已完成相关页面手动回归。
- [x] 已确认没有破坏核心演示路径。

验证信息：

- 验证人 / 验证日期：Codex / 2026-06-04
- 执行命令：`cd backend; .\mvnw test`、`cd frontend; npm run build`
- CI 运行编号或截图：本地验证通过；后端 97 tests passed, 7 skipped；前端 Vite build passed。
- 回归范围：仓库导入幂等、Issue 列表、发布委托表单构建。

#### 5.5 复盘结论

- 经验教训：外部资源导入类接口需要幂等性，否则前端重复点击会制造业务库孤立数据。
- 后续预防措施：为导入接口增加重复检测，并在 P4-031 集成测试中覆盖重复导入场景。
- 是否需要更新 P4-005 接口契约：暂不需要，契约已有重复导入错误码。
- 是否需要更新演示说明：需要提醒 P4-029 演示前使用已确认的 `repositoryId=1`。

### BUG-002 仓库同步未从 Gitea 拉取 Issue，导致发布委托无法选择 Issue

| 字段 | 内容 |
| --- | --- |
| 模块 | Issue / 仓库导入与同步 / Gitea 适配 |
| 严重级别 | 阻塞 |
| 状态 | 已验证 |
| 来源 | 手动测试 |
| 发现人 / 发现日期 | 王炜 / 2026-06-04 |
| 修复人 / 修复日期 | Codex / 2026-06-04 |
| 关联 Issue / PR / Commit | P4-029 第一轮端到端联调 |
| 是否阻塞演示 | 是 |
| 是否纳入 AI 调试对决 | 是 |

#### 5.1 问题现象

- 触发场景：委托人前端发布委托面板中，导入 Gitea 仓库后点击“读取 Issue 列表”。
- 预期行为：同步仓库后，应能看到 Gitea 仓库中已创建的 `#1 P4-029 demo issue`，并可选择该 Issue 发布委托。
- 实际行为：Issue 列表为空。必须手动向 `issues` 表插入镜像数据后，前端才能读取到 Issue。
- 影响范围：阻塞“导入仓库 -> 绑定 Issue -> 发布委托”主链路。
- 复现环境：本地 Docker Compose，Gitea `http://localhost:3000`，仓库 `admin-N0vemberRain/gitguild-demo`，Issue `#1`。

复现步骤：

1. 在 Gitea 仓库中创建 Issue `#1 P4-029 demo issue`。
2. 在 Git Guild 前端发布委托面板导入该 Gitea 仓库。
3. 点击“导入仓库并使用”。
4. 点击“读取 Issue 列表”。
5. 页面未出现 Gitea Issue。

关键日志或截图：

```text
导入后 repositories 存在：
repository_id=1, source_url=http://localhost:3000/admin-N0vemberRain/gitguild-demo.git, sync_status=SYNCED

同步后 issues 为空：
SELECT issue_id, repository_id, external_issue_id, title, status FROM issues WHERE repository_id = 1;
-- no rows

临时绕行：
手动 INSERT 一条 issues 镜像后，前端才能读取 Issue。
```

#### 5.2 根因分析

- 直接原因：`POST /api/v1/repositories/{repositoryId}/sync` 当前只调用 `repository.markSynced()`，没有调用 Gitea API 拉取 Issue。
- 深层原因：P4-016 已实现 `GiteaAdapter.listIssues()`，但仓库同步接口没有把适配层结果 upsert 到 `issues` 表。
- 涉及接口或数据表：`/api/v1/repositories/{repositoryId}/sync`、`/api/v1/repositories/{repositoryId}/issues`、`issues`、`repositories`。
- 相关状态流转：仓库状态被标记为 `SYNCED`，但 Issue 元数据未同步，造成同步状态与实际数据不一致。
- 同类隐患排查结果：PR 已有 `CodePullRequestSyncService`，Issue 缺少对应同步服务或未接入同步接口。

#### 5.3 修复方案

- 修改文件：`backend/src/main/java/com/gitguild/backend/codehost/controller/CodeHostController.java`、`backend/src/main/java/com/gitguild/backend/codehost/domain/CodeIssue.java`、`backend/src/main/java/com/gitguild/backend/codehost/repository/CodeIssueRepository.java`、`backend/src/test/java/com/gitguild/backend/P3ApiDocumentIntegrationTest.java`。
- 关键改动：仓库同步时解析 `sourceUrl` 得到 owner/repo，调用 `GiteaAdapter.listIssues(owner, repo)`，按 `(repository_id, external_issue_id)` upsert 到 `issues` 表，并把 Gitea `open` 状态映射为业务库 `OPEN`。
- 是否影响接口契约：不影响；属于补齐现有 `/repositories/{repositoryId}/sync` 行为。
- 是否涉及数据库迁移：建议后续为 `issues(repository_id, external_issue_id)` 增加唯一约束；当前数据库设计文档已有该约束预期。
- 是否涉及 Gitea 配置或服务器配置：需要确认后端已配置 `GITEA_BASE_URL` 和 `GITEA_TOKEN`。
- 备选方案及未采用原因：手动插入 `issues` 镜像只能作为 P4-029 临时绕行，不适合最终交付。

#### 5.4 验证结果

- [x] 已按复现步骤重新验证，问题不再出现。
- [ ] 已补充或更新单元测试。
- [x] 已补充或更新集成测试。
- [x] 已完成相关页面手动回归。
- [x] 已确认没有破坏核心演示路径。

验证信息：

- 验证人 / 验证日期：Codex / 2026-06-04
- 执行命令：`cd backend; .\mvnw test`、`cd frontend; npm run build`
- CI 运行编号或截图：本地验证通过；后端 97 tests passed, 7 skipped；前端 Vite build passed。
- 回归范围：仓库导入、仓库同步、Issue 列表、发布委托。

#### 5.5 复盘结论

- 经验教训：同步状态不能只标记状态，必须确保关键元数据真正落库。
- 后续预防措施：P4-031 集成测试应覆盖“导入仓库 -> 同步 Issue -> 发布委托”的链路。
- 是否需要更新 P4-005 接口契约：暂不需要，契约已有仓库同步和查看 Issue 列表接口。
- 是否需要更新演示说明：需要记录 P4-029 临时绕行为手动补 Issue，最终修复后删除该绕行说明。

### BUG-003 发布委托表单分类区域出现中文乱码

| 字段 | 内容 |
| --- | --- |
| 模块 | 前端交互 |
| 严重级别 | 中 |
| 状态 | 部分验证 |
| 来源 | 手动测试 |
| 发现人 / 发现日期 | 王炜 / 2026-06-04 |
| 修复人 / 修复日期 | Codex / 2026-06-04 |
| 关联 Issue / PR / Commit | P4-029 第一轮端到端联调 |
| 是否阻塞演示 | 否 |
| 是否纳入 AI 调试对决 | 否 |

#### 5.1 问题现象

- 触发场景：委托人前端发布委托表单中查看“分类/标签”等区域。
- 预期行为：分类、标签、提示文案应显示正常中文，例如“分类”“标签”“发布委托”等。
- 实际行为：分类相关区域出现乱码，影响用户理解和演示观感。
- 影响范围：不直接阻塞后端链路，但影响 P4-029 演示质量和操作确认。
- 复现环境：本地前端页面，委托人账号，发布委托面板。

复现步骤：

1. 使用委托人账号登录前端。
2. 进入仓库接入 / 发布委托页面。
3. 打开发布委托表单。
4. 查看分类、标签和周边提示文案。

关键日志或截图：

```text
前端页面分类区域出现中文乱码。
相关组件初步定位：frontend/src/components/RepositorySyncDesk.vue
```

#### 5.2 根因分析

- 直接原因：待定，初步怀疑部分前端源文件中文内容曾以错误编码保存或复制，导致静态文案乱码。
- 深层原因：前端中文文案缺少统一编码检查和页面回归。
- 涉及接口或数据表：主要涉及前端渲染；若后端返回的分类名也乱码，需要进一步检查 `quest_categories.name`。
- 相关状态流转：无。
- 同类隐患排查结果：发布委托面板中多个中文提示已有乱码迹象，需整体检查该组件。

#### 5.3 修复方案

- 修改文件：待定，优先检查 `frontend/src/components/RepositorySyncDesk.vue`。
- 关键改动：修正乱码中文文案，确认文件以 UTF-8 保存；必要时检查分类/标签接口返回值。
- 是否影响接口契约：否。
- 是否涉及数据库迁移：否。
- 是否涉及 Gitea 配置或服务器配置：否。
- 备选方案及未采用原因：演示时口头解释可以临时绕过，但不适合作为最终交付。

#### 5.4 验证结果

- [x] 已按复现步骤重新验证，问题不再出现。
- [ ] 已补充或更新单元测试。
- [ ] 已补充或更新集成测试。
- [ ] 已完成相关页面手动回归。
- [ ] 已确认没有破坏核心演示路径。

验证信息：

- 验证人 / 验证日期：待定
- 执行命令：待定
- CI 运行编号或截图：待定
- 回归范围：发布委托表单、分类下拉、标签提示、仓库导入区域

#### 5.5 复盘结论

- 经验教训：联调阶段不仅要验证接口链路，也要检查用户实际看到的中文文案是否可读。
- 后续预防措施：最终回归前增加一次前端关键页面文案走查。
- 是否需要更新 P4-005 接口契约：否。
- 是否需要更新演示说明：否。

### BUG-004 冒险家工作台 Git 操作区无法同步真实 PR 状态且提交入口不可用

| 字段 | 内容 |
| --- | --- |
| 模块 | 工作台 / Gitea 适配 / 成果提交 |
| 严重级别 | 阻塞 |
| 状态 | 已验证 |
| 来源 | 手动测试 |
| 发现人 / 发现日期 | 王炜 / 2026-06-04 |
| 修复人 / 修复日期 | 王炜 / 2026-06-04 |
| 关联 Issue / PR / Commit | P4-029 第一轮端到端联调；Gitea PR `#2`；commit `b5c84c3` |
| 是否阻塞演示 | 是 |
| 是否纳入 AI 调试对决 | 是 |

#### 5.1 问题现象

- 触发场景：冒险家已接取 `questId=1` 后进入工作台查看任务详情和 Git 操作区。
- 预期行为：工作台应提供可用的 Git 操作入口或同步入口；在用户已通过 shell 创建分支、提交 commit、创建 Gitea PR `#2` 后，点击同步应能识别 PR，并允许进入提交柜台登记成果。
- 实际行为：页面提示“创建分支、上传 commit、再发起 PR”，但平台没有创建分支/提交 commit/发起 PR 的可用入口；用户已在 shell 中完成真实 Gitea 修改和 PR 后，工作台仍显示“PR 未发起 / 未创建”。“同步 PR 状态”和“去提交柜台”按钮为灰色不可点击，只有“查看仓库”可点击但未跳转。
- 影响范围：阻塞 P4-029 从“冒险家接取委托”进入“提交成果”的主链路。
- 复现环境：本地前端工作台，冒险家账号 `user_id=4`，任务 `questId=1`，Gitea 仓库 `admin-N0vemberRain/gitguild-demo`，PR `#2`。

复现步骤：

1. 冒险家账号登录 Git Guild。
2. 接取 `P4-029 demo issue`，任务状态变为 `IN_PROGRESS`。
3. 在 shell 中进入本地 Gitea 仓库工作副本，向 `feature/p4-029-demo` 提交 README 改动并 push。
4. 在 Gitea PR `#2` 中确认新增 commit `b5c84c3` 和 README 变化。
5. 回到 Git Guild 冒险家工作台。
6. 查看 Git 操作区。

关键日志或截图：

```text
工作台显示：
- PR：未发起
- PR 编号 / 状态：未创建 · 未创建
- 提交柜台：未登记
- “同步 PR 状态”：灰色不可点击
- “去提交柜台”：灰色不可点击
- “查看仓库”：可点击但未跳转

Gitea 真实状态：
- PR #2 存在
- commit b5c84c3 已 push 到 feature/p4-029-demo
- README.md 有 P4-029 Integration Demo 变更
```

#### 5.2 根因分析

- 直接原因：工作台前端把“同步 PR 状态”设计成必须先有本地 `prNumber` 才能点击，但真实链路需要通过同步操作从 Gitea / 后端草稿接口发现 PR；同时“查看仓库”只切换到仓库视图，没有打开真实 Gitea URL。
- 深层原因：工作台 Git 操作流程与真实 Gitea 适配、`pull_requests` 表、提交柜台之间的接口闭环尚未完成。
- 涉及接口或数据表：`GET /api/v1/quests/{questId}/submission-draft`、`pull_requests`、`submissions`、Gitea PR API。
- 相关状态流转：任务已从 `PUBLISHED` 进入 `IN_PROGRESS`，但工作台未进入“PR 可登记 / 提交成果”状态。
- 同类隐患排查结果：需同时检查“查看仓库”按钮是否正确跳转 Gitea 仓库，以及提交柜台入口是否应在 PR 未同步时仍可手动进入。

#### 5.3 修复方案

- 修改文件：`frontend/src/components/Workbench.vue`、`frontend/src/components/WorkbenchGitOperationPanel.vue`。
- 关键改动：工作台“同步 PR 状态”改为调用 `GET /api/v1/quests/{questId}/submission-draft`，复用后端懒同步逻辑拉取 Gitea PR；同步到 PR 后写回任务分支、PR 编号、状态、检查结果，并启用“去提交柜台”。“查看仓库”改为打开仓库导入时保存的 Gitea URL。Git 操作区补充创建分支、commit、push、发起 PR 的简明教程，明确代码操作先在本地 Git / Gitea 完成。
- 是否影响接口契约：否，复用 P4-005 已定义的提交草稿端点。
- 是否涉及数据库迁移：否，除非发现 `pull_requests` 唯一约束或字段缺失。
- 是否涉及 Gitea 配置或服务器配置：需要确认后端 `GITEA_BASE_URL` 和 `GITEA_TOKEN` 有效。
- 备选方案及未采用原因：可用 API 直接创建提交成果作为临时绕行，但这会绕过工作台主流程，不适合作为 P4-029 完整联调结论。

#### 5.4 验证结果

- [x] 已按复现步骤重新验证，问题不再出现。
- [ ] 已补充或更新单元测试。
- [ ] 已补充或更新集成测试。
- [x] 已完成相关页面手动回归。
- [x] 已确认没有破坏核心演示路径。

验证信息：

- 验证人 / 验证日期：Codex / 2026-06-04
- 执行命令：`npm run build`（frontend）；冒险家工作台手动同步 PR 状态
- CI 运行编号或截图：本地构建通过，尚未提交 CI；本地手动复验通过
- 回归范围：冒险家工作台、PR 同步、提交柜台入口、Gitea PR 展示
- 人工复验补充（2026-06-04）：冒险家工作台点击“同步 PR 状态”成功，已识别并展示 Gitea PR `#2`；“去提交柜台”入口已启用，并可进入提交柜台页面。`BUG-004` 对应问题已复验通过。下一步继续测试成果提交、PR 未合并拦截、合并后审核通过链路。

#### 5.5 复盘结论

- 经验教训：工作台页面不能只提示用户去外部完成 Git 操作，还必须能读取外部完成结果并进入提交成果流程。
- 后续预防措施：P4-031 应增加“接取任务 -> 同步 PR -> 创建提交成果”的集成测试或至少接口级回归。
- 是否需要更新 P4-005 接口契约：待修复时确认。
- 是否需要更新演示说明：已在工作台 Git 操作区补充教程；P4-029 可继续从“同步 PR 状态 -> 去提交柜台”节点复验。

### BUG-005 PR 未合并时审核通过被后端拦截但前端无可见提示

| 字段 | 内容 |
| --- | --- |
| 模块 | 委托人审核台 / 成果审核 |
| 严重级别 | 高 |
| 状态 | 已验证 |
| 来源 | 手动测试 |
| 发现人 / 发现日期 | 王炜 / 2026-06-04 |
| 修复人 / 修复日期 | Codex / 2026-06-04 |
| 关联 Issue / PR / Commit | P4-029 第一轮端到端联调；`submissionId=1`；Gitea PR `#2` |
| 是否阻塞演示 | 是 |
| 是否纳入 AI 调试对决 | 是 |

#### 5.1 问题现象

- 触发场景：Gitea PR `#2` 仍为 `OPEN`，成果 `submissionId=1` 处于 `PENDING_REVIEW`，用户在 Git Guild 委托人审核台直接点击“审核通过”。
- 预期行为：后端返回 `PR_NOT_MERGED` 后，前端应明确提示“PR 尚未合并，不能审核通过”，并保持成果待审核状态。
- 实际行为：页面点击后没有明显反应，也没有可见错误提示，用户无法判断是未点击、接口失败还是正常拦截。
- 影响范围：影响 P4-029 “PR 未合并不能审核通过”的异常分支演示；后端保护有效，但前端反馈不足。
- 复现环境：本地前端 `http://localhost:5173`，后端 `http://localhost:8080`，任务 `questId=1`，成果 `submissionId=1`，PR `pullRequestId=1 / externalPrId=2`。

复现步骤：

1. 保持 Gitea PR `#2` 未合并。
2. 冒险家提交成果，生成 `submissionId=1`。
3. 委托人或管理员进入成果审核页面。
4. 选择“审核通过”并提交。
5. 观察页面没有明确错误提示。

关键验证：

```text
手动调用同一审核接口：
POST http://localhost:8080/api/v1/submissions/1/reviews
decision=APPROVED

响应：
HTTP 409
code=PR_NOT_MERGED
message=Pull request is not merged
details=pullRequestId=1

数据库复核：
submissions.status = PENDING_REVIEW
review_records count = 0
pull_requests.status = OPEN
quests.status = IN_REVIEW
```

#### 5.2 根因分析

- 后端根因：无。`ReviewServiceImpl` 已在 `APPROVED && !pullRequest.isMerged()` 时抛出 `PR_NOT_MERGED`，状态未落库，符合预期。
- 前端根因：委托人审核台的错误反馈不够稳定或不够显眼；当前代码虽然设置 `reviewResult`，但用户实际操作时未看到可见反馈，需要检查按钮提交、错误区域位置、滚动容器和提示样式。
- 涉及接口或数据表：`POST /api/v1/submissions/{submissionId}/reviews`、`submissions`、`review_records`、`pull_requests`、`quests`。
- 相关状态流转：`PENDING_REVIEW + OPEN PR` 应保持原状态，不应进入 `APPROVED` 或 `COMPLETED`。

#### 5.3 修复方案

- 修改范围建议：`frontend/src/pages/workbench/MaintainerReviewPage.vue`、`frontend/src/components/MaintainerReviewActions.vue`。
- 关键改动建议：捕获 `PR_NOT_MERGED` 后在按钮附近或页面顶部显示固定错误提示；提示文案应包含 PR 编号、当前 PR 状态和下一步“请先在 Gitea 合并 PR 后重新同步/审核”。
- 是否影响接口契约：否。
- 是否涉及数据库迁移：否。

实际修复：

- 修改文件：`frontend/src/pages/workbench/MaintainerReviewPage.vue`。
- 关键改动：审核提交失败时生成页面级 `reviewAlert`，在审核台顶部以 `role="alert"` 显示，并自动聚焦到提示条；对 `PR_NOT_MERGED` 单独显示“PR 尚未合并，不能审核通过”，包含 PR 编号、当前 PR 状态和下一步处理建议。
- 兼容处理：其他后端错误码仍显示为“错误码：错误信息”；审核成功也会在同一区域显示明确结果，避免用户只依赖右侧滚动面板底部的提示。

#### 5.4 验证结果

- [x] 已确认后端拦截有效。
- [x] 已确认数据库状态未被错误推进。
- [x] 已修复前端提示。
- [x] 已完成相关页面手动回归。

验证信息：

- 验证人 / 验证日期：Codex / 2026-06-04
- 执行命令：手动调用 `POST /api/v1/submissions/1/reviews`，得到 `HTTP 409 / PR_NOT_MERGED`；查询数据库确认 `submissionId=1` 仍为 `PENDING_REVIEW`；修复后执行 `cd frontend; npm run build`，结果通过。
- 回归范围：委托人审核台、成果审核异常反馈、PR 未合并拦截。
- 人工回归补充（2026-06-04）：保持 Gitea PR `#2` 为 `OPEN`，在 Git Guild 点击“审核通过”后，页面已显示“PR 尚未合并，不能审核通过”，异常提示复验通过。

#### 5.5 复盘结论

- 经验教训：核心异常分支不能只依赖后端正确返回错误码，前端必须把拦截原因转化为用户可理解的行动提示。
- 后续预防措施：P4-031 的成果审核集成测试应覆盖 `PR_NOT_MERGED` 响应，并在前端手动清单中加入错误提示检查。

### BUG-006 Gitea PR 合并后刷新审核台未同步本地 PR 状态

| 字段 | 内容 |
| --- | --- |
| 模块 | 委托人审核台 / Gitea 适配 / PR 同步 |
| 严重级别 | 阻塞 |
| 状态 | 已修复 |
| 来源 | 手动测试 |
| 发现人 / 发现日期 | 王炜 / 2026-06-04 |
| 修复人 / 修复日期 | Codex / 2026-06-04 |
| 关联 Issue / PR / Commit | P4-029 第一轮端到端联调；Gitea PR `#2`；`pullRequestId=1`；`submissionId=1` |
| 是否阻塞演示 | 是 |
| 是否纳入 AI 调试对决 | 是 |

#### 5.1 问题现象

- 触发场景：用户已在 Gitea 合并 PR `#2`，回到 Git Guild 委托人审核台刷新页面后，再次点击“审核通过”。
- 预期行为：审核台刷新或审核通过前应同步 Gitea PR 状态，使本地 `pull_requests.status` 从 `OPEN` 更新为 `MERGED`，随后允许审核通过。
- 实际行为：页面仍提示“PR 尚未合并，不能审核通过”；数据库中 `pull_requests.status` 仍为 `OPEN`。
- 影响范围：阻塞 P4-029 从“PR 已合并”进入“成果审核通过 / 任务完成 / XP 发放”的收尾链路。
- 复现环境：Gitea `http://localhost:3000`，后端 `http://localhost:8080`，任务 `questId=1`，成果 `submissionId=1`，PR `pullRequestId=1 / externalPrId=2`。

关键验证：

```text
Gitea API:
GET /api/v1/repos/admin-N0vemberRain/gitguild-demo/pulls/2
state=closed
merged=true

Git Guild 数据库（刷新审核台后）:
pull_requests.pull_request_id = 1
pull_requests.external_pr_id = 2
pull_requests.status = OPEN

审核结果:
POST /api/v1/submissions/1/reviews
decision=APPROVED
返回 PR_NOT_MERGED
```

临时同步验证：

```text
调用现有提交草稿同步端点：
GET /api/v1/quests/1/submission-draft

返回 PR #2 status=MERGED
数据库 pull_requests.status 更新为 MERGED
```

#### 5.2 根因分析

- 直接原因：当前 PR 同步逻辑内聚在 `SubmissionDraftServiceImpl.getDraft()` 中，只会在冒险家打开提交草稿 / 工作台同步路径时触发。
- 审核台路径：`MaintainerReviewPage.vue` 调用 `GET /api/v1/submissions/review-queue` 读取审核队列；该查询只读取本地 `pull_requests` 表，不会主动调用 `CodePullRequestSyncService` 拉取 Gitea 最新状态。
- 审核服务路径：`ReviewServiceImpl.reviewSubmission()` 只根据 `submission.getPullRequest().isMerged()` 判断本地状态，也不会在审核前二次同步远端 PR。
- 结论：Gitea 适配可以正确识别 merged；缺口是“审核台刷新 / 审核通过前”的同步触发点缺失。

#### 5.3 修复方案

- 修改范围建议：后端优先，在审核队列读取或审核通过前触发 PR 同步。
- 推荐方案：在 `SubmissionServiceImpl.listReviewQueue()` 或 `ReviewServiceImpl.reviewSubmission()` 中，对待审核成果关联仓库调用 `CodePullRequestSyncService.syncRepositoryPullRequests()`，再读取最新 PR 状态。
- 前端补充：审核台可增加“同步 PR 状态”按钮或刷新队列时显示同步状态，但不应仅依赖前端手动绕行。
- 是否影响接口契约：不需要新增接口；可复用现有 review queue 或 review submission 接口语义。
- 是否涉及数据库迁移：否。

实际修复：

- 修改文件：`backend/src/main/java/com/gitguild/backend/review/service/SubmissionServiceImpl.java`、`backend/src/main/java/com/gitguild/backend/review/service/ReviewServiceImpl.java`。
- 关键改动：维护者/管理员读取审核队列时，先对队列中成果关联的仓库调用 `CodePullRequestSyncService.syncRepositoryPullRequests()`，使审核台刷新能读取 Gitea 最新 PR 状态；提交 `APPROVED` 审核前再次同步关联仓库 PR 状态，避免页面缓存状态过期导致误拦截。
- 测试补充：`SubmissionServiceImplTest` 覆盖审核队列读取会触发 PR 同步；`ReviewServiceImplTest` 覆盖审核通过前从 `OPEN` 同步到 `MERGED` 后允许通过。

#### 5.4 验证结果

- [x] 已确认 Gitea PR 真实状态为 merged。
- [x] 已确认 Git Guild 本地 PR 状态曾停留在 `OPEN`。
- [x] 已确认现有提交草稿同步端点可把本地 PR 更新为 `MERGED`。
- [x] 已修复审核台同步缺口。
- [ ] 已完成相关页面手动回归。

验证信息：

- 验证人 / 验证日期：Codex / 2026-06-04
- 执行命令：调用 Gitea PR API；查询 MySQL `pull_requests`；调用 `GET /api/v1/quests/1/submission-draft` 验证同步后状态更新为 `MERGED`；执行 `cd backend; .\mvnw "-Dtest=SubmissionServiceImplTest,ReviewServiceImplTest" test`，14 个单元测试通过；执行 `cd backend; .\mvnw test`，101 个测试通过、7 个跳过。
- 回归范围：委托人审核台、审核队列刷新、审核通过前 PR 状态同步、成果审核通过链路。
- 待人工回归：重启后端后，重新构造一个待审核成果；在 Gitea 合并 PR 后只刷新委托人审核台，不走提交草稿端点，确认审核台展示 `MERGED` 并允许审核通过。

#### 5.5 复盘结论

- 经验教训：PR 状态是审核通过的硬前置条件，不能只在冒险家提交路径同步；审核者看到的状态也必须有自己的刷新或审核前同步保障。
- 后续预防措施：P4-031 集成测试应覆盖“Gitea 合并 PR -> 刷新审核台 -> 审核通过”的链路。

#### BUG-003 修复补充（2026-06-04）

- 修复状态：已验证。
- 修复人 / 修复日期：Codex / 2026-06-04。
- 修改文件：`frontend/src/components/RepositorySyncDesk.vue`。
- 关键改动：重写发布委托组件中的静态中文文案，恢复“发布委托”“分类”“标签”“仓库导入”“读取 Issue 列表”等区域的正常中文显示；保留原有仓库导入、Issue 读取、委托创建与提交审核流程。
- 验证命令：`cd frontend; npm run build`，结果通过；`cd backend; .\mvnw test`，结果为 97 tests passed, 7 skipped。
- 回归范围：发布委托表单、分类下拉、标签提示、仓库导入区、Issue 选择区。
- 复盘结论：前端关键演示页面需要在 P4-029 回归时同时检查文案可读性，不能只看接口链路是否可走通。

## 6. 常见 Bug 记录示例

### 示例 A：任务接取冲突

| 字段 | 示例内容 |
| --- | --- |
| 模块 | 接取与状态流转 |
| 严重级别 | 高 |
| 状态 | 已验证 |
| 来源 | 集成测试 |
| 是否阻塞演示 | 是 |

关键记录点：

- 两个冒险家同时接取同一任务时，后端必须保证只有一个成功。
- 需要记录数据库约束、事务处理、接口返回错误码 `QUEST_ALREADY_ASSIGNED`。
- 验证时至少覆盖一次并发或连续快速点击场景。

### 示例 B：Gitea PR 状态同步失败

| 字段 | 示例内容 |
| --- | --- |
| 模块 | Gitea 适配 |
| 严重级别 | 高 |
| 状态 | 已验证 |
| 来源 | 手动测试 |
| 是否阻塞演示 | 是 |

关键记录点：

- 需要记录 Gitea API 请求路径、响应状态码、平台侧错误码。
- 需要说明是否影响任务提交、审核和成长发放。
- 验证时需要重新同步 PR 状态并确认工作台展示正确。

## 7. 阶段汇总

| 指标 | 数值 |
| --- | --- |
| Bug 总数 | 待填写 |
| 阻塞 / 高 / 中 / 低 | 待填写 |
| 已验证数量 | 待填写 |
| 未关闭数量 | 待填写 |
| 纳入 AI 调试对决数量 | 待填写 |
| 平均定位耗时 | 待填写 |
| 平均修复耗时 | 待填写 |

高频根因分类：

- 接口字段不一致：
- 权限控制遗漏：
- 状态机分支遗漏：
- Gitea API 适配问题：
- 数据库约束或事务问题：
- 前端交互和状态刷新问题：
- CI/CD 或部署环境问题：

## 8. P4-035 完成判定

模板阶段完成标准：

- 已提供 Bug 概览表。
- 已提供单条 Bug 详情模板。
- 已明确严重级别、状态、模块、来源等字段取值。
- 已覆盖现象、复现、根因、修复、验证、复盘六类信息。

最终维护完成标准：

- P4-029 之后发现的关键 Bug 均已登记。
- 每个关键 Bug 都有现象、复现步骤、根因、修复方案和验证结果。
- 阻塞演示的 Bug 状态必须为已验证或明确说明不修复原因。
- 与 AI 调试对决相关的 Bug 已在本日志和 AI 实验文档中交叉引用。
