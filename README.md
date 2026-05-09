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

## Tech Stack

| Layer      | Technology                                                        |
| ---------- | ----------------------------------------------------------------- |
| Frontend   | Vue 3 + Vite + Element Plus + Vue Router + Pinia + Axios          |
| Backend    | Spring Boot 3 + Spring Security + JWT + JPA                       |
| Database   | MySQL 8                                                           |
| Cache      | Redis 7                                                           |
| Code Host  | Gitea 1.22                                                        |
| DevOps     | Docker Compose + GitHub Actions                                   |

---

## Quick Start

### Prerequisites

- [Docker Desktop](https://docs.docker.com/get-docker/) (or Docker + Docker Compose)
- Java 17 (e.g. [Temurin](https://adoptium.net/))
- Node.js 20+

### 1. Clone & configure environment

```bash
git clone <repo-url> && cd Git-Guild
cp .env.example .env          # edit passwords if needed
```

### 2. Start infrastructure

```bash
docker compose up -d
# wait ~30s for all services to be healthy
docker compose ps             # confirm mysql / redis / gitea are "healthy"
```

### 3. Start backend

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

### 4. Start frontend

```bash
cd frontend
npm install
npm run dev
```

App: <http://localhost:5173>

### 5. Initialize Gitea (first time only)

Open <http://localhost:3000>, complete the setup wizard, create an admin account, then generate an API token and add it to `.env` as `GITEA_TOKEN`.

---

## Project Structure

```
Git-Guild/
├── docker-compose.yml          # MySQL 8 + Redis 7 + Gitea 1.22
├── .env.example                # environment variable template
├── .github/workflows/          # CI (all branches) + Main pipeline
│
├── backend/                    # Spring Boot 3 application
│   └── src/main/java/com/gitguild/backend/
│       ├── common/             # unified response, global exception
│       ├── user/               # auth, registration, roles, JWT
│       ├── codehost/           # GitHub/Gitea adapter, webhooks
│       ├── quest/              # quest board, bounties, state machine
│       ├── recommendation/     # matching engine, ranking
│       ├── review/             # submission, review workflow
│       ├── guide/              # onboarding, templates, project explainer
│       ├── notification/       # in-app + email notifications
│       └── growth/             # XP, levels, badges, leaderboard (P1/P2)
│
├── frontend/                   # Vue 3 SPA
│   └── src/
│       ├── api/                # Axios instance + endpoint calls
│       ├── router/             # Vue Router (history mode)
│       ├── stores/             # Pinia (auth store + JWT persistence)
│       └── views/              # page-level components
│
└── docs/                       # P1 requirements · P2 architecture · P3 design
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
