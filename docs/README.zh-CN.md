# Git Guild
<div align="center">

<h3>Language / 语言</h3>

[**English**](README.md) | [简体中文](docs/README.zh-CN.md)

</div>

> SEC II 课程项目

**一个融合代码托管与游戏化悬赏协作的平台，让开源新手通过真实任务持续升级成长。**

Git-Guild 将 **GitHub/Gitee 风格的代码托管能力**（仓库、Issue、Pull Request、代码评审）与“冒险者公会”成长体系结合。  
项目维护者可以从真实工程工作中发布任务，新手开发者可以接取任务、提交代码、获得反馈，并通过 XP、等级和徽章完成成长闭环。



---

## 功能亮点

### 代码托管能力（GitHub/Gitee 风格）

- 创建/导入仓库，管理公开性与协作者
- 在 Web 页面浏览分支、提交记录、文件与差异（Diff）
- 提供 Issue + Pull Request 协作流程与评审评论
- 通过 Webhook 事件实现自动化与任务状态联动

### 面向冒险者（初级开发者）

- 浏览 **任务看板（Quest Board）**，按难度、技术栈、奖励与标签筛选任务
- 接取与真实 Issue 关联的任务并提交 PR/代码
- 获取可执行的审核反馈与学习建议
- 通过 RPG 式成长系统获得 **XP**、升级并解锁徽章

### 面向公会导师（项目维护者）

- 将仓库中的 Issue 结构化发布为任务
- 设置完成标准、奖励规则与评审检查点
- 使用 approve / request-changes 工作流审核提交内容

### 平台与成长能力

- 用户系统：注册、登录、个人资料管理
- **排行榜（Leaderboard）**：周榜、月榜与总榜
- 生涯统计：XP、等级、完成任务数、获得徽章等
- 新手引导：贡献流程说明、模板与教程支持

## 开发状态

- `进行中`：代码托管核心能力（Repo/Issue/PR 集成）、任务流程、XP 成长系统
- `规划中`：推荐引擎、导师机制、高级数据看板

---

## 当前仓库技术栈

| 层级 | 技术 |
| ---- | ---- |
| 前端 | Vue 3 + Vite |
| 后端 | Spring Boot 3 |
| 数据库 | MySQL |
| DevOps | Docker、Docker Compose、GitHub Actions |

---

## 当前项目结构

```
Git-Guild/
├── README.md
├── .gitignore
│
├── docs/                         # 文档与设计资料
│
├── .github/                      # GitHub Actions 工作流
│
├── backend/                      # Spring Boot 应用
│
└── frontend/                     # Vue 3 应用

```

---

## 贡献指南

欢迎贡献！你可以按以下步骤开始：

1. Fork（复制）本仓库
2. 创建功能分支：`git checkout -b feat/your-feature`
3. 使用 Conventional Commits 提交：`git commit -m "feat: add quest filter by language"`
4. 推送并发起 Pull Request（合并请求）

请遵循现有代码风格，并为新功能补充测试。

### 分支命名规范

| 前缀 | 用途 |
| ---- | ---- |
| `feat/` | 新功能 |
| `fix/` | Bug 修复 |
| `docs/` | 文档更新 |
| `refactor/` | 代码重构 |
| `test/` | 新增或更新测试 |

---

## 团队

|  成员  |    角色    |
| :----: | :--------: |
| 闵振昌 | 需求负责人 |
| 阳旅帆 | 架构负责人 |
|  王炜  | 开发负责人 |
| 谭咏俊 | 测试负责人 |

---

<p align="center">
  <i>每位专家都曾是初学者。今天就开启你的冒险任务吧。</i>
</p>
