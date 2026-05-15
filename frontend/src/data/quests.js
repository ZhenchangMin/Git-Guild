export const questDetails = {
  'QST-0412': {
    description:
      '仓库管理页需要让维护者和冒险家看到 Issue 同步是否正常，包括最近同步时间、Webhook 状态和失败后的处理建议。',
    estimatedHours: 5,
    workflowState: 'in-review',
    repository: {
      name: 'git-guild / backend',
      branch: 'main',
      syncStatus: 'Sync warning',
    },
    issue: {
      number: '#31',
      title: 'Issue sync status is invisible on repository page',
      status: 'Open',
    },
    pr: {
      number: 'PR #18',
      status: 'Open',
    },
  },
  'QST-0427': {
    description:
      '当前成果提交表单把任务、分支、PR 和审核材料混在一起，新手不容易判断哪些信息在工作台完成、哪些信息要到提交柜台登记。本任务需要重整提交路径，让 PR 链接、提交说明和退回修改说明更清楚。',
    estimatedHours: 6,
    workflowState: 'available',
    repository: {
      name: 'git-guild / frontend',
      branch: 'main',
      syncStatus: 'Synced',
    },
    issue: {
      number: '#42',
      title: 'Submission flow needs clearer PR and review linkage',
      status: 'Open',
    },
    pr: {
      number: 'Not created',
      status: 'Not started',
    },
  },
  'QST-0431': {
    description:
      '仓库从 GitHub 导入或同步失败时，维护者需要知道失败原因、影响范围和下一步重试入口。本任务聚焦导入异常视图，不处理底层同步算法。',
    estimatedHours: 4,
    workflowState: 'pr-ready',
    repository: {
      name: 'git-guild / frontend',
      branch: 'main',
      syncStatus: 'Synced',
    },
    issue: {
      number: '#38',
      title: 'Repository import failures need an actionable view',
      status: 'Open',
    },
    pr: {
      number: 'PR #21',
      status: 'Review requested',
    },
  },
  'QST-0438': {
    description:
      '任务详情页需要把贡献流程拆成可执行步骤，帮助第一次参与项目的新手知道从查看 Issue 到提交审核的完整路径。',
    estimatedHours: 3,
    workflowState: 'available',
    repository: {
      name: 'git-guild / docs',
      branch: 'main',
      syncStatus: 'Synced',
    },
    issue: {
      number: '#44',
      title: 'Beginner contribution checklist is missing',
      status: 'Open',
    },
    pr: {
      number: 'Not created',
      status: 'Not started',
    },
  },
  'QST-0440': {
    description:
      '任务板筛选条件应在用户返回页面时保留，避免用户查看详情后丢失上下文。该任务只处理本地状态模拟，不接真实后端。',
    estimatedHours: 4,
    workflowState: 'in-progress',
    repository: {
      name: 'git-guild / frontend',
      branch: 'main',
      syncStatus: 'Synced',
    },
    issue: {
      number: '#45',
      title: 'Quest filters should survive returning from detail pages',
      status: 'Open',
    },
    pr: {
      number: 'Not created',
      status: 'Not started',
    },
  },
  'QST-0444': {
    description:
      '用户完成任务后需要能回看审核意见和学习建议。本任务将最近审核反馈归档到个人资料中，突出可执行修改建议。',
    estimatedHours: 7,
    workflowState: 'changes-requested',
    repository: {
      name: 'git-guild / frontend',
      branch: 'main',
      syncStatus: 'Synced',
    },
    issue: {
      number: '#47',
      title: 'Review feedback should be visible from user profile',
      status: 'Open',
    },
    pr: {
      number: 'PR #23',
      status: 'Changes requested',
    },
  },
}

export const defaultContributionSteps = [
  '查看关联 Issue、仓库说明和完成标准。',
  '接取任务，确认任务状态变为进行中。',
  '进入工作台，在关联仓库中创建任务分支。',
  '在工作台上传变更文件并生成 commit。',
  '在工作台发起 Pull Request，等待基础检查。',
  '到提交柜台填写成果说明并提交审核。',
]

export const defaultSubmissionRequirements = [
  '关联任务编号和任务标题。',
  '关联 Pull Request 链接。',
  '成果说明，说明本次修改解决了什么问题。',
  '完成标准逐项自检。',
  '测试说明或运行截图；MVP 中附件可以为空。',
]
