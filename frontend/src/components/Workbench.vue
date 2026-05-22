<script setup>
import { computed, ref, watch } from 'vue'

import QuestStatusFlow from './QuestStatusFlow.vue'
import {
  maintainerIssueBacklog,
  maintainerNotifications,
  maintainerPublishedQuests,
  maintainerWorkbenchStats,
  notifications,
  pullRequests,
  recentContributions,
  repositories,
  reviewQueue,
  reviewFeedbacks,
  taskGroups,
  workbenchEmails,
  workbenchStats,
  workbenchUser,
} from '../data/workbench'

const emit = defineEmits(['open-submission', 'open-id-card'])

const props = defineProps({
  initialView: {
    type: String,
    default: 'adventurer',
  },
})

const workbenchModes = ['adventurer', 'guild-master']

function normalizeWorkbenchView(view) {
  return workbenchModes.includes(view) ? view : 'adventurer'
}

const workbenchView = ref(normalizeWorkbenchView(props.initialView))
const maintainerReviews = ref(reviewQueue.map((review) => ({ ...review, checklist: [...review.checklist] })))
const mailboxMessages = ref(workbenchEmails.map((email) => ({ ...email, body: [...email.body] })))
const taskGroupList = ref(
  taskGroups.map((group) => ({
    ...group,
    tasks: group.tasks.map((task) => ({ ...task, actions: [...task.actions] })),
  })),
)
const repositoryList = ref(repositories.map((repository) => ({ ...repository })))
const pullRequestList = ref(pullRequests.map((pullRequest) => ({ ...pullRequest })))
const isMailboxOpen = ref(false)
const selectedTaskId = ref(null)
const selectedEmailId = ref(null)
const selectedRepositoryName = ref(null)
const selectedNotificationText = ref(null)
const selectedFeedbackId = ref(null)
const selectedReviewId = ref(reviewQueue[0]?.id ?? null)
const selectedPublishingIssueId = ref(maintainerIssueBacklog[0]?.id ?? null)
const taskDraft = ref(createDraftFromIssue(maintainerIssueBacklog[0]))
const publishingReviewStatus = ref({
  label: '草稿待检查',
  body: '选择左侧 Issue 后会自动生成悬赏任务草稿，补齐字段后可提交管理员审核。',
  submittedAt: '',
})
const isGrowthDetailOpen = ref(false)
const operationResult = ref({
  title: 'Git 操作',
  body: '选择创建分支、上传提交或发起 PR 后，这里会展示模拟结果。',
})
const simulatedCommitIndex = ref(2)
const simulatedPullRequestIndex = ref(24)

const allTasks = computed(() =>
  taskGroupList.value.flatMap((group) =>
    group.tasks.map((task) => ({
      ...task,
      statusLabel: group.label,
      workflowState: group.id,
    })),
  ),
)
const selectedTask = computed(() => allTasks.value.find((task) => task.id === selectedTaskId.value) ?? null)
const selectedEmail = computed(() => mailboxMessages.value.find((email) => email.id === selectedEmailId.value) ?? null)
const selectedNotification = computed(() =>
  notifications.find((notification) => notification.text === selectedNotificationText.value) ?? null,
)
const selectedFeedback = computed(() =>
  reviewFeedbacks.find((feedback) => feedback.id === selectedFeedbackId.value) ?? null,
)
const feedbackTaskFilter = ref('all')
const feedbackStatusFilter = ref('all')
const unreadMailCount = computed(() => mailboxMessages.value.filter((email) => email.unread).length)
const displayStats = computed(() =>
  workbenchView.value === 'guild-master'
    ? maintainerWorkbenchStats
    : workbenchStats.map((stat) => (stat.label === '未读邮件' ? { ...stat, value: unreadMailCount.value } : stat)),
)
const selectedTaskRepository = computed(() =>
  repositoryList.value.find((repository) => repository.name === selectedTask.value?.repository),
)
const selectedRepository = computed(() =>
  repositoryList.value.find((repository) => repository.name === selectedRepositoryName.value) ?? null,
)
const selectedTaskPullRequests = computed(() => {
  if (!selectedTask.value) return []

  const prId = selectedTask.value.prNumber || selectedTask.value.prStatus.match(/PR #\d+/)?.[0]
  return pullRequestList.value.filter((pr) => pr.id === prId || pr.taskId === selectedTask.value.id)
})
const selectedRepositoryTasks = computed(() =>
  allTasks.value.filter((task) => task.repository === selectedRepository.value?.name),
)
const selectedFeedbackTask = computed(() =>
  allTasks.value.find((task) => task.id === selectedFeedback.value?.questId) ?? null,
)
const feedbackTaskOptions = computed(() => [
  { value: 'all', label: '全部任务' },
  ...reviewFeedbacks.map((feedback) => ({
    value: feedback.questId,
    label: `${feedback.questId} · ${feedback.questTitle}`,
  })),
])
const feedbackStatusOptions = [
  { value: 'all', label: '全部状态' },
  { value: 'approved', label: '通过' },
  { value: 'changes-requested', label: '退回修改' },
  { value: 'in-review', label: '待复审' },
]
const feedbackArchiveStats = computed(() => {
  const counts = reviewFeedbacks.reduce(
    (summary, feedback) => {
      summary[feedback.status] = (summary[feedback.status] ?? 0) + 1
      return summary
    },
    { approved: 0, 'changes-requested': 0, 'in-review': 0 },
  )

  return [
    { label: '通过', value: counts.approved },
    { label: '退回修改', value: counts['changes-requested'] },
    { label: '待复审', value: counts['in-review'] },
  ]
})
const filteredFeedbacks = computed(() =>
  reviewFeedbacks.filter(
    (feedback) =>
      (feedbackTaskFilter.value === 'all' || feedback.questId === feedbackTaskFilter.value) &&
      (feedbackStatusFilter.value === 'all' || feedback.status === feedbackStatusFilter.value),
  ),
)
const selectedFeedbackStatusMeta = computed(() => getFeedbackStatusMeta(selectedFeedback.value?.status))
const selectedFeedbackFlowStatus = computed(() => selectedFeedback.value?.flowStatus ?? selectedFeedback.value?.status ?? 'review')
const canResubmitSelectedFeedback = computed(() => selectedFeedback.value?.status === 'changes-requested')
const displayableContributionRecords = computed(() =>
  reviewFeedbacks.filter((feedback) => feedback.status === 'approved' && feedback.contributionRecord),
)
const archivedXpEarned = computed(() =>
  reviewFeedbacks.reduce((total, feedback) => total + (Number(feedback.xpEarned) || 0), 0),
)
const selectedTaskFlowContext = computed(() => {
  if (!selectedTask.value) return {}

  return {
    quest: `${selectedTask.value.id} · ${selectedTask.value.title}`,
    repository: selectedTask.value.repository,
    branch: selectedTask.value.branch ? `任务分支：${selectedTask.value.branch}` : '任务分支未创建',
    prStatus: selectedTask.value.prStatus,
    counter:
      selectedTask.value.workflowState === 'in-progress'
        ? 'PR 准备好后到提交柜台登记'
        : selectedTask.value.workflowState === 'changes-requested'
          ? '修改完成后重新提交成果'
          : '已登记成果或等待记录归档',
    feedback:
      selectedTask.value.workflowState === 'changes-requested'
        ? '维护者已退回，请查看逐项反馈'
        : selectedTask.value.workflowState === 'completed'
          ? '审核通过，已写入成长记录'
          : '等待审核结果或通知',
    syncStatus: selectedTaskRepository.value?.syncStatus,
    viewer: workbenchUser.name,
  }
})
const linkPanelTask = computed(() => {
  if (selectedTask.value) return selectedTask.value
  if (!selectedRepository.value) return null

  return (
    selectedRepositoryTasks.value.find((task) => task.workflowState !== 'completed') ??
    selectedRepositoryTasks.value[0] ??
    null
  )
})
const linkPanelRepository = computed(() =>
  repositoryList.value.find((repository) => repository.name === linkPanelTask.value?.repository) ?? selectedRepository.value,
)
const linkPanelPullRequest = computed(() => {
  if (!linkPanelTask.value) return null

  return (
    pullRequestList.value.find(
      (pullRequest) => pullRequest.taskId === linkPanelTask.value.id || pullRequest.id === linkPanelTask.value.prNumber,
    ) ?? null
  )
})
const linkPanelChecks = computed(() => {
  if (!linkPanelTask.value) return []

  return [
    {
      label: '任务分支',
      passed: Boolean(linkPanelTask.value.branch),
      detail: linkPanelTask.value.branch || '还没有创建任务分支。',
    },
    {
      label: '最近 commit',
      passed: linkPanelTask.value.recentCommit !== '待上传',
      detail: linkPanelTask.value.recentCommit || '还没有上传提交。',
    },
    {
      label: 'PR 与检查',
      passed: Boolean(linkPanelTask.value.prNumber) && !linkPanelTask.value.checkResult.includes('未通过'),
      detail: linkPanelTask.value.prNumber
        ? `${linkPanelTask.value.prNumber} · ${linkPanelTask.value.prState} · ${linkPanelTask.value.checkResult}`
        : '还没有创建 PR。',
    },
    {
      label: '提交柜台',
      passed: ['待登记成果', '需重新提交', '已登记成果', '已归档'].includes(linkPanelTask.value.counterLink),
      detail: linkPanelTask.value.counterDetail,
    },
  ]
})
const canOpenSubmissionCounter = computed(() => {
  if (!linkPanelTask.value) return false

  return Boolean(linkPanelTask.value.prNumber) && linkPanelTask.value.recentCommit !== '待上传'
})
const selectedFeedbackFlowContext = computed(() => {
  if (!selectedFeedback.value) return {}

  const isReturned = selectedFeedback.value.status === 'changes-requested'
  const isApproved = selectedFeedback.value.status === 'approved'

  return {
    quest: `${selectedFeedback.value.questId} · ${selectedFeedback.value.questTitle}`,
    repository: selectedFeedback.value.repository,
    branch: isReturned
      ? `继续更新 ${selectedFeedback.value.pullRequest} 对应任务分支`
      : isApproved
        ? '任务分支已完成，可在贡献记录中回看'
        : '保持任务分支可同步，等待复审结果',
    pullRequest: `${selectedFeedback.value.pullRequest} · ${selectedFeedback.value.pullRequestTitle}`,
    counter: isReturned ? '修复后回到提交柜台重新提交成果记录' : '当前无需重新提交成果',
    feedback: `${selectedFeedback.value.conclusion} · ${selectedFeedback.value.reviewedAt}`,
    syncStatus: repositoryList.value.find((repository) => repository.name === selectedFeedback.value.repository)?.syncStatus,
    viewer: workbenchUser.name,
  }
})
const xpProgress = computed(() => `${Math.round((workbenchUser.xpCurrent / workbenchUser.xpTarget) * 100)}%`)
const selectedReview = computed(() =>
  maintainerReviews.value.find((review) => review.id === selectedReviewId.value) ?? maintainerReviews.value[0] ?? null,
)
const selectedPublishingIssue = computed(() =>
  maintainerIssueBacklog.find((issue) => issue.id === selectedPublishingIssueId.value) ?? maintainerIssueBacklog[0] ?? null,
)
const draftCompletionItems = computed(() =>
  taskDraft.value.completionStandards
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean),
)
const clarityChecks = computed(() => [
  {
    label: 'Issue 来源明确',
    passed: Boolean(taskDraft.value.issueId && taskDraft.value.repository),
    detail: taskDraft.value.issueId
      ? `${taskDraft.value.issueId} · ${taskDraft.value.repository}`
      : '需要先从左侧选择一个 Issue。',
  },
  {
    label: '任务标题和摘要可读',
    passed: taskDraft.value.title.trim().length >= 8 && taskDraft.value.summary.trim().length >= 24,
    detail: '标题至少 8 个字符，摘要需要说明背景、目标和用户价值。',
  },
  {
    label: '难度、技术栈和奖励完整',
    passed: Boolean(
      taskDraft.value.difficulty.trim() && taskDraft.value.techStack.trim() && taskDraft.value.reward.trim(),
    ),
    detail: '三项会影响推荐、接取预期和课程激励。',
  },
  {
    label: '完成标准可验收',
    passed: draftCompletionItems.value.length >= 3,
    detail: `当前 ${draftCompletionItems.value.length} 项，建议至少 3 项并且每项能被维护者检查。`,
  },
])
const claritySummary = computed(() => {
  const passed = clarityChecks.value.filter((item) => item.passed).length
  return { passed, total: clarityChecks.value.length }
})
const isDraftReady = computed(() => claritySummary.value.passed === claritySummary.value.total)
const activeRoleLabel = computed(() => (workbenchView.value === 'guild-master' ? '委托人' : workbenchUser.role))

function createDraftFromIssue(issue) {
  if (!issue) {
    return {
      issueId: '',
      repository: '',
      title: '',
      summary: '',
      difficulty: '',
      techStack: '',
      reward: '',
      completionStandards: '',
    }
  }

  return {
    issueId: issue.id,
    repository: issue.repository,
    title: issue.title,
    summary: issue.summary,
    difficulty: issue.suggestedDifficulty,
    techStack: issue.suggestedTechStack,
    reward: issue.suggestedReward,
    completionStandards: issue.suggestedStandards.join('\n'),
  }
}

function switchWorkbenchView(view) {
  workbenchView.value = view
  isMailboxOpen.value = false
  operationResult.value =
    view === 'guild-master'
      ? {
          title: '维护者工作台已打开',
          body: '审核队列、提交详情和反馈操作已准备好。点击队列中的提交可以查看逐项检查。',
        }
      : {
          title: '冒险家工作台已打开',
          body: '我的待办、仓库操作、邮件通知和成长记录已恢复。',
        }
}

watch(
  () => props.initialView,
  (nextView) => {
    workbenchView.value = normalizeWorkbenchView(nextView)
  },
)

function selectReview(review) {
  selectedReviewId.value = review.id
  operationResult.value = {
    title: `${review.id} 已选中`,
    body: `正在查看 ${review.questId} 的提交详情、完成标准和审核建议。`,
  }
}

function selectPublishingIssue(issue) {
  selectedPublishingIssueId.value = issue.id
  taskDraft.value = createDraftFromIssue(issue)
  publishingReviewStatus.value = {
    label: '草稿已生成',
    body: `${issue.id} 的标题、摘要和关联仓库已带入草稿。请检查奖励、技术栈和完成标准。`,
    submittedAt: '',
  }
  operationResult.value = {
    title: `${issue.id} 已生成任务草稿`,
    body: `维护者发布工坊已载入 ${issue.repository} 的 Issue 上下文，可继续做清晰度检查。`,
  }
}

function submitTaskDraftForReview() {
  if (!isDraftReady.value) {
    publishingReviewStatus.value = {
      label: '清晰度检查未通过',
      body: '请先补齐待补项，再提交给管理员审核。',
      submittedAt: '刚刚',
    }
    operationResult.value = {
      title: '任务发布申请暂未提交',
      body: '清晰度检查仍有待补项，课堂演示可继续编辑草稿字段并再次提交。',
    }
    return
  }

  publishingReviewStatus.value = {
    label: '已提交管理员审核',
    body: `${taskDraft.value.issueId} 已作为悬赏任务草稿提交，管理员审核通过后会出现在悬赏任务板。`,
    submittedAt: '刚刚',
  }
  operationResult.value = {
    title: '任务发布申请已提交',
    body: `${taskDraft.value.title} 已进入管理员审核队列，当前为本地 mock 状态。`,
  }
}

function runReviewAction(action, review = selectedReview.value) {
  if (!review) return

  const resultMap = {
    approve: ['审核已通过', `${review.questId} 已通过审核，冒险家侧将收到成长记录和完成通知。`],
    changes: ['修改请求已发送', `${review.questId} 的逐项反馈已发送，冒险家工作台将出现待修改事项。`],
    reject: ['提交已驳回', `${review.questId} 已标记为驳回，任务仍需维护者后续处理。`],
    draft: ['草稿已保存', `${review.questId} 的审核意见已保存为本地模拟草稿。`],
    pr: ['PR 状态已打开', `${review.pullRequest} 的检查结果和审核状态已定位。`],
  }

  const [title, body] = resultMap[action] ?? ['审核操作已记录', `${review.questId} 的模拟操作已完成。`]
  operationResult.value = { title, body }
}

function selectTask(task) {
  selectedTaskId.value = task.id
  selectedEmailId.value = null
  selectedRepositoryName.value = null
  selectedNotificationText.value = null
  selectedFeedbackId.value = null
  isGrowthDetailOpen.value = false
  operationResult.value = {
    title: `${task.id} 已选中`,
    body: `中间区域正在显示 ${task.title} 的仓库、Issue、PR 状态和下一步操作。`,
  }
}

function selectEmail(email) {
  selectedEmailId.value = email.id
  selectedTaskId.value = null
  selectedRepositoryName.value = null
  selectedNotificationText.value = null
  selectedFeedbackId.value = null
  isGrowthDetailOpen.value = false
  isMailboxOpen.value = false
  email.unread = false
  operationResult.value = {
    title: '邮件已打开',
    body: `${email.subject} 已显示在中间详情区。`,
  }
}

function selectRepository(repository) {
  selectedRepositoryName.value = repository.name
  selectedTaskId.value = null
  selectedEmailId.value = null
  selectedNotificationText.value = null
  selectedFeedbackId.value = null
  isGrowthDetailOpen.value = false
  operationResult.value = {
    title: `${repository.name} 已打开`,
    body: '中间区域正在显示该仓库的同步状态、分支、Issue、PR 和常用 Git 操作。',
  }
}

function selectNotification(notification) {
  selectedNotificationText.value = notification.text
  selectedTaskId.value = null
  selectedEmailId.value = null
  selectedRepositoryName.value = null
  selectedFeedbackId.value = null
  isGrowthDetailOpen.value = false
  isMailboxOpen.value = false
  operationResult.value = {
    title: '通知已打开',
    body: `${notification.type}：${notification.text}`,
  }
}

function showGrowthDetails(source = '个人成长档案') {
  isGrowthDetailOpen.value = true
  selectedTaskId.value = null
  selectedEmailId.value = null
  selectedRepositoryName.value = null
  selectedNotificationText.value = null
  selectedFeedbackId.value = null
  operationResult.value = {
    title: '成长详情已打开',
    body: `${source} 已显示 Level、XP、完成任务数和最近贡献记录。`,
  }
}

function openIdCard() {
  emit('open-id-card')
}

function resolveNotificationAction(notification) {
  const actionMap = {
    查看反馈: 'feedback',
    '查看 PR': 'pr-view',
    查看任务: 'feedback',
    处理异常: 'exception',
  }
  return { type: actionMap[notification.action] ?? 'feedback', feedbackId: notification.feedbackId }
}

function getFeedbackStatusMeta(status) {
  const statusMap = {
    approved: { label: '通过', tone: 'approved', flowStatus: 'approved' },
    'changes-requested': { label: '退回修改', tone: 'warning', flowStatus: 'changes-requested' },
    'in-review': { label: '待复审', tone: 'review', flowStatus: 'in-review' },
  }

  return statusMap[status] ?? { label: '待复审', tone: 'review', flowStatus: 'in-review' }
}

function selectFeedbackArchive(feedback) {
  selectedFeedbackId.value = feedback.id
  selectedTaskId.value = null
  selectedEmailId.value = null
  selectedRepositoryName.value = null
  selectedNotificationText.value = null
  isGrowthDetailOpen.value = false
  isMailboxOpen.value = false
  operationResult.value = {
    title: `${feedback.questId} 反馈已打开`,
    body: `当前归档状态为“${getFeedbackStatusMeta(feedback.status).label}”。筛选结果已定位到这条审核记录。`,
  }
}

function setFeedbackTaskFilter(value) {
  feedbackTaskFilter.value = value
  operationResult.value = {
    title: '反馈归档已按任务筛选',
    body: value === 'all' ? '当前显示全部任务的审核意见。' : `当前只显示 ${value} 的审核意见。`,
  }
}

function setFeedbackStatusFilter(value) {
  feedbackStatusFilter.value = value
  const label = feedbackStatusOptions.find((option) => option.value === value)?.label ?? '全部状态'
  operationResult.value = {
    title: '反馈归档已按状态筛选',
    body: `当前筛选状态：${label}。点击任一归档卡片可在右侧查看详情。`,
  }
}

function runEmailAction(action, email) {
  if (!email) return

  if (action === 'read') {
    email.unread = false
    operationResult.value = { title: '邮件已标记为已读', body: email.subject }
    return
  }

  if (action === 'unread') {
    email.unread = true
    operationResult.value = { title: '邮件已标记为未读', body: email.subject }
    return
  }

  const resultMap = {
    reply: ['回复草稿已创建', `正在回复 ${email.from}：${email.subject}`],
    archive: ['邮件已归档', `${email.subject} 已移动到归档箱。`],
    delete: ['删除确认已触发', `${email.subject} 已进入删除确认流程。`],
  }
  const [title, body] = resultMap[action] ?? ['邮件操作已记录', email.subject]
  operationResult.value = { title, body }
}

function findTaskGroup(taskId) {
  return taskGroupList.value.find((group) => group.tasks.some((task) => task.id === taskId)) ?? null
}

function findTaskRecord(taskId) {
  return findTaskGroup(taskId)?.tasks.find((task) => task.id === taskId) ?? null
}

function findRepositoryRecord(repositoryName) {
  return repositoryList.value.find((repository) => repository.name === repositoryName) ?? null
}

function findActionTaskRecord() {
  if (selectedTask.value) return findTaskRecord(selectedTask.value.id)
  if (!selectedRepository.value) return null

  const task = selectedRepositoryTasks.value.find((item) => item.workflowState !== 'completed') ?? selectedRepositoryTasks.value[0]
  return task ? findTaskRecord(task.id) : null
}

function updateRepositoryActivity(task) {
  const repository = findRepositoryRecord(task.repository)
  if (!repository) return

  if (task.branch && !repository.activeBranchAdded) {
    repository.branches += 1
    repository.activeBranchAdded = true
  }

  if (task.recentCommit && task.recentCommit !== '待上传') {
    repository.lastCommit = task.recentCommit
  }
}

function ensureTaskBranch(task) {
  if (task.branch) return false

  task.branch = `feature/${task.id.toLowerCase()}-${task.title.toLowerCase().replaceAll(' ', '-')}`
  task.nextStep = '上传提交后创建 PR'
  task.counterLink = '未登记'
  task.counterDetail = '分支已创建，但还没有 commit 和 PR；任务成果暂时不能去提交柜台登记。'
  updateRepositoryActivity(task)
  return true
}

function ensureTaskCommit(task) {
  ensureTaskBranch(task)
  const commitId = `c0ffee${simulatedCommitIndex.value}`
  simulatedCommitIndex.value += 1
  task.recentCommit = commitId
  task.nextStep = task.prNumber ? '同步 PR 状态后去提交柜台登记成果' : '创建 PR 并等待基础检查'
  task.counterLink = task.prNumber ? '待登记成果' : '未登记'
  task.counterDetail = task.prNumber
    ? '项目提交已上传到当前 PR；任务成果提交仍需到提交柜台完成。'
    : 'commit 已上传，但还没有 PR；提交柜台需要 PR 链接后才能登记成果。'
  updateRepositoryActivity(task)
  return commitId
}

function ensureTaskPullRequest(task) {
  ensureTaskCommit(task)

  if (!task.prNumber) {
    const prId = `PR #${simulatedPullRequestIndex.value}`
    simulatedPullRequestIndex.value += 1
    task.prNumber = prId
    task.prState = '打开'
    task.prStatus = `${prId} 打开`
    task.checkResult = '基础检查排队中'
    pullRequestList.value.push({
      id: prId,
      taskId: task.id,
      title: `${task.id} ${task.title}`,
      status: '打开',
      checks: '基础检查排队中',
      action: '查看 PR',
    })

    const repository = findRepositoryRecord(task.repository)
    if (repository) repository.pullRequests += 1
  }

  task.counterLink = '待登记成果'
  task.counterDetail = '项目提交已在工作台完成；任务成果还没有在提交柜台登记。'
  task.nextStep = '基础检查通过后去提交柜台登记成果'
}

function syncTaskPullRequest(task) {
  if (!task.prNumber) {
    ensureTaskPullRequest(task)
  }

  const pullRequest = pullRequestList.value.find((item) => item.id === task.prNumber)
  const taskGroup = findTaskGroup(task.id)
  const isRevision = taskGroup?.id === 'changes-requested' || ['Changes requested', '退回修改'].includes(task.prState)
  task.prState = isRevision ? '待审核' : '待审核'
  task.prStatus = `${task.prNumber} ${task.prState}`
  task.checkResult = isRevision ? '复审检查通过' : '基础检查通过'
  task.nextStep = isRevision ? '去提交柜台重新提交成果' : '去提交柜台登记任务成果'
  task.counterLink = isRevision ? '需重新提交' : '待登记成果'
  task.counterDetail = isRevision
    ? 'PR 已更新且复审检查通过；现在需要到提交柜台重新提交任务成果。'
    : 'PR 检查已通过；现在可以到提交柜台提交任务成果说明。'

  if (pullRequest) {
    pullRequest.status = task.prState
    pullRequest.checks = task.checkResult
  }
}

function runLinkedGitAction(actionType, source) {
  const task = findActionTaskRecord()

  if (!task) {
    operationResult.value = {
      title: '没有可联动的任务',
      body: `${source} 当前没有关联任务，无法模拟任务分支、commit 或 PR。`,
    }
    return true
  }

  if (actionType === 'branch') {
    const created = ensureTaskBranch(task)
    operationResult.value = {
      title: created ? '已创建任务分支' : '任务分支已存在',
      body: `${task.id} 当前分支为 ${task.branch}。这一步只完成项目代码工作，还不能提交任务成果。`,
    }
    return true
  }

  if (actionType === 'commit') {
    const commitId = ensureTaskCommit(task)
    operationResult.value = {
      title: '上传提交已记录',
      body: `${task.id} 已生成最近 commit ${commitId}。还需要 PR 通过检查后，才能到提交柜台登记任务成果。`,
    }
    return true
  }

  if (actionType === 'pull-request') {
    ensureTaskPullRequest(task)
    operationResult.value = {
      title: 'PR 已创建',
      body: `${task.id} 已关联 ${task.prNumber}，检查结果为“${task.checkResult}”。项目提交已在工作台完成，任务成果提交仍在提交柜台完成。`,
    }
    return true
  }

  if (actionType === 'sync-pr') {
    syncTaskPullRequest(task)
    operationResult.value = {
      title: 'PR 状态已同步',
      body: `${task.id} 当前为 ${task.prNumber} · ${task.prState} · ${task.checkResult}。${task.counterDetail}`,
    }
    return true
  }

  return false
}

function runAction(action, source = '当前事项') {
  if (action.type === 'submit') {
    const taskId = action.questId ?? selectedFeedback.value?.questId ?? selectedTask.value?.id ?? linkPanelTask.value?.id
    const task = taskId ? findTaskRecord(taskId) : null

    if (task && (!task.prNumber || task.recentCommit === '待上传')) {
      operationResult.value = {
        title: '暂时不能提交任务成果',
        body: `${task.id} 还缺少 ${!task.branch ? '任务分支、' : ''}${
          task.recentCommit === '待上传' ? 'commit、' : ''
        }${!task.prNumber ? 'PR' : ''}。请先在工作台完成项目提交，再去提交柜台提交任务成果。`,
      }
      return
    }

    emit('open-submission', taskId)
    return
  }

  if (action.type === 'feedback') {
    openFeedback(action.feedbackId ?? selectedTask.value?.feedbackId, source)
    return
  }

  if (action.type === 'growth') {
    showGrowthDetails(source)
    return
  }

  if (action.type === 'repository' && selectedTaskRepository.value && !selectedRepository.value) {
    selectRepository(selectedTaskRepository.value)
    return
  }

  if (['branch', 'commit', 'pull-request', 'sync-pr'].includes(action.type)) {
    if (runLinkedGitAction(action.type, source)) return
  }

  const resultMap = {
    feedback: ['反馈已打开', `${source} 的退回意见已定位到审核反馈区。`],
    repository: ['仓库视图已打开', `${source} 的分支、提交、Issue 和 PR 摘要已定位。`],
    history: ['提交记录已打开', `${source} 的最近提交记录已定位。`],
    'pr-view': ['PR 状态已打开', `${source} 的 PR 检查状态已定位。`],
    contribution: ['贡献记录已打开', `${source} 已完成并写入个人贡献记录。`],
    growth: ['成长结果已打开', `${source} 对应 XP 和等级进度已更新。`],
    exception: ['同步状态已刷新', `${source} 已完成一次模拟同步；如果 PR 刚创建，请继续点击“同步 PR 状态”查看检查结果。`],
  }
  const [title, body] = resultMap[action.type] ?? ['操作已记录', `${source} 的模拟操作已完成。`]
  operationResult.value = { title, body }
}

function openFeedback(feedbackId, source = '审核反馈') {
  const fallbackId = reviewFeedbacks.find((feedback) => feedback.questId === selectedTask.value?.id)?.id
  selectedFeedbackId.value = feedbackId ?? fallbackId ?? reviewFeedbacks[0]?.id ?? null
  isGrowthDetailOpen.value = false
  isMailboxOpen.value = false
  operationResult.value = {
    title: '反馈详情已打开',
    body: `${source} 的逐项审核意见已显示。请先在工作台更新 PR，再到提交柜台重新提交成果。`,
  }
}
</script>

<template>
  <div class="workbench-workspace" aria-label="工作台">
    <header class="workbench-statusbar">
      <div class="workbench-user">
        <p class="kicker">工作台</p>
        <h1>工作台</h1>
        <div class="user-identity">
          <span class="user-avatar" aria-hidden="true">{{ workbenchUser.name.slice(0, 1) }}</span>
          <div>
            <strong>{{ workbenchUser.name }}</strong>
            <span>{{ activeRoleLabel }}</span>
          </div>
        </div>
        <div class="view-switch" aria-label="工作台视图切换">
          <button
            type="button"
            :class="{ active: workbenchView === 'adventurer' }"
            @click="switchWorkbenchView('adventurer')"
          >
            冒险家视图
          </button>
          <button
            type="button"
            :class="{ active: workbenchView === 'guild-master' }"
            @click="switchWorkbenchView('guild-master')"
          >
            委托人视图
          </button>
        </div>
      </div>

      <div class="workbench-level">
        <div class="level-head">
          <div>
            <strong>Level {{ workbenchUser.level }}</strong>
            <span>{{ workbenchUser.xpCurrent }} / {{ workbenchUser.xpTarget }} XP</span>
          </div>
          <button class="quiet-action detail-link" type="button" @click="openIdCard">
            查看详细信息
          </button>
        </div>
        <div class="xp-track" aria-label="XP 进度">
          <span :style="{ width: xpProgress }"></span>
        </div>
      </div>

      <dl class="workbench-stat-grid">
        <div v-for="stat in displayStats" :key="stat.label">
          <dt>{{ stat.label }}</dt>
          <dd>{{ stat.value }}</dd>
        </div>
      </dl>

      <div class="repository-shortcuts" aria-label="仓库快捷入口">
        <span>仓库</span>
        <button
          v-for="repository in repositoryList"
          :key="repository.name"
          class="repository-chip"
          :class="{ active: selectedRepositoryName === repository.name }"
          type="button"
          @click="selectRepository(repository)"
        >
          <strong>{{ repository.name.replace('git-guild / ', '') }}</strong>
          <small :class="{ warning: repository.syncStatus.includes('warning') }">{{ repository.syncStatus }}</small>
        </button>
      </div>

      <div class="mailbox-area">
        <button
          class="mail-button"
          type="button"
          aria-label="打开邮箱"
          :aria-expanded="isMailboxOpen"
          @click="isMailboxOpen = !isMailboxOpen"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h16v11H4z" />
            <path d="m4 7 8 6 8-6" />
          </svg>
          <span v-if="unreadMailCount > 0" class="mail-alert" aria-label="有未读邮件">!</span>
        </button>

        <section v-if="isMailboxOpen" class="mailbox-popover" aria-label="收到的邮件">
          <div class="mailbox-head">
            <div>
              <p class="kicker">Mailbox</p>
              <h2>收到的邮件</h2>
            </div>
            <span>{{ unreadMailCount }} 未读</span>
          </div>

          <button
            v-for="email in mailboxMessages"
            :key="email.id"
            class="mail-preview"
            :class="{ unread: email.unread, active: selectedEmailId === email.id }"
            type="button"
            @click="selectEmail(email)"
          >
            <span>{{ email.from }} · {{ email.receivedAt }}</span>
            <strong>{{ email.subject }}</strong>
            <small>{{ email.preview }}</small>
          </button>

          <div class="mailbox-section-title">系统通知</div>
          <button
            v-for="notice in notifications"
            :key="notice.text"
            class="mail-preview notice-preview"
            :class="{ active: selectedNotificationText === notice.text }"
            type="button"
            @click="selectNotification(notice)"
          >
            <span>{{ notice.type }}</span>
            <strong>{{ notice.text }}</strong>
            <small>操作：{{ notice.action }}</small>
          </button>
        </section>
      </div>
    </header>

    <aside v-if="workbenchView === 'adventurer'" class="workbench-panel todo-panel">
      <div class="panel-head">
        <p class="kicker">My Todo</p>
        <h2>我的待办</h2>
      </div>

      <div class="todo-group-list">
        <section v-for="group in taskGroupList" :key="group.id" class="todo-group">
          <header>
            <h3>{{ group.label }}</h3>
            <span>{{ group.tasks.length }}</span>
          </header>

          <button
            v-for="task in group.tasks"
            :key="task.id"
            class="todo-task"
            :class="{ active: selectedTaskId === task.id }"
            type="button"
            @click="selectTask(task)"
          >
            <span class="status-pill">{{ group.label }}</span>
            <strong>{{ task.id }} · {{ task.title }}</strong>
            <small>下一步：{{ task.nextStep }}</small>
          </button>
        </section>
      </div>

      <section class="feedback-archive-card" aria-label="反馈归档">
        <header class="archive-head">
          <div>
            <p class="kicker">Feedback Archive</p>
            <h3>反馈归档</h3>
          </div>
          <span>{{ filteredFeedbacks.length }} 条</span>
        </header>

        <dl class="archive-stat-grid">
          <div v-for="stat in feedbackArchiveStats" :key="stat.label">
            <dt>{{ stat.label }}</dt>
            <dd>{{ stat.value }}</dd>
          </div>
        </dl>

        <div class="archive-filter-group">
          <span>按任务</span>
          <select
            :value="feedbackTaskFilter"
            aria-label="按任务筛选反馈"
            @change="setFeedbackTaskFilter($event.target.value)"
          >
            <option v-for="option in feedbackTaskOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>

        <div class="archive-filter-group">
          <span>按状态</span>
          <div class="archive-filter-chips">
            <button
              v-for="option in feedbackStatusOptions"
              :key="option.value"
              type="button"
              :class="{ active: feedbackStatusFilter === option.value }"
              :aria-pressed="feedbackStatusFilter === option.value"
              @click="setFeedbackStatusFilter(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div v-if="filteredFeedbacks.length > 0" class="feedback-archive-list">
          <button
            v-for="feedback in filteredFeedbacks"
            :key="feedback.id"
            class="feedback-archive-row"
            :class="[{ active: selectedFeedbackId === feedback.id }, getFeedbackStatusMeta(feedback.status).tone]"
            type="button"
            @click="selectFeedbackArchive(feedback)"
          >
            <span>{{ feedback.questId }} · {{ getFeedbackStatusMeta(feedback.status).label }}</span>
            <strong>{{ feedback.questTitle }}</strong>
            <small>{{ feedback.reviewedAt }} · {{ feedback.pullRequest }}</small>
            <small>{{ feedback.archiveNote }}</small>
          </button>
        </div>

        <p v-else class="archive-empty">当前筛选没有匹配反馈，切换任务或状态即可恢复归档列表。</p>
      </section>
    </aside>

    <aside v-else class="workbench-panel todo-panel review-queue-panel">
      <div class="panel-head">
        <p class="kicker">委托人工作区</p>
        <h2>任务发布与审核</h2>
      </div>

      <div class="todo-group-list">
        <section class="todo-group">
          <header>
            <h3>Issue 悬赏草稿</h3>
            <span>{{ maintainerIssueBacklog.length }}</span>
          </header>

          <button
            v-for="issue in maintainerIssueBacklog"
            :key="issue.id"
            class="todo-task issue-draft-item"
            :class="{ active: selectedPublishingIssue?.id === issue.id }"
            type="button"
            @click="selectPublishingIssue(issue)"
          >
            <span class="status-pill">{{ issue.id }}</span>
            <strong>{{ issue.title }}</strong>
            <small>{{ issue.repository }}</small>
            <small>{{ issue.labels.join(' · ') }}</small>
          </button>
        </section>

        <section class="todo-group">
          <header>
            <h3>提交审核队列</h3>
            <span>{{ maintainerReviews.length }}</span>
          </header>

        <button
          v-for="review in maintainerReviews"
          :key="review.id"
          class="todo-task review-queue-item"
          :class="{ active: selectedReview?.id === review.id }"
          type="button"
          @click="selectReview(review)"
        >
          <span class="status-pill" :class="{ warning: review.status !== '待审核' }">{{ review.status }}</span>
          <strong>{{ review.id }} · {{ review.questId }}</strong>
          <small>{{ review.questTitle }}</small>
          <small>提交人：{{ review.submitter }} · {{ review.pullRequest }}</small>
          <small>提交时间：{{ review.submittedAt }}</small>
        </button>
        </section>
      </div>
    </aside>

    <main class="workbench-main">
      <section v-if="workbenchView === 'guild-master'" class="workbench-panel task-detail-panel maintainer-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">委托人审核</p>
            <h2>{{ selectedReview?.questId }} · {{ selectedReview?.questTitle }}</h2>
          </div>
          <span class="status-pill" :class="{ warning: selectedReview?.status !== '待审核' }">
            {{ selectedReview?.status }}
          </span>
        </div>

        <div v-if="selectedReview" class="maintainer-detail-grid">
          <article class="detail-card maintainer-publisher-card">
            <div class="publisher-hero">
              <div>
                <p class="kicker">Issue → 悬赏任务草稿 → 清晰度检查 → 管理员审核</p>
                <h3>维护者任务发布工坊</h3>
                <p>
                  点击左侧 Issue 会自动带出标题、摘要和关联仓库；补齐字段后，清晰度检查会判断是否可以提交给管理员审核。
                </p>
              </div>
              <div class="publisher-status-card">
                <span>发布状态</span>
                <strong>{{ publishingReviewStatus.label }}</strong>
                <small v-if="publishingReviewStatus.submittedAt">{{ publishingReviewStatus.submittedAt }}</small>
              </div>
            </div>

            <div class="publisher-grid">
              <section class="issue-context-card">
                <h4>Issue 上下文</h4>
                <dl>
                  <div>
                    <dt>Issue</dt>
                    <dd>{{ taskDraft.issueId }}</dd>
                  </div>
                  <div>
                    <dt>关联仓库</dt>
                    <dd>{{ taskDraft.repository }}</dd>
                  </div>
                </dl>
                <p>{{ selectedPublishingIssue?.summary }}</p>
              </section>

              <form class="quest-draft-form" @submit.prevent="submitTaskDraftForReview">
                <label>
                  <span>任务标题</span>
                  <input v-model="taskDraft.title" type="text" />
                </label>
                <label>
                  <span>难度</span>
                  <input v-model="taskDraft.difficulty" type="text" placeholder="初级 / 中级 / 高级" />
                </label>
                <label>
                  <span>技术栈</span>
                  <input v-model="taskDraft.techStack" type="text" placeholder="Vue, CSS, API..." />
                </label>
                <label>
                  <span>奖励</span>
                  <input v-model="taskDraft.reward" type="text" placeholder="XP / Gold / 徽章" />
                </label>
                <label class="wide-field">
                  <span>任务摘要</span>
                  <textarea v-model="taskDraft.summary" rows="3"></textarea>
                </label>
                <label class="wide-field">
                  <span>完成标准（每行一项）</span>
                  <textarea v-model="taskDraft.completionStandards" rows="5"></textarea>
                </label>
              </form>

              <section class="clarity-check-card">
                <div>
                  <h4>清晰度检查</h4>
                  <span class="status-pill" :class="{ warning: !isDraftReady }">
                    {{ claritySummary.passed }} / {{ claritySummary.total }} 通过
                  </span>
                </div>
                <div class="feedback-check-list">
                  <section
                    v-for="item in clarityChecks"
                    :key="item.label"
                    class="feedback-check-row"
                    :class="{ passed: item.passed, failed: !item.passed }"
                  >
                    <div>
                      <strong>{{ item.label }}</strong>
                      <span>{{ item.passed ? '通过' : '待补' }}</span>
                    </div>
                    <p>{{ item.detail }}</p>
                  </section>
                </div>
              </section>

              <section class="admin-review-card">
                <h4>提交审核</h4>
                <p>{{ publishingReviewStatus.body }}</p>
                <div class="card-actions">
                  <button class="primary-action" type="button" @click="submitTaskDraftForReview">
                    提交给管理员审核
                  </button>
                  <button class="quiet-action" type="button" @click="selectPublishingIssue(selectedPublishingIssue)">
                    重新带入 Issue
                  </button>
                </div>
              </section>
            </div>
          </article>

          <article class="maintainer-brief-card">
            <div>
              <p class="kicker">Submission Detail</p>
              <h3>{{ selectedReview.summary }}</h3>
            </div>
            <dl>
              <div>
                <dt>提交编号</dt>
                <dd>{{ selectedReview.id }}</dd>
              </div>
              <div>
                <dt>任务</dt>
                <dd>{{ selectedReview.questId }} · {{ selectedReview.questTitle }}</dd>
              </div>
              <div>
                <dt>提交人</dt>
                <dd>{{ selectedReview.submitter }}</dd>
              </div>
              <div>
                <dt>关联仓库</dt>
                <dd>{{ selectedReview.repository }}</dd>
              </div>
              <div>
                <dt>关联 PR</dt>
                <dd>{{ selectedReview.pullRequest }}</dd>
              </div>
              <div>
                <dt>提交时间</dt>
                <dd>{{ selectedReview.submittedAt }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card maintainer-check-card">
            <h3>完成标准逐项检查</h3>
            <div class="feedback-check-list">
              <section
                v-for="item in selectedReview.checklist"
                :key="item.item"
                class="feedback-check-row"
                :class="{ passed: item.passed, failed: !item.passed }"
              >
                <div>
                  <strong>{{ item.item }}</strong>
                  <span>{{ item.passed ? '通过' : '需修改' }}</span>
                </div>
                <p>{{ item.comment }}</p>
              </section>
            </div>
          </article>

          <article class="detail-card maintainer-feedback-card">
            <h3>审核总结</h3>
            <p>{{ selectedReview.summary }}</p>
            <h3>必须修改</h3>
            <ul class="feedback-list urgent">
              <li v-for="item in selectedReview.requiredChanges" :key="item">{{ item }}</li>
            </ul>
            <h3>学习建议</h3>
            <ul class="feedback-list">
              <li v-for="item in selectedReview.learningSuggestions" :key="item">{{ item }}</li>
            </ul>
          </article>

          <article class="detail-card maintainer-action-card">
            <h3>审核操作</h3>
            <div class="card-actions detail-actions">
              <button class="primary-action" type="button" @click="runReviewAction('approve')">通过</button>
              <button class="quiet-action" type="button" @click="runReviewAction('changes')">请求修改</button>
              <button class="quiet-action danger" type="button" @click="runReviewAction('reject')">驳回</button>
              <button class="quiet-action" type="button" @click="runReviewAction('pr')">查看 PR</button>
              <button class="quiet-action" type="button" @click="runReviewAction('draft')">保存草稿</button>
            </div>
            <p>这里仅做静态模拟，不保存真实审核结果；通过或请求修改后，冒险家侧会在工作台看到反馈或成长状态。</p>
          </article>

          <article class="detail-card maintainer-quest-card">
            <h3>我发布的任务</h3>
            <div class="maintainer-list">
              <section v-for="quest in maintainerPublishedQuests" :key="quest.id">
                <strong>{{ quest.id }} · {{ quest.title }}</strong>
                <span>{{ quest.status }} · {{ quest.assignee }}</span>
              </section>
            </div>
          </article>

          <article class="detail-card maintainer-repository-card">
            <h3>仓库状态</h3>
            <div class="maintainer-list">
              <section v-for="repository in repositoryList" :key="repository.name">
                <strong>{{ repository.name }}</strong>
                <span :class="{ warning: repository.syncStatus.includes('warning') }">
                  {{ repository.syncStatus }} · Issue {{ repository.issues }} · PR {{ repository.pullRequests }}
                </span>
              </section>
            </div>
          </article>

          <article class="detail-card maintainer-notice-card">
            <h3>维护者通知</h3>
            <div class="maintainer-list">
              <section v-for="notice in maintainerNotifications" :key="notice.text">
                <strong>{{ notice.type }}</strong>
                <span>{{ notice.text }}</span>
              </section>
            </div>
          </article>

          <article class="detail-card maintainer-operation-card">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <template v-else>
      <section v-if="selectedFeedback" class="workbench-panel task-detail-panel feedback-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">审核反馈</p>
            <h2>{{ selectedFeedback.questId }} · {{ selectedFeedback.questTitle }}</h2>
          </div>
          <span class="status-pill" :class="selectedFeedbackStatusMeta.tone">{{ selectedFeedbackStatusMeta.label }}</span>
        </div>

        <div class="feedback-detail-grid">
          <article class="feedback-brief-card">
            <div>
              <p class="kicker">Feedback Archive</p>
              <h3>{{ selectedFeedback.summary }}</h3>
            </div>
            <dl>
              <div>
                <dt>任务编号与标题</dt>
                <dd>{{ selectedFeedback.questId }} · {{ selectedFeedback.questTitle }}</dd>
              </div>
              <div>
                <dt>关联仓库</dt>
                <dd>{{ selectedFeedback.repository }}</dd>
              </div>
              <div>
                <dt>关联 PR</dt>
                <dd>{{ selectedFeedback.pullRequest }} · {{ selectedFeedback.pullRequestTitle }}</dd>
              </div>
              <div>
                <dt>审核人</dt>
                <dd>{{ selectedFeedback.reviewer }}</dd>
              </div>
              <div>
                <dt>审核结论</dt>
                <dd>{{ selectedFeedback.conclusion }}</dd>
              </div>
              <div>
                <dt>获得 XP</dt>
                <dd>{{ selectedFeedback.xpEarned > 0 ? `+${selectedFeedback.xpEarned} XP` : '待复审后结算' }}</dd>
              </div>
              <div>
                <dt>等级进度</dt>
                <dd>{{ selectedFeedback.xpProgressAfter }}</dd>
              </div>
              <div>
                <dt>审核时间</dt>
                <dd>{{ selectedFeedback.reviewedAt }}</dd>
              </div>
            </dl>
          </article>

          <QuestStatusFlow
            class="feedback-flow-card"
            :status="selectedFeedbackFlowStatus"
            :context="selectedFeedbackFlowContext"
            compact
          />

          <article class="detail-card feedback-check-card">
            <h3>逐项检查结果</h3>
            <div class="feedback-check-list">
              <section
                v-for="item in selectedFeedback.checks"
                :key="item.checkpoint"
                class="feedback-check-row"
                :class="{ passed: item.passed, failed: !item.passed }"
              >
                <div>
                  <strong>{{ item.checkpoint }}</strong>
                  <span>{{ item.passed ? '通过' : '需修改' }}</span>
                </div>
                <p>{{ item.comment }}</p>
              </section>
            </div>
          </article>

          <article class="detail-card action-note-card">
            <h3>工作台与提交柜台分工</h3>
            <p>代码文件上传、commit 和 PR 更新仍在工作台完成；完成修改后，成果记录需要到提交柜台重新提交。</p>
          </article>

          <article class="detail-card required-change-card">
            <h3>必须修改</h3>
            <ul class="feedback-list urgent">
              <li v-for="item in selectedFeedback.requiredChanges" :key="item">{{ item }}</li>
            </ul>
          </article>

          <article class="detail-card learning-tip-card">
            <h3>学习建议</h3>
            <ul class="feedback-list">
              <li v-for="item in selectedFeedback.learningTips" :key="item">{{ item }}</li>
            </ul>
          </article>

          <article class="detail-card feedback-action-card">
            <h3>下一步操作</h3>
            <div class="card-actions detail-actions">
              <button
                class="quiet-action"
                type="button"
                @click="runAction({ type: 'pull-request' }, `${selectedFeedback.questId} ${selectedFeedback.questTitle}`)"
              >
                进入工作台更新 PR
              </button>
              <button
                class="primary-action"
                type="button"
                :disabled="!canResubmitSelectedFeedback"
                @click="runAction({ type: 'submit', questId: selectedFeedback.questId }, `${selectedFeedback.questId} ${selectedFeedback.questTitle}`)"
              >
                {{ canResubmitSelectedFeedback ? '重新提交成果' : '无需重新提交' }}
              </button>
              <button
                class="quiet-action"
                type="button"
                @click="runAction({ type: 'pr-view' }, `${selectedFeedback.pullRequest} ${selectedFeedback.pullRequestTitle}`)"
              >
                查看 PR
              </button>
            </div>
            <p v-if="selectedFeedbackTask">当前待办状态：{{ selectedFeedbackTask.statusLabel }} · {{ selectedFeedbackTask.nextStep }}</p>
            <p>
              代码和 PR 更新在工作台完成；任务成果重新提交在提交柜台完成。通过后可展示贡献记录：
              {{ selectedFeedback.contributionRecord }}
            </p>
          </article>

          <article class="detail-card feedback-operation-card">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="selectedEmail" class="workbench-panel task-detail-panel email-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Mail Detail</p>
            <h2>{{ selectedEmail.subject }}</h2>
          </div>
          <span class="status-pill">{{ selectedEmail.unread ? '未读' : '已读' }}</span>
        </div>

        <div class="email-detail-body">
          <article class="detail-card wide">
            <h3>邮件信息</h3>
            <dl>
              <div>
                <dt>发件人</dt>
                <dd>{{ selectedEmail.from }}</dd>
              </div>
              <div>
                <dt>收件人</dt>
                <dd>{{ selectedEmail.to }}</dd>
              </div>
              <div>
                <dt>时间</dt>
                <dd>{{ selectedEmail.receivedAt }}</dd>
              </div>
              <div>
                <dt>关联对象</dt>
                <dd>{{ selectedEmail.related }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card wide mail-content-card">
            <h3>正文</h3>
            <p v-for="paragraph in selectedEmail.body" :key="paragraph">{{ paragraph }}</p>
          </article>

          <article class="detail-card wide">
            <h3>邮件操作</h3>
            <div class="card-actions detail-actions">
              <button
                v-if="selectedEmail.feedbackId"
                class="primary-action"
                type="button"
                @click="runAction({ type: 'feedback', feedbackId: selectedEmail.feedbackId }, selectedEmail.related)"
              >
                查看审核反馈
              </button>
              <button class="quiet-action" type="button" @click="runEmailAction('read', selectedEmail)">标记已读</button>
              <button class="quiet-action" type="button" @click="runEmailAction('unread', selectedEmail)">标记未读</button>
              <button class="quiet-action" type="button" @click="runEmailAction('reply', selectedEmail)">回复</button>
              <button class="quiet-action" type="button" @click="runEmailAction('archive', selectedEmail)">归档</button>
              <button class="quiet-action" type="button" @click="runEmailAction('delete', selectedEmail)">删除</button>
            </div>
          </article>

          <article class="detail-card wide">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="selectedNotification" class="workbench-panel task-detail-panel notice-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Notice Detail</p>
            <h2>{{ selectedNotification.type }}</h2>
          </div>
          <span class="status-pill">{{ selectedNotification.action }}</span>
        </div>

        <div class="detail-grid">
          <article class="detail-card wide">
            <h3>通知内容</h3>
            <p>{{ selectedNotification.text }}</p>
          </article>

          <article class="detail-card">
            <h3>建议处理</h3>
            <p>该通知来自工作台消息中心，点击下方按钮会进入对应的模拟处理流程。</p>
            <div class="card-actions detail-actions">
              <button
                class="primary-action"
                type="button"
                @click="runAction(resolveNotificationAction(selectedNotification), selectedNotification.text)"
              >
                {{ selectedNotification.action }}
              </button>
            </div>
          </article>

          <article class="detail-card">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="selectedTask" class="workbench-panel task-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">悬赏任务详情</p>
            <h2>{{ selectedTask.id }} · {{ selectedTask.title }}</h2>
          </div>
          <span class="status-pill">{{ selectedTask.statusLabel }}</span>
        </div>

        <div class="detail-grid">
          <QuestStatusFlow
            class="task-flow-card"
            :status="selectedTask.workflowState"
            :context="selectedTaskFlowContext"
            compact
          />

          <article v-if="linkPanelTask" class="detail-card wide link-ledger-card">
            <div class="link-ledger-head">
              <div>
                <p class="kicker">PR × Quest Ledger</p>
                <h3>PR 与任务联动面板</h3>
              </div>
              <span class="status-pill" :class="{ warning: !canOpenSubmissionCounter }">
                {{ linkPanelTask.counterLink }}
              </span>
            </div>
            <dl class="link-ledger-grid">
              <div>
                <dt>任务 ID</dt>
                <dd>{{ linkPanelTask.id }}</dd>
              </div>
              <div>
                <dt>关联 Issue</dt>
                <dd>{{ linkPanelTask.issue }}</dd>
              </div>
              <div>
                <dt>当前分支</dt>
                <dd>{{ linkPanelTask.branch || '未创建' }}</dd>
              </div>
              <div>
                <dt>最近 commit</dt>
                <dd>{{ linkPanelTask.recentCommit }}</dd>
              </div>
              <div>
                <dt>PR 编号 / 状态</dt>
                <dd>{{ linkPanelTask.prNumber || '未创建' }} · {{ linkPanelTask.prState }}</dd>
              </div>
              <div>
                <dt>检查结果</dt>
                <dd>{{ linkPanelTask.checkResult }}</dd>
              </div>
              <div>
                <dt>关联仓库</dt>
                <dd>{{ linkPanelRepository?.name ?? linkPanelTask.repository }}</dd>
              </div>
              <div>
                <dt>提交柜台</dt>
                <dd>{{ linkPanelTask.counterLink }}</dd>
              </div>
            </dl>
            <div class="link-check-list">
              <section
                v-for="check in linkPanelChecks"
                :key="check.label"
                class="link-check-row"
                :class="{ passed: check.passed, blocked: !check.passed }"
              >
                <strong>{{ check.label }}</strong>
                <span>{{ check.passed ? '已就绪' : '待完成' }}</span>
                <p>{{ check.detail }}</p>
              </section>
            </div>
            <p class="link-ledger-note">
              工作台负责项目提交：创建分支、上传 commit、创建并同步 PR。提交柜台负责任务成果提交：登记任务、PR、说明和截图。
            </p>
            <div class="card-actions detail-actions">
              <button class="quiet-action" type="button" @click="runAction({ type: 'branch' }, `${linkPanelTask.id} 联动面板`)">
                创建分支
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'commit' }, `${linkPanelTask.id} 联动面板`)">
                上传提交
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'pull-request' }, `${linkPanelTask.id} 联动面板`)">
                创建 PR
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'sync-pr' }, `${linkPanelTask.id} 联动面板`)">
                同步 PR 状态
              </button>
              <button
                class="primary-action"
                type="button"
                :disabled="!canOpenSubmissionCounter"
                @click="runAction({ type: 'submit', questId: linkPanelTask.id }, `${linkPanelTask.id} 联动面板`)"
              >
                去提交柜台
              </button>
            </div>
          </article>

          <article class="detail-card wide">
            <h3>任务信息</h3>
            <dl>
              <div>
                <dt>关联仓库</dt>
                <dd>{{ selectedTask.repository }}</dd>
              </div>
              <div>
                <dt>关联 Issue</dt>
                <dd>{{ selectedTask.issue }}</dd>
              </div>
              <div>
                <dt>PR 状态</dt>
                <dd>{{ selectedTask.prStatus }}</dd>
              </div>
              <div>
                <dt>下一步</dt>
                <dd>{{ selectedTask.nextStep }}</dd>
              </div>
            </dl>
          </article>

          <article v-if="selectedTaskRepository" class="detail-card">
            <h3>仓库摘要</h3>
            <dl>
              <div>
                <dt>同步状态</dt>
                <dd>{{ selectedTaskRepository.syncStatus }}</dd>
              </div>
              <div>
                <dt>默认分支</dt>
                <dd>{{ selectedTaskRepository.defaultBranch }}</dd>
              </div>
              <div>
                <dt>分支 / Issue / PR</dt>
                <dd>{{ selectedTaskRepository.branches }} / {{ selectedTaskRepository.issues }} / {{ selectedTaskRepository.pullRequests }}</dd>
              </div>
              <div>
                <dt>最近 commit</dt>
                <dd>{{ selectedTaskRepository.lastCommit }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <h3>PR 状态</h3>
            <div v-if="selectedTaskPullRequests.length > 0" class="linked-pr-list">
              <button
                v-for="pr in selectedTaskPullRequests"
                :key="pr.id"
                class="linked-pr-row"
                type="button"
                @click="runAction({ type: 'pr-view' }, `${pr.id} ${pr.title}`)"
              >
                <strong>{{ pr.id }}</strong>
                <span>{{ pr.status }}</span>
              </button>
            </div>
            <p v-else>{{ selectedTask.prStatus }}</p>
          </article>

          <article class="detail-card">
            <h3>可执行操作</h3>
            <div class="card-actions detail-actions">
              <button
                v-for="action in selectedTask.actions"
                :key="action.label"
                :class="action.primary ? 'primary-action' : 'quiet-action'"
                type="button"
                @click="runAction(action, `${selectedTask.id} ${selectedTask.title}`)"
              >
                {{ action.label }}
              </button>
            </div>
          </article>

          <article class="detail-card wide">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="selectedRepository" class="workbench-panel task-detail-panel repository-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Repository Detail</p>
            <h2>{{ selectedRepository.name }}</h2>
          </div>
          <span class="status-pill" :class="{ warning: selectedRepository.syncStatus.includes('warning') }">
            {{ selectedRepository.syncStatus }}
          </span>
        </div>

        <div class="repository-detail-grid">
          <article class="repository-overview-card">
            <div>
              <p class="kicker">Repository Workspace</p>
              <h3>{{ selectedRepository.name }}</h3>
              <p>用于查看仓库状态、创建分支、上传文件生成 commit，并从这里发起 PR。这里不提供在线代码编辑器。</p>
            </div>
            <div class="repository-sync-card" :class="{ warning: selectedRepository.syncStatus.includes('warning') }">
              <span>同步状态</span>
              <strong>{{ selectedRepository.syncStatus }}</strong>
            </div>
          </article>

          <div class="repo-metric-grid" aria-label="仓库关键指标">
            <article class="repo-metric">
              <span>默认分支</span>
              <strong>{{ selectedRepository.defaultBranch }}</strong>
            </article>
            <article class="repo-metric">
              <span>分支</span>
              <strong>{{ selectedRepository.branches }}</strong>
            </article>
            <article class="repo-metric">
              <span>Issue</span>
              <strong>{{ selectedRepository.issues }}</strong>
            </article>
            <article class="repo-metric">
              <span>PR</span>
              <strong>{{ selectedRepository.pullRequests }}</strong>
            </article>
            <article class="repo-metric">
              <span>最近 commit</span>
              <strong>{{ selectedRepository.lastCommit }}</strong>
            </article>
          </div>

          <article class="detail-card repository-actions-card">
            <h3>常用 Git 操作</h3>
            <div class="card-actions detail-actions">
              <button class="quiet-action" type="button" @click="runAction({ type: 'repository' }, selectedRepository.name)">
                查看仓库
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'branch' }, selectedRepository.name)">
                创建分支
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'commit' }, selectedRepository.name)">
                上传提交
              </button>
              <button class="primary-action" type="button" @click="runAction({ type: 'pull-request' }, selectedRepository.name)">
                发起 PR
              </button>
            </div>
          </article>

          <article class="detail-card repository-flow-card">
            <h3>推荐操作顺序</h3>
            <ol>
              <li>查看仓库、Issue 和最近提交，确认任务上下文。</li>
              <li>创建任务分支，上传文件生成 commit。</li>
              <li>发起 PR，再到提交柜台关联任务成果。</li>
            </ol>
          </article>

          <article v-if="linkPanelTask" class="detail-card repository-link-card link-ledger-card">
            <div class="link-ledger-head">
              <div>
                <p class="kicker">PR × Quest Ledger</p>
                <h3>仓库联动任务</h3>
              </div>
              <span class="status-pill" :class="{ warning: !canOpenSubmissionCounter }">
                {{ linkPanelTask.counterLink }}
              </span>
            </div>
            <div v-if="selectedRepositoryTasks.length > 1" class="repository-task-strip">
              <button
                v-for="task in selectedRepositoryTasks"
                :key="task.id"
                class="quiet-action"
                type="button"
                @click="selectTask(task)"
              >
                {{ task.id }} · {{ task.statusLabel }}
              </button>
            </div>
            <dl class="link-ledger-grid">
              <div>
                <dt>任务 ID</dt>
                <dd>{{ linkPanelTask.id }}</dd>
              </div>
              <div>
                <dt>关联 Issue</dt>
                <dd>{{ linkPanelTask.issue }}</dd>
              </div>
              <div>
                <dt>当前分支</dt>
                <dd>{{ linkPanelTask.branch || '未创建' }}</dd>
              </div>
              <div>
                <dt>最近 commit</dt>
                <dd>{{ linkPanelTask.recentCommit }}</dd>
              </div>
              <div>
                <dt>PR 编号 / 状态</dt>
                <dd>{{ linkPanelTask.prNumber || '未创建' }} · {{ linkPanelTask.prState }}</dd>
              </div>
              <div>
                <dt>检查结果</dt>
                <dd>{{ linkPanelTask.checkResult }}</dd>
              </div>
            </dl>
            <div class="link-check-list">
              <section
                v-for="check in linkPanelChecks"
                :key="check.label"
                class="link-check-row"
                :class="{ passed: check.passed, blocked: !check.passed }"
              >
                <strong>{{ check.label }}</strong>
                <span>{{ check.passed ? '已就绪' : '待完成' }}</span>
                <p>{{ check.detail }}</p>
              </section>
            </div>
            <p class="link-ledger-note">
              当前仓库操作默认联动 {{ linkPanelTask.id }}。工作台完成代码与 PR，提交柜台完成课程任务成果登记。
            </p>
            <div class="card-actions detail-actions">
              <button class="quiet-action" type="button" @click="runAction({ type: 'branch' }, `${linkPanelTask.id} 仓库联动`)">
                创建分支
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'commit' }, `${linkPanelTask.id} 仓库联动`)">
                上传提交
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'pull-request' }, `${linkPanelTask.id} 仓库联动`)">
                创建 PR
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'sync-pr' }, `${linkPanelTask.id} 仓库联动`)">
                同步 PR 状态
              </button>
              <button
                class="primary-action"
                type="button"
                :disabled="!canOpenSubmissionCounter"
                @click="runAction({ type: 'submit', questId: linkPanelTask.id }, `${linkPanelTask.id} 仓库联动`)"
              >
                去提交柜台
              </button>
            </div>
          </article>

          <article class="detail-card repository-sync-detail">
            <h3>同步状态</h3>
            <p v-if="selectedRepository.syncStatus.includes('warning')">
              最近同步存在异常，Issue 或 PR 状态可能不是最新。可以先手动同步，再查看异常日志。
            </p>
            <p v-else>
              仓库数据已同步，分支、Issue、PR 和最近提交可用于任务协作。
            </p>
            <div class="card-actions detail-actions">
              <button class="quiet-action" type="button" @click="runAction({ type: 'repository' }, `${selectedRepository.name} 同步日志`)">
                查看同步日志
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'exception' }, `${selectedRepository.name} 手动同步`)">
                手动同步
              </button>
            </div>
          </article>

          <article class="detail-card repository-operation-card">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="isGrowthDetailOpen" class="workbench-panel task-detail-panel growth-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Growth Detail</p>
            <h2>成长详细信息</h2>
          </div>
          <span class="status-pill">Level {{ workbenchUser.level }}</span>
        </div>

        <div class="growth-detail-grid">
          <article class="growth-hero-card">
            <div>
              <span>当前等级</span>
              <strong>Level {{ workbenchUser.level }}</strong>
              <p>{{ workbenchUser.xpCurrent }} / {{ workbenchUser.xpTarget }} XP</p>
            </div>
            <div class="xp-track large">
              <span :style="{ width: xpProgress }"></span>
            </div>
          </article>

          <article class="detail-card">
            <h3>完成任务</h3>
            <p>
              已完成 {{ workbenchUser.completedQuests }} 个任务，反馈归档中已确认获得 {{ archivedXpEarned }} XP。
              待复审任务通过后会继续更新等级进度。
            </p>
          </article>

          <article class="detail-card">
            <h3>最近贡献</h3>
            <ul class="contribution-list">
              <li v-for="record in recentContributions" :key="record">{{ record }}</li>
            </ul>
          </article>

          <article class="detail-card">
            <h3>可展示贡献记录</h3>
            <ul class="contribution-list">
              <li v-for="feedback in displayableContributionRecords" :key="feedback.id">
                {{ feedback.contributionRecord }} · +{{ feedback.xpEarned }} XP
              </li>
            </ul>
          </article>

          <article class="detail-card wide">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else class="workbench-panel task-detail-panel blank-detail" aria-hidden="true"></section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.workbench-workspace {
  position: absolute;
  inset: 74px 28px 28px;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(250px, 310px) minmax(560px, 1fr);
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  color: #ffe7b5;
  text-wrap: pretty;
}

.workbench-statusbar,
.workbench-panel {
  border: 1px solid rgba(238, 184, 91, 0.58);
  border-radius: var(--radius);
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.8), rgba(15, 8, 4, 0.76)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.16), transparent 58%);
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.36), inset 0 1px 0 rgba(255, 229, 163, 0.14);
  backdrop-filter: blur(6px);
}

.workbench-statusbar {
  position: relative;
  z-index: 60;
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns:
    minmax(210px, 0.95fr) minmax(230px, 0.85fr) minmax(340px, 1.15fr) minmax(230px, 0.85fr)
    auto;
  align-items: center;
  gap: 18px;
  min-height: 96px;
  padding: 16px 18px;
}

.workbench-user h1 {
  font-size: 2rem;
}

.workbench-user span,
.workbench-level span,
.panel-head span,
.operation-result {
  color: rgba(255, 231, 183, 0.74);
}

.user-identity {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border: 1px solid rgba(238, 184, 91, 0.6);
  border-radius: 50%;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.3rem;
  background:
    radial-gradient(circle at 38% 28%, rgba(255, 232, 176, 0.3), transparent 0 26%, transparent 50%),
    linear-gradient(145deg, #173154, #0d1a2e 48%, #44220e);
  box-shadow: inset 0 0 0 2px rgba(255, 225, 160, 0.18), 0 10px 24px rgba(0, 0, 0, 0.28);
}

.user-identity strong {
  display: block;
  color: #ffe8b9;
  font-size: 1rem;
  line-height: 1.2;
}

.user-identity div span {
  display: block;
  margin-top: 3px;
  font-size: 0.84rem;
}

.view-switch {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.view-switch button {
  min-height: 32px;
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 999px;
  padding: 0 11px;
  color: rgba(255, 231, 183, 0.78);
  font-size: 0.76rem;
  background: rgba(11, 6, 3, 0.36);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.view-switch button:hover,
.view-switch button:focus-visible,
.view-switch button.active {
  border-color: var(--gold-bright);
  color: #ffe8b9;
  background: rgba(82, 45, 16, 0.62);
  transform: translateY(-1px);
}

.workbench-level {
  display: grid;
  gap: 6px;
}

.level-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.workbench-level strong {
  display: block;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.2rem;
}

.detail-link {
  min-height: 34px;
  padding: 0 11px;
  white-space: nowrap;
  font-size: 0.82rem;
}

.mailbox-area {
  position: relative;
  z-index: 70;
  justify-self: end;
}

.mail-button {
  position: relative;
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 1px solid rgba(238, 184, 91, 0.62);
  border-radius: 50%;
  color: #ffe4ad;
  background: rgba(17, 9, 5, 0.44);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.32);
  transition: background 160ms ease, border-color 160ms ease, transform 160ms ease;
}

.mail-button:hover,
.mail-button:focus-visible {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.72);
  transform: translateY(-1px);
}

.mail-button svg {
  width: 24px;
  height: 24px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.mail-alert {
  position: absolute;
  right: -3px;
  top: -3px;
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border: 1px solid rgba(255, 226, 153, 0.9);
  border-radius: 50%;
  color: #fff6e5;
  font-size: 0.76rem;
  font-weight: 800;
  line-height: 1;
  background: #b5251d;
  box-shadow: 0 0 0 3px rgba(181, 37, 29, 0.22);
}

.mailbox-popover {
  position: absolute;
  right: 0;
  top: calc(100% + 12px);
  z-index: 1000;
  width: min(420px, calc(100vw - 56px));
  max-height: min(520px, 72svh);
  overflow: auto;
  border: 1px solid rgba(238, 184, 91, 0.68);
  border-radius: var(--radius);
  padding: 12px;
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.96), rgba(15, 8, 4, 0.94)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.18), transparent 58%);
  box-shadow: 0 24px 58px rgba(0, 0, 0, 0.52), inset 0 1px 0 rgba(255, 229, 163, 0.14);
}

.mailbox-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 10px;
}

.mailbox-head h2 {
  font-size: 1.1rem;
}

.mailbox-head > span {
  color: rgba(255, 231, 183, 0.72);
  font-size: 0.82rem;
  white-space: nowrap;
}

.mail-preview {
  display: grid;
  gap: 5px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  margin-top: 8px;
  padding: 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(11, 6, 3, 0.38);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.mail-preview:hover,
.mail-preview:focus-visible,
.mail-preview.active {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.58);
  transform: translateY(-1px);
}

.mail-preview.unread {
  border-color: rgba(245, 118, 82, 0.54);
}

.mail-preview span,
.mail-preview small {
  color: rgba(255, 231, 183, 0.7);
  font-size: 0.78rem;
}

.mail-preview strong {
  overflow-wrap: anywhere;
  line-height: 1.24;
}

.mailbox-section-title {
  border-top: 1px solid rgba(240, 198, 118, 0.18);
  margin: 12px 0 2px;
  padding-top: 10px;
  color: rgba(255, 231, 183, 0.68);
  font-size: 0.78rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.notice-preview {
  border-color: rgba(240, 198, 118, 0.24);
}

.xp-track {
  height: 8px;
  overflow: hidden;
  border: 1px solid rgba(238, 184, 91, 0.34);
  border-radius: 999px;
  background: rgba(7, 4, 2, 0.52);
}

.xp-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #b56c22, #ffd98a);
}

.xp-track.large {
  height: 11px;
  margin: 10px 0 0;
}

.workbench-stat-grid,
.growth-panel dl {
  display: grid;
  margin: 0;
}

.workbench-stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.repository-shortcuts {
  display: grid;
  grid-template-columns: auto repeat(2, minmax(0, 1fr));
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.repository-shortcuts > span {
  color: rgba(255, 231, 183, 0.66);
  font-size: 0.78rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  white-space: nowrap;
}

.repository-chip {
  display: grid;
  gap: 3px;
  min-width: 0;
  min-height: 48px;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 6px;
  padding: 8px 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(11, 6, 3, 0.34);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.repository-chip:hover,
.repository-chip:focus-visible,
.repository-chip.active {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.56);
  transform: translateY(-1px);
}

.repository-chip strong {
  overflow: hidden;
  font-size: 0.86rem;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.repository-chip small {
  color: rgba(255, 231, 183, 0.66);
  font-size: 0.72rem;
  line-height: 1;
}

.repository-chip small.warning {
  color: #ffd7c9;
}

.workbench-stat-grid div,
.notice-row,
.pr-row {
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  background: rgba(11, 6, 3, 0.34);
}

.workbench-stat-grid div {
  padding: 10px 12px;
}

dt {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.76rem;
}

dd {
  margin: 3px 0 0;
  color: #ffe2a0;
  font-weight: 700;
}

.workbench-panel {
  min-height: 0;
  padding: 14px;
  overflow: hidden;
}

.todo-panel,
.workbench-main {
  min-height: 0;
}

.todo-panel,
.workbench-main {
  display: grid;
  gap: 12px;
}

.workbench-main {
  grid-template-rows: minmax(0, 1fr);
}

.panel-head {
  display: grid;
  gap: 2px;
  margin-bottom: 12px;
}

.panel-head.inline {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
}

.panel-head h2 {
  font-size: 1.15rem;
}

.panel-caption {
  margin: -5px 0 10px;
  color: rgba(255, 231, 183, 0.68);
  font-size: 0.8rem;
  line-height: 1.35;
}

.todo-panel {
  overflow: auto;
}

.todo-group-list {
  display: grid;
  gap: 14px;
}

.todo-group {
  display: grid;
  gap: 8px;
}

.todo-group header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid rgba(240, 198, 118, 0.18);
  padding-bottom: 6px;
}

.todo-group h3 {
  margin: 0;
  color: rgba(255, 226, 160, 0.9);
  font-family: var(--font-display);
  font-size: 0.96rem;
}

.todo-group header span {
  display: grid;
  place-items: center;
  min-width: 24px;
  height: 24px;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 50%;
  color: #ffe4ad;
  font-size: 0.78rem;
  background: rgba(80, 43, 18, 0.44);
}

.todo-task {
  display: grid;
  gap: 7px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 10px;
  color: inherit;
  text-align: left;
  background: rgba(11, 6, 3, 0.34);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.todo-task:hover,
.todo-task:focus-visible,
.todo-task.active {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.56);
  transform: translateY(-1px);
}

.todo-task strong,
.detail-card h3 {
  display: block;
  color: #ffe8b9;
  line-height: 1.22;
}

.todo-task small,
.operation-panel p,
.notice-row p,
.detail-card p {
  margin: 0;
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.36;
}

.feedback-archive-card {
  display: grid;
  gap: 10px;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 7px;
  margin-top: 4px;
  padding: 12px;
  background:
    linear-gradient(180deg, rgba(39, 22, 10, 0.58), rgba(12, 7, 4, 0.42)),
    radial-gradient(circle at 100% 0%, rgba(255, 217, 138, 0.1), transparent 0 32%, transparent 58%);
}

.archive-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 10px;
}

.archive-head h3 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: 1rem;
}

.archive-head > span {
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 3px 8px;
  color: #ffe4ad;
  font-size: 0.72rem;
  background: rgba(80, 43, 18, 0.44);
}

.archive-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin: 0;
}

.archive-stat-grid div {
  display: grid;
  gap: 3px;
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 5px;
  padding: 7px;
  background: rgba(7, 4, 2, 0.28);
}

.archive-stat-grid dt,
.archive-filter-group > span {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.72rem;
}

.archive-stat-grid dd {
  margin: 0;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1rem;
}

.archive-filter-group {
  display: grid;
  gap: 6px;
}

.archive-filter-group select {
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.22);
  border-radius: 5px;
  padding: 8px 9px;
  color: #ffe8b9;
  background: rgba(11, 6, 3, 0.54);
  outline: none;
}

.archive-filter-group select:focus {
  border-color: var(--gold-bright);
}

.archive-filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.archive-filter-chips button {
  min-height: 28px;
  border: 1px solid rgba(240, 198, 118, 0.22);
  border-radius: 999px;
  padding: 0 9px;
  color: rgba(255, 231, 183, 0.78);
  font-size: 0.74rem;
  background: rgba(11, 6, 3, 0.34);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.archive-filter-chips button:hover,
.archive-filter-chips button:focus-visible,
.archive-filter-chips button.active {
  border-color: var(--gold-bright);
  color: #ffe8b9;
  background: rgba(82, 45, 16, 0.58);
  transform: translateY(-1px);
}

.feedback-archive-list {
  display: grid;
  gap: 7px;
}

.feedback-archive-row {
  display: grid;
  gap: 5px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 9px;
  color: inherit;
  text-align: left;
  background: rgba(7, 4, 2, 0.3);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.feedback-archive-row:hover,
.feedback-archive-row:focus-visible,
.feedback-archive-row.active {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.54);
  transform: translateY(-1px);
}

.feedback-archive-row.approved {
  border-color: rgba(111, 158, 87, 0.42);
}

.feedback-archive-row.warning {
  border-color: rgba(204, 95, 65, 0.5);
}

.feedback-archive-row.review {
  border-color: rgba(102, 152, 204, 0.46);
}

.feedback-archive-row span {
  width: fit-content;
  border: 1px solid rgba(238, 184, 91, 0.34);
  border-radius: 999px;
  padding: 2px 7px;
  color: #ffe4ad;
  font-size: 0.68rem;
  background: rgba(80, 43, 18, 0.36);
}

.feedback-archive-row strong {
  color: #ffe8b9;
  line-height: 1.2;
}

.feedback-archive-row small,
.archive-empty {
  color: rgba(255, 231, 183, 0.7);
  font-size: 0.76rem;
  line-height: 1.35;
}

.status-pill,
.notice-row span {
  display: inline-flex;
  width: fit-content;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 2px 8px;
  color: #ffe4ad;
  font-size: 0.72rem;
  background: rgba(80, 43, 18, 0.44);
}

.status-pill.warning {
  border-color: rgba(204, 95, 65, 0.72);
  color: #ffd7c9;
  background: rgba(110, 42, 36, 0.44);
}

.status-pill.approved {
  border-color: rgba(129, 184, 98, 0.68);
  color: #dcf4c2;
  background: rgba(44, 91, 36, 0.44);
}

.status-pill.review {
  border-color: rgba(102, 152, 204, 0.68);
  color: #d7ecff;
  background: rgba(32, 62, 92, 0.44);
}

.status-pill {
  margin-bottom: 0;
}

.task-detail-panel {
  display: grid;
  align-content: start;
  min-height: 100%;
}

.blank-detail {
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.34), rgba(15, 8, 4, 0.28)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.06), transparent 58%);
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(260px, 0.9fr);
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.detail-card {
  display: grid;
  align-content: start;
  gap: 12px;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 13px;
  background: rgba(11, 6, 3, 0.34);
}

.detail-card.wide {
  grid-column: 1 / -1;
}

.detail-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px 12px;
  margin: 0;
}

.detail-card dl div {
  min-width: 0;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 6px;
}

.detail-card dd {
  overflow-wrap: anywhere;
}

.link-ledger-card {
  gap: 14px;
  border-color: rgba(238, 184, 91, 0.34);
  background:
    linear-gradient(135deg, rgba(72, 38, 14, 0.62), rgba(11, 6, 3, 0.46)),
    radial-gradient(circle at 92% 0%, rgba(255, 217, 138, 0.13), transparent 0 30%, transparent 58%);
}

.link-ledger-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.link-ledger-head h3 {
  margin-top: 2px;
}

.link-ledger-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 9px;
  margin: 0;
}

.link-ledger-grid div {
  min-width: 0;
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 5px;
  padding: 9px 10px;
  background: rgba(7, 4, 2, 0.28);
}

.link-check-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.link-check-row {
  display: grid;
  gap: 5px;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 10px;
  background: rgba(7, 4, 2, 0.3);
}

.link-check-row.passed {
  border-color: rgba(111, 158, 87, 0.58);
  background: rgba(44, 73, 36, 0.24);
}

.link-check-row.blocked {
  border-color: rgba(204, 95, 65, 0.58);
  background: rgba(88, 31, 23, 0.28);
}

.link-check-row strong {
  color: #ffe8b9;
  line-height: 1.2;
}

.link-check-row span {
  width: fit-content;
  border: 1px solid rgba(238, 184, 91, 0.36);
  border-radius: 999px;
  padding: 2px 8px;
  color: #ffe4ad;
  font-size: 0.7rem;
  background: rgba(80, 43, 18, 0.42);
}

.link-check-row.passed span {
  border-color: rgba(129, 184, 98, 0.64);
  color: #dcf4c2;
  background: rgba(44, 91, 36, 0.42);
}

.link-check-row.blocked span {
  border-color: rgba(238, 120, 82, 0.66);
  color: #ffd7c9;
  background: rgba(110, 42, 36, 0.46);
}

.link-check-row p,
.link-ledger-note {
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.4;
}

.repository-task-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.primary-action:disabled,
.quiet-action:disabled {
  cursor: not-allowed;
  opacity: 0.52;
  transform: none;
}

.detail-actions {
  align-content: start;
}

.linked-pr-list {
  display: grid;
  gap: 8px;
}

.linked-pr-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 9px 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(7, 4, 2, 0.32);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.linked-pr-row:hover,
.linked-pr-row:focus-visible {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.5);
  transform: translateY(-1px);
}

.linked-pr-row span {
  flex: 0 0 auto;
  color: rgba(255, 231, 183, 0.68);
  font-size: 0.78rem;
}

.email-detail-body {
  display: grid;
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.mail-content-card {
  gap: 10px;
}

.mail-content-card p {
  line-height: 1.58;
}

.feedback-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.72fr);
  grid-template-areas:
    "brief brief"
    "flow flow"
    "checks note"
    "checks required"
    "tips actions"
    "operation operation";
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.feedback-brief-card {
  grid-area: brief;
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(320px, 1.05fr);
  gap: 16px;
  border: 1px solid rgba(240, 198, 118, 0.28);
  border-radius: 7px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.58), rgba(11, 6, 3, 0.46)),
    radial-gradient(circle at 92% 12%, rgba(255, 217, 138, 0.14), transparent 0 30%, transparent 56%);
}

.feedback-flow-card {
  grid-area: flow;
}

.task-flow-card {
  grid-column: 1 / -1;
}

.feedback-brief-card h3 {
  margin: 2px 0 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: 1.08rem;
  line-height: 1.38;
}

.feedback-brief-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin: 0;
}

.feedback-brief-card dl div {
  min-width: 0;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 6px;
}

.feedback-brief-card dd {
  overflow-wrap: anywhere;
}

.feedback-check-card {
  grid-area: checks;
}

.action-note-card {
  grid-area: note;
  border-color: rgba(238, 184, 91, 0.34);
  background:
    linear-gradient(180deg, rgba(39, 22, 10, 0.66), rgba(12, 7, 4, 0.46)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.12), transparent 58%);
}

.required-change-card {
  grid-area: required;
}

.learning-tip-card {
  grid-area: tips;
}

.feedback-action-card {
  grid-area: actions;
}

.feedback-operation-card {
  grid-area: operation;
}

.feedback-check-list {
  display: grid;
  gap: 9px;
}

.feedback-check-row {
  display: grid;
  gap: 7px;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 10px;
  background: rgba(7, 4, 2, 0.3);
}

.feedback-check-row.passed {
  border-color: rgba(111, 158, 87, 0.58);
  background: rgba(44, 73, 36, 0.26);
}

.feedback-check-row.failed {
  border-color: rgba(204, 95, 65, 0.68);
  background: rgba(88, 31, 23, 0.32);
}

.feedback-check-row div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.feedback-check-row strong {
  color: #ffe8b9;
  line-height: 1.25;
}

.feedback-check-row span {
  flex: 0 0 auto;
  border: 1px solid rgba(238, 184, 91, 0.38);
  border-radius: 999px;
  padding: 2px 8px;
  color: #ffe4ad;
  font-size: 0.72rem;
  background: rgba(80, 43, 18, 0.44);
}

.feedback-check-row.passed span {
  border-color: rgba(129, 184, 98, 0.68);
  color: #dcf4c2;
  background: rgba(44, 91, 36, 0.44);
}

.feedback-check-row.failed span {
  border-color: rgba(238, 120, 82, 0.72);
  color: #ffd7c9;
  background: rgba(110, 42, 36, 0.5);
}

.feedback-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  color: rgba(255, 231, 183, 0.78);
  list-style: none;
}

.feedback-list li {
  position: relative;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding: 0 0 8px 17px;
  line-height: 1.42;
}

.feedback-list li::before {
  position: absolute;
  left: 0;
  top: 0.58em;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  content: '';
  background: rgba(238, 184, 91, 0.76);
}

.feedback-list.urgent li::before {
  background: #d66a48;
}

.feedback-action-card p {
  border-top: 1px solid rgba(240, 198, 118, 0.16);
  padding-top: 10px;
}

.review-queue-item {
  gap: 6px;
}

.issue-draft-item small:last-child {
  color: rgba(255, 214, 143, 0.82);
}

.maintainer-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.72fr);
  grid-template-areas:
    "publisher publisher"
    "brief brief"
    "checks feedback"
    "actions quests"
    "repos notices"
    "operation operation";
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.maintainer-publisher-card {
  grid-area: publisher;
  gap: 16px;
  border-color: rgba(238, 184, 91, 0.34);
  background:
    linear-gradient(135deg, rgba(72, 38, 14, 0.64), rgba(11, 6, 3, 0.48)),
    radial-gradient(circle at 8% 0%, rgba(255, 217, 138, 0.16), transparent 0 28%, transparent 56%);
}

.publisher-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(180px, 240px);
  gap: 14px;
  align-items: stretch;
}

.publisher-hero h3,
.publisher-grid h4 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
}

.publisher-hero h3 {
  margin-top: 2px;
  font-size: 1.28rem;
}

.publisher-hero p {
  max-width: 76ch;
  margin-top: 8px;
}

.publisher-status-card {
  display: grid;
  align-content: center;
  gap: 7px;
  border: 1px solid rgba(238, 184, 91, 0.34);
  border-radius: 6px;
  padding: 13px;
  background: rgba(7, 4, 2, 0.34);
}

.publisher-status-card span,
.publisher-status-card small,
.quest-draft-form label span {
  color: rgba(255, 231, 183, 0.64);
  font-size: 0.76rem;
}

.publisher-status-card strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.08rem;
  line-height: 1.2;
}

.publisher-grid {
  display: grid;
  grid-template-columns: minmax(220px, 0.7fr) minmax(320px, 1fr) minmax(260px, 0.82fr);
  grid-template-areas:
    "issue form clarity"
    "review form clarity";
  gap: 12px;
}

.issue-context-card,
.clarity-check-card,
.admin-review-card {
  display: grid;
  align-content: start;
  gap: 11px;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 12px;
  background: rgba(7, 4, 2, 0.3);
}

.issue-context-card {
  grid-area: issue;
}

.issue-context-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.issue-context-card dd {
  overflow-wrap: anywhere;
}

.quest-draft-form {
  grid-area: form;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quest-draft-form label {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.quest-draft-form .wide-field {
  grid-column: 1 / -1;
}

.quest-draft-form input,
.quest-draft-form textarea {
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.22);
  border-radius: 5px;
  padding: 9px 10px;
  color: #ffe8b9;
  font: inherit;
  background: rgba(11, 6, 3, 0.45);
  outline: none;
  transition: border-color 150ms ease, background 150ms ease;
}

.quest-draft-form textarea {
  min-height: 0;
  resize: vertical;
  line-height: 1.45;
}

.quest-draft-form input:focus,
.quest-draft-form textarea:focus {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.42);
}

.quest-draft-form input::placeholder,
.quest-draft-form textarea::placeholder {
  color: rgba(255, 231, 183, 0.42);
}

.clarity-check-card {
  grid-area: clarity;
}

.clarity-check-card > div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.admin-review-card {
  grid-area: review;
  border-color: rgba(238, 184, 91, 0.32);
  background:
    linear-gradient(180deg, rgba(39, 22, 10, 0.62), rgba(12, 7, 4, 0.4)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.1), transparent 58%);
}

.maintainer-brief-card {
  grid-area: brief;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(320px, 1.08fr);
  gap: 16px;
  border: 1px solid rgba(240, 198, 118, 0.28);
  border-radius: 7px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.58), rgba(11, 6, 3, 0.46)),
    radial-gradient(circle at 90% 14%, rgba(255, 217, 138, 0.14), transparent 0 30%, transparent 56%);
}

.maintainer-brief-card h3 {
  margin: 2px 0 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: 1.08rem;
  line-height: 1.38;
}

.maintainer-brief-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin: 0;
}

.maintainer-brief-card dl div {
  min-width: 0;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 6px;
}

.maintainer-brief-card dd {
  overflow-wrap: anywhere;
}

.maintainer-check-card {
  grid-area: checks;
}

.maintainer-feedback-card {
  grid-area: feedback;
}

.maintainer-action-card {
  grid-area: actions;
  border-color: rgba(238, 184, 91, 0.34);
  background:
    linear-gradient(180deg, rgba(39, 22, 10, 0.66), rgba(12, 7, 4, 0.46)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.12), transparent 58%);
}

.maintainer-quest-card {
  grid-area: quests;
}

.maintainer-repository-card {
  grid-area: repos;
}

.maintainer-notice-card {
  grid-area: notices;
}

.maintainer-operation-card {
  grid-area: operation;
}

.maintainer-list {
  display: grid;
  gap: 8px;
}

.maintainer-list section {
  display: grid;
  gap: 4px;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 8px;
}

.maintainer-list strong {
  color: #ffe8b9;
  line-height: 1.25;
}

.maintainer-list span {
  color: rgba(255, 231, 183, 0.72);
  font-size: 0.8rem;
  line-height: 1.35;
}

.maintainer-list span.warning {
  color: #ffd7c9;
}

.quiet-action.danger {
  border-color: rgba(204, 95, 65, 0.68);
  color: #ffd7c9;
}

.quiet-action.danger:hover,
.quiet-action.danger:focus-visible {
  background: rgba(110, 42, 36, 0.48);
}

.repository-panel {
  overflow: auto;
}

.repository-row {
  display: grid;
  gap: 6px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  margin-top: 7px;
  padding: 9px 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(11, 6, 3, 0.34);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.repository-row:hover,
.repository-row:focus-visible,
.repository-row.active {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.56);
  transform: translateY(-1px);
}

.repository-row.active {
  box-shadow: inset 0 0 0 1px rgba(255, 217, 138, 0.16);
}

.repository-row div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.repository-row strong {
  overflow-wrap: anywhere;
  line-height: 1.2;
}

.repository-row span {
  flex: 0 0 auto;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 2px 7px;
  color: #ffe4ad;
  font-size: 0.7rem;
  background: rgba(80, 43, 18, 0.44);
}

.repository-row span.warning {
  border-color: rgba(204, 95, 65, 0.72);
  color: #ffd7c9;
  background: rgba(110, 42, 36, 0.44);
}

.repository-row small {
  color: rgba(255, 231, 183, 0.68);
  font-size: 0.76rem;
  line-height: 1.35;
}

.repository-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(250px, 0.66fr);
  grid-template-areas:
    "overview overview"
    "metrics metrics"
    "actions flow"
    "link link"
    "sync operation";
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.repository-overview-card {
  grid-area: overview;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(170px, 220px);
  gap: 14px;
  align-items: stretch;
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 7px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.56), rgba(11, 6, 3, 0.44)),
    radial-gradient(circle at 90% 10%, rgba(255, 217, 138, 0.16), transparent 0 28%, transparent 54%);
}

.repository-overview-card h3 {
  margin: 2px 0 8px;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: 1.42rem;
}

.repository-overview-card p {
  max-width: 62ch;
  margin: 0;
  color: rgba(255, 231, 183, 0.75);
  line-height: 1.48;
}

.repository-sync-card {
  display: grid;
  align-content: center;
  gap: 8px;
  border: 1px solid rgba(238, 184, 91, 0.32);
  border-radius: 6px;
  padding: 14px;
  background: rgba(7, 4, 2, 0.34);
}

.repository-sync-card.warning {
  border-color: rgba(204, 95, 65, 0.68);
  background: rgba(79, 30, 24, 0.38);
}

.repository-sync-card span,
.repo-metric span {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.76rem;
}

.repository-sync-card strong,
.repo-metric strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.08rem;
}

.repo-metric-grid {
  grid-area: metrics;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.repo-metric {
  display: grid;
  gap: 5px;
  min-width: 0;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 11px 12px;
  background: rgba(11, 6, 3, 0.34);
}

.repo-metric strong {
  overflow-wrap: anywhere;
}

.repository-actions-card {
  grid-area: actions;
}

.repository-flow-card {
  grid-area: flow;
}

.repository-link-card {
  grid-area: link;
}

.repository-sync-detail {
  grid-area: sync;
}

.repository-operation-card {
  grid-area: operation;
}

.repository-flow-card ol {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 1.15rem;
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.42;
}

.growth-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.78fr);
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.growth-hero-card {
  grid-column: 1 / -1;
  display: grid;
  gap: 14px;
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 7px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.56), rgba(11, 6, 3, 0.44)),
    radial-gradient(circle at 88% 18%, rgba(255, 217, 138, 0.14), transparent 0 30%, transparent 56%);
}

.growth-hero-card span {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.78rem;
}

.growth-hero-card strong {
  display: block;
  margin-top: 4px;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.75rem;
}

.growth-hero-card p {
  margin: 4px 0 0;
  color: rgba(255, 231, 183, 0.74);
}

.contribution-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  color: rgba(255, 231, 183, 0.78);
  list-style: none;
}

.contribution-list li {
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 7px;
}

.pr-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pr-row span {
  color: rgba(255, 231, 183, 0.72);
  font-size: 0.78rem;
}

.growth-panel dl div {
  min-width: 0;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 5px;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.card-actions .primary-action,
.card-actions .quiet-action,
.notice-row .quiet-action,
.pr-row .quiet-action {
  min-height: 32px;
  padding: 0 10px;
  font-size: 0.82rem;
}

.notification-panel,
.pr-panel,
.growth-panel,
.operation-panel {
  overflow: auto;
}

.notice-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  padding: 10px;
}

.pr-row {
  margin-top: 8px;
  padding: 10px;
}

.pr-row div {
  display: grid;
  min-width: 0;
}

.pr-row strong {
  color: #ffe2a0;
}

.growth-panel dl {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.growth-panel ul {
  display: grid;
  gap: 6px;
  margin: 12px 0 0;
  padding: 0;
  color: rgba(255, 231, 183, 0.78);
  font-size: 0.84rem;
  list-style: none;
}

.growth-panel li {
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 6px;
}

@media (max-width: 1240px) {
  .workbench-workspace {
    grid-template-columns: minmax(230px, 280px) minmax(420px, 1fr);
  }

  .workbench-statusbar {
    grid-template-columns: 1fr auto;
  }

  .workbench-stat-grid {
    grid-column: 1 / -1;
  }

  .workbench-level {
    grid-column: 1 / -1;
  }

  .repository-shortcuts {
    grid-column: 1 / -1;
  }

  .mailbox-area {
    grid-column: 2;
    grid-row: 1;
  }
}

@media (max-width: 940px) {
  .workbench-workspace {
    inset: 68px 16px 18px;
    display: block;
    overflow: auto;
  }

  .workbench-statusbar,
  .workbench-panel,
  .workbench-main {
    margin-bottom: 12px;
  }

  .workbench-statusbar {
    display: grid;
    grid-template-columns: 1fr;
  }

  .mailbox-area {
    grid-column: auto;
    grid-row: auto;
    justify-self: start;
  }

  .mailbox-popover {
    left: 0;
    right: auto;
  }

  .workbench-stat-grid,
  .detail-card dl {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .level-head {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .growth-detail-grid {
    grid-template-columns: 1fr;
  }

  .feedback-detail-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "brief"
      "flow"
      "checks"
      "note"
      "required"
      "tips"
      "actions"
      "operation";
  }

  .feedback-brief-card {
    grid-template-columns: 1fr;
  }

  .feedback-brief-card dl {
    grid-template-columns: 1fr;
  }

  .maintainer-detail-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "publisher"
      "brief"
      "checks"
      "feedback"
      "actions"
      "quests"
      "repos"
      "notices"
      "operation";
  }

  .publisher-hero,
  .publisher-grid {
    grid-template-columns: 1fr;
  }

  .publisher-grid {
    grid-template-areas:
      "issue"
      "form"
      "clarity"
      "review";
  }

  .quest-draft-form {
    grid-template-columns: 1fr;
  }

  .maintainer-brief-card {
    grid-template-columns: 1fr;
  }

  .maintainer-brief-card dl {
    grid-template-columns: 1fr;
  }

  .repository-detail-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "overview"
      "metrics"
      "actions"
      "flow"
      "link"
      "sync"
      "operation";
  }

  .repository-overview-card {
    grid-template-columns: 1fr;
  }

  .repo-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .link-ledger-grid,
  .link-check-list {
    grid-template-columns: 1fr;
  }

  .repository-shortcuts {
    grid-template-columns: 1fr;
  }

  .panel-head.inline {
    align-items: start;
    flex-direction: column;
  }
}
</style>
