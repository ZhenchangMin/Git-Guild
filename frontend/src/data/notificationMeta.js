// 站内通知类型的展示元数据：标题、色调、图标、以及点开后可采取的动作（路由跳转）。
// 与后端 NotificationType 一一对应，供通知铃、角落 toast、详情弹窗共用同一套文案与动作。

export const NOTIFICATION_META = {
  SUBMISSION_RECEIVED: {
    title: '新成果待审核',
    tone: 'tone-pending',
    icon: '✉',
    audience: '委托人',
    action: { label: '前往审核台', route: 'maintainer-review' },
  },
  REVIEW_APPROVED: {
    title: '成果已通过',
    tone: 'tone-approved',
    icon: '🏆',
    audience: '冒险家',
    action: { label: '查看成长记录', route: 'profile' },
  },
  REVIEW_CHANGES_REQUESTED: {
    title: '需要修改',
    tone: 'tone-changes',
    icon: '✎',
    audience: '冒险家',
    action: { label: '前往工作台修改', route: 'adventurer-workbench' },
  },
  REVIEW_REJECTED: {
    title: '提交被驳回',
    tone: 'tone-rejected',
    icon: '⊘',
    audience: '冒险家',
    action: { label: '前往工作台', route: 'adventurer-workbench' },
  },
  QUEST_APPROVED: {
    title: '委托已上架',
    tone: 'tone-approved',
    icon: '✓',
    audience: '委托人',
    action: { label: '前往委托人工作台', route: 'maintainer-workbench' },
  },
  QUEST_REJECTED: {
    title: '委托被退回上架',
    tone: 'tone-rejected',
    icon: '⊘',
    audience: '委托人',
    action: { label: '前往委托人工作台', route: 'maintainer-workbench' },
  },
  QUEST_TAKEN_DOWN: {
    title: '委托已下架',
    tone: 'tone-rejected',
    icon: '▼',
    audience: '委托人',
    action: { label: '前往委托人工作台', route: 'maintainer-workbench' },
  },
  QUEST_REOPENED: {
    title: '委托重新上架',
    tone: 'tone-approved',
    icon: '▲',
    audience: '委托人',
    action: { label: '前往委托人工作台', route: 'maintainer-workbench' },
  },
  QUEST_ACCEPTED: {
    title: '委托被接取',
    tone: 'tone-pending',
    icon: '⚑',
    audience: '委托人',
    action: { label: '前往委托人工作台', route: 'maintainer-workbench' },
  },
  PR_MERGED: {
    title: 'PR 已合并',
    tone: 'tone-approved',
    icon: '⎇',
    audience: '冒险家',
    action: { label: '前往工作台', route: 'adventurer-workbench' },
  },
  LEVEL_UP: {
    title: '升级啦！',
    tone: 'tone-approved',
    icon: '★',
    audience: '冒险家',
    action: { label: '查看成长记录', route: 'profile' },
  },
  BADGE_UNLOCKED: {
    title: '解锁新徽章',
    tone: 'tone-approved',
    icon: '🏅',
    audience: '冒险家',
    action: { label: '查看成长记录', route: 'profile' },
  },
  MESSAGE_RECEIVED: {
    title: '收到新信笺',
    tone: 'tone-pending',
    icon: '✉',
    audience: '任务伙伴',
    action: { label: '打开信笺', messageThread: true },
  },
}

const FALLBACK = { title: '站内通知', tone: 'tone-pending', icon: '✉', audience: '', action: null }

export function notificationMeta(type) {
  return NOTIFICATION_META[type] ?? FALLBACK
}
