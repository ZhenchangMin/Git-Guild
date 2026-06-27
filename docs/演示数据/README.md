# Git Guild 演示数据（真实数据填充）

> 本目录说明 Git Guild 的"丰富演示数据集"——通过真实 REST API + 真实 Gitea 全链路填充而成：
> 数十位用户、不同用户的委托任务（发布）与接取任务（接单），以及由此自动产生的提交、PR、
> XP、成长档案、贡献记录与站内通知。生成器与素材见仓库 `scripts/` 目录。

最后更新：2026-06-27

---

## 1. 这套数据是什么

`docs/P4/演示说明.md` 描述的是**干净最短起点**（3 用户、0 任务，演示时现场发布）。
本数据集则是一份**已经"活"起来**的演示数据：任务板、工作台、提交柜台、审核队列、
成长档案、排行榜都直接有真实内容，适合不想现场逐步操作、直接展示成品形态的场景。

数据通过 `scripts/seed_demo_data.py` 驱动真实接口生成，每条已完成任务都走完整链路：

```text
维护者发布 -> 管理员审核上架 -> 冒险者接取 -> 建任务分支 -> 真实提交
-> 自动创建 Gitea PR -> 维护者审核通过 -> 合并 PR -> XP / 成长 / 贡献 / 通知更新
```

仓库、Issue、分支、提交、PR 全部是平台 Gitea 中真实存在的对象（统一托管在 Gitea 管理员
`spike-admin` 名下）。

---

## 2. 数据清单

| 维度 | 内容 |
| --- | --- |
| 用户 | **35 个**：1 个 `ADMIN` + 11 个委托人 `MAINTAINER` + 23 个冒险家 `BEGINNER`，密码均 `admin123`，均带中文座右铭（`motto`） |
| 仓库 | 6 个新建真实 Gitea 仓库：`gitguild-web` / `gitguild-api` / `quest-cli` / `guild-docs` / `notify-service` / `leaderboard-svc` |
| 分类 | 前端开发 / 后端开发 / 文档工程 / 测试质量 / DevOps / 数据与存储 |
| 标签 | good-first-issue、feature、bugfix、refactor、docs、enhancement、urgent |
| 技术栈 | Java、Spring Boot、Spring Security、JPA、Vue、TypeScript、JavaScript、Vite、MySQL、Redis、Docker、Git、Markdown、Python、JUnit、Maven、REST API、Gitea |
| 委托任务 | **48 个**：`PUBLISHED` 26 / `COMPLETED` 13 / `IN_PROGRESS` 4 / `PENDING_ADMIN_REVIEW` 3 / `DRAFT` 2；中英双语标题与描述，真实难度（A–D）、奖励 XP、预估工时。悬赏板（PUBLISHED + IN_PROGRESS）共 **30 张可见卡片**，足以验证分页 / 筛选 / 布局可扩展性 |
| 接取记录 | **18 条**：`COMPLETED` 13 / `ACTIVE` 4 / `ABANDONED` 1（跨多名冒险者） |
| 成果提交 / PR | 13 份提交、真实 Gitea PR（11 已合并），含任务分支与提交历史 |
| 成长 / XP / 贡献 / 通知 | 12 份成长档案、13 笔 XP 流水、13 条贡献记录、约 89 条站内通知；排行榜与个人档案均非空 |

> 提示：审核通过即将任务标记 `COMPLETED` 并发放 XP；合并 PR 为 Gitea 侧收尾动作。

---

## 3. 登录账号

**所有账号密码均为 `admin123`**。固定入口账号：

| 角色 | 用户名 | 后端角色 |
| --- | --- | --- |
| 管理员 | `admin` | `ADMIN` |
| 委托人 | `guild` | `MAINTAINER` |
| 冒险家 | `advent` | `BEGINNER` |

公会风味人设账号（节选）：

- 委托人 / Guild Master：`guildmaster_ye`、`archmage_lin`、`warden_zhao`、`smith_huang`、`scribe_qian`、`captain_zhou` …
- 冒险家 / Adventurer：`novice_chen`、`archer_deng`、`sorcerer_yu`、`mage_apprentice_xu`、`scout_jiang`、`squire_liu` …

想看排行榜靠前、成长档案丰富的账号，可登录 `archer_deng` 或 `sorcerer_yu`。
完整人设见 [`scripts/demo_seed_data.json`](../../scripts/demo_seed_data.json)。

---

## 4. 如何生成 / 重新生成

### 方式 A：直接使用已固化的快照（推荐演示）

`seed/mysql-seed.sql` 与 `seed/gitea-data.tar.gz` 已重新导出为含本数据集的快照。
干净启动即自带全部数据：

```bash
docker compose down -v
docker compose up -d        # 空卷首启自动灌入丰富快照
# 待三容器 healthy 后启动后端 / 前端
```

### 方式 B：在干净库上用脚本重建

适合想从"3 用户 0 任务"的起点重新跑一遍真实链路：

```bash
# 1. 起干净栈
docker compose down -v && docker compose up -d
# 2. 启动后端（继承根 .env 的 GITEA_TOKEN）
cd backend && ./mvnw spring-boot:run
# 3. 另开终端，驱动填充
python3 scripts/seed_demo_data.py
```

脚本仅依赖 Python 标准库；用户名/邮箱唯一约束保证幂等，仓库按名复用。建议在较干净的库上
首次运行以获得最佳效果。

### 相关文件

| 文件 | 作用 |
| --- | --- |
| `scripts/seed_demo_data.py` | 数据驱动脚本（真实 API + Gitea 全链路） |
| `scripts/demo_seed_data.json` | 人设 / 仓库 / 任务素材 |
| `scripts/add_published_quests.py` | 批量追加"已上架"任务，扩充悬赏板（`python3 scripts/add_published_quests.py [数量]`） |
| `seed/mysql-seed.sql` | 重新导出的 MySQL 快照（含本数据集） |
| `seed/gitea-data.tar.gz` | 重新导出的 Gitea 数据卷快照（含 6 个新仓库） |

---

## 5. 设计说明

- **为何走真实 API 而非直接灌 SQL**：保证数据经过真实业务逻辑校验，且仓库 / Issue / 分支 / PR
  在 Gitea 中真实存在，工作台 clone→提交→PR→合并 链路可跑通。
- **为何不为每个用户单独建 Gitea 账号**：平台所有 Git 操作经管理员 `spike-admin` 的 token 完成
  （与既有工作方式一致），单独账号既无必要也会与 PR 作者校验冲突；任务与 PR 在 Gitea 中仍是真实的。
- **PR mergeable 异步竞态**：Gitea 在 PR 刚创建时异步计算可合并性，立即合并可能返回 HTTP 405；
  脚本的合并步骤已加退避重试。
- **人设风味**：用户名 / 座右铭借鉴 fabric `create_npc`（冒险者公会风格），任务标题与描述
  采用贴近真实软件工程的中英双语内容。
