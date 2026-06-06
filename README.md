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

## Quickstart — Try the Shortest MVP Path

Run the full **real-Gitea** workflow locally: a maintainer publishes a quest → an admin approves it → an adventurer accepts, clones & pushes code → submits → the maintainer approves and the PR is auto-merged.

**Prerequisites:** Docker + Docker Compose, JDK 17, Node 18+.

This repo ships a **byte-for-byte snapshot** of a ready-to-test MVP-start state under `seed/`
(a Gitea data volume + a MySQL dump + a matching `.env`), so nothing is generated at runtime —
everyone gets the exact same state with a Gitea token that already matches.

```bash
# 1. Restore the demo snapshot (writes .env, loads the Gitea volume, starts MySQL+Redis+Gitea)
bash scripts/bootstrap.sh

# 2. Backend — Terminal A (auto-loads .env)
cd backend && bash run-dev.sh

# 3. Frontend — Terminal B
cd frontend && npm install && npm run dev
# then open http://localhost:5173
```

**Test accounts** — password `admin123` for all three:

| Role | Username | What they do |
| --- | --- | --- |
| Guild Master (maintainer) | `guild` | Publish quests, review & merge submissions |
| Adventurer (beginner) | `advent` | Accept a quest, clone & push, submit results |
| Admin | `admin` | Approve quests for publication |

**Walkthrough:** sign in as `guild` → open the Commission Office → **Publish Quest** (repo and issue are pre-filled) → sign in as `admin` → `/admin/review` → approve → sign in as `advent` → Quest Board → accept → workbench **Clone & Push** card (the clone URL already carries credentials, so `git push` won't prompt for a password) → make a commit and push → **Submission Counter** (a pull request is created automatically) → back as `guild` → review queue → approve (the PR is auto-merged and XP is granted).

> **Reset to the clean MVP start at any time:** `docker compose down -v && bash scripts/bootstrap.sh`
> (`down -v` drops the volumes so the snapshot is restored fresh.)
>
> **Rebuild from scratch instead of the snapshot** (generates a brand-new Gitea admin + token):
> `cp .env.example .env && docker compose up -d && bash scripts/setup-gitea.sh`, then start the
> backend and run `bash scripts/seed-platform.sh`. Use this only if you want a clean-room setup.

> **⚠️ Local demo only.** Gitea runs with a single admin account, and both the committed snapshot
> token (`seed/demo.env`) and the workbench clone URL embed that admin token so push is seamless.
> The token only unlocks the throwaway `localhost:3000` Gitea each teammate runs — it is the same
> disposable-secret category as the `admin123` demo password, **not** a production auth model.

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
