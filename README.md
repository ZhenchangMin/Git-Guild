# Git Guild

<div align="center">

<h3>Language / 语言</h3>

[**English**](README.md) |  [简体中文](docs/README.zh-CN.md)

</div>

>  Course project for SEC II

**A gamified bounty platform where open-source beginners level up by completing real-world tasks.**


**A gamified bounty platform where open-source beginners level up by completing real-world tasks.**

Git-Guild brings the Adventurer's Guild fantasy to open source. Project maintainers post quests (tasks with bounties), beginners accept them, submit their work, and earn XP, levels, and badges — turning the intimidating "first contribution" into an RPG adventure.



---

## Features

### For Adventurers (Beginner Developers)

- Browse a **Quest Board** with tasks filtered by difficulty, language, and reward
- Accept quests, submit pull requests or code, and receive reviewer feedback
- Earn **XP** and **level up** through an RPG-style progression system
- Collect **badges** for milestones (first quest, 10-quest streak, language mastery, etc.)
- Finish official tutorials and challenge quests for XP and achievements

### For Guild Masters (Project Maintainers)

- Post quests with structured descriptions, difficulty ratings, tags, and XP rewards
- Review submissions with approve / request-changes workflow

### Platform

- User system: registration, login, profile management
- **Leaderboard** — weekly, monthly, and all-time rankings
- Career stats: XP, level, quests completed, badges earned, etc.

---

## Current Repo Shape

| Layer    | Technology                             |
| -------- | -------------------------------------- |
| Frontend | Vue 3 + Vite                           |
| Backend  | Spring Boot 3                          |
| Database | MySQL                                  |
| DevOps   | Docker, Docker Compose, GitHub Actions |

---

## Current Project Structure

```
Git-Guild/
├── README.md
├── .gitignore
│
├── docs/                         # Documentation, design docs
│
├── .github/                      # GitHub Actions workflows
│
├── backend/                      # Spring Boot application
│
└── frontend/                     # Vue 3 application

```

---

## Contributing

We welcome contributions! Here's how to get started:

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Commit with conventional commits: `git commit -m "feat: add quest filter by language"`
4. Push and open a Pull Request

Please follow the existing code style and include tests for new features.

### Branch Naming Convention

| Prefix        | Purpose                  |
| ------------- | ------------------------ |
| `feat/`     | New feature              |
| `fix/`      | Bug fix                  |
| `docs/`     | Documentation            |
| `refactor/` | Code refactoring         |
| `test/`     | Adding or updating tests |

---

## Team

|    Member    |       Role       |
| :-----------: | :---------------: |
| Zhenchang Min | Requirements Lead |
|  Lvfan Yang  | Architecture Lead |
|   Wei Wang   | Development Lead |
|  Yongjun Tan  |   Testing Lead   |

---

<p align="center">
  <i>Every expert was once a beginner. Start your quest today.</i>
</p>
