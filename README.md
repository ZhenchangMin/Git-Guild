# Git Guild

<div align="center">

<h3>Language / 语言</h3>

[**English**](README.md) |  [简体中文](docs/README.zh-CN.md)

</div>

> Course project for SEC II

**A gamified code hosting and bounty collaboration platform where open-source beginners level up by completing real-world tasks.**

Git-Guild combines **GitHub/Gitee-style code hosting** (repositories, issues, pull requests, code review) with an **Adventurer's Guild** progression system.  
Project maintainers can publish quests from real engineering work, and beginners can pick tasks, submit code, receive feedback, and grow through XP, levels, and badges.



---

## Features

### Repository Hosting (GitHub/Gitee-style)

- Create/import repositories and manage visibility/collaborators
- Browse branches, commits, files, and diffs in the web UI
- Use Issue + Pull Request workflows with reviewer comments
- Integrate webhook events for automation and quest status sync

### For Adventurers (Beginner Developers)

- Browse a **Quest Board** filtered by difficulty, language, reward, and tags
- Accept quests linked to real issues and submit pull requests/code
- Receive reviewer feedback and follow suggested learning paths
- Earn **XP**, level up, and unlock milestone badges

### For Guild Masters (Project Maintainers)

- Publish structured quests from repository issues
- Define completion criteria, rewards, and review checkpoints
- Review submissions with approve / request-changes workflow

### Platform & Growth

- User system: registration, login, profile management
- **Leaderboard**: weekly, monthly, all-time rankings
- Career stats: XP, level, quest completion, badge collection
- Beginner onboarding: contribution guide, templates, tutorials

## Development Status

- `In Progress`: code hosting core (repo/issue/PR integration), quest workflow, XP system
- `Planned`: recommendation engine, mentor mode, advanced analytics dashboard

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
