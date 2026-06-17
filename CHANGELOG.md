# Changelog

## [Unreleased] - 2026-06-17
### Features
- 异常处理中心（`/admin/exceptions`）接入真实后端，不再是纯前端 mock。
  - 新增后端模块 `com.gitguild.backend.ops`：实体 `PlatformException`（表 `platform_exceptions` + 日志子表 `platform_exception_logs`）、`ExceptionRecorder`（写侧采集）、`AdminExceptionService`（list/get/resolve/retry）、`AdminExceptionController`（`/api/v1/admin/exceptions`，要求 ADMIN 角色）。
  - 仓库同步逻辑从 `CodeHostController` 下沉到新 `RepositorySyncService`；同步失败时自动登记一条 SYNC 异常并标记仓库 `FAILED`，再抛出。异常中心「重试」复用同一同步实现。
  - 前端 `adminApi` 异常方法由 `requestMock` 切换为真实 `request`；`AdminExceptionsPage` 改为按需加载真实数据，含空态/错误态；`data/adminExceptions.js` 仅保留分类与处置动作的静态映射。

### Design Rationale
- 异常采集与各业务流程解耦：`ExceptionRecorder` 是唯一写侧入口，`recordSyncFailure` 用 `REQUIRES_NEW` 独立事务提交，确保即便调用方因同步失败回滚，异常记录仍留存。
- 按「同一仓库未闭环的 SYNC 异常」去重，避免连续同步失败刷爆异常队列。
- resolve 动作语义化映射状态：`KEEP_LAST/MARK_RESOLVED/REBIND` → RESOLVED；`REQUEST_FIX/REQUEST_GRANT/BLOCK` → IN_REVIEW（等待外部结果）。

### Notes & Caveats
- **诚实边界**：当前只有 **SYNC**（仓库同步失败）有真实事件源。**RELATION/PERMISSION** 暂无采集点，对应分类在 UI 上留空而非展示假数据；后续可在 PR 关联校验 / 越权发布路径调用 `ExceptionRecorder` 补齐。
- **部署**：生产 `ddl-auto: validate` 不会自动建表。上线新 jar 前必须先在生产 MySQL 执行 `docs/P4/database/init.sql` 中新增的 `platform_exceptions` 与 `platform_exception_logs` 两张表，否则启动时 schema 校验失败。
