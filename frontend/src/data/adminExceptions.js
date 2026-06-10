// 异常处理中心数据（M3 同步异常 / M4 关联异常 / M5 权限违规）。
// 字段对齐 docs/P4 契约：repository-exceptions（type/reason/impact/suggestion/status/logs）
// 与 /admin/exceptions/{id}/resolve（action + comment）。

// 异常分类 → 标签筛选用。
export const exceptionCategories = [
  { key: 'ALL', label: '全部' },
  { key: 'SYNC', label: '同步异常' },
  { key: 'RELATION', label: '关联异常' },
  { key: 'PERMISSION', label: '权限违规' },
]

// 每类异常可执行的处理动作（resolve 的 action 取值）。
export const exceptionActions = {
  SYNC: [
    { value: 'RETRY', label: '重试同步' },
    { value: 'KEEP_LAST', label: '保留上次成功数据' },
    { value: 'MARK_RESOLVED', label: '标记已解决' },
  ],
  RELATION: [
    { value: 'REQUEST_FIX', label: '退回修正关联' },
    { value: 'REBIND', label: '管理员手动绑定' },
    { value: 'MARK_RESOLVED', label: '标记已解决' },
  ],
  PERMISSION: [
    { value: 'REQUEST_GRANT', label: '要求补授权' },
    { value: 'BLOCK', label: '拦截违规操作' },
    { value: 'MARK_RESOLVED', label: '标记已解决' },
  ],
}

export const adminExceptions = [
  {
    id: 'EX-SYNC-013',
    category: 'SYNC',
    type: '仓库同步失败',
    title: 'git-guild / backend 同步连续失败',
    status: 'UNRESOLVED',
    statusLabel: '未解决',
    statusTone: 'danger',
    repository: 'git-guild / backend',
    relatedQuest: 'QST-0461',
    detectedAt: '今天 08:50',
    reason: '外部 Gitea 接口连续 3 次返回 504，最近一次成功同步在 6 小时前。',
    impact: '该仓库关联任务的 Issue 与 PR 状态停留在旧快照，新提交无法被识别。',
    suggestion: '保留上一次成功同步的数据；接口恢复后发起重试，不要清空已有快照。',
    retryable: true,
    logs: [
      '08:32 sync ok（last success snapshot #4821）',
      '08:41 sync failed · 504 Gateway Timeout',
      '08:46 sync failed · 504 Gateway Timeout',
      '08:50 sync failed · 504 Gateway Timeout',
    ],
  },
  {
    id: 'EX-REL-024',
    category: 'RELATION',
    type: 'PR 链接不匹配',
    title: '提交记录指向了其他任务的 PR',
    status: 'UNRESOLVED',
    statusLabel: '未解决',
    statusTone: 'danger',
    repository: 'git-guild / frontend',
    relatedQuest: 'QST-0427',
    detectedAt: '今天 09:10',
    reason: '冒险家提交的 PR #24 属于 git-guild-demo，但任务要求关联 git-guild / frontend 的 QST-0427。',
    impact: '维护者无法确认代码是否对应当前任务，成果审核停留在“待复核”。',
    suggestion: '要求提交者重新选择正确仓库与 PR；若确属同一任务迁移，由管理员手动绑定后复核。',
    retryable: false,
    logs: [
      '09:08 submission bound PR git-guild-demo#24',
      '09:10 relation check failed · repository mismatch（expected git-guild/frontend）',
    ],
  },
  {
    id: 'EX-PERM-031',
    category: 'PERMISSION',
    type: '权限不足',
    title: '维护者尝试上架受限仓库任务',
    status: 'UNRESOLVED',
    statusLabel: '未解决',
    statusTone: 'return',
    repository: 'git-guild / backend',
    relatedQuest: 'QST-0461',
    detectedAt: '昨天 19:40',
    reason: '维护者只拥有 git-guild / frontend 权限，却提交了 git-guild / backend 的任务上架申请。',
    impact: '任务若直接上架，后续 Issue 同步、PR 审核与奖励结算都可能无法完成。',
    suggestion: '要求仓库 owner 授权，或将任务转交给有权限的维护者后再提交管理员审核。',
    retryable: false,
    logs: [
      '19:38 publish request by user#52（scope: frontend）',
      '19:40 permission check failed · target git-guild/backend not in scope',
    ],
  },
  {
    id: 'EX-REL-026',
    category: 'RELATION',
    type: '任务被他人接取',
    title: '重复接取同一独占任务',
    status: 'IN_REVIEW',
    statusLabel: '需复核',
    statusTone: 'return',
    repository: 'git-guild / frontend',
    relatedQuest: 'QST-0440',
    detectedAt: '昨天 16:05',
    reason: '该任务已由 Minerva Dawn 接取，另一位冒险家在本地缓存过期后继续点击接取。',
    impact: '两个提交可能争抢同一奖励，维护者无法判断主线贡献者。',
    suggestion: '保留第一位接取者；为后来者推荐同标签任务，并刷新任务板状态。',
    retryable: false,
    logs: [
      '16:01 claim ok · Minerva Dawn',
      '16:05 duplicate claim attempt · user#73（stale cache）',
    ],
  },
]
