# AI 调试对决实验

> 对应 P4 任务 5.2「关键实验二 —— AI 调试对决」。
> 配套文档：[`Bug修复日志.md`](./Bug修复日志.md)、[`AI 代码信任度实验.md`](./AI%20代码信任度实验.md)。
> 最后更新：2026-06-03

## 1. 实验目的

在 Git Guild 前后端联调阶段，选取 3 个有代表性的 Bug，对同一个 Bug 分别走「纯人工调试」和「AI 辅助调试」两条路径，记录定位耗时、定位准确度、修复方案质量和最终采用来源，分析 AI 在调试环节的价值与局限。

实验关注的不是单点效率，而是回答三个问题：

- AI 在哪类 Bug 上能快速定位根因？
- AI 在哪类 Bug 上会只修表象、不找根因？
- 人工补充哪些上下文后，AI 的调试建议明显变好？

## 2. 实验设置

| 项目 | 说明 |
| --- | --- |
| 后端 | Spring Boot 3.x + Spring Data JPA，H2（测试）/ MySQL（演示） |
| 前端 | Vue 3 + Vite |
| AI 工具 | Claude（IDE 内对话），统一以「报错信息 + 复现步骤 + 相关代码片段」作为输入 |
| 计时口径 | 人工：从开始看报错到能口头说清根因；AI：从贴入完整上下文到给出可判断的定位建议 |
| 公平性约束 | 两条路径由不同成员独立进行，互不透露结论；AI 路径只允许提供已掌握的真实上下文，不提前喂答案 |

> 说明：本实验用于对比方法差异，时间为单次实测的近似值，存在个体熟练度影响，仅作定性参考，不作为精确基准。

## 3. 参选 Bug 与选取理由

| Bug # | 标题 | 模块 | 类型 | 选取理由 |
| --- | --- | --- | --- | --- |
| BUG-002 | Gitea 4xx 异常透传为 HTTP 500 | Gitea 适配 / 后端接口 | 错误处理 / 异常映射 | 报错堆栈清晰、根因局部，验证 AI 在「有明确堆栈」类问题上的表现 |
| BUG-003 | 快速重复接取导致同一任务被双接取 | 接取与状态流转 | 并发竞态 / 数据一致性 | 单次日志不暴露根因，验证 AI 在「需要全局并发推理」类问题上的表现 |
| BUG-004 | PR 重复同步触发唯一约束冲突 500 | Gitea 适配 / 数据库 | 幂等 / 数据库约束 | 介于上述两者之间，验证 AI 方案是否考虑并发边界 |

三个 Bug 分别对应 AI 预期「擅长 / 不擅长 / 中等」三档，来源于 [`P4-016 已知潜在问题清单`](./临时文档/P4-016%20已知潜在问题清单.md) 与 [`Bug修复日志.md`](./Bug修复日志.md) 示例 A。

---

## 4. BUG-002：Gitea 4xx 异常透传为 HTTP 500

### 4.1 问题现象

- 触发场景：委托人导入一个不存在的 Gitea 仓库，或 `GITEA_TOKEN` 失效后读取仓库。
- 预期行为：前端收到明确错误，如「仓库不存在」（404）或「代码托管服务不可用」。
- 实际行为：前端统一收到 HTTP 500 + `{"code":"INTERNAL_ERROR","message":"服务器内部错误"}`，无法区分。

关键日志：

```text
org.springframework.web.client.HttpClientErrorException$NotFound: 404 Not Found on GET https://gitea.example.com/api/v1/repos/foo/bar
    at GiteaAdapterImpl.getRepository(GiteaAdapterImpl.java:31)
    ...
被 GlobalExceptionHandler.handleAll(...) 捕获 -> 返回 500 INTERNAL_ERROR
```

### 4.2 对决过程

| 步骤 | 纯人工调试 | AI 辅助调试 |
| --- | --- | --- |
| 1. 问题描述 | 成员看到前端 500，去后端日志找堆栈 | 将堆栈、复现步骤、`GiteaAdapterImpl` 与 `GlobalExceptionHandler` 代码一并提供给 AI |
| 2. 定位耗时 | 约 12 分钟 | 约 2 分钟 |
| 3. 定位准确度 | 准确：定位到 `HttpClientErrorException` 未被捕获，落入 catch-all | 准确：直接指出 RestClient 抛出的 4xx 未转业务异常，被兜底处理器吞为 500 |
| 4. 修复方案 | 在 `GiteaAdapterImpl` 各方法捕获 `HttpClientErrorException`，按状态码转 `BusinessException` | 同向：建议捕获后映射 `GITEA_REPO_NOT_FOUND` / `CODE_HOST_UNAVAILABLE`，并给出 4xx→业务码映射表 |
| 5. 方案质量 | 正确，但需逐方法补 try-catch，略重复 | 额外建议抽取统一映射方法，减少重复，且提示 401 应区分为「Token 无效」 |
| 6. 最终修复 | 采用 AI 的「统一映射方法」写法，人工确认错误码与 API 契约一致 | 采纳，仅调整错误码命名以对齐 P4-005 契约 |

### 4.3 最终修复

- 修改文件：`GiteaAdapterImpl.java`、`GlobalExceptionHandler.java`
- 关键改动：捕获 `HttpClientErrorException`，按 `getStatusCode()` 映射为 `BusinessException`（`GITEA_REPO_NOT_FOUND` 404、`CODE_HOST_UNAVAILABLE` 5xx、`GITEA_AUTH_FAILED` 401）。
- 验证：构造不存在仓库的请求，确认前端收到 404 + 明确提示；`mvnw test` 全绿。

---

## 5. BUG-003：快速重复接取导致同一任务被双接取

### 5.1 问题现象

- 触发场景：冒险家在任务详情页快速双击「接取」按钮，或两名冒险家几乎同时接取同一任务。
- 预期行为：同一任务只能有一条 `ACTIVE` 接取记录，第二次返回 `QUEST_ALREADY_ASSIGNED`。
- 实际行为：偶发两条 `ACTIVE` 接取记录同时写入，任务被两人「接取成功」，后续提交、审核数据错乱。

关键代码（修复前）：

```java
// acceptQuest：先查再插，非原子
long active = assignmentRepository.countByQuestIdAndStatus(questId, ACTIVE);
if (active > 0) {
    throw new BusinessException("QUEST_ALREADY_ASSIGNED", ...);
}
assignmentRepository.save(new QuestAssignment(quest, adventurer, ACTIVE)); // 并发下两个线程都通过了 if
```

### 5.2 对决过程

| 步骤 | 纯人工调试 | AI 辅助调试 |
| --- | --- | --- |
| 1. 问题描述 | 成员发现工作台同一任务出现在两人名下，怀疑前端重复提交 | 将「双接取现象 + `acceptQuest` 代码 + 接取表结构」提供给 AI（**首轮未说明是并发/快速双击场景**） |
| 2. 定位耗时 | 约 35 分钟（难以稳定复现，靠读代码推断时序） | 首轮约 3 分钟给出建议，但方向偏表象 |
| 3. 定位准确度 | 准确：识别为「先 count 再 insert」的检查-执行非原子（TOCTOU），且数据库缺唯一约束 | 首轮**偏表象**：建议前端按钮加 loading 防抖、后端加 `if` 二次判断；未指出根因是缺少原子约束 |
| 4. 修复方案 | DB 层加 `UNIQUE(quest_id, status='ACTIVE')` 约束 + 捕获 `DataIntegrityViolationException` 兜底 | 补充上下文后改进（见下） |
| 5. 方案质量 | 根因级修复，可彻底防住并发 | 首轮方案治标不治本，前端防抖只降低概率，二次 `if` 仍非原子 |
| 6. 最终修复 | 采用人工的「唯一约束 + 异常兜底」方案 | 补充并发上下文后采纳 AI 改进版的实现细节 |

**关键转折 —— 人工补充上下文后 AI 明显变好：**

第一轮 AI 只看到单次代码，默认这是「普通重复提交」，给出防抖 + if 判断。人工补充以下三条上下文后再次提问：

1. 这是**并发 / 快速双击**场景，两个请求几乎同时到达；
2. 使用 JPA + MySQL，`quest_assignments` 表当前**没有**针对 ACTIVE 的唯一约束；
3. 要求方案在数据库层面杜绝，而非降低概率。

AI 随即修正定位为 TOCTOU 竞态，并给出与人工一致的方案：加部分唯一索引 / 唯一约束，`save` 时捕获 `DataIntegrityViolationException` 转 `QUEST_ALREADY_ASSIGNED`，去掉对前端防抖的依赖。

### 5.3 最终修复

- 修改文件：`QuestAssignmentRepository.java`、接取 Service、数据库 `init.sql`（迁移脚本）。
- 关键改动：数据库增加唯一约束保证「一个任务至多一条 ACTIVE 接取」；Service 捕获唯一约束冲突映射为 `QUEST_ALREADY_ASSIGNED`。
- 验证：用两个线程并发接取同一任务的集成测试，断言只有一条成功、另一条返回 `QUEST_ALREADY_ASSIGNED`。

---

## 6. BUG-004：PR 重复同步触发唯一约束冲突 500

### 6.1 问题现象

- 触发场景：Gitea Webhook 对同一 PR 重复投递，或定时同步重跑。
- 预期行为：重复同步应幂等，更新已有 PR 记录。
- 实际行为：第二次 `save(new CodePullRequest(...))` 触发 `uk_pr_repository_external` 唯一约束冲突，返回 HTTP 500。

关键日志：

```text
org.springframework.dao.DataIntegrityViolationException:
could not execute statement; constraint [uk_pr_repository_external]
... Duplicate entry '12-87' for key 'uk_pr_repository_external'
```

### 6.2 对决过程

| 步骤 | 纯人工调试 | AI 辅助调试 |
| --- | --- | --- |
| 1. 问题描述 | 成员看到 Webhook 重投后偶发 500，查日志见唯一约束冲突 | 将异常堆栈、`CodePullRequest` 实体与 Repository 提供给 AI |
| 2. 定位耗时 | 约 10 分钟 | 约 2 分钟 |
| 3. 定位准确度 | 准确：同步逻辑总是 new 新对象 save，未做 upsert | 准确：指出唯一约束冲突源于无「查找-或-更新」 |
| 4. 修复方案 | `findByRepositoryIdAndExternalPrId(...).orElse(new ...)` 后 set 字段再 save | 同向，给出标准 find-or-update 写法 |
| 5. 方案质量 | 正确，但未考虑高并发下两请求同时 miss 仍会冲突 | **AI 方案同样遗漏并发边界**，需人工补：捕获 `DataIntegrityViolationException` 重试一次 |
| 6. 最终修复 | 采用 find-or-update + 唯一约束冲突兜底重试 | 采纳骨架，人工补并发兜底 |

### 6.3 最终修复

- 修改文件：PR 同步 Service、`CodePullRequestRepository.java`。
- 关键改动：改为「查找-或-更新」幂等写入；并对极端并发下的唯一约束冲突捕获后重试一次。
- 验证：对同一 PR 连续同步两次，断言只生成一条记录、状态被更新且无 500。

---

## 7. 实验记录表

| Bug # | Bug 描述 | 纯人工定位耗时 | AI 辅助定位耗时 | AI 定位是否准确 | AI 修复方案是否可用 | 最终方案来源 |
| --- | --- | --- | --- | --- | --- | --- |
| BUG-002 | Gitea 4xx 异常透传为 HTTP 500 | ~12 min | ~2 min | 准确（首轮即对） | 可用，且优于人工初版（统一映射） | AI 方案 + 人工对齐错误码 |
| BUG-003 | 快速重复接取导致双接取 | ~35 min | 首轮 ~3 min（偏表象），补上下文后准确 | 首轮不准（只修表象），补上下文后准确 | 首轮不可用（防抖+if），补上下文后可用 | 人工根因 + AI 补充实现 |
| BUG-004 | PR 重复同步唯一约束冲突 500 | ~10 min | ~2 min | 准确 | 基本可用，遗漏并发兜底 | AI 骨架 + 人工补并发处理 |

---

## 8. 分析

### 8.1 AI 在哪类 Bug 上表现较好

- **有明确堆栈、根因局部的问题**（BUG-002）：异常类型、抛出位置、被谁吞掉，AI 能顺着堆栈快速给出准确定位，且常给出比人工初版更整洁的写法（统一映射、抽取方法）。
- **可由代码静态推断的约束类问题**（BUG-004）：唯一约束冲突这类「报错信息已点名约束」的问题，AI 几乎一次到位。

### 8.2 AI 在哪类 Bug 上表现较差

- **并发竞态 / 需要全局时序推理的问题**（BUG-003）：当输入只有单次代码、缺少「这是并发场景」的信息时，AI 默认按串行语义理解，给出防抖、二次判断等表象修复，无法识别 TOCTOU 根因。

### 8.3 AI 是否出现只修表象、不找根因的情况

- 出现，集中在 BUG-003：首轮建议「前端按钮防抖 + 后端再 if 判断一次」，只降低并发概率，未触及「检查-执行非原子 + 缺唯一约束」的根因。
- BUG-004 属于「方向对但边界不全」：找到了根因（缺 upsert），但遗漏并发下两请求同时 miss 的兜底，属于半根因。

### 8.4 人工提供哪些上下文后，AI 的调试建议明显变好

对 BUG-003，补充以下三类上下文后，AI 从表象修复跃升为根因修复：

1. **运行场景**：明确「并发 / 快速双击，两个请求几乎同时到达」，而非单次调用。
2. **持久层事实**：说明用 JPA + MySQL，且当前表**没有**对应唯一约束。
3. **约束要求**：要求「在数据库层面杜绝，而非降低概率」，堵死防抖类治标方案。

> 经验：对竞态、状态机、跨表一致性类 Bug，必须主动告诉 AI「这是并发场景 / 当前缺哪些约束 / 要根治而非缓解」，否则 AI 会按最常见的串行假设给出治标方案。这与 [`AI 代码信任度实验.md`](./AI%20代码信任度实验.md) 的结论一致：AI 的盲区不在语法，而在权限、状态机与跨表一致性。

## 9. 结论

- AI 在调试中是**高效的初筛与定位助手**，尤其擅长「堆栈明确、根因局部、约束已点名」的问题，能显著压缩定位耗时（BUG-002/004 由 10–12 分钟降到 2 分钟级）。
- AI 的主要短板是**并发与全局时序推理**，在缺乏场景上下文时倾向给出表象修复，存在「只治标」风险（BUG-003）。
- 最佳实践是「AI 定位 + 人工根因校验」：让 AI 快速缩小范围，由人工确认是否触及根因、是否覆盖并发与边界，再回喂上下文让 AI 完善实现。本实验中 3 个 Bug 的最终方案均为「AI 与人工协同」产物，无一例完全照搬 AI 首轮输出。

## 10. 调试阶段 Prompt 最佳实践（沉淀）

1. 一次性提供「报错堆栈 + 复现步骤 + 相关代码 + 表结构 / 约束现状」，不要挤牙膏。
2. 显式标注运行场景：是否并发、是否重复投递、是否跨请求、数据规模。
3. 提出根治要求并堵死治标路径（如「不要依赖前端防抖」「要在 DB 层杜绝」）。
4. 拿到 AI 方案后追问一句：「这个方案在并发 / 重复调用 / 异常中断下会不会失效？」用于逼出边界缺口。
5. 与 P4-005 接口契约、P3 状态机对照，确认错误码、状态流转一致后再合入。
