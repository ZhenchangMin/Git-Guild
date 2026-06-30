#!/usr/bin/env python3
"""为演示账户 warden_zhao（委托人 / MAINTAINER, user_id=14, repo gitguild-api=7）
补齐"委托人视角"每个流程的真实数据：

  1. 待发布委托    -> DRAFT（起草中，尚未提交管理员审核）
  2. 待完成委托    -> IN_PROGRESS（已上架并被冒险者接取，等待其完成）
  3. 待委托者审核  -> IN_REVIEW + 提交 PENDING_REVIEW（冒险者已提交成果，等 warden_zhao 在提交柜台审核）

全部经真实 REST API + 真实 Gitea 驱动，复用 scripts/seed_demo_data.py 的 helpers。
幂等性一般，建议只跑一次；可用 --skip 数量微调。
用法：python3 scripts/demo_warden_flows.py
"""
import sys
from seed_demo_data import (login, get, post, data_of, page_items,
                            create_quest, submit_quest, admin_decision, accept_quest)

PWD = "admin123"
WARDEN = "warden_zhao"
REPO_NAME = "gitguild-api"

# 候选冒险者（接取/提交用，非发布者）。密码均 admin123。
ADVENTURERS = ["novice_chen", "archer_deng", "sorcerer_yu", "squire_liu",
               "knight_cao", "scout_jiang", "rogue_tang", "warrior_ma"]

# 三类待造委托（贴近 gitguild-api 后端真实工程，中英双语）。
DRAFTS = [
    {"title": "接口限流中间件 / API rate limiting middleware",
     "issue": "为公开接口加入基于令牌桶的限流",
     "criteria": "按用户与 IP 维度限流；超限返回 429 与重试头；含并发测试。",
     "difficulty": "B", "xp": 150, "hours": 10, "tech": ["Java", "Spring Boot", "Redis"], "tags": ["feature"]},
    {"title": "数据库连接池监控 / Connection pool metrics",
     "issue": "暴露 HikariCP 连接池指标到健康检查",
     "criteria": "活跃/空闲/等待连接数可观测；接入 actuator；附说明。",
     "difficulty": "C", "xp": 90, "hours": 6, "tech": ["Java", "Spring Boot"], "tags": ["enhancement"]},
    {"title": "全局异常响应规范化 / Unified error response",
     "issue": "统一异常响应体结构与错误码",
     "criteria": "所有异常返回统一结构（code/message/traceId）；补单测。",
     "difficulty": "C", "xp": 100, "hours": 7, "tech": ["Java", "Spring Boot"], "tags": ["refactor"]},
]
IN_PROGRESS = [
    {"title": "任务搜索按截止时间排序 / Sort by deadline",
     "issue": "任务搜索支持按截止时间排序",
     "criteria": "sortBy=deadline 升降序；分页正确；新增 service 测试。",
     "difficulty": "B", "xp": 130, "hours": 9, "tech": ["Java", "Spring Boot", "JPA"], "tags": ["feature"]},
    {"title": "用户资料接口字段校验 / Profile field validation",
     "issue": "完善用户资料更新的字段校验",
     "criteria": "昵称/座右铭长度与字符集校验；越界返回明确错误码；含测试。",
     "difficulty": "C", "xp": 95, "hours": 6, "tech": ["Java", "Spring Security"], "tags": ["bugfix"]},
    {"title": "委托标签批量绑定 / Bulk tag binding",
     "issue": "支持一次为委托绑定多个标签",
     "criteria": "批量绑定幂等；非法标签忽略并告警；补集成测试。",
     "difficulty": "B", "xp": 140, "hours": 10, "tech": ["Java", "JPA", "MySQL"], "tags": ["feature"]},
]
IN_REVIEW = [
    {"title": "审核记录分页查询 / Paginated review records",
     "issue": "委托审核记录支持分页查询",
     "criteria": "按时间倒序分页；空结果正确；含数据层测试。",
     "difficulty": "C", "xp": 100, "hours": 7, "tech": ["Java", "JPA", "MySQL"], "tags": ["feature"]},
    {"title": "JWT 过期时间可配置 / Configurable JWT TTL",
     "issue": "令牌有效期改为可配置",
     "criteria": "访问/刷新令牌 TTL 走配置；默认值兼容；含安全测试。",
     "difficulty": "B", "xp": 135, "hours": 9, "tech": ["Java", "Spring Security"], "tags": ["enhancement"]},
    {"title": "接取记录导出 CSV / Export assignments as CSV",
     "issue": "支持导出某委托的接取记录为 CSV",
     "criteria": "字段含接取人/状态/时间；中文不乱码；附下载说明。",
     "difficulty": "C", "xp": 90, "hours": 6, "tech": ["Java", "Spring Boot"], "tags": ["feature"]},
]


def build_quest(spec):
    return {
        "issueTitle": spec["issue"][:60],
        "description": f"{spec['title']}\n\n{spec['issue']}。本任务用于 warden_zhao 演示数据。",
        "title": spec["title"],
        "completionCriteria": spec["criteria"],
        "difficulty": spec["difficulty"],
        "techStack": spec["tech"],
        "estimatedHours": spec["hours"],
        "rewardXp": spec["xp"],
    }


def main():
    print("== 登录 ==")
    warden_token, warden_user = login(WARDEN, PWD)
    admin_token, _ = login("admin", "admin123")
    if not warden_token or not admin_token:
        print("warden_zhao 或 admin 登录失败，后端是否在跑？"); sys.exit(1)
    print(f"  warden_zhao userId={warden_user['userId']}")

    # 分类 / 标签 / 仓库 id
    _, cats = get("/api/v1/quest-categories?includeDisabled=true")
    cat_map = {c["name"]: c["categoryId"] for c in page_items(cats)}
    cat_id = cat_map.get("后端开发") or next(iter(cat_map.values()))
    _, tags = get("/api/v1/quest-tags?includeDisabled=true&size=100")
    tag_map = {t["name"]: t["tagId"] for t in page_items(tags)}
    _, repos = get("/api/v1/repositories", warden_token)
    repo_id = next((r["repositoryId"] for r in data_of(repos) if r["name"] == REPO_NAME), None)
    if not repo_id:
        print(f"未找到仓库 {REPO_NAME}"); sys.exit(1)
    print(f"  仓库 {REPO_NAME} id={repo_id}, 分类 id={cat_id}")

    adv_tokens = {}
    for name in ADVENTURERS:
        tok, _ = login(name, PWD)
        if tok:
            adv_tokens[name] = tok
    adv_names = list(adv_tokens.keys())
    adv_idx = 0

    def next_adv():
        nonlocal adv_idx
        name = adv_names[adv_idx % len(adv_names)]
        adv_idx += 1
        return name, adv_tokens[name]

    counts = {"draft": 0, "in_progress": 0, "in_review": 0}

    # 1) 待发布委托 -> DRAFT（只创建，不提交）
    print("== 1. 待发布委托 (DRAFT) ==")
    for spec in DRAFTS:
        tag_ids = [tag_map[t] for t in spec["tags"] if t in tag_map]
        qid = create_quest(warden_token, repo_id, cat_id, tag_ids, build_quest(spec))
        if qid:
            counts["draft"] += 1
            print(f"  · DRAFT q{qid} {spec['title'][:34]}")

    # 2) 待完成委托 -> IN_PROGRESS（create -> submit -> approve -> accept，停在已接取）
    print("== 2. 待完成委托 (IN_PROGRESS) ==")
    for spec in IN_PROGRESS:
        tag_ids = [tag_map[t] for t in spec["tags"] if t in tag_map]
        qid = create_quest(warden_token, repo_id, cat_id, tag_ids, build_quest(spec))
        if not qid:
            continue
        submit_quest(warden_token, qid)
        ok, _ = admin_decision(admin_token, qid, approve=True, reason="信息完整，准予上架。")
        if not ok:
            print(f"  ! 上架失败 q{qid}"); continue
        name, tok = next_adv()
        asg = accept_quest(tok, qid)
        if asg:
            counts["in_progress"] += 1
            print(f"  · IN_PROGRESS q{qid} 接取者 {name} · {spec['title'][:30]}")
        else:
            print(f"  ! 接取失败 q{qid}")

    # 3) 待委托者审核 -> IN_REVIEW + 提交 PENDING_REVIEW
    #    create -> submit -> approve -> accept -> task-branch -> commit -> createSubmission（停，不审核）
    print("== 3. 待委托者审核 (IN_REVIEW + 待审提交) ==")
    for spec in IN_REVIEW:
        tag_ids = [tag_map[t] for t in spec["tags"] if t in tag_map]
        qid = create_quest(warden_token, repo_id, cat_id, tag_ids, build_quest(spec))
        if not qid:
            continue
        submit_quest(warden_token, qid)
        ok, _ = admin_decision(admin_token, qid, approve=True, reason="信息完整，准予上架。")
        if not ok:
            print(f"  ! 上架失败 q{qid}"); continue
        name, tok = next_adv()
        if not accept_quest(tok, qid):
            print(f"  ! 接取失败 q{qid}"); continue
        # 任务分支
        st, resp = post(f"/api/v1/quests/{qid}/task-branch", tok)
        if st not in (200, 201):
            print(f"  ! task-branch 失败 q{qid}: {st}"); continue
        branch = data_of(resp).get("taskBranch")
        # 真实提交，使 PR 有差异
        st, _ = post(f"/api/v1/repositories/{repo_id}/commits", warden_token, {
            "branch": branch,
            "message": f"feat: 完成委托 #{qid} {spec['title'][:30]}",
            "path": f"solutions/quest-{qid}.md",
            "content": f"# {spec['title']}\n\n{spec['criteria']}\n",
        })
        if st not in (200, 201):
            print(f"  ! commit 失败 q{qid}: {st}"); continue
        # 提交成果（自动建 PR，quest->IN_REVIEW，submission PENDING_REVIEW）
        st, resp = post("/api/v1/submissions", tok, {
            "questId": qid,
            "description": f"已按完成标准实现并自测：{spec['criteria'][:50]}",
            "checklist": ["功能完成", "本地自测通过", "符合完成标准"],
        })
        if st in (200, 201):
            counts["in_review"] += 1
            print(f"  · IN_REVIEW q{qid} 提交者 {name} · 待 warden_zhao 审核 · {spec['title'][:24]}")
        else:
            print(f"  ! 提交失败 q{qid}: {st} {resp}")

    print("\n==================== 汇总（warden_zhao 视角）====================")
    print(f"  待发布委托 DRAFT        : 新增 {counts['draft']}")
    print(f"  待完成委托 IN_PROGRESS  : 新增 {counts['in_progress']}（已被冒险者接取，等待完成）")
    print(f"  待委托者审核 IN_REVIEW  : 新增 {counts['in_review']}（提交柜台等 warden_zhao 审核）")
    print("===============================================================")
    print("登录 warden_zhao / admin123：")
    print("  · 工作台「我发布的委托」可见 DRAFT（待发布）与各状态委托")
    print("  · 维护者审核队列 / 提交柜台可见待审核的成果提交")


if __name__ == "__main__":
    main()
