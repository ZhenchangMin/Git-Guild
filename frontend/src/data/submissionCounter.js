export const linkedSubmission = {
  quest: 'QST-0427 · 重构提交流程',
  meta: 'Vue / REST API · 难度 D · 240 XP',
}

export const submissionFields = [
  {
    id: 'repository',
    label: '所属仓库',
    ariaLabel: '所属仓库',
    value: 'git-guild / frontend',
  },
  {
    id: 'branch',
    label: '提交分支',
    ariaLabel: '提交分支',
    value: 'feature/refactor-submission-flow',
  },
  {
    id: 'pull-request',
    label: 'PR',
    ariaLabel: 'PR 链接',
    placeholder: '粘贴 PR 链接',
    wide: true,
  },
  {
    id: 'submission-note',
    label: '提交说明',
    ariaLabel: '提交说明',
    type: 'textarea',
    rows: 4,
    value: '已重整提交表单布局，补充 PR 链接校验、状态同步入口和退回修改说明。',
    wide: true,
  },
]

export const submissionChecks = [
  '已关联接取的委托编号',
  'PR 链接可打开且指向正确仓库',
  '分支名称与任务记录一致',
  '完成标准逐项自检',
]

export const submissionReviewSteps = [
  { label: '草稿', note: '补全任务、分支与 PR 信息', state: 'done' },
  { label: '已提交', note: '成果已放入柜台审核队列', state: 'active' },
  { label: '审核中', note: '维护者查看差异与运行结果', state: 'next' },
  { label: '成长记录', note: '通过后写入 XP 与贡献记录', state: 'next' },
]

export const reviewerSlot = {
  eyebrow: '审核反馈',
  title: '维护者反馈位',
  body: '审核意见、退回原因和学习建议集中放在这里，避免用户在任务、PR、通知之间来回找反馈。',
}
