# Git Guild

[English](README.md) | [简体中文](docs/README.zh-CN.md)

Git Guild is a course project for Software Engineering II. It combines a local Gitea-backed code collaboration workflow with an Adventurer's Guild style quest system: maintainers publish real engineering work as quests, adventurers complete those quests through branches and pull requests, and the platform records reviews, XP, growth profiles, notifications, and leaderboards.

P4 is the current integration stage. The project is no longer a static mock: the main user flows are wired to backend APIs, backed by MySQL, and integrated with a seeded local Gitea instance.

## Tech Stack


| Layer     | Technology                                            |
| ----------- | ------------------------------------------------------- |
| Frontend  | Vue 3, Vite, Vue Router                               |
| Backend   | Spring Boot 3, Spring Security, Spring Data JPA       |
| Database  | MySQL 8 for local development, H2 for backend tests   |
| Code host | Gitea 1.22 local container                            |
| Infra     | Docker Compose, Redis, GitHub Actions                 |
| Tests     | JUnit 5, Mockito, Spring MVC tests, integration tests |

## Repository Layout

```text
Git-Guild/
├── README.md                 # English project README
├── docs/                     # Course deliverables and design docs
│   ├── README.zh-CN.md       # Chinese project README
│   ├── P0/ ... P4/           # Milestone deliverables (charter, ADRs, design, logs)
│   ├── hci/                  # HCI / interaction design notes
│   └── 演示数据/             # Demo data notes
├── .github/workflows/        # GitHub Actions CI (ci.yml)
├── .gitlab-ci.yml            # GitLab CI mirror
├── docker-compose.yml        # Local dev
├── docker-compose.prod.yml   # Production compose
├── .env.example              # Optional local environment overrides
├── deploy/                   # Dockerfiles, nginx.conf, probe.sh
├── scripts/                  # CI static check + Python demo-seed scripts
├── seed/                     # Seeded MySQL dump + Gitea snapshot
├── backend/                  # Spring Boot 3 backend (own README)
└── frontend/                 # Vue 3 + Vite frontend (own README)
```

## Prerequisites

- Docker Desktop or Docker Engine with Compose v2. Compose v1 also works with `docker-compose`.
- JDK 17.
- Node.js 22 recommended, matching CI.
- npm.

## Local Quickstart

Start infrastructure from the repository root:

```bash
docker compose up -d
```

This starts:


| Service | URL / Port            | Notes                        |
| --------- | ----------------------- | ------------------------------ |
| Gitea   | http://localhost:3000 | Seeded demo code host        |
| MySQL   | localhost:3307        | Database`gitguild`           |
| Redis   | localhost:6379        | Notification/session support |

Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Start the frontend:

```bash
cd frontend
npm ci
npm run dev
```

Open the app at:

```text
http://localhost:5173
```

Backend API documentation is available after the backend starts:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/api-docs
```

## Seeded Demo Accounts

All seeded accounts use password `admin123`.


| Role       | Username | Typical Use                                                        |
| ------------ | ---------- | -------------------------------------------------------------------- |
| Admin      | `admin`  | Approve quest publication and manage platform configuration        |
| Maintainer | `guild`  | Import repositories, publish quests, review submissions, merge PRs |
| Adventurer | `advent` | Accept quests, push work, submit results, gain XP                  |

## Recommended Demo Walkthrough

1. Sign in as `guild`.
2. Open the maintainer workbench and import or sync a repository if needed.
3. Publish a quest from an existing repository issue.
4. Sign in as `admin` and approve the quest in the admin review page.
5. Sign in as `advent` and accept the quest from the quest board.
6. Open the workbench, prepare the task branch, clone the repository, commit, and push work.
7. Open the submission counter and submit the result. The backend creates or reuses a pull request.
8. Sign in as `guild`, open the review desk, review the submission, then merge the PR as a separate action.
9. Check the adventurer profile, notifications, and leaderboard updates.

Maintainers can also use the completion workbench to accept and finish quests with the same adventurer workflow.

## Reset Local Demo Data

The local demo state is restored from the committed seed snapshot when Docker volumes are empty.

```bash
docker compose down -v
docker compose up -d
```

`down -v` deletes local MySQL, Redis, and Gitea volumes. The next `up` recreates the seeded MVP state.

## Verification Commands

Run the same core checks used by CI:

```bash
node scripts/ci-static-check.mjs
```

```bash
cd frontend
npm ci
npm run build
```

```bash
cd backend
./mvnw -B verify
```

On Windows PowerShell for backend verification:

```powershell
cd backend
.\mvnw.cmd -B verify
```

## CI/CD

GitHub Actions runs on pushes to `main`, `P4`, `front`, `feat/**`, and `feature/**`, and on pull requests targeting `main` or `P4`.

The pipeline includes:

- Static and format checks through `scripts/ci-static-check.mjs`.
- Frontend dependency install and production build.
- Backend Maven verify and package.
- Frontend `dist/` artifact upload.
- Backend JAR and JaCoCo report artifact upload.

## Troubleshooting

### Localhost or Proxy Problems

If Gitea, backend, or Vite cannot connect through `localhost`, check whether a system proxy is intercepting local traffic. Add these addresses to the proxy bypass list:

```text
localhost,127.0.0.1,::1
```

For WSL users, mirrored networking can also help:

```text
[wsl2]
networkingMode=mirrored
```

Then run `wsl --shutdown` from PowerShell and restart WSL.

### Gitea Token

The backend has a default token matching the seeded local Gitea snapshot. It is for local demo use only. Replace it with `GITEA_TOKEN` when using a different Gitea instance.

### Ports Already in Use

Default ports are:

- Frontend dev server: `5173`
- Frontend preview server: `4173`
- Backend: `8080`
- Gitea: `3000`
- MySQL: `3307`
- Redis: `6379`

Change local service ports or stop conflicting processes if a port is occupied.

## Team


| Member        | Responsibility |
| --------------- | ---------------- |
| Zhenchang Min | Requirements   |
| Lvfan Yang    | Architecture   |
| Wei Wang      | Development    |
| Yongjun Tan   | Testing        |

---

Every expert was once a beginner. Start your quest today.
