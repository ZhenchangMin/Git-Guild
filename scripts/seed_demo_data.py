#!/usr/bin/env python3
"""Git-Guild 演示数据驱动脚本（真实 API + 真实 Gitea 全链路）。

通过后端 REST API 与平台 Gitea，把 scripts/demo_seed_data.json 里的人设/仓库/任务
灌成"活"的业务数据：数十位用户、各维护者发布的委托任务（多状态）、冒险者接取与
完成任务（真实分支 / 提交 / PR / 合并），并自动产出 XP、成长档案、贡献、通知。

前置：
  - docker compose 已起（gitguild-mysql / gitguild-gitea / gitguild-redis healthy）
  - 后端已在 http://localhost:8080 运行（cd backend && ./mvnw spring-boot:run）
  - 根 .env 的 GITEA_TOKEN 已注入后端（默认演示快照 token 即可）

用法：
  python3 scripts/seed_demo_data.py

幂等性：用户名/邮箱唯一，已存在则改走登录复用；仓库按名复用。完整闭环部分按"尽力而为"
执行，单条失败不影响其余（建议在较干净的库上首次运行以获得最佳效果）。
仅依赖标准库。
"""
import json
import os
import sys
import urllib.request
import urllib.error
from pathlib import Path

BACKEND = os.environ.get("GG_BACKEND", "http://localhost:8080")
HERE = Path(__file__).resolve().parent
DATA_FILE = HERE / "demo_seed_data.json"

# docker exec 直连 MySQL，用于 register 接口覆盖不到的字段（motto / role / 个别状态）。
MYSQL_CONTAINER = os.environ.get("GG_MYSQL_CONTAINER", "gitguild-mysql")
MYSQL_USER = os.environ.get("GG_MYSQL_USER", "gitguild")
MYSQL_PASS = os.environ.get("GG_MYSQL_PASS", "gitguild123")
MYSQL_DB = os.environ.get("GG_MYSQL_DB", "gitguild")

import subprocess

CHECKLIST_6 = [
    {"label": "标题清晰", "passed": True},
    {"label": "描述完整", "passed": True},
    {"label": "完成标准可验证", "passed": True},
    {"label": "技术栈合规", "passed": True},
    {"label": "奖励合理", "passed": True},
    {"label": "无敏感信息", "passed": True},
]
REVIEW_ITEMS = [
    {"checkpoint": "功能符合验收标准", "comment": "已对照完成标准逐项核对。", "passed": True},
    {"checkpoint": "代码质量", "comment": "结构清晰，命名规范。", "passed": True},
    {"checkpoint": "测试覆盖", "comment": "关键路径有测试覆盖。", "passed": True},
]


# ----------------------------- HTTP helpers ----------------------------------
def _req(method, path, token=None, body=None):
    url = path if path.startswith("http") else BACKEND + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode()
            return resp.status, (json.loads(raw) if raw else {})
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}
    except urllib.error.URLError as e:
        return 0, {"error": str(e)}


def post(path, token=None, body=None):
    return _req("POST", path, token, body)


def get(path, token=None):
    return _req("GET", path, token)


def data_of(resp):
    return resp.get("data", resp) if isinstance(resp, dict) else resp


def page_items(body):
    """从分页或列表响应中稳健取出条目列表（兼容 {data:{items:[]}} / {data:[]} / 错误体）。"""
    d = data_of(body)
    if isinstance(d, list):
        return d
    if isinstance(d, dict) and isinstance(d.get("items"), list):
        return d["items"]
    return []


# ----------------------------- MySQL helper ----------------------------------
def mysql(sql):
    cmd = ["docker", "exec", MYSQL_CONTAINER, "mysql",
           f"-u{MYSQL_USER}", f"-p{MYSQL_PASS}", MYSQL_DB, "-N", "-e", sql]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"  ! mysql 失败: {r.stderr.strip()[:200]}")
    return r.stdout.strip()


def sql_str(s):
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


# ----------------------------- domain steps ----------------------------------
def login(account, password):
    st, body = post("/api/v1/auth/login", body={"account": account, "password": password})
    if st == 200:
        d = data_of(body)
        return d["accessToken"], d["user"]
    return None, None


def register_or_login(username, email, password):
    st, body = post("/api/v1/auth/register",
                    body={"username": username, "email": email, "password": password})
    if st == 201:
        uid = data_of(body)["userId"]
        token, _ = login(username, password)
        return uid, token, "created"
    # 已存在或校验冲突 → 尝试登录复用
    token, user = login(username, password)
    if token:
        return user["userId"], token, "reused"
    print(f"  ! 用户 {username} 注册失败且无法登录: {st} {body}")
    return None, None, "failed"


def ensure_taxonomy(admin_token, data):
    # categories
    _, cats = get("/api/v1/quest-categories?includeDisabled=true")
    cat_map = {c["name"]: c["categoryId"] for c in page_items(cats)}
    for c in data["categories"]:
        if c["name"] not in cat_map:
            st, body = post("/api/v1/quest-categories", admin_token,
                            {"name": c["name"], "description": c["description"]})
            if st in (200, 201):
                cat_map[c["name"]] = data_of(body)["categoryId"]
                print(f"  + 分类 {c['name']}")
    # tags
    _, tags = get("/api/v1/quest-tags?includeDisabled=true&size=100")
    tag_map = {t["name"]: t["tagId"] for t in page_items(tags)}
    for t in data["tags"]:
        if t["name"] not in tag_map:
            st, body = post("/api/v1/quest-tags", admin_token,
                            {"name": t["name"], "color": t["color"]})
            if st in (200, 201):
                tag_map[t["name"]] = data_of(body)["tagId"]
                print(f"  + 标签 {t['name']}")
    # tech stacks
    _, stacks = get("/api/v1/quest-tech-stacks?includeDisabled=true&size=100")
    stack_names = {s["name"].lower() for s in page_items(stacks)}
    for name in data["techStacks"]:
        if name.lower() not in stack_names:
            st, _ = post("/api/v1/quest-tech-stacks", admin_token, {"name": name})
            if st in (200, 201):
                stack_names.add(name.lower())
                print(f"  + 技术栈 {name}")
    return cat_map, tag_map


def ensure_repo(owner_token, name, description):
    _, existing = get("/api/v1/repositories", owner_token)
    for r in data_of(existing):
        if r["name"] == name:
            return r
    st, body = post("/api/v1/repositories", owner_token,
                    {"name": name, "description": description})
    if st in (200, 201):
        return data_of(body)
    print(f"  ! 仓库 {name} 创建失败: {st} {body}")
    return None


def create_quest(pub_token, repo_id, cat_id, tag_ids, q):
    body = {
        "repositoryId": repo_id,
        "categoryId": cat_id,
        "giteaIssueTitle": q["issueTitle"],
        "giteaIssueBody": q["description"],
        "title": q["title"],
        "description": q["description"],
        "completionCriteria": q["completionCriteria"],
        "difficulty": q["difficulty"],
        "techStack": q["techStack"],
        "estimatedHours": q["estimatedHours"],
        "rewardXp": q["rewardXp"],
        "tagIds": tag_ids,
    }
    st, resp = post("/api/v1/quests", pub_token, body)
    if st in (200, 201):
        return data_of(resp)["questId"]
    print(f"  ! 任务创建失败 [{q['title']}]: {st} {resp}")
    return None


def submit_quest(pub_token, qid):
    return post(f"/api/v1/quests/{qid}/submit", pub_token)[0] in (200, 201)


def admin_decision(admin_token, qid, approve, reason):
    body = {"decision": "APPROVE_PUBLISH" if approve else "REJECT_PUBLISH",
            "reason": reason, "visibleToPublisher": True}
    if approve:
        body["checklist"] = CHECKLIST_6
    st, resp = post(f"/api/v1/quests/{qid}/admin-reviews", admin_token, body)
    return st in (200, 201), resp


def accept_quest(adv_token, qid):
    st, resp = post(f"/api/v1/quests/{qid}/assignments", adv_token)
    if st in (200, 201):
        return data_of(resp)
    return None


def complete_chain(adv_token, owner_token, qid, repo_id, q, summary_counts):
    # 1) 任务分支（真实 Gitea 分支）
    st, resp = post(f"/api/v1/quests/{qid}/task-branch", adv_token)
    if st not in (200, 201):
        print(f"    ! task-branch 失败 q{qid}: {st} {resp}")
        return False
    task_branch = data_of(resp).get("taskBranch")
    # 2) 在任务分支上真实提交一个文件，使 PR 有差异
    commit_body = {
        "branch": task_branch,
        "message": f"feat: 完成任务 #{qid} {q['title'][:40]}",
        "path": f"solutions/quest-{qid}.md",
        "content": f"# {q['title']}\n\n{q['completionCriteria']}\n\n由冒险者通过 Git Guild 提交。\n",
    }
    st, resp = post(f"/api/v1/repositories/{repo_id}/commits", owner_token, commit_body)
    if st not in (200, 201):
        print(f"    ! commit 失败 q{qid}: {st} {resp}")
        return False
    # 3) 创建提交（自动建 PR），任务转 IN_REVIEW
    sub_body = {
        "questId": qid,
        "description": f"已按完成标准实现并自测：{q['completionCriteria'][:60]}",
        "checklist": ["功能完成", "本地自测通过", "符合完成标准"],
    }
    st, resp = post("/api/v1/submissions", adv_token, sub_body)
    if st not in (200, 201):
        print(f"    ! submission 失败 q{qid}: {st} {resp}")
        return False
    submission_id = data_of(resp)["submissionId"]
    summary_counts["submissions"] += 1
    # 4) 维护者审核通过
    rev_body = {"decision": "APPROVED", "summary": "实现完整，验收通过，准予合并。",
                "items": REVIEW_ITEMS}
    st, resp = post(f"/api/v1/submissions/{submission_id}/reviews", owner_token, rev_body)
    if st not in (200, 201):
        print(f"    ! review 失败 q{qid}: {st} {resp}")
        return False
    # 5) 合并 PR，任务转 COMPLETED，发放 XP。
    #    Gitea 的 mergeable 是异步计算的，PR 刚建好立即合并可能返回 405；带退避重试。
    import time
    merged = False
    for _ in range(6):
        st, resp = post(f"/api/v1/submissions/{submission_id}/merge", owner_token)
        if st in (200, 201):
            merged = True
            break
        time.sleep(2)
    # 审核通过即已将任务标记 COMPLETED 并发放 XP（merge 仅收尾 Gitea PR）。
    summary_counts["completed"] += 1
    if merged:
        summary_counts["merged"] += 1
    else:
        print(f"    ! merge 重试后仍失败 q{qid}（任务已 COMPLETED，PR 仍 OPEN）: {st} {resp}")
    return merged


# ----------------------------- main ------------------------------------------
def main():
    data = json.loads(DATA_FILE.read_text(encoding="utf-8"))
    password = data["password"]

    print("== 0. 健康检查 ==")
    admin_token, admin_user = login("admin", "admin123")
    if not admin_token:
        print("后端未就绪或 admin 登录失败。请确认 ./mvnw spring-boot:run 已启动。")
        sys.exit(1)
    print(f"  admin 登录成功 (userId={admin_user['userId']})")

    print("== 1. 分类 / 标签 / 技术栈 ==")
    cat_map, tag_map = ensure_taxonomy(admin_token, data)

    print("== 2. 用户（维护者 + 冒险者）==")
    users = {}  # username -> {userId, token, role, motto}
    for p in data["maintainers"]:
        uid, token, how = register_or_login(p["username"], p["email"], password)
        if uid:
            users[p["username"]] = {"userId": uid, "token": token, "role": "MAINTAINER", "motto": p["motto"]}
    for p in data["adventurers"]:
        uid, token, how = register_or_login(p["username"], p["email"], password)
        if uid:
            users[p["username"]] = {"userId": uid, "token": token, "role": "BEGINNER", "motto": p["motto"]}
    print(f"  就绪用户 {len(users)} 位（维护者 {len(data['maintainers'])} / 冒险者 {len(data['adventurers'])}）")

    # register 接口覆盖不到的字段：座右铭 + 冒险者降级为 BEGINNER
    print("== 2b. 回填 motto 与角色（SQL）==")
    for uname, u in users.items():
        mottos = sql_str(u["motto"])
        if u["role"] == "BEGINNER":
            mysql(f"UPDATE users SET motto={mottos}, role='BEGINNER' WHERE username={sql_str(uname)};")
        else:
            mysql(f"UPDATE users SET motto={mottos} WHERE username={sql_str(uname)};")

    print("== 3. 仓库（真实 Gitea 仓库）==")
    repos = {}  # name -> {id, owner_token, owner, defaultBranch}
    for r in data["repos"]:
        owner = users.get(r["owner"])
        if not owner:
            continue
        info = ensure_repo(owner["token"], r["name"], r["description"])
        if info:
            repos[r["name"]] = {
                "id": info["repositoryId"],
                "owner_token": owner["token"],
                "owner": r["owner"],
                "defaultBranch": info.get("defaultBranch", "main"),
            }
            print(f"  + 仓库 {r['name']} (id={info['repositoryId']}, owner={r['owner']})")

    print("== 4 & 5. 委托任务发布 + 接取 + 完成 ==")
    counts = {"quests": 0, "published": 0, "draft": 0, "pending": 0,
              "rejected": 0, "assignments": 0, "submissions": 0,
              "merged": 0, "completed": 0, "active": 0, "abandoned": 0}
    adventurer_names = [p["username"] for p in data["adventurers"]]
    adv_idx = 0

    for q in data["quests"]:
        repo = repos.get(q["repo"])
        if not repo:
            continue
        pub_token = repo["owner_token"]
        cat_id = cat_map.get(q["category"])
        tag_ids = [tag_map[t] for t in q.get("tags", []) if t in tag_map]
        qid = create_quest(pub_token, repo["id"], cat_id, tag_ids, q)
        if not qid:
            continue
        counts["quests"] += 1
        outcome = q["outcome"]

        if outcome == "draft":
            counts["draft"] += 1
            print(f"  · DRAFT  q{qid} {q['title'][:30]}")
            continue

        if outcome == "pending":
            submit_quest(pub_token, qid)
            counts["pending"] += 1
            print(f"  · PENDING q{qid} {q['title'][:30]}")
            continue

        if outcome == "rejected":
            submit_quest(pub_token, qid)
            admin_decision(admin_token, qid, approve=False, reason="完成标准过于笼统，请细化验收点后重新提交。")
            counts["rejected"] += 1
            print(f"  · REJECT q{qid} {q['title'][:30]}")
            continue

        # published / active / complete → submit + approve → PUBLISHED
        submit_quest(pub_token, qid)
        ok, _ = admin_decision(admin_token, qid, approve=True, reason="信息完整，技术栈合规，准予上架。")
        if not ok:
            print(f"  ! 审核上架失败 q{qid}")
            continue
        counts["published"] += 1

        if outcome == "published":
            print(f"  · PUBLISHED q{qid} {q['title'][:30]}")
            continue

        # 选一位非发布者的冒险者接取
        assignee = None
        for _ in range(len(adventurer_names)):
            cand = users[adventurer_names[adv_idx % len(adventurer_names)]]
            adv_idx += 1
            if cand["userId"] != users[repo["owner"]]["userId"]:
                assignee = cand
                break
        if not assignee:
            continue
        asg = accept_quest(assignee["token"], qid)
        if not asg:
            print(f"  ! 接取失败 q{qid}")
            continue
        counts["assignments"] += 1

        if outcome == "active":
            counts["active"] += 1
            print(f"  · ACTIVE q{qid} 接取者 {assignee['userId']}")
            continue

        if outcome == "abandoned":
            mysql(f"UPDATE quest_assignments SET status='ABANDONED' "
                  f"WHERE quest_id={qid} AND assignee_id={assignee['userId']};")
            mysql(f"UPDATE quests SET status='PUBLISHED' WHERE quest_id={qid};")
            counts["abandoned"] += 1
            print(f"  · ABANDONED q{qid}")
            continue

        if outcome == "complete":
            done = complete_chain(assignee["token"], repo["owner_token"], qid, repo["id"], q, counts)
            tag = "COMPLETED" if done else "IN_REVIEW(部分失败)"
            print(f"  · {tag} q{qid} {q['title'][:24]} 接取者 {assignee['userId']}")

    print("\n==================== 汇总 ====================")
    print(f"  用户就绪          : {len(users)}")
    print(f"  仓库              : {len(repos)}")
    print(f"  任务创建          : {counts['quests']}")
    print(f"    已上架 PUBLISHED: {counts['published']}（含进行中/已完成）")
    print(f"    草稿 DRAFT      : {counts['draft']}")
    print(f"    待审 PENDING    : {counts['pending']}")
    print(f"    已驳回 REJECTED : {counts['rejected']}")
    print(f"  接取记录          : {counts['assignments']}")
    print(f"    进行中 ACTIVE   : {counts['active']}")
    print(f"    放弃 ABANDONED  : {counts['abandoned']}")
    print(f"  提交 submissions  : {counts['submissions']}")
    print(f"  合并 merged PR    : {counts['merged']}")
    print(f"  完成 COMPLETED    : {counts['completed']}")
    print("=============================================")
    print("提示：完成的任务会自动产生 XP 流水 / 成长档案 / 贡献记录 / 通知。")


if __name__ == "__main__":
    main()
