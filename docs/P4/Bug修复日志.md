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

| Bug 编号 | 标题 | 模块 | 严重级别 | 状态 | 来源 | 发现人 | 修复人 | 发现日期 | 修复日期 | 关联提交/PR |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BUG-001 | 重复导入同一 Gitea 仓库会生成多个业务库仓库编号并导致 Issue 列表为空 | 仓库导入与同步 | 高 | 已验证 | 手动测试 | 王炜 | Codex | 2026-06-04 | 2026-06-04 | 待补充 |
| BUG-002 | 仓库同步未从 Gitea 拉取 Issue，导致发布委托无法选择 Issue | Issue / 仓库导入与同步 / Gitea 适配 | 阻塞 | 已验证 | 手动测试 | 王炜 | Codex | 2026-06-04 | 2026-06-04 | 待补充 |
| BUG-003 | 发布委托表单分类区域出现中文乱码 | 前端交互 | 中 | 已验证 | 手动测试 | 王炜 | Codex | 2026-06-04 | 2026-06-04 | 待补充 |

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
| 状态 | 待修复 |
| 来源 | 手动测试 |
| 发现人 / 发现日期 | 王炜 / 2026-06-04 |
| 修复人 / 修复日期 | 待定 |
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

- [ ] 已按复现步骤重新验证，问题不再出现。
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
