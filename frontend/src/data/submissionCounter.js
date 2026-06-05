// Mock seed data for the Submission Counter.
//
// The Counter UI is keyed by a quest passed in via props, so the values here
// are only the fallbacks shown when the page is opened without a `questId`
// query (e.g. design previews). Anything that *isn't* quest-specific —
// checklist items, review stages, evidence categories — lives here and is
// considered the single source of truth.

export const linkedSubmission = {
  quest: 'QST-0427 · 重构提交流程',
  meta: 'Vue / REST API · 难度 D · 240 XP',
}

// Kept for backward compatibility with anything that still imports it. The
// new SubmissionCounter component drives input state via v-model instead of
// reading `value` off this list, so consumers should treat these as labels +
// placeholders only.
export const submissionFields = [
  {
    id: 'repository',
    label: '所属仓库',
    ariaLabel: '所属仓库',
    placeholder: 'git-guild / frontend',
  },
  {
    id: 'branch',
    label: '任务分支（自动）',
    ariaLabel: '任务分支',
    placeholder: '接取后自动创建',
  },
  {
    id: 'submission-note',
    label: '提交说明',
    ariaLabel: '提交说明',
    type: 'textarea',
    rows: 4,
    placeholder: '本次修改、测试结果、完成标准自检情况',
    wide: true,
  },
]

export const submissionChecks = [
  '已关联接取的委托编号',
  '改动已推送到任务分支',
  '完成标准逐项自检',
]

// Each step exposes a `state` used by the parchment-themed stepper:
//   done   — passed, filled circle in green
//   active — current spotlight, wine-red halo
//   next   — upcoming, muted dot
// SubmissionCounter.vue mutates these states based on the live submission
// stage, so the values below are just the resting (draft) snapshot.
export const submissionReviewSteps = [
  { label: '草稿', note: '补全成果说明与提交前自检', state: 'active' },
  { label: '已提交', note: '成果已放入柜台审核队列', state: 'next' },
  { label: '审核中', note: '维护者查看差异与运行结果', state: 'next' },
  { label: '成长记录', note: '通过后写入 XP 与贡献记录', state: 'next' },
]

export const reviewerSlot = {
  eyebrow: '审核反馈',
  title: '维护者反馈位',
  body: '审核意见、退回原因和学习建议集中放在这里，避免用户在任务、PR、通知之间来回找反馈。',
}

// Evidence categories the clerk accepts at the counter. The glyph doubles as
// a tiny rune on the chip — kept to single Unicode characters so we don't
// have to ship an icon set just for four buttons.
export const evidenceTypes = [
  { id: 'screenshot', label: '运行截图', glyph: '◈', hint: '功能跑通的截图或录屏链接' },
  { id: 'log', label: '测试日志', glyph: '≡', hint: '单元 / 端到端测试输出' },
  { id: 'design', label: '设计稿', glyph: '◐', hint: 'Figma 或评审稿链接' },
  { id: 'doc', label: '辅助文档', glyph: '✦', hint: '说明、迁移指南或风险记录' },
]

// Map of submission stage → ribbon label and color hint used by the head badge.
export const stageRibbon = {
  draft: { label: '草稿', tone: 'draft' },
  submitting: { label: '呈递中', tone: 'busy' },
  submitted: { label: '审核中', tone: 'sealed' },
  error: { label: '需补正', tone: 'warn' },
}
