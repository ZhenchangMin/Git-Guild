export const workbenchUser = {
  name: 'Minerva Dawn',
  role: 'Adventurer',
  level: 3,
  xpCurrent: 720,
  xpTarget: 1000,
  completedQuests: 6,
}

export const workbenchStats = [
  { label: '进行中任务', value: 2 },
  { label: '审核中', value: 1 },
  { label: '待修改', value: 1 },
  { label: '未读邮件', value: 3 },
]

export const taskGroups = [
  {
    id: 'in-progress',
    label: '进行中',
    tasks: [
      {
        id: 'QST-0427',
        title: '重构提交流程',
        repository: 'git-guild / frontend',
        issue: '#42',
        prStatus: '未发起',
        nextStep: '创建 feature 分支并上传提交',
        actions: [
          { label: '查看仓库', type: 'repository' },
          { label: '创建分支', type: 'branch' },
          { label: '发起 PR', type: 'pull-request' },
          { label: '提交成果', type: 'submit', primary: true },
        ],
      },
      {
        id: 'QST-0431',
        title: '仓库导入异常页',
        repository: 'git-guild / frontend',
        issue: '#38',
        prStatus: 'PR #21 Review requested',
        nextStep: '补充运行截图后提交成果',
        actions: [
          { label: '查看仓库', type: 'repository' },
          { label: '创建分支', type: 'branch' },
          { label: '上传提交', type: 'commit' },
          { label: '提交成果', type: 'submit', primary: true },
        ],
      },
    ],
  },
  {
    id: 'in-review',
    label: '审核中',
    tasks: [
      {
        id: 'QST-0412',
        title: 'Issue sync status page',
        repository: 'git-guild / backend',
        issue: '#31',
        prStatus: 'PR #18 Open',
        nextStep: '等待维护者审核',
        actions: [
          { label: '查看提交记录', type: 'history' },
          { label: '查看 PR', type: 'pr-view', primary: true },
        ],
      },
    ],
  },
  {
    id: 'changes-requested',
    label: '待修改',
    tasks: [
      {
        id: 'QST-0444',
        title: '审核反馈归档',
        repository: 'git-guild / frontend',
        issue: '#47',
        prStatus: 'PR #23 Changes requested',
        nextStep: '根据逐项反馈更新 PR',
        actions: [
          { label: '查看反馈', type: 'feedback', feedbackId: 'feedback-qst-0444', primary: true },
          { label: '更新 PR', type: 'pull-request' },
          { label: '重新提交', type: 'submit' },
        ],
      },
    ],
  },
  {
    id: 'completed',
    label: '已完成',
    tasks: [
      {
        id: 'QST-0398',
        title: '任务筛选空态优化',
        repository: 'git-guild / frontend',
        issue: '#24',
        prStatus: 'PR #12 Merged',
        nextStep: '已写入贡献记录',
        actions: [
          { label: '查看贡献记录', type: 'contribution', primary: true },
          { label: '查看成长结果', type: 'growth' },
        ],
      },
    ],
  },
]

export const repositories = [
  {
    name: 'git-guild / frontend',
    syncStatus: 'Synced',
    defaultBranch: 'main',
    branches: 8,
    issues: 12,
    pullRequests: 3,
    lastCommit: '9f31c2a',
  },
  {
    name: 'git-guild / backend',
    syncStatus: 'Sync warning',
    defaultBranch: 'main',
    branches: 5,
    issues: 9,
    pullRequests: 1,
    lastCommit: '71ad0e4',
  },
]

export const pullRequests = [
  { id: 'PR #18', title: 'QST-0427 submission flow', status: 'Open', action: '查看 PR' },
  { id: 'PR #21', title: 'repository import error view', status: 'Review requested', action: '查看 PR' },
  { id: 'PR #23', title: 'QST-0444 review feedback archive', status: 'Changes requested', action: '查看 PR' },
]

export const reviewFeedbacks = [
  {
    id: 'feedback-qst-0444',
    questId: 'QST-0444',
    questTitle: '审核反馈归档',
    repository: 'git-guild / frontend',
    pullRequest: 'PR #23',
    pullRequestTitle: 'QST-0444 review feedback archive',
    reviewer: 'Guild Master · Review Desk',
    conclusion: '请求修改',
    reviewedAt: '今天 09:18',
    summary:
      '整体方向正确，已经能展示最近审核意见。但当前版本还不能稳定支持按任务归档查看，学习建议也没有和必须修改项分开。请先补齐这两处，再通过提交柜台重新提交成果。',
    checks: [
      {
        checkpoint: 'PR 指向正确仓库与任务分支',
        passed: true,
        comment: 'PR #23 指向 git-guild / frontend，分支名称和 QST-0444 对应，关联关系清楚。',
      },
      {
        checkpoint: '审核反馈按任务归档展示',
        passed: false,
        comment:
          '当前只按时间列出反馈。请增加任务编号筛选或分组，让用户能直接定位 QST-0444 对应的审核记录。',
      },
      {
        checkpoint: '区分通过、退回和待修改状态',
        passed: true,
        comment: '状态标签已经能区分通过和退回，但待修改状态建议继续保留警示色。',
      },
      {
        checkpoint: '必须修改与学习建议分区',
        passed: false,
        comment:
          '现在所有文字都放在同一段说明里。请拆成“必须修改”和“学习建议”两个区域，避免新手不知道先做哪一项。',
      },
      {
        checkpoint: '重新提交路径清晰',
        passed: false,
        comment:
          '页面需要明确说明：代码和 PR 更新在工作台完成，成果重新提交在提交柜台完成，并提供对应入口。',
      },
    ],
    requiredChanges: [
      '在反馈归档区域增加任务编号筛选或按任务分组，默认高亮当前任务 QST-0444。',
      '把“必须修改”和“学习建议”拆成两个清晰区域，必须修改项放在学习建议前面。',
      '在反馈详情底部补充下一步操作：进入工作台更新 PR、重新提交成果、查看 PR。',
    ],
    learningTips: [
      '先修正会阻塞验收的内容，再处理体验优化；这样维护者复审时能更快判断是否达标。',
      '每条修改建议最好对应一个可检查结果，例如“能按 QST-0444 筛选”比“优化筛选体验”更容易验收。',
      '重新提交时在提交说明中列出已完成的检查项，能减少维护者来回确认的成本。',
    ],
  },
]

export const maintainerWorkbenchStats = [
  { label: '待审核提交', value: 3 },
  { label: '请求修改', value: 2 },
  { label: '已发布任务', value: 6 },
  { label: '仓库预警', value: 1 },
]

export const reviewQueue = [
  {
    id: 'SUB-0427-01',
    questId: 'QST-0427',
    questTitle: '重构提交流程',
    submitter: 'Minerva Dawn',
    repository: 'git-guild / frontend',
    pullRequest: 'PR #18',
    status: '待审核',
    submittedAt: '今天 10:30',
    summary: '已重整任务成果提交路径，补充 PR 链接校验和退回修改入口。',
    checklist: [
      {
        item: 'PR 链接指向正确仓库',
        passed: true,
        comment: 'PR #18 属于 git-guild / frontend，和任务仓库一致。',
      },
      {
        item: '完成标准逐项覆盖',
        passed: false,
        comment: '提交说明中缺少退回修改状态截图，需要补充后再复审。',
      },
      {
        item: '提交说明清晰',
        passed: true,
        comment: '说明覆盖了表单字段、PR 关联和提交审核入口。',
      },
    ],
    requiredChanges: ['补充退回修改状态截图。', '在提交说明中注明本次验证使用的浏览器和测试数据。'],
    learningSuggestions: ['复审前先按完成标准逐项自检。', '提交说明可以按“做了什么 / 怎么验证 / 还剩什么风险”组织。'],
  },
  {
    id: 'SUB-0431-01',
    questId: 'QST-0431',
    questTitle: '仓库导入异常页',
    submitter: 'Evan Stone',
    repository: 'git-guild / frontend',
    pullRequest: 'PR #21',
    status: '待审核',
    submittedAt: '今天 09:48',
    summary: '新增导入失败状态、重试入口和同步日志摘要。',
    checklist: [
      {
        item: '异常原因可见',
        passed: true,
        comment: '网络失败和权限不足两类原因已经分开展示。',
      },
      {
        item: '提供重试入口',
        passed: true,
        comment: '页面右侧有手动重试按钮，状态反馈清楚。',
      },
      {
        item: '同步状态能回到工作台',
        passed: false,
        comment: '需要补充从异常页返回仓库工作台的入口。',
      },
    ],
    requiredChanges: ['增加返回仓库工作台的入口。'],
    learningSuggestions: ['异常页面应同时给出原因、影响和下一步，避免用户只看到失败提示。'],
  },
  {
    id: 'SUB-0444-02',
    questId: 'QST-0444',
    questTitle: '审核反馈归档',
    submitter: 'Minerva Dawn',
    repository: 'git-guild / frontend',
    pullRequest: 'PR #23',
    status: '已请求修改',
    submittedAt: '昨天 18:16',
    summary: '反馈归档区域已能显示历史意见，但筛选和学习建议分区仍需完善。',
    checklist: [
      {
        item: '反馈按任务归档',
        passed: false,
        comment: '当前仍主要按时间排列，需要按任务编号筛选。',
      },
      {
        item: '修改建议可执行',
        passed: true,
        comment: '必须修改项已经较具体。',
      },
      {
        item: '重新提交路径清晰',
        passed: true,
        comment: '已说明更新 PR 后到提交柜台重新提交。',
      },
    ],
    requiredChanges: ['增加任务编号筛选或分组。', '学习建议需要和必须修改项分开展示。'],
    learningSuggestions: ['把 Review 反馈写成可检查项，有助于新手按项修复。'],
  },
]

export const maintainerPublishedQuests = [
  { id: 'QST-0427', title: '重构提交流程', status: '审核中', assignee: 'Minerva Dawn' },
  { id: 'QST-0431', title: '仓库导入异常页', status: '待审核', assignee: 'Evan Stone' },
  { id: 'QST-0444', title: '审核反馈归档', status: '待修改', assignee: 'Minerva Dawn' },
  { id: 'QST-0450', title: '推荐任务理由展示', status: '已发布', assignee: '未接取' },
]

export const maintainerNotifications = [
  { type: '新提交', text: 'Minerva Dawn 提交了 QST-0427 的成果审核。' },
  { type: 'PR 更新', text: 'PR #21 已通过基础检查，等待维护者 Review。' },
  { type: '同步预警', text: 'git-guild/backend 同步状态为 Sync warning。' },
]

export const notifications = [
  { type: '审核反馈', text: '维护者请求修改 QST-0444', action: '查看反馈', feedbackId: 'feedback-qst-0444' },
  { type: 'PR 状态更新', text: 'PR #18 已通过基础检查', action: '查看 PR' },
  { type: '系统异常', text: '仓库 git-guild/backend 同步失败，需要重新同步', action: '处理异常' },
  { type: '管理员通知', text: '管理员退回了一个任务发布申请', action: '查看任务' },
]

export const workbenchEmails = [
  {
    id: 'mail-001',
    unread: true,
    from: 'Review Desk',
    to: 'Minerva Dawn',
    subject: 'QST-0444 需要修改后重新提交',
    receivedAt: '今天 09:24',
    related: 'QST-0444 · 审核反馈归档',
    feedbackId: 'feedback-qst-0444',
    preview: '维护者已经留下逐项反馈，请补充归档筛选和学习建议展示。',
    body: [
      '你的 PR 已经完成初步审核，核心功能方向正确，但还需要补充两个细节后再进入下一轮审核。',
      '第一，反馈归档需要能按任务编号筛选。第二，学习建议区域需要区分“必须修改”和“建议优化”。',
      '完成后请更新当前 PR，并通过提交柜台重新提交成果记录。',
    ],
  },
  {
    id: 'mail-002',
    unread: true,
    from: 'CI Relay',
    to: 'Minerva Dawn',
    subject: 'PR #18 已通过基础检查',
    receivedAt: '今天 08:10',
    related: 'PR #18 · QST-0427 submission flow',
    preview: '构建和静态检查已通过，可以等待维护者 review。',
    body: [
      'PR #18 的基础检查已经通过，包括前端构建和提交信息检查。',
      '当前状态仍为 Open，维护者还没有给出最终审核意见。你可以继续查看 PR 或等待通知。',
    ],
  },
  {
    id: 'mail-003',
    unread: true,
    from: 'System Monitor',
    to: 'Minerva Dawn',
    subject: 'git-guild/backend 同步失败',
    receivedAt: '昨天 21:42',
    related: 'git-guild / backend',
    preview: '仓库同步遇到权限或网络异常，需要重新同步。',
    body: [
      '平台在同步 git-guild/backend 时收到异常响应，Issue 和 PR 状态可能不是最新数据。',
      '建议先查看仓库同步日志，再触发一次手动同步。如果仍然失败，请联系管理员处理 Gitea 连接配置。',
    ],
  },
  {
    id: 'mail-004',
    unread: false,
    from: 'Admin Office',
    to: 'Minerva Dawn',
    subject: '一个任务发布申请已被退回',
    receivedAt: '昨天 16:05',
    related: '任务发布审核',
    preview: '管理员要求补充完成标准和验证步骤。',
    body: [
      '你的任务发布申请没有通过管理员上架审核，原因是完成标准不够具体。',
      '请补充可验证的检查项，例如页面字段、异常状态、重试入口和截图要求，然后重新提交审核。',
    ],
  },
]

export const recentContributions = [
  '完成 QST-0398 任务筛选空态优化',
  '合并 PR #12 到 git-guild/frontend',
]
