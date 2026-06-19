// 管理员审核任务发布 / 下架的演示数据。
// 状态、决策与字段命名对齐 docs/P3/API规范-“管理员审核”部分.md：
//   questStatus ∈ { PENDING_ADMIN_REVIEW, DRAFT, PUBLISHED, REJECTED, CLOSED }
//   decision    ∈ { APPROVE_PUBLISH, REJECT_PUBLISH, TAKE_DOWN }

// 任务状态 → 中文标签与展示色调（statusTone 复用既有 CSS：pending/approved/return/danger）。
export const questStatusMeta = {
  PENDING_ADMIN_REVIEW: { label: '待管理员审核', tone: 'pending' },
  DRAFT: { label: '已退回草稿', tone: 'return' },
  PUBLISHED: { label: '已发布上架', tone: 'approved' },
  REJECTED: { label: '已退回补充', tone: 'return' },
  CLOSED: { label: '已下架关闭', tone: 'danger' },
}

// 每个决策对应的目标状态、要求的前置状态与提示文案（业务规则 2-5）。
export const decisionMeta = {
  APPROVE_PUBLISH: {
    label: '通过上架',
    requires: 'PENDING_ADMIN_REVIEW',
    nextStatus: 'PUBLISHED',
    intent: 'primary',
    message: '任务审核已通过并发布，已进入悬赏任务板供冒险家接取。',
  },
  REJECT_PUBLISH: {
    label: '退回补充',
    requires: 'PENDING_ADMIN_REVIEW',
    nextStatus: 'REJECTED',
    intent: 'quiet',
    message: '任务发布申请已退回补充，发布者需补齐后重新提交管理员审核。',
  },
  TAKE_DOWN: {
    label: '下架处理',
    requires: 'PUBLISHED',
    nextStatus: 'CLOSED',
    intent: 'danger',
    message: '任务已下架，不再于悬赏任务板展示，并已通知发布者。',
  },
  REOPEN: {
    label: '重新上架',
    requires: 'CLOSED',
    nextStatus: 'PUBLISHED',
    intent: 'quiet',
    message: '任务已重新上架，重新进入悬赏任务板供冒险家接取。',
  },
}

export const adminQuestApplications = [
  {
    id: 'APP-058',
    questId: 2058,
    questCode: 'QST-0458',
    title: '提交柜台缺少任务草稿预检',
    publisher: '委托人 · 审核台',
    repository: 'git-guild / frontend',
    issue: '#58',
    submittedAt: '今天 10:46',
    questStatus: 'PENDING_ADMIN_REVIEW',
    summary:
      '维护者希望把 Issue #58 上架为悬赏任务，帮助新手补齐任务草稿预检和管理员审核前的清晰度提示。',
    targetAudience: '新手贡献者 · 前端课堂演示',
    reward: '420 XP · 70 Gold',
    difficulty: '中级',
    clarityChecks: [
      { label: '任务目标明确', note: '摘要说明了要新增草稿预检，并明确发生在任务发布前。' },
      { label: '完成标准可验收', note: '标准包含字段、检查状态和提交审核后的状态展示。' },
      { label: '边界说明充分', note: '明确不处理成果提交审核，只处理维护者发布任务申请。' },
    ],
    complianceChecks: [
      { label: '适合课堂公开展示', note: '未包含真实密钥、账号或敏感仓库信息。' },
      { label: '奖励与难度设置合理', note: '中级前端任务对应 420 XP，范围合理。' },
    ],
    risks: ['需要确认任务详情页是否也能展示“管理员审核中”的发布状态。', '若维护者重复提交，需要合并同一 Issue 的审核记录。'],
    reviewRecords: [
      {
        decision: 'SUBMITTED',
        adminName: '委托人 · 审核台',
        reviewedAt: '今天 10:46',
        reason: '维护者提交发布申请，等待管理员审核。',
      },
    ],
  },
  {
    id: 'APP-061',
    questId: 2061,
    questCode: 'QST-0461',
    title: '仓库同步异常需要课堂演示入口',
    publisher: '委托人 · 导入大厅',
    repository: 'git-guild / backend',
    issue: '#61',
    submittedAt: '今天 09:32',
    questStatus: 'PENDING_ADMIN_REVIEW',
    summary:
      '将后端同步异常包装为新手任务，但当前完成标准偏少，无法判断学生需要提交什么证据。',
    targetAudience: '后端入门贡献者',
    reward: '待补充',
    difficulty: '初级',
    clarityChecks: [
      { label: '任务目标明确', note: '目标聚焦同步异常演示入口。' },
      { label: '完成标准可验收', note: '只有两项标准，缺少截图、日志和验证路径。' },
      { label: '边界说明充分', note: '没有说明是否允许修改真实同步配置。' },
    ],
    complianceChecks: [
      { label: '适合课堂公开展示', note: '当前文本没有敏感配置。' },
      { label: '奖励与难度设置合理', note: '奖励字段为空，上架前需要补齐。' },
    ],
    risks: ['同步异常可能涉及后端配置，需限制为 mock 日志或脱敏数据。', '奖励为空，悬赏任务板无法正确排序或展示激励。'],
    reviewRecords: [
      {
        decision: 'SUBMITTED',
        adminName: '委托人 · 导入大厅',
        reviewedAt: '今天 09:32',
        reason: '维护者提交发布申请，等待管理员审核。',
      },
    ],
  },
  {
    id: 'APP-052',
    questId: 2052,
    questCode: 'QST-0452',
    title: '悬赏任务板筛选支持多技术栈标签',
    publisher: '委托人 · 任务板',
    repository: 'git-guild / frontend',
    issue: '#52',
    submittedAt: '昨天 16:08',
    questStatus: 'PUBLISHED',
    summary:
      '该任务已通过管理员审核并发布。近日维护者反馈关联 Issue 已合并关闭，需要管理员判断是否下架。',
    targetAudience: '前端进阶贡献者',
    reward: '500 XP · 90 Gold',
    difficulty: '中级',
    clarityChecks: [
      { label: '任务目标明确', note: '目标是为任务板增加多标签筛选。' },
      { label: '完成标准可验收', note: '含交互、空态与持久化要求。' },
      { label: '边界说明充分', note: '明确仅改前端筛选逻辑，不动后端检索。' },
    ],
    complianceChecks: [
      { label: '适合课堂公开展示', note: '无敏感信息。' },
      { label: '奖励与难度设置合理', note: '中级任务奖励合理。' },
      { label: '关联 Issue 仍然有效', note: 'Issue #52 已被合并关闭，任务可能不再需要继续展示。' },
    ],
    risks: ['关联 Issue 已关闭，继续展示可能误导新手接取已无需求的任务。'],
    reviewRecords: [
      {
        decision: 'SUBMITTED',
        adminName: '委托人 · 任务板',
        reviewedAt: '昨天 16:08',
        reason: '维护者提交发布申请。',
      },
      {
        decision: 'APPROVE_PUBLISH',
        adminName: '管理员 · 审核台',
        reviewedAt: '昨天 17:20',
        reason: '任务描述清晰、完成标准明确，准予发布。',
        visibleToPublisher: true,
      },
    ],
  },
  {
    id: 'APP-064',
    questId: 2064,
    questCode: 'QST-0464',
    title: '个人成长卡片缺少贡献回放',
    publisher: '委托人 · 资料台',
    repository: 'git-guild / frontend',
    issue: '#64',
    submittedAt: '昨天 18:20',
    questStatus: 'PENDING_ADMIN_REVIEW',
    summary:
      '维护者提交了成长卡片增强任务，但 Issue 缺少完成标准，且申请里混入了“线上用户数据回放”的描述。',
    targetAudience: '前端进阶贡献者',
    reward: '300 XP · 45 Gold',
    difficulty: '待补充',
    clarityChecks: [
      { label: '任务目标明确', note: '目标是补充贡献回放。' },
      { label: '完成标准可验收', note: '没有列出可检查的 UI、数据或交互标准。' },
      { label: '边界说明充分', note: '需要明确只使用本地 mock 贡献记录。' },
    ],
    complianceChecks: [
      { label: '适合课堂公开展示', note: '“线上用户数据回放”需要管理员确认是否脱敏。' },
      { label: '奖励与难度设置合理', note: '难度为空，无法判断奖励是否合理。' },
    ],
    risks: ['可能误用真实用户贡献记录，必须改为 mock 数据。', '缺少验收标准会导致冒险家无法判断完成边界。'],
    reviewRecords: [
      {
        decision: 'SUBMITTED',
        adminName: '委托人 · 资料台',
        reviewedAt: '昨天 18:20',
        reason: '维护者提交发布申请，等待管理员审核。',
      },
    ],
  },
]

export const adminExceptionCases = [
  {
    id: 'EX-PR-017',
    type: 'PR 链接不匹配',
    title: '提交记录指向了其他任务的 PR',
    status: '待处理',
    statusTone: 'danger',
    owner: '管理员 · 审核台',
    relatedQuest: 'QST-0427',
    reason: '冒险家提交的 PR #24 属于 git-guild-demo，但任务要求关联 git-guild / frontend 的 QST-0427 分支。',
    impact: '维护者无法确认代码是否对应当前任务，成果审核会停留在“待复核”。',
    suggestion: '要求提交者重新选择正确仓库与 PR；若确属同一任务迁移，需要管理员手动绑定后复核。',
    actionLabel: '退回重选 PR',
    resultStatus: '需复核',
    resultTone: 'return',
    resultMessage: '已通知提交者重新绑定 PR，任务成果保持待复核，不进入通过流程。',
  },
  {
    id: 'EX-CLAIM-022',
    type: '任务被他人接取',
    title: '重复接取同一独占任务',
    status: '待处理',
    statusTone: 'return',
    owner: '管理员 · 悬赏任务板',
    relatedQuest: 'QST-0440',
    reason: '该任务已由 Minerva Dawn 接取，另一位冒险家在本地缓存过期后继续点击接取。',
    impact: '如果不拦截，两个提交可能争抢同一奖励，维护者也无法判断主线贡献者。',
    suggestion: '保留第一位接取者；为后来者推荐同标签任务，并刷新任务板状态。',
    actionLabel: '释放重复接取',
    resultStatus: '已处理',
    resultTone: 'approved',
    resultMessage: '重复接取已取消，并给后来者返回可接取任务推荐。',
  },
  {
    id: 'EX-AUTH-031',
    type: '权限不足',
    title: '维护者尝试上架受限仓库任务',
    status: '待处理',
    statusTone: 'danger',
    owner: '管理员 · 权限入口',
    relatedQuest: 'QST-0461',
    reason: '维护者只拥有 git-guild / frontend 权限，却提交了 git-guild / backend 的任务上架申请。',
    impact: '任务若直接上架，后续 Issue 同步、PR 审核与奖励结算都可能无法完成。',
    suggestion: '要求仓库 owner 授权，或将任务转交给有权限的维护者后再提交管理员审核。',
    actionLabel: '要求补授权',
    resultStatus: '需复核',
    resultTone: 'return',
    resultMessage: '已把申请退回权限补充队列，授权完成前不会进入悬赏任务板。',
  },
  {
    id: 'EX-DESC-044',
    type: '任务描述不清晰',
    title: '完成边界缺失',
    status: '待处理',
    statusTone: 'return',
    owner: '管理员 · 清晰度审核台',
    relatedQuest: 'QST-0464',
    reason: '任务描述只写“补充贡献回放”，未说明页面位置、数据来源、验收截图或禁止使用真实用户数据。',
    impact: '冒险家无法判断要交付什么，维护者也难以给出一致审核结论。',
    suggestion: '退回维护者补齐目标、非目标、验收标准和 mock 数据边界。',
    actionLabel: '退回补描述',
    resultStatus: '已处理',
    resultTone: 'approved',
    resultMessage: '已退回维护者补齐描述，并附带课堂 MVP 描述模板。',
  },
]
