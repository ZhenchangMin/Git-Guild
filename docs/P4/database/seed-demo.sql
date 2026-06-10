-- ============================================================================
-- Git Guild 演示数据 seed（纯 INSERT，MySQL 5.7 / 8.0 通用）
--
-- 目的：让线上演示跑的是数据库里的真实数据（经真实 API 读取），而非前端兜底假数据。
-- 表已由 Hibernate(ddl-auto=update) 建好，本脚本只插数据、不建表，故与排序规则无关，
-- 可直接在生产 MySQL 5.7 上执行。
--
-- 内容：
--   · 3 个账号：委托人 guild / 冒险家 hero（有成长数据）/ 冒险家 newbie（新手）
--   · 3 个分类、2 个仓库（行记录；Gitea 未部署，仓库链接仅作展示）
--   · 6 个 PUBLISHED 委托（任务板可接取）+ 3 个 COMPLETED 委托（hero 已完成）
--   · hero 的接取记录 / 贡献历程 / 成长档案 / XP 流水 —— 等级与 XP 自洽
--
-- 账号（密码 bcrypt，可正常登录）：
--   guild@gitguild.local  / guild123   (MAINTAINER 委托人)
--   hero@gitguild.local   / hero123    (BEGINNER 冒险家，Lv3 / 270XP / 完成 3)
--   newbie@gitguild.local / newbie123  (BEGINNER 冒险家，全新)
--   （管理员 admin@gitguild.local / admin123 已存在，本脚本不动）
--
-- 幂等：开头按固定 ID 段清理旧的演示数据，可重复执行。ID 段用 100+/200+/... 高位，
-- 避免与 API 自增产生的真实用户冲突。
-- ============================================================================

SET NAMES utf8mb4;

-- ---- 清理旧演示数据（子表 → 父表顺序）---------------------------------------
DELETE FROM xp_transactions     WHERE transaction_id BETWEEN 901 AND 999;
DELETE FROM contribution_records WHERE record_id     BETWEEN 701 AND 799;
DELETE FROM growth_profiles     WHERE profile_id      BETWEEN 801 AND 899;
DELETE FROM quest_assignments   WHERE assignment_id   BETWEEN 601 AND 699;
DELETE FROM quests              WHERE quest_id        BETWEEN 501 AND 599;
DELETE FROM repositories        WHERE repository_id   BETWEEN 301 AND 399;
DELETE FROM quest_categories    WHERE category_id     BETWEEN 201 AND 299;
DELETE FROM users               WHERE user_id         BETWEEN 101 AND 199;

-- ---- 用户 -------------------------------------------------------------------
INSERT INTO users
  (user_id, created_at, email, password_hash, role, status, token_version, updated_at, username, avatar_url, display_badge_id, motto)
VALUES
  (101,'2026-05-01 09:00:00.000000','guild@gitguild.local','$2b$10$b4ANzfjWg751Ie7VKt37vO2HEEwuIqXMFlXc9PkpBtmNjcDfBivrW','MAINTAINER','ACTIVE',0,'2026-05-01 09:00:00.000000','guild-master',NULL,NULL,'万事俱备，只欠一位英雄揭榜。'),
  (102,'2026-05-02 10:30:00.000000','hero@gitguild.local','$2b$10$vzAl9c8perKonL8IqAr/9uw8VIiwFdLLmoj5kToR/o7DSV.1YKUm.','BEGINNER','ACTIVE',0,'2026-06-05 18:00:00.000000','shadowblade',NULL,'FIRST_COMPLETION','代码如剑，越用越利。'),
  (103,'2026-05-20 14:15:00.000000','newbie@gitguild.local','$2b$10$ArrA7NxC0Y1rIEaSDTGNa.xv7Jmn6gIpYANYN4yBHcg6g.tijbTVC','BEGINNER','ACTIVE',0,'2026-05-20 14:15:00.000000','green-rookie',NULL,NULL,'刚踏入公会，请多指教。');

-- ---- 委托分类 ---------------------------------------------------------------
INSERT INTO quest_categories
  (category_id, created_at, description, enabled, name, updated_at)
VALUES
  (201,'2026-05-01 09:10:00.000000','前端界面与交互相关委托',b'1','前端开发','2026-05-01 09:10:00.000000'),
  (202,'2026-05-01 09:10:00.000000','后端服务与接口相关委托',b'1','后端开发','2026-05-01 09:10:00.000000'),
  (203,'2026-05-01 09:10:00.000000','缺陷修复与稳定性改进',b'1','Bug 修复','2026-05-01 09:10:00.000000');

-- ---- 仓库（行记录；Gitea 未部署，source_url 仅作展示）------------------------
INSERT INTO repositories
  (repository_id, created_at, default_branch, external_repository_id, host_type, last_synced_at, name, source_url, sync_error_message, sync_status, updated_at, owner_id, description)
VALUES
  (301,'2026-05-01 09:20:00.000000','main',NULL,'GITEA','2026-05-01 09:20:00.000000','gitguild-web','http://47.236.0.207:3000/guild-master/gitguild-web',NULL,'SYNCED','2026-05-01 09:20:00.000000',101,'Git Guild 前端仓库'),
  (302,'2026-05-01 09:20:00.000000','main',NULL,'GITEA','2026-05-01 09:20:00.000000','gitguild-core','http://47.236.0.207:3000/guild-master/gitguild-core',NULL,'SYNCED','2026-05-01 09:20:00.000000',101,'Git Guild 后端核心仓库');

-- ---- 委托：6 个 PUBLISHED（任务板可接取）-----------------------------------
INSERT INTO quests
  (quest_id, completion_criteria, created_at, description, difficulty, estimated_hours, published_at, reward_xp, status, tech_stack, title, updated_at, category_id, issue_id, publisher_id, repository_id)
VALUES
  (501,'移动端各分辨率下筛选器对齐，无横向滚动条','2026-05-10 09:00:00.000000','任务板的难度/技术栈筛选器在窄屏下会错位，需要重排为可换行布局。','C',4,'2026-05-10 09:30:00.000000',60,'PUBLISHED','["Vue","CSS"]','修复任务板筛选器在移动端错位','2026-05-10 09:30:00.000000',201,NULL,101,301),
  (502,'分页参数生效，热点查询命中 Redis 缓存','2026-05-11 09:00:00.000000','悬赏列表接口目前全量返回，需加入分页并对热门查询接入 Redis 缓存。','B',8,'2026-05-11 09:30:00.000000',90,'PUBLISHED','["Spring Boot","Redis"]','为悬赏接口增加分页与缓存','2026-05-11 09:30:00.000000',202,NULL,101,302),
  (503,'并发结算下 XP 不重复发放，附事务设计文档','2026-05-12 09:00:00.000000','需要设计一套幂等的 XP 结算事务方案，保证审核通过时奖励只发一次。','A',16,'2026-05-12 09:30:00.000000',150,'PUBLISHED','["Java","MySQL"]','设计 XP 结算的幂等事务方案','2026-05-12 09:30:00.000000',202,NULL,101,302),
  (504,'公告可自动轮播、可手动切换，支持空数据态','2026-05-13 09:00:00.000000','公会大厅顶部新增一个公告轮播组件，用于展示平台动态。','D',3,'2026-05-13 09:30:00.000000',40,'PUBLISHED','["Vue"]','公会大厅新增公告轮播组件','2026-05-13 09:30:00.000000',201,NULL,101,301),
  (505,'任一标签页登录/登出后，其余标签页登录态同步','2026-05-14 09:00:00.000000','多标签页之间登录态不同步，需通过 storage 事件广播会话变化。','C',5,'2026-05-14 09:30:00.000000',70,'PUBLISHED','["JavaScript"]','登录态在多标签页同步','2026-05-14 09:30:00.000000',203,NULL,101,301),
  (506,'PR 合并/关闭后，平台内 PR 状态自动更新','2026-05-15 09:00:00.000000','接入 Gitea Webhook，使 PR 状态变化能自动同步到平台，无需手动刷新。','B',10,'2026-05-15 09:30:00.000000',110,'PUBLISHED','["Spring Boot","Gitea"]','接入 Gitea Webhook 自动同步 PR 状态','2026-05-15 09:30:00.000000',202,NULL,101,302);

-- ---- 委托：3 个 COMPLETED（hero 已完成，用于贡献历程/成长）-------------------
INSERT INTO quests
  (quest_id, completion_criteria, created_at, description, difficulty, estimated_hours, published_at, reward_xp, status, tech_stack, title, updated_at, category_id, issue_id, publisher_id, repository_id)
VALUES
  (507,'抽出独立头像上传组件并在两处复用','2026-05-03 09:00:00.000000','把重复的头像上传逻辑抽成可复用组件，降低维护成本。','D',6,'2026-05-03 09:30:00.000000',80,'COMPLETED','["Vue"]','重构头像上传为可复用组件','2026-05-25 16:00:00.000000',201,NULL,101,301),
  (508,'用户资料接口核心分支单测覆盖','2026-05-04 09:00:00.000000','为用户资料读取/更新接口补充单元测试，提升回归信心。','C',7,'2026-05-04 09:30:00.000000',90,'COMPLETED','["JUnit","Java"]','补充用户资料接口的单元测试','2026-05-30 17:30:00.000000',202,NULL,101,302),
  (509,'成长档案接口并行加载，首屏数字 2 秒内出现','2026-05-05 09:00:00.000000','成长档案多个接口串行请求导致加载缓慢，需要改为并行加载。','C',6,'2026-05-05 09:30:00.000000',100,'COMPLETED','["Vue","JavaScript"]','修复成长档案串行请求导致的加载缓慢','2026-06-05 18:00:00.000000',203,NULL,101,301);

-- ---- hero 的接取记录（COMPLETED）-------------------------------------------
INSERT INTO quest_assignments
  (assignment_id, accepted_at, completed_at, created_at, status, updated_at, assignee_id, quest_id, task_branch)
VALUES
  (601,'2026-05-22 10:00:00.000000','2026-05-25 16:00:00.000000','2026-05-22 10:00:00.000000','COMPLETED','2026-05-25 16:00:00.000000',102,507,'quest-507-shadowblade'),
  (602,'2026-05-27 11:00:00.000000','2026-05-30 17:30:00.000000','2026-05-27 11:00:00.000000','COMPLETED','2026-05-30 17:30:00.000000',102,508,'quest-508-shadowblade'),
  (603,'2026-06-02 09:30:00.000000','2026-06-05 18:00:00.000000','2026-06-02 09:30:00.000000','COMPLETED','2026-06-05 18:00:00.000000',102,509,'quest-509-shadowblade');

-- ---- hero 的贡献历程 -------------------------------------------------------
INSERT INTO contribution_records
  (record_id, completed_at, created_at, summary, quest_id, repository_id, user_id)
VALUES
  (701,'2026-05-25 16:00:00.000000','2026-05-25 16:00:00.000000','抽出 AvatarUploader 组件，登录页与档案页共用，删除重复代码约 120 行。',507,301,102),
  (702,'2026-05-30 17:30:00.000000','2026-05-30 17:30:00.000000','为用户资料接口补充 12 个单元测试，覆盖正常与边界分支。',508,302,102),
  (703,'2026-06-05 18:00:00.000000','2026-06-05 18:00:00.000000','成长档案改为 Promise.allSettled 并行加载，首屏数字加载从约 5 秒降到 2 秒内。',509,301,102);

-- ---- hero 的成长档案（level = totalXp/100 + 1）------------------------------
-- 270 XP → Lv3，下一级阈值 300，完成数 3
INSERT INTO growth_profiles
  (profile_id, completed_quest_count, created_at, level, total_xp, updated_at, user_id)
VALUES
  (801,3,'2026-05-25 16:00:00.000000',3,270,'2026-06-05 18:00:00.000000',102);

-- ---- hero 的 XP 流水 -------------------------------------------------------
INSERT INTO xp_transactions
  (transaction_id, amount, created_at, reason, quest_id, user_id)
VALUES
  (901,80, '2026-05-25 16:00:00.000000','QUEST_COMPLETION',507,102),
  (902,90, '2026-05-30 17:30:00.000000','QUEST_COMPLETION',508,102),
  (903,100,'2026-06-05 18:00:00.000000','QUEST_COMPLETION',509,102);
