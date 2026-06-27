#!/usr/bin/env python3
"""批量发布更多"已上架"委托任务，用于测试悬赏板的可扩展性（卡片数量 / 分页 / 筛选）。

复用 seed_demo_data.py 的 API 助手：以各仓库 owner 身份创建任务 -> 提交 -> 管理员审核通过
-> 落到悬赏板（PUBLISHED）。可重复运行（每次追加一批，标题带序号避免歧义）。

用法：
  python3 scripts/add_published_quests.py [数量]   # 默认 24
"""
import sys
from seed_demo_data import (login, get, page_items, create_quest,
                            submit_quest, admin_decision)

PWD = "admin123"

# repo 名 -> (owner 用户名, 分类, 技术栈池)
REPOS = {
    "gitguild-web":     ("archmage_lin", "前端开发", ["Vue", "TypeScript", "JavaScript", "Vite"]),
    "gitguild-api":     ("warden_zhao", "后端开发", ["Java", "Spring Boot", "Spring Security", "JPA", "REST API"]),
    "quest-cli":        ("smith_huang", "DevOps", ["Python", "Docker", "Git"]),
    "guild-docs":       ("scribe_qian", "文档工程", ["Markdown", "REST API"]),
    "notify-service":   ("alchemist_sun", "后端开发", ["Java", "Spring Boot", "Redis"]),
    "leaderboard-svc":  ("captain_zhou", "数据与存储", ["Java", "MySQL", "JPA"]),
}

# 每个仓库一组贴近真实的任务模板：(标题, 完成标准简述, 难度, XP, 工时, 标签)
TEMPLATES = {
    "gitguild-web": [
        ("任务卡片骨架屏加载 / Skeleton loading for quest cards", "首屏与翻页时展示骨架屏；数据到达后平滑替换；含组件测试。", "C", 80, 5, ["enhancement"]),
        ("悬赏板支持关键字搜索 / Keyword search on board", "标题/描述模糊匹配；与现有筛选叠加；防抖输入。", "B", 130, 8, ["feature"]),
        ("任务详情页响应式适配 / Responsive quest detail", "移动端/平板布局自适应；图片与代码块不溢出。", "C", 90, 6, ["enhancement"]),
        ("暗色主题切换 / Dark mode toggle", "全局暗色主题；偏好持久化到本地存储。", "B", 150, 10, ["feature"]),
        ("接取按钮加二次确认 / Confirm before accept", "点击接取弹确认框，避免误触；含交互测试。", "D", 45, 3, ["good-first-issue"]),
    ],
    "gitguild-api": [
        ("任务列表接口加游标分页 / Cursor pagination", "大数据量下用游标分页替代 offset；兼容旧参数。", "A", 220, 16, ["enhancement"]),
        ("接取并发幂等保护 / Idempotent accept under race", "并发接取同一任务只产生一条有效接取；补并发测试。", "B", 160, 11, ["bugfix"]),
        ("发布任务参数校验增强 / Stronger create validation", "对 XP/工时上限、技术栈非空做服务层校验并返回明确错误码。", "C", 90, 6, ["enhancement"]),
        ("任务搜索支持多标签过滤 / Multi-tag filter", "tagIds 传多个时取交集；分页正确；新增 MVC 测试。", "B", 140, 9, ["feature"]),
        ("操作审计日志 / Audit log for quest actions", "发布/审核/接取/合并写审计；可按用户与时间查询。", "A", 240, 18, ["feature"]),
    ],
    "quest-cli": [
        ("CLI 增加 quest accept 命令 / `quest accept`", "按 questId 接取并打印任务分支信息；无 token 友好提示。", "C", 100, 7, ["feature"]),
        ("CLI 输出支持 JSON 格式 / JSON output", "所有列表命令加 --json；便于脚本消费。", "D", 50, 3, ["good-first-issue"]),
        ("CLI 配置文件支持 / Config file support", "支持 ~/.questrc 存 token 与服务地址；环境变量覆盖。", "B", 130, 8, ["enhancement"]),
    ],
    "guild-docs": [
        ("撰写维护者发布指南 / Maintainer publishing guide", "覆盖建仓→建 Issue→发布→审核全流程；中英双语。", "C", 85, 6, ["docs"]),
        ("整理常见问题 FAQ / FAQ page", "汇总登录、接取、提交常见问题与排查；通过文档站构建。", "D", 40, 2, ["docs"]),
        ("补充架构概览图 / Architecture overview", "前端/后端/Gitea/MySQL 关系图 + 文字说明。", "C", 95, 6, ["docs"]),
    ],
    "notify-service": [
        ("任务被接取时通知发布者 / Notify on accept", "有人接取即给发布者发 UNREAD 通知；含任务标题。", "C", 90, 6, ["feature"]),
        ("通知支持分页拉取 / Paginated notifications", "未读优先、按时间倒序、分页返回；含测试。", "B", 130, 9, ["enhancement"]),
        ("邮件通知开关 / Email notification opt-in", "用户可开关邮件通知；关闭时仅站内通知。", "B", 150, 11, ["feature"]),
    ],
    "leaderboard-svc": [
        ("排行榜分页与我的排名 / Pagination & my rank", "排行榜分页；额外返回当前用户排名，即使不在本页。", "B", 140, 9, ["feature"]),
        ("成长等级曲线可配置 / Configurable level curve", "等级所需 XP 改为可配置表；现有数据平滑迁移。", "A", 230, 17, ["enhancement"]),
        ("贡献热力统计 / Contribution heatmap data", "按日聚合贡献次数，提供热力图所需接口。", "B", 150, 10, ["feature"]),
        ("排行榜缓存与定时刷新 / Cache & refresh", "排行榜结果缓存并定时刷新；缓存击穿保护。", "A", 220, 16, ["enhancement"]),
    ],
}

CHECKLIST_6 = [{"label": l, "passed": True} for l in
               ["标题清晰", "描述完整", "完成标准可验证", "技术栈合规", "奖励合理", "无敏感信息"]]


def main():
    want = int(sys.argv[1]) if len(sys.argv) > 1 else 24

    admin_token, admin_user = login("admin", "admin123")
    if not admin_token:
        print("后端未就绪或 admin 登录失败"); sys.exit(1)

    # 分类 / 标签映射
    _, cats = get("/api/v1/quest-categories?includeDisabled=true")
    cat_map = {c["name"]: c["categoryId"] for c in page_items(cats)}
    _, tags = get("/api/v1/quest-tags?includeDisabled=true&size=100")
    tag_map = {t["name"]: t["tagId"] for t in page_items(tags)}

    # 仓库 id + owner token
    repo_ctx = {}
    for rname, (owner, cat, techpool) in REPOS.items():
        tok, _ = login(owner, PWD)
        _, repos = get("/api/v1/repositories", tok)
        rid = next((r["repositoryId"] for r in page_items(repos) if r["name"] == rname), None)
        if rid and tok:
            repo_ctx[rname] = {"id": rid, "token": tok, "cat": cat, "tech": techpool}

    # 轮流从各仓库取模板，直到达到目标数量
    queue = []
    pools = {k: list(v) for k, v in TEMPLATES.items()}
    while len(queue) < want and any(pools.values()):
        for rname in REPOS:
            if pools.get(rname):
                queue.append((rname, pools[rname].pop(0)))
                if len(queue) >= want:
                    break

    published = 0
    for rname, (title, criteria, diff, xp, hours, qtags) in queue:
        ctx = repo_ctx.get(rname)
        if not ctx:
            continue
        q = {
            "issueTitle": title.split(" / ")[0][:60],
            "description": f"{title}\n\n本任务用于丰富悬赏板演示数据。\n{criteria}",
            "title": title,
            "completionCriteria": criteria,
            "difficulty": diff,
            "techStack": ctx["tech"][:3],
            "estimatedHours": hours,
            "rewardXp": xp,
        }
        tag_ids = [tag_map[t] for t in qtags if t in tag_map]
        qid = create_quest(ctx["token"], ctx["id"], cat_map.get(ctx["cat"]), tag_ids, q)
        if not qid:
            continue
        submit_quest(ctx["token"], qid)
        ok, _ = admin_decision(admin_token, qid, approve=True, reason="信息完整，技术栈合规，准予上架。")
        if ok:
            published += 1
            print(f"  + PUBLISHED q{qid} [{rname}] {title[:40]}")

    print(f"\n本次新增上架任务：{published} 个")


if __name__ == "__main__":
    main()
