# GitGuild

**Where Open Source Meets Adventure**

## 软件需求规格说明书

Software Requirements Specification
IEEE Std 830-1998

- 版本：v1.0
- 日期：2026 年 4 月 9 日
- 课程：SEC II
- 仓库：<https://github.com/ZhenchangMin/Git-Guild>

---

## 修订历史

| 版本 | 日期 | 作者 | 说明 |
| --- | --- | --- | --- |
| 1.0 | 2026-04-09 | GitGuild 团队 | 初始版本，包含全部功能需求 |

---

## 1 引言

### 1.1 编写目的

本文档是 GitGuild 平台的软件需求规格说明书（Software Requirements Specification, SRS），按照 IEEE Std 830-1998 标准编写。本文档旨在完整、准确地描述 GitGuild 平台的功能需求、非功能需求、外部接口及约束条件，作为开发团队内部的技术参考与课程交付物。

### 1.2 项目背景

GitGuild 是一个融合"冒险家协会"游戏化世界观的代码托管与开源成长平台。平台基于 Gitea 提供完整的 Git 仓库托管能力，同时叠加任务悬赏、经验值/等级、徽章、排行榜等游戏化激励机制，解决开源生态中"项目维护者难以吸引贡献者"与"新手开发者缺乏实战路径与持续动力"两大痛点。

### 1.3 术语定义

| 术语 | 说明 |
| --- | --- |
| 冒险家 (Adventurer) | 平台上的开发者/贡献者角色，通过完成任务获得成长 |
| 委托人 (Guild Master) | 项目维护者角色，发布悬赏任务 |
| 悬赏/任务 (Quest) | 由维护者发布的结构化技术需求，对应一个或多个 Issue |
| 经验值 (XP) | 完成任务后获得的可量化成长数值 |
| 等级 (Rank) | 经验值累积触发的冷却等级：见习→青铜→白银→黄金→传说 |
| 徽章 (Badge) | 满足特定条件后解锁的成就标记 |
| Gitea | 开源自托管 Git 服务，提供仓库、Issue、PR 等基础功能 |
| PR (Pull Request) | 代码合并请求，用于代码贡献的审核流程 |
| Webhook | Gitea 在特定事件发生时向后端发送 HTTP 回调 |
| OAuth2 | 开放授权协议，用于用户通过 Gitea 账户登录平台 |

### 1.4 参考文献

- IEEE Std 830-1998: IEEE Recommended Practice for Software Requirements Specifications
- Gitea API Documentation: <https://docs.gitea.com/api/>
- Vue 3 Documentation: <https://vuejs.org/>
- Spring Boot Reference: <https://spring.io/projects/spring-boot>

### 1.5 文档概述

本文档共分五章。第 1 章为引言，介绍项目背景与术语；第 2 章为总体描述，说明产品视角、用户角色与约束；第 3 章详细列举功能需求；第 4 章描述非功能需求；第 5 章为附录。

---

## 2 总体描述

### 2.1 产品视角

GitGuild 是一个独立部署的 Web 平台，由三个核心子系统组成：

1. **代码托管子系统（基于 Gitea）**：提供 Git 仓库托管、Issue 跟踪、Pull Request、代码审查等基础能力。
2. **冒险家协会子系统（Spring Boot 后端）**：实现任务悬赏、经验值、等级、徽章、排行榜、智能匹配等游戏化功能。
3. **前端应用（Vue 3 SPA）**：统一的用户界面，整合代码托管与游戏化体验。

平台通过 Gitea REST API 与 Webhook 实现两个子系统的数据联动。用户通过 Gitea OAuth2 进行统一身份认证。

### 2.2 用户角色

| 角色 | 世界观名称 | 描述 | 典型操作 |
| --- | --- | --- | --- |
| 开发者 | 冒险家 Adventurer | 平台主要用户，包括开源新手和资深开发者 | 浏览/接取任务、提交 PR、获得 XP、升级、解锁徽章 |
| 项目维护者 | 委托人 Guild Master | 开源项目的拥有者或核心维护者 | 注册项目、发布悬赏、审核贡献、查看项目数据 |
| 平台管理员 | 协会管理员 | 负责平台运营与内容审核 | 审核内容、管理标签、查看数据看板、处理争议 |

### 2.3 产品功能概览

- **用户系统**：OAuth2 登录、角色管理、个人主页
- **代码托管**：Git 仓库 CRUD、在线代码浏览、Issue、Pull Request、代码审查
- **任务悬赏**：发布/接取/提交/审核任务，完整状态机
- **成长激励**：经验值、等级、徽章、排行榜
- **智能匹配**：技术栈+等级任务推荐、GitHub good-first-issue 爬取
- **社区互动**：任务讨论、项目评价、导师机制
- **数据看板**：平台运营数据、项目维护者数据

### 2.4 运行环境

| 类别 | 要求 |
| --- | --- |
| 服务器操作系统 | Linux (Ubuntu 22.04+) |
| 运行时 | JDK 17+, Node.js 18+, Docker 24+ |
| 数据库 | MySQL 8.0+, Redis 7+ |
| Git 服务 | Gitea 1.21+ |
| 客户端浏览器 | Chrome 90+, Firefox 90+, Edge 90+, Safari 15+ |
| 网络 | 服务器需开放 80/443 (HTTP/S)、22 (SSH)、3000 (Gitea) 端口 |

### 2.5 设计与实现约束

- 前端必须使用 Vue 3 + Vite + Element Plus 技术栈
- 后端必须使用 Spring Boot 3 + MyBatis-Plus
- 代码托管必须基于 Gitea 二次开发，不得重新实现 Git 协议层
- 部署必须支持 Docker Compose 一键启动
- 所有 API 接口遵循 RESTful 设计规范

### 2.6 假设与依赖

- 用户拥有基本的 Git 操作知识（clone、commit、push、PR）
- 服务器具备稳定的网络连接和足够的存储空间（≥ 20GB）
- Gitea 实例正常运行且 API 可达
- GitHub API 可用且未被限流（用于 good-first-issue 爬取）

---

## 3 具体需求

### 3.1 用户系统

#### 3.1.1 用户注册与登录

- **FR-USR-001** 系统应支持用户通过 Gitea OAuth2 进行登录，登录后自动在平台数据库创建用户记录。
- **FR-USR-002** 首次登录时，系统应自动从 Gitea 获取用户头像、用户名、邮箱等基本信息。
- **FR-USR-003** 首次登录时，系统应引导用户选择技术栈标签（如 Java、Python、Go、JavaScript 等）和兴趣领域。
- **FR-USR-004** 系统应支持用户选择角色："冒险家"（开发者）或"委托人"（维护者），一个用户可同时拥有两种角色。

#### 3.1.2 个人主页

- **FR-USR-005** 每个用户应拥有公开的个人主页，展示：当前等级与经验值进度条、已获得徽章墙、完成任务历史时间线、拥有/贡献的仓库列表。
- **FR-USR-006** 用户应可编辑个人简介、技术栈标签、社交链接。

### 3.2 代码托管功能

#### 3.2.1 仓库管理

- **FR-GIT-001** 系统应支持创建、删除、Fork 仓库，设置可见性（公开/私有）。
- **FR-GIT-002** 系统应支持通过 SSH 和 HTTPS 协议进行 git clone、push、pull 操作。
- **FR-GIT-003** 系统应提供在线代码浏览，支持目录导航、文件查看、语法高亮、分支切换。
- **FR-GIT-004** 系统应展示提交历史列表，包含提交消息、作者、时间、Diff 查看。

#### 3.2.2 Issue 管理

- **FR-GIT-005** 系统应支持创建、编辑、关闭 Issue，支持标签、指派人、里程碑。
- **FR-GIT-006** 系统应支持从 Issue 一键生成悬赏任务（Quest）。

#### 3.2.3 Pull Request

- **FR-GIT-007** 系统应支持创建、审查、合并 Pull Request，支持行级代码评论。
- **FR-GIT-008** 当 PR 被合并时，系统应通过 Webhook 自动触发关联任务的完成流程。

### 3.3 任务悬赏系统（Quest System）

#### 3.3.1 任务发布

- **FR-QST-001** 委托人应可创建悬赏任务，包含以下字段：任务标题、详细描述（Markdown）、难度等级（D/C/B/A/S/SS/SSS）、所需技术栈标签、预计工时、奖励经验值、关联 Issue。
- **FR-QST-002** 系统应根据难度等级自动建议 XP 奖励范围，委托人可在范围内调整。

#### 3.3.2 任务状态机

- **FR-QST-003** 任务应支持以下状态流转：

| 状态 | 英文 | 触发条件 | 下一状态 |
| --- | --- | --- | --- |
| 待接取 | Open | 委托人发布任务 | 进行中 / 已取消 |
| 进行中 | In Progress | 冒险家接取任务 | 待审核 / 已放弃 |
| 待审核 | Under Review | 冒险家提交 PR | 已完成 / 已退回 |
| 已完成 | Completed | PR 合并或委托人确认 | 终态 |
| 已退回 | Returned | 委托人要求修改 | 进行中 |
| 已放弃 | Abandoned | 冒险家主动放弃 | 待接取 |
| 已取消 | Cancelled | 委托人取消任务 | 终态 |

- **FR-QST-004** 任务完成后，系统应自动将奖励 XP 记入冒险家账户。
- **FR-QST-005** 每个任务同一时间只允许一名冒险家接取（防止重复劳动）。
- **FR-QST-006** 冒险家接取任务后超过预计工时的 3 倍未提交，系统应自动释放任务回"待接取"状态。

#### 3.3.3 任务大厅

- **FR-QST-007** 系统应提供任务大厅页面，支持按难度、技术栈、状态、XP 奖励范围进行筛选和排序。
- **FR-QST-008** 任务卡片应展示：标题、难度星级、技术栈标签、XP 奖励、发布者、发布时间。

### 3.4 成长激励系统

#### 3.4.1 经验值与等级

- **FR-GRW-001** 系统应定义以下等级体系：

| 等级 | 英文 | 所需累计 XP | 解锁权限 |
| --- | --- | --- | --- |
| 见习冒险家 | Apprentice | 0 | 可接取 D/C 级任务 |
| 青铜冒险家 | Bronze | 500 | 可接取 B 级任务 |
| 白银冒险家 | Silver | 2000 | 可接取 A 级任务 |
| 黄金冒险家 | Gold | 5000 | 可接取 S 级任务、解锁导师资格 |
| 传说冒险家 | Legendary | 15000 | 可接取 SS/SSS 级任务、专属标识 |

- **FR-GRW-002** 经验值只增不减，升级时系统应展示动画效果并发送系统通知。

#### 3.4.2 徽章系统

- **FR-GRW-003** 系统应实现以下预定义徽章（可扩展）：

| 徽章名称 | 解锁条件 |
| --- | --- |
| 初次贡献 (First Blood) | 完成第一个任务 |
| Bug 猎人 (Bug Hunter) | 完成 10 个 Bug 修复类任务 |
| 文档大师 (Doc Master) | 完成 5 个文档类任务 |
| 连续活跃 (Streak 7) | 连续 7 天每天至少提交一次 |
| 多面手 (Polyglot) | 使用 3 种不同语言完成任务 |
| 导师 (Mentor) | 成功指导 3 名新手完成任务 |
| 速度之星 (Speed Star) | 在预计工时 50% 内完成任务 |
| 百任斜杠 (Centurion) | 累计完成 100 个任务 |

- **FR-GRW-004** 徽章解锁时应弹出全屏动画提示，并在个人主页徽章墙永久展示。

#### 3.4.3 排行榜

- **FR-GRW-005** 系统应提供全局排行榜，支持按 XP 总量、本月 XP、任务完成数、连续活跃天数等维度排序。
- **FR-GRW-006** 排行榜数据应通过 Redis 缓存，每 5 分钟更新一次。

### 3.5 智能匹配与推荐

#### 3.5.1 任务匹配

- **FR-MAT-001** 系统应根据用户的技术栈标签、当前等级、历史完成记录，在任务大厅首页展示"推荐悬赏"区域。
- **FR-MAT-002** 匹配算法应基于标签交集得分 + 等级范围过滤，按匹配度降序排列。

#### 3.5.2 GitHub 爬取

- **FR-MAT-003** 系统应提供定时任务（每天一次），通过 GitHub API 爬取带有 good-first-issue、beginner-friendly、help-wanted 标签的 Issue。
- **FR-MAT-004** 爬取结果应存入平台数据库，在"发现"页面展示，支持按语言、星标数、更新时间筛选。

### 3.6 社区与互动

- **FR-COM-001** 每个任务应提供讨论区，支持 Markdown 格式、@提及用户、图片上传。
- **FR-COM-002** 冒险家完成任务后可对项目进行评价（文档质量、维护者响应速度、新手友好度，各 1-5 星）。
- **FR-COM-003** 黄金等级以上的冒险家可申请成为"导师"，与新手建立一对一指导关系。

### 3.7 Webhook 与自动化

- **FR-WHK-001** 系统应监听 Gitea Webhook 的以下事件：push、pull_request (opened/merged/closed)、issues (opened/closed)。
- **FR-WHK-002** 当与任务关联的 PR 被合并时，系统应自动将任务状态变更为"已完成"并结算 XP。
- **FR-WHK-003** 系统应在 XP 结算后自动检查是否触发升级或徽章解锁，并发送对应通知。

### 3.8 数据看板

- **FR-DSH-001** 平台管理员应可查看运营数据看板，包括：活跃用户数、任务完成率、热门技术栈分布、新增用户趋势。
- **FR-DSH-002** 项目维护者应可查看项目维度看板，包括：任务领取率、平均完成时长、贡献者画像、项目评分。

---

## 4 非功能需求

### 4.1 性能需求

- **NFR-PRF-001** 普通 API 请求响应时间应在 500ms 以内（P95）。
- **NFR-PRF-002** 任务大厅页面首屏加载时间应在 2 秒以内。
- **NFR-PRF-003** 排行榜查询应在 200ms 以内返回（基于 Redis 缓存）。
- **NFR-PRF-004** 系统应支持至少 100 个并发用户同时在线。

### 4.2 安全需求

- **NFR-SEC-001** 所有用户认证必须通过 OAuth2 协议，禁止明文存储密码。
- **NFR-SEC-002** API 接口必须进行身份验证和权限校验，禁止未授权访问。
- **NFR-SEC-003** Webhook 接收端点必须验证 Gitea 签名，防止伪造请求。
- **NFR-SEC-004** 敏感配置（数据库密码、API Token）必须通过环境变量注入，禁止硬编码。
- **NFR-SEC-005** 前端必须防范 XSS 攻击，所有用户输入内容必须进行转义处理。

### 4.3 可用性需求

- **NFR-AVL-001** 系统应支持通过 Docker Compose 一键部署与重启。
- **NFR-AVL-002** 数据库应配置自动备份策略（建议每日增量备份）。

### 4.4 可维护性需求

- **NFR-MNT-001** 代码必须遵循统一的编码规范（后端：Alibaba Java 规范；前端：ESLint + Prettier）。
- **NFR-MNT-002** 核心业务逻辑（任务状态机、XP 结算、徽章解锁）必须编写单元测试，覆盖率 ≥ 60%。
- **NFR-MNT-003** 所有 API 接口必须提供 Swagger/OpenAPI 文档。

### 4.5 可移植性需求

- **NFR-PRT-001** 系统必须能在 Docker 环境中运行，不依赖特定云厂商服务。
- **NFR-PRT-002** 前端应兼容主流现代浏览器（Chrome、Firefox、Edge、Safari 最近两个主版本）。

### 4.6 可扩展性需求

- **NFR-EXT-001** 徽章系统应采用规则引擎模式，新增徽章只需添加规则配置，无需修改代码。
- **NFR-EXT-002** 任务难度等级、等级体系参数应可配置，不应硬编码在业务逻辑中。

---

## 5 附录

### 5.1 数据库核心表设计（摘要）

以下为冒险家协会子系统的核心表结构（Gitea 自身数据库由 Gitea 管理，此处不列出）：

| 表名 | 主要字段 | 说明 |
| --- | --- | --- |
| user_profile | id, gitea_uid, username, email, avatar_url, tech_tags, xp, rank, created_at | 用户扩展信息（与 Gitea 用户关联） |
| quest | id, title, description, difficulty, tech_tags, xp_reward, status, issuer_id, assignee_id, issue_url, created_at, deadline | 悬赏任务 |
| quest_log | id, quest_id, user_id, action, timestamp | 任务状态变更日志 |
| badge_definition | id, name, description, icon_url, rule_type, rule_params | 徽章定义与解锁规则 |
| user_badge | id, user_id, badge_id, unlocked_at | 用户已获得徽章 |
| project_rating | id, project_repo, user_id, doc_score, response_score, beginner_score, comment, created_at | 项目评价 |
| github_issue_cache | id, repo_full_name, issue_number, title, labels, language, stars, fetched_at | GitHub 爬取缓存 |

### 5.2 API 接口规划（摘要）

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 认证 | GET | /api/auth/login | 跳转 Gitea OAuth2 登录 |
| 认证 | GET | /api/auth/callback | OAuth2 回调处理 |
| 用户 | GET | /api/users/{id}/profile | 获取用户主页数据 |
| 任务 | GET | /api/quests | 任务列表（支持筛选、分页） |
| 任务 | POST | /api/quests | 创建悬赏任务 |
| 任务 | POST | /api/quests/{id}/claim | 接取任务 |
| 任务 | POST | /api/quests/{id}/submit | 提交任务成果 |
| 任务 | POST | /api/quests/{id}/review | 审核任务 |
| 成长 | GET | /api/leaderboard | 排行榜数据 |
| 成长 | GET | /api/users/{id}/badges | 用户徽章列表 |
| 匹配 | GET | /api/quests/recommended | 推荐任务 |
| 发现 | GET | /api/discover/github | GitHub 好项目列表 |
| Webhook | POST | /api/webhooks/gitea | Gitea 事件接收 |

### 5.3 需求追溯矩阵

下表展示功能需求与系统模块的映射关系：

| 需求编号 | 模块 | 优先级 | 计划阶段 |
| --- | --- | --- | --- |
| FR-USR-001~004 | 用户系统 | 高 (P0) | Phase 1 |
| FR-USR-005~006 | 用户系统 | 中 (P1) | Phase 3 |
| FR-GIT-001~008 | 代码托管 | 高 (P0) | Phase 2 |
| FR-QST-001~008 | 任务悬赏 | 高 (P0) | Phase 3 |
| FR-GRW-001~006 | 成长激励 | 高 (P0) | Phase 3 |
| FR-MAT-001~004 | 智能匹配 | 中 (P1) | Phase 4 |
| FR-COM-001~003 | 社区互动 | 低 (P2) | Phase 5 |
| FR-WHK-001~003 | Webhook | 高 (P0) | Phase 3 |
| FR-DSH-001~002 | 数据看板 | 低 (P2) | Phase 5 |
