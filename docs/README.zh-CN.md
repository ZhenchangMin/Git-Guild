# Git Guild
<div align="center">

<h3>Language / 语言</h3>

[**English**](README.md) | [简体中文](docs/README.zh-CN.md)

</div>

> SEC II 课程项目

**一个游戏化的悬赏协作平台，让开源新手通过完成真实任务不断升级成长。**

Git-Guild 将“冒险者公会”的幻想体验带入开源世界。项目维护者发布任务（带奖励的 Quest），新手开发者接取任务、提交成果，并获得经验值（XP）、等级与徽章，把令人望而生畏的“第一次贡献”变成一场 RPG 冒险。



---

## 功能亮点

### 面向冒险者（初级开发者）

- 浏览 **任务看板（Quest Board）**，按难度、语言与奖励筛选任务
- 接取任务，提交 Pull Request 或代码，并接收审核反馈
- 通过 RPG 式成长系统获取 **经验值（XP）** 并 **提升等级**
- 收集里程碑 **徽章（Badge）**（首次任务、10 连续任务、语言专精等）
- 完成官方发布的教程和挑战任务，获得经验值和成就

### 面向公会导师（项目维护者）

- 发布结构化任务，包含描述、难度评级、标签与 XP 奖励
- 使用 approve / request-changes 工作流审核提交内容

### 平台能力

- 用户系统：注册、登录、个人资料管理
- **排行榜（Leaderboard）**：周榜、月榜与总榜
- 生涯统计：XP、等级、完成任务数、获得徽章等

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
