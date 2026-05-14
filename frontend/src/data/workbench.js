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
          { label: '查看反馈', type: 'feedback', primary: true },
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
]

export const notifications = [
  { type: '审核反馈', text: '维护者请求修改 QST-0444', action: '查看反馈' },
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
