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

**Prerequisites:** Docker (Docker Desktop on Windows/macOS), JDK 17, Node 18+. No bash scripts — works
the same on **WSL, Windows PowerShell, macOS, and Linux**.

This repo ships a **byte-for-byte snapshot** of a ready-to-test MVP-start state under `seed/`
(a Gitea data volume + a MySQL dump), and the restore is baked **into the containers** — so a single
`docker compose up -d` brings up Gitea + MySQL already populated, with a Gitea token that already
matches. Nothing is generated at runtime; everyone gets the exact same state.

**Step 1 — infra + data (identical on every OS/shell):**

```bash
docker compose up -d        # Docker Desktop / Compose v2
# older standalone Compose v1:  docker-compose up -d
```

This starts MySQL (auto-loads `seed/mysql-seed.sql` on the empty volume) and Gitea (self-restores
`seed/gitea-data.tar.gz` into its volume on first start). Wait ~30s for Gitea to report healthy.

**Step 2 — backend** (new terminal; the demo Gitea token is already a config default, so no `.env` needed):

```bash
# WSL / macOS / Linux
cd backend && ./mvnw spring-boot:run
```
```powershell
# Windows PowerShell
cd backend ; .\mvnw.cmd spring-boot:run
```

**Step 3 — frontend** (new terminal):

```bash
cd frontend && npm install && npm run dev   # then open http://localhost:5173
```
```powershell
cd frontend ; npm install ; npm run dev
```

**Test accounts** — password `admin123` for all three:

| Role | Username | What they do |
| --- | --- | --- |
| Guild Master (maintainer) | `guild` | Publish quests, review & merge submissions |
| Adventurer (beginner) | `advent` | Accept a quest, clone & push, submit results |
| Admin | `admin` | Approve quests for publication |

**Walkthrough:** sign in as `guild` → open the Commission Office → **Publish Quest** (repo and issue are pre-filled) → sign in as `admin` → `/admin/review` → approve → sign in as `advent` → Quest Board → accept → workbench **Clone & Push** card (the clone URL already carries credentials, so `git push` won't prompt for a password) → make a commit and push → **Submission Counter** (a pull request is created automatically) → back as `guild` → review queue → approve (the PR is auto-merged and XP is granted).

> **Reset to the clean MVP start at any time:** `docker compose down -v && docker compose up -d`
> (`down -v` drops the volumes; the next `up` re-restores the snapshot from scratch.)

### Troubleshooting (Windows / proxy)

If services start but `localhost` connections fail — or you see a proxy/​WSL warning like
*"detected localhost proxy, WSL networking is NAT mode / not mirrored"* — your **system proxy is
hijacking `localhost`**. The backend (→ Gitea `localhost:3000`), Vite, and your browser all rely on
`localhost`, so fix it one of these ways:

- **Bypass `localhost` in your proxy client** — add `localhost,127.0.0.1,::1` to the proxy's
  *no-proxy / bypass* list (also Docker Desktop → *Settings → Resources → Proxies*).
- **Or switch WSL to mirrored networking** — create/edit `C:\Users\<you>\.wslconfig` with
  `[wsl2]` + `networkingMode=mirrored`, then run `wsl --shutdown` in PowerShell and reopen.

This is a local environment setting, independent of this repo — both PowerShell and WSL users need it
when a system proxy is on.

> **⚠️ Local demo only.** Gitea runs with a single admin account, and that admin token ships as a
> config default (`backend/.../application.yml`) and is embedded in the workbench clone URL so push is
> seamless. The token only unlocks the throwaway `localhost:3000` Gitea each teammate runs — it is the
> same disposable-secret category as the `admin123` demo password, **not** a production auth model.

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
