<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

import WorkbenchGitOperationPanel from './WorkbenchGitOperationPanel.vue'
import WorkbenchPrLinkPanel from './WorkbenchPrLinkPanel.vue'
import WorkbenchRepositoryPanel from './WorkbenchRepositoryPanel.vue'
import QuestStatusFlow from './QuestStatusFlow.vue'
import { questApi } from '../api/questApi'
import { growthApi } from '../api/growthApi'
import { repositoryApi } from '../api/repositoryApi'
import { submissionApi } from '../api/submissionApi'
import {
  notifications,
  pullRequests as fallbackPullRequests,
  recentContributions,
  repositories as fallbackRepositories,
  reviewFeedbacks,
  taskGroups as fallbackTaskGroups,
  workbenchEmails,
  workbenchStats,
  workbenchUser,
} from '../data/workbench'
import { sessionStore } from '../stores/sessionStore'
import {
  loadNotifications,
  markNotificationRead,
  notificationStore,
  startNotificationPolling,
  stopNotificationPolling,
} from '../stores/notificationStore'

const emit = defineEmits(['open-submission', 'open-id-card', 'open-review-desk'])

defineProps({
  showReviewDeskEntry: {
    type: Boolean,
    default: false,
  },
})

// ── Live data (fetched from backend) ──────────────────────────────────────
const liveUser = ref({
  ...workbenchUser,
  name: sessionStore.user?.displayName || sessionStore.user?.username || workbenchUser.name,
})
const liveAssignments = ref([])       // MyAssignmentItem[]
const liveRepositories = ref([])      // Repo[]
const livePullRequests = ref([])      // PR[]
const liveStats = ref(workbenchStats.map((stat) => ({ ...stat })))
const liveLoading = ref(false)

async function fetchWorkbenchData() {
  liveLoading.value = true
  try {
    const [growthRes, assignmentRes, repoRes] = await Promise.all([
      growthApi.summary().catch(() => null),
      questApi.myAssignments().catch(() => null),
      repositoryApi.myRepositories().catch(() => null),
    ])
    // Growth
    const g = growthRes?.data
    if (g) {
      liveUser.value = {
        ...workbenchUser,
        name: sessionStore.user?.displayName || sessionStore.user?.username || '冒险家',
        level: g.level ?? 1,
        xpCurrent: g.totalXp ?? 0,
        xpTarget: g.nextLevelXp ?? 1000,
        completedQuests: g.completedQuestCount ?? 0,
      }
    }
    // Assignments + stats
    const a = assignmentRes?.data
    if (a) {
      liveAssignments.value = a.items ?? []
      if (a.stats) {
        liveStats.value = [
          { label: '进行中任务', value: a.stats.inProgress ?? 0 },
          { label: '审核中', value: a.stats.inReview ?? 0 },
          { label: '待修改', value: a.stats.changesRequested ?? 0 },
          { label: '未读邮件', value: 3 },
        ]
      }
    }
    // Repositories
    const repos = repoRes?.data?.items ?? repoRes?.data ?? []
    if (Array.isArray(repos)) {
      liveRepositories.value = repos.map(r => ({
        name: r.name,
        syncStatus: r.syncStatus || 'Synced',
        defaultBranch: r.defaultBranch || 'main',
        branches: 0,
        issues: 0,
        pullRequests: 0,
        lastCommit: '',
        repositoryId: r.repositoryId,
        hostType: r.hostType,
        sourceUrl: r.sourceUrl,
        giteaUrl: r.giteaUrl,
      }))
    }
  } catch {
    // fallback silently; component renders fallback state
  } finally {
    liveLoading.value = false
  }
}

// ── Reactive local state (derived / editable) ─────────────────────────────

// Task groups derived from live assignments
const taskGroups = computed(() => {
  const items = liveAssignments.value
  if (items.length === 0) return fallbackTaskGroups

  const groups = {
    'in-progress': { id: 'in-progress', label: '进行中', tasks: [] },
    'in-review': { id: 'in-review', label: '审核中', tasks: [] },
    'changes-requested': { id: 'changes-requested', label: '待修改', tasks: [] },
    completed: { id: 'completed', label: '已完成', tasks: [] },
  }
  for (const item of items) {
    const status = item.questStatus ?? item.assignmentStatus
    // Map backend questStatus to group
    let groupId = 'in-progress'
    if (status === 'IN_REVIEW') groupId = 'in-review'
    else if (status === 'CHANGES_REQUESTED') groupId = 'changes-requested'
    else if (status === 'COMPLETED') groupId = 'completed'
    else if (status === 'IN_PROGRESS') groupId = 'in-progress'
    const task = mapAssignmentToTask(item)
    groups[groupId].tasks.push(task)
  }
  return Object.values(groups)
})

function mapAssignmentToTask(item) {
  const pr = item.pr
  const prNumber = toPullRequestDisplayId(pr?.externalPrId)
  return {
    id: `QST-${String(item.questId).padStart(4, '0')}`,
    questId: item.questId,
    title: item.questTitle || '未命名任务',
    repository: item.repository?.name || '',
    repositoryId: item.repository?.repositoryId,
    issue: item.issue?.externalIssueId ? `#${item.issue.externalIssueId}` : '',
    prStatus: pr ? `${prNumber} ${toPullRequestStatusLabel(pr.status)}` : '未发起',
    branch: pr?.sourceBranch || '',
    recentCommit: pr ? '已同步' : '待上传',
    prNumber,
    prState: pr ? toPullRequestStatusLabel(pr.status) : '未创建',
    checkResult: pr ? '基础检查通过' : '等待检查',
    counterLink: pr ? '待登记成果' : '未登记',
    counterDetail: pr ? '已同步到可提交的 PR；请到提交柜台登记成果说明。' : '',
    nextStep: pr ? '去提交柜台登记任务成果' : '按教程创建分支、提交 commit、发起 PR 后同步状态',
    externalUrl: pr?.externalUrl || '',
    actions: [
      { label: '查看仓库', type: 'repository' },
      { label: '同步 PR 状态', type: 'sync-pr' },
      { label: '去提交柜台', type: 'submit', primary: true },
    ],
  }
}

// Keep compatibility with existing computed properties that use reactive versions
const repositoryList = ref(fallbackRepositories.map((repository) => ({ ...repository })))
const pullRequestList = ref(fallbackPullRequests.map((pullRequest) => ({ ...pullRequest })))

// Sync live data into reactive lists when data arrives
watch(liveRepositories, (repos) => {
  if (repos.length === 0) return
  repositoryList.value = repos.map(r => ({ ...r }))
})
watch(livePullRequests, (prs) => {
  if (prs.length === 0) return
  pullRequestList.value = prs.map(pr => ({ ...pr }))
})

function cloneTaskGroups(groups) {
  return groups.map(group => ({
    ...group,
    tasks: group.tasks.map(task => ({ ...task, actions: [...task.actions] })),
  }))
}

const taskGroupList = ref([])
watch(taskGroups, (groups) => {
  taskGroupList.value = cloneTaskGroups(groups)
}, { immediate: true })
const isMailboxOpen = ref(false)
const mailboxMessages = ref(workbenchEmails.map((email) => ({ ...email, body: [...email.body] })))
const selectedTaskId = ref(null)
// Init selectedTaskId once data loads — compute from taskGroupList
const defaultTaskId = computed(() => taskGroupList.value[0]?.tasks[0]?.id ?? null)
watch(defaultTaskId, (id) => {
  if (id && !selectedTaskId.value) selectedTaskId.value = id
}, { immediate: true })
const selectedEmailId = ref(null)
const selectedRepositoryName = ref(null)
const selectedNotificationText = ref(null)
const selectedFeedbackId = ref(null)
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
watch(
  allTasks,
  (tasks) => {
    if (tasks.length === 0) {
      selectedTaskId.value = null
      return
    }
    if (!tasks.some((task) => task.id === selectedTaskId.value)) {
      selectedTaskId.value = tasks[0].id
    }
  },
  { immediate: true },
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

// 当 fetched user 数据还为空时，回退到 session user
const effectiveUserName = computed(() =>
  liveUser.value.name || sessionStore.user?.displayName || sessionStore.user?.username || '冒险家',
)
const effectiveLevel = computed(() => liveUser.value.level || 1)
const effectiveXpCurrent = computed(() => liveUser.value.xpCurrent || 0)
const effectiveXpTarget = computed(() => liveUser.value.xpTarget || 1000)
const effectiveCompletedQuests = computed(() => liveUser.value.completedQuests || 0)
const effectiveRole = computed(() =>
  sessionStore.role === 'MAINTAINER' ? '委托人' : (liveUser.value.role || sessionStore.user?.role || '冒险家'),
)

// P4-024：工作台信箱与站内通知合并为同一数据源。
// "系统通知"区域优先展示后端真实通知（与公会大厅通知铃共享 notificationStore）；
// 后端无数据时回退到 mock 通知做演示（hybrid + fallback，详见合并决策）。
const NOTIFICATION_TYPE_LABEL = {
  SUBMISSION_RECEIVED: '新成果待审核',
  REVIEW_APPROVED: '审核通过',
  REVIEW_CHANGES_REQUESTED: '审核反馈',
  REVIEW_REJECTED: '审核驳回',
}
const liveNotifications = computed(() =>
  notificationStore.items.map((item) => ({
    id: item.notificationId,
    type: NOTIFICATION_TYPE_LABEL[item.type] ?? '站内通知',
    text: item.content,
    unread: item.status === 'UNREAD',
    live: true,
  })),
)
const noticeList = computed(() => (liveNotifications.value.length > 0 ? liveNotifications.value : notifications))
// 信箱整体未读 = 未读邮件（mock 演示）+ 真实未读通知。
const totalUnreadCount = computed(() => unreadMailCount.value + notificationStore.unreadCount)
const displayStats = computed(() =>
  liveStats.value.map((stat) => (stat.label === '未读邮件' ? { ...stat, value: unreadMailCount.value } : stat)),
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
const selectedTaskGitActions = computed(() => {
  const actions = selectedTask.value?.actions ?? []
  const hasGitWorkflow = actions.some((action) =>
    ['branch', 'commit', 'pull-request', 'sync-pr', 'submit'].includes(action.type),
  )

  if (!hasGitWorkflow || actions.some((action) => action.type === 'sync-pr')) return actions

  const nextActions = [...actions]
  const submitIndex = nextActions.findIndex((action) => action.type === 'submit')
  const syncAction = { label: '同步 PR 状态', type: 'sync-pr' }

  if (submitIndex >= 0) {
    nextActions.splice(submitIndex, 0, syncAction)
  } else {
    nextActions.push(syncAction)
  }

  return nextActions
})
const selectedRepositoryTasks = computed(() =>
  allTasks.value.filter((task) => task.repository === selectedRepository.value?.name),
)
const selectedRepositoryPullRequests = computed(() => {
  if (!selectedRepository.value) return []

  return pullRequestList.value.filter(
    (pullRequest) => findTaskRecord(pullRequest.taskId)?.repository === selectedRepository.value.name,
  )
})
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
const repositoryGitActions = [
  { label: '查看仓库', type: 'repository' },
  { label: '同步 PR 状态', type: 'sync-pr' },
  { label: '去提交柜台', type: 'submit', primary: true },
]
const taskGitOperationHelper =
  '工作台负责引导和同步项目提交；代码修改、分支、commit 和 PR 先在本地 Git 与 Gitea 完成，提交柜台负责任务成果登记。'
const repositoryGitOperationHelper =
  '推荐顺序：确认仓库上下文 -> 在本地创建任务分支 -> push commit -> 在 Gitea 发起 PR -> 回到工作台同步 PR -> 去提交柜台。'
const taskGitTutorialSteps = computed(() => buildGitTutorialSteps(linkPanelTask.value, linkPanelRepository.value))
const repositoryGitTutorialSteps = computed(() => buildGitTutorialSteps(linkPanelTask.value, selectedRepository.value))
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
    viewer: effectiveUserName.value,
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

  return isGitActionReady('submit', linkPanelTask.value)
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
    viewer: effectiveUserName.value,
  }
})
const xpProgress = computed(() => `${Math.round((effectiveXpCurrent.value / Math.max(effectiveXpTarget.value, 1)) * 100)}%`)
const activeRoleLabel = computed(() => effectiveRole.value)

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

// 真实站内通知：点击即通过共享 store 标记已读（与公会大厅通知铃同步），并在中间区展示内容。
function selectLiveNotification(notice) {
  if (notice.unread) {
    markNotificationRead(notice.id)
  }
  selectedNotificationText.value = null
  selectedTaskId.value = null
  selectedEmailId.value = null
  selectedRepositoryName.value = null
  selectedFeedbackId.value = null
  isGrowthDetailOpen.value = false
  isMailboxOpen.value = false
  operationResult.value = {
    title: notice.type,
    body: notice.text,
  }
}

function toggleMailbox() {
  isMailboxOpen.value = !isMailboxOpen.value
  if (isMailboxOpen.value) {
    loadNotifications()
  }
}

onMounted(() => {
  // 进入工作台即拉取并轮询真实通知，使信箱未读数与公会大厅通知铃保持一致。
  startNotificationPolling()
  fetchWorkbenchData()
})

onUnmounted(() => {
  stopNotificationPolling()
})

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

function getPullRequestTask(pullRequest) {
  return pullRequest?.taskId ? findTaskRecord(pullRequest.taskId) : null
}

function findRepositoryRecord(repositoryName) {
  return repositoryList.value.find((repository) => repository.name === repositoryName) ?? null
}

function findRepositoryForTask(task) {
  if (!task) return null
  return (
    repositoryList.value.find((repository) => repository.repositoryId && repository.repositoryId === task.repositoryId) ??
    findRepositoryRecord(task.repository) ??
    null
  )
}

function getRepositoryUrl(repository) {
  return repository?.giteaUrl || repository?.sourceUrl || repository?.url || ''
}

function normalizeRepositoryName(name = '') {
  return name.toLowerCase().replace(/\s+/g, '')
}

function findRepositoryFromSource(source = '') {
  const normalizedSource = normalizeRepositoryName(source)
  return repositoryList.value.find((repository) => normalizedSource.includes(normalizeRepositoryName(repository.name))) ?? null
}

function hasSyncWarning(repository) {
  return Boolean(repository?.syncStatus?.toLowerCase().includes('warning'))
}

function hasTaskCommit(task) {
  return Boolean(task?.recentCommit && task.recentCommit !== '待上传')
}

function extractQuestNumericId(value) {
  const match = String(value ?? '').match(/\d+/)
  return match ? Number(match[0]) : null
}

function toPullRequestDisplayId(externalPrId) {
  if (!externalPrId) return ''
  const value = String(externalPrId).trim()
  if (!value) return ''
  if (/^PR\s+#/i.test(value)) return value.replace(/^pr/i, 'PR')
  if (value.startsWith('#')) return `PR ${value}`
  return `PR #${value}`
}

function toPullRequestStatusLabel(status) {
  const statusMap = {
    OPEN: '打开',
    MERGED: '已合并',
    CLOSED: '已关闭',
    CHANGES_REQUESTED: '退回修改',
  }
  return statusMap[String(status ?? '').toUpperCase()] ?? status ?? '打开'
}

function buildGitTutorialSteps(task, repository) {
  const repositoryUrl = getRepositoryUrl(repository) || 'Gitea 仓库地址'
  const defaultBranch = repository?.defaultBranch || 'main'
  const branch = task?.branch || `feature/${(task?.id || 'qst-0000').toLowerCase()}`
  const commitMessage = task?.id ? `${task.id}: complete task` : 'complete task'

  return [
    {
      title: '打开或克隆仓库',
      body: '点击“查看仓库”进入 Gitea；如果本地还没有代码，先克隆仓库。',
      command: `git clone ${repositoryUrl}`,
    },
    {
      title: '创建任务分支',
      body: `从 ${defaultBranch} 拉出一个任务分支，分支名建议能看出任务编号。`,
      command: `git checkout -b ${branch}`,
    },
    {
      title: '提交并推送代码',
      body: '完成修改后生成 commit，并把任务分支推送到 Gitea。',
      command: `git add . && git commit -m "${commitMessage}" && git push -u origin ${branch}`,
    },
    {
      title: '发起并同步 PR',
      body: `在 Gitea 创建 ${branch} -> ${defaultBranch} 的 PR，然后回到这里点击“同步 PR 状态”。`,
      command: '',
    },
  ]
}

function isGitActionReady(actionType, task = linkPanelTask.value) {
  if (actionType === 'repository') return true
  if (!task) return false
  if (actionType === 'branch') return !task.branch
  if (actionType === 'commit') return Boolean(task.branch)
  if (actionType === 'pull-request') return Boolean(task.branch) && hasTaskCommit(task) && !task.prNumber
  if (actionType === 'sync-pr') return Boolean(task.questId || extractQuestNumericId(task.id))
  if (actionType === 'submit') {
    return Boolean(task.prNumber) && hasTaskCommit(task) && !task.checkResult.includes('排队中') && !task.checkResult.includes('等待 PR')
  }

  return true
}

function getGitActionDisabledReason(actionType, task = linkPanelTask.value) {
  if (isGitActionReady(actionType, task)) return ''
  if (!task) return '当前没有可联动任务。'

  const reasonMap = {
    branch: '任务分支已经存在。可以继续上传 commit 或创建 PR。',
    commit: '请先创建任务分支，再上传提交。',
    'pull-request': !task.branch
      ? '请先创建任务分支。'
      : !hasTaskCommit(task)
        ? '请先上传至少一个 commit。'
        : '当前任务已经有关联 PR。',
    'sync-pr': '当前任务缺少后端任务编号，无法同步 PR。',
    submit: !task.prNumber
      ? '请先发起 PR。'
      : !hasTaskCommit(task)
        ? '请先上传 commit。'
        : 'PR 检查还没有可提交结果，请先同步 PR 状态。',
  }

  return reasonMap[actionType] ?? '当前状态暂不可执行。'
}

function getGitActionButtonTitle(actionType, task = linkPanelTask.value) {
  return getGitActionDisabledReason(actionType, task) || '点击执行当前 Git 操作'
}

function openRepositoryExternal(repository) {
  const url = getRepositoryUrl(repository)
  if (!url) return false
  window.open(url, '_blank', 'noopener,noreferrer')
  return true
}

function openPullRequestStatus(pullRequest) {
  if (pullRequest?.url) {
    window.open(pullRequest.url, '_blank', 'noopener,noreferrer')
    return
  }
  runAction({ type: 'pr-view' }, `${pullRequest.id} ${pullRequest.title}`)
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
  const commitId = `c0ffee${simulatedCommitIndex.value}`
  simulatedCommitIndex.value += 1
  task.recentCommit = commitId
  task.nextStep = task.prNumber ? '同步 PR 状态后去提交柜台登记成果' : '创建 PR 并等待基础检查'
  task.counterLink = task.prNumber ? '待登记成果' : '未登记'
  task.checkResult = task.prNumber ? '基础检查排队中' : task.checkResult
  task.counterDetail = task.prNumber
    ? '项目提交已上传到当前 PR；任务成果提交仍需到提交柜台完成。'
    : 'commit 已上传，但还没有 PR；提交柜台需要 PR 链接后才能登记成果。'
  const pullRequest = task.prNumber ? pullRequestList.value.find((item) => item.id === task.prNumber) : null
  if (pullRequest) {
    pullRequest.status = task.prState === '退回修改' ? '退回修改' : '打开'
    pullRequest.checks = task.checkResult
  }
  updateRepositoryActivity(task)
  return commitId
}

function ensureTaskPullRequest(task) {
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

function markTaskPullRequestReady(task) {
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

function upsertSyncedPullRequest(task, pr) {
  const displayId = toPullRequestDisplayId(pr.externalPrId)
  const status = toPullRequestStatusLabel(pr.status)
  const nextRecord = {
    id: displayId,
    pullRequestId: pr.pullRequestId,
    externalPrId: pr.externalPrId,
    taskId: task.id,
    title: pr.title || `${task.id} ${task.title}`,
    status,
    checks: '基础检查通过',
    action: '查看 PR',
    url: pr.externalUrl,
    sourceBranch: pr.sourceBranch,
    targetBranch: pr.targetBranch,
  }
  const index = pullRequestList.value.findIndex(
    (item) =>
      (nextRecord.pullRequestId && item.pullRequestId === nextRecord.pullRequestId) ||
      (nextRecord.externalPrId && item.externalPrId === nextRecord.externalPrId) ||
      item.id === nextRecord.id,
  )
  if (index >= 0) {
    pullRequestList.value[index] = { ...pullRequestList.value[index], ...nextRecord }
  } else {
    pullRequestList.value.push(nextRecord)
  }
  return nextRecord
}

function applySubmissionDraftToTask(task, draft) {
  const pullRequests = Array.isArray(draft?.pullRequests) ? draft.pullRequests : []
  const currentExternalId = String(task.prNumber || '').match(/\d+/)?.[0]
  const selectedPr =
    pullRequests.find((pr) => currentExternalId && String(pr.externalPrId) === currentExternalId) ??
    pullRequests.find((pr) => pr.sourceBranch && task.branch && pr.sourceBranch === task.branch) ??
    pullRequests[0] ??
    null

  const repository =
    findRepositoryForTask(task) ??
    repositoryList.value.find((item) => item.repositoryId && item.repositoryId === draft?.repository?.repositoryId)
  if (repository && draft?.repository) {
    repository.repositoryId = draft.repository.repositoryId ?? repository.repositoryId
    repository.name = draft.repository.name || repository.name
    repository.sourceUrl = draft.repository.sourceUrl || repository.sourceUrl
    repository.syncStatus = 'Synced'
  }

  if (!selectedPr) return null

  const syncedPr = upsertSyncedPullRequest(task, selectedPr)
  task.branch = selectedPr.sourceBranch || task.branch || draft.branch || ''
  task.recentCommit = '已同步'
  task.prNumber = syncedPr.id
  task.prState = syncedPr.status
  task.prStatus = `${syncedPr.id} ${syncedPr.status}`
  task.checkResult = syncedPr.checks
  task.nextStep = '去提交柜台登记任务成果'
  task.counterLink = '待登记成果'
  task.counterDetail = '已从 Gitea 同步到 PR；现在可以到提交柜台提交任务成果说明。'
  task.externalUrl = syncedPr.url
  updateRepositoryActivity(task)
  const activeRepository = findRepositoryForTask(task)
  if (activeRepository) {
    activeRepository.syncStatus = 'Synced'
    activeRepository.pullRequests = Math.max(Number(activeRepository.pullRequests) || 0, pullRequests.length)
  }
  return syncedPr
}

async function syncTaskPullRequest(task) {
  const questId = task.questId ?? extractQuestNumericId(task.id)
  if (!questId) {
    markTaskPullRequestReady(task)
    return { mode: 'local', pullRequest: pullRequestList.value.find((item) => item.id === task.prNumber) ?? null }
  }

  const response = await submissionApi.getDraft(questId)
  const syncedPr = applySubmissionDraftToTask(task, response?.data)
  return { mode: 'remote', pullRequest: syncedPr, draft: response?.data }
}

function blockGitAction(actionType, task, source) {
  operationResult.value = {
    title: '当前步骤暂不可执行',
    body: `${source}：${getGitActionDisabledReason(actionType, task)}`,
  }
}

async function runLinkedGitAction(actionType, source) {
  const task = findActionTaskRecord()

  if (!task) {
    operationResult.value = {
      title: '没有可联动的任务',
      body: `${source} 当前没有关联任务，无法模拟任务分支、commit 或 PR。`,
    }
    return true
  }

  if (!isGitActionReady(actionType, task)) {
    blockGitAction(actionType, task, source)
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
    operationResult.value = {
      title: '正在同步 PR 状态',
      body: `${task.id} 正在从 Gitea 拉取 PR 候选，请稍候。`,
    }
    try {
      const result = await syncTaskPullRequest(task)
      if (!result.pullRequest) {
        operationResult.value = {
          title: '还没有同步到 PR',
          body: `${task.id} 当前仓库没有可登记的 PR。请按教程在 Gitea 发起 PR 后，再点击“同步 PR 状态”。`,
        }
        return true
      }
      operationResult.value = {
        title: 'PR 状态已同步',
        body: `${task.id} 当前为 ${task.prNumber} · ${task.prState} · ${task.checkResult}。${task.counterDetail}`,
      }
    } catch (err) {
      operationResult.value = {
        title: 'PR 同步失败',
        body: err?.message || '无法从 Gitea 同步 PR，请确认后端服务、GITEA_TOKEN 和本地 Gitea 仓库状态。',
      }
    }
    return true
  }

  return false
}

async function runAction(action, source = '当前事项') {
  if (action.type === 'submit') {
    const taskId = action.questId ?? selectedFeedback.value?.questId ?? selectedTask.value?.id ?? linkPanelTask.value?.id
    const task = taskId ? findTaskRecord(taskId) : null

    if (task && !isGitActionReady('submit', task)) {
      operationResult.value = {
        title: '暂时不能提交任务成果',
        body: `${task.id}：${getGitActionDisabledReason('submit', task)}`,
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

  if (action.type === 'repository') {
    const repository = selectedTaskRepository.value ?? selectedRepository.value ?? findRepositoryFromSource(source)
    if (repository) {
      if (!selectedRepository.value) selectRepository(repository)
      const opened = openRepositoryExternal(repository)
      operationResult.value = {
        title: opened ? '仓库已打开' : '仓库视图已打开',
        body: opened
          ? `${repository.name} 已在新窗口打开。完成分支、commit 和 PR 后，请回到工作台同步 PR 状态。`
          : `${repository.name} 缺少可打开的 Gitea URL；请检查仓库导入记录。`,
      }
      return
    }
  }

  if (action.type === 'repository' && selectedTaskRepository.value && !selectedRepository.value) {
    selectRepository(selectedTaskRepository.value)
    return
  }

  if (action.type === 'exception') {
    const repository = selectedRepository.value ?? selectedTaskRepository.value ?? findRepositoryFromSource(source)
    if (repository) {
      const previousStatus = repository.syncStatus
      repository.syncStatus = 'Synced'
      operationResult.value = {
        title: '同步状态已刷新',
        body: `${repository.name} 已从 ${previousStatus} 更新为 Synced。Issue、PR 和最近提交状态已恢复为可演示数据。`,
      }
      return
    }
  }

  if (['branch', 'commit', 'pull-request', 'sync-pr'].includes(action.type)) {
    if (await runLinkedGitAction(action.type, source)) return
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
          <span class="user-avatar" aria-hidden="true">{{ effectiveUserName.slice(0, 1) }}</span>
          <div>
            <strong>{{ effectiveUserName }}</strong>
            <span>{{ activeRoleLabel }}</span>
          </div>
        </div>
        <button
          v-if="showReviewDeskEntry"
          class="review-desk-entry"
          type="button"
          @click="emit('open-review-desk')"
        >
          <strong>进入成果审核台</strong>
        </button>
      </div>

      <div class="workbench-level">
        <div class="level-head">
          <div>
            <strong>Level {{ effectiveLevel }}</strong>
            <span>{{ effectiveXpCurrent }} / {{ effectiveXpTarget }} XP</span>
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
          <small :class="{ warning: hasSyncWarning(repository) }">{{ repository.syncStatus }}</small>
        </button>
      </div>

      <div class="mailbox-area">
        <button
          class="mail-button"
          type="button"
          aria-label="打开信箱"
          :aria-expanded="isMailboxOpen"
          @click="toggleMailbox"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h16v11H4z" />
            <path d="m4 7 8 6 8-6" />
          </svg>
          <span v-if="totalUnreadCount > 0" class="mail-alert" aria-label="有未读消息">!</span>
        </button>

        <section v-if="isMailboxOpen" class="mailbox-popover" aria-label="信箱与站内通知">
          <div class="mailbox-head">
            <div>
              <p class="kicker">Mailbox</p>
              <h2>信箱与通知</h2>
            </div>
            <span>{{ totalUnreadCount }} 未读</span>
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
            v-for="notice in noticeList"
            :key="notice.id ?? notice.text"
            class="mail-preview notice-preview"
            :class="{ unread: notice.unread, active: !notice.live && selectedNotificationText === notice.text }"
            type="button"
            @click="notice.live ? selectLiveNotification(notice) : selectNotification(notice)"
          >
            <span>{{ notice.type }}</span>
            <strong>{{ notice.text }}</strong>
            <small v-if="notice.live">{{ notice.unread ? '未读' : '已读' }}</small>
            <small v-else>操作：{{ notice.action }}</small>
          </button>
        </section>
      </div>
    </header>

    <aside class="workbench-panel todo-panel">
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

    <main class="workbench-main">
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

          <WorkbenchRepositoryPanel
            v-if="selectedTaskRepository"
            :repository="selectedTaskRepository"
            :task="selectedTask"
          />

          <WorkbenchPrLinkPanel
            v-if="linkPanelTask"
            :task="linkPanelTask"
            :repository="linkPanelRepository"
            :checks="linkPanelChecks"
            :pull-requests="selectedTaskPullRequests"
            :counter-ready="canOpenSubmissionCounter"
            show-repository
            @open-pr="openPullRequestStatus"
          />

          <WorkbenchGitOperationPanel
            :task="selectedTask"
            :actions="selectedTaskGitActions"
            :counter-ready="canOpenSubmissionCounter"
            :helper="taskGitOperationHelper"
            :tutorial-steps="taskGitTutorialSteps"
            :source="`${selectedTask.id} ${selectedTask.title}`"
            :is-action-ready="isGitActionReady"
            :get-action-title="getGitActionButtonTitle"
            @run-action="runAction"
          />

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
          <span class="status-pill" :class="{ warning: hasSyncWarning(selectedRepository) }">
            {{ selectedRepository.syncStatus }}
          </span>
        </div>

        <div class="repository-detail-grid">
          <WorkbenchRepositoryPanel :repository="selectedRepository" variant="repository" />

          <WorkbenchPrLinkPanel
            :task="linkPanelTask"
            :checks="linkPanelChecks"
            :pull-requests="selectedRepositoryPullRequests"
            :repository-tasks="selectedRepositoryTasks"
            :counter-ready="canOpenSubmissionCounter"
            :resolve-pull-request-task="getPullRequestTask"
            empty-mode="repository"
            show-task-strip
            @select-task="selectTask"
            @open-pr="openPullRequestStatus"
          />

          <WorkbenchGitOperationPanel
            class="repository-actions-card"
            :task="linkPanelTask"
            :actions="repositoryGitActions"
            :counter-ready="canOpenSubmissionCounter"
            :helper="repositoryGitOperationHelper"
            :tutorial-steps="repositoryGitTutorialSteps"
            :source="selectedRepository.name"
            :is-action-ready="isGitActionReady"
            :get-action-title="getGitActionButtonTitle"
            @run-action="runAction"
          />

          <article class="detail-card repository-sync-detail">
            <h3>同步状态</h3>
            <p v-if="hasSyncWarning(selectedRepository)">
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
          <span class="status-pill">Level {{ effectiveLevel }}</span>
        </div>

        <div class="growth-detail-grid">
          <article class="growth-hero-card">
            <div>
              <span>当前等级</span>
              <strong>Level {{ effectiveLevel }}</strong>
              <p>{{ effectiveXpCurrent }} / {{ effectiveXpTarget }} XP</p>
            </div>
            <div class="xp-track large">
              <span :style="{ width: xpProgress }"></span>
            </div>
          </article>

          <article class="detail-card">
            <h3>完成任务</h3>
            <p>
              已完成 {{ effectiveCompletedQuests }} 个任务，反馈归档中已确认获得 {{ archivedXpEarned }} XP。
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

.review-desk-entry {
  display: inline-grid;
  place-items: center;
  margin-top: 12px;
  border: 1px solid rgba(238, 184, 91, 0.46);
  border-radius: 12px;
  padding: 10px 14px;
  color: #ffe8b9;
  text-align: left;
  background:
    linear-gradient(135deg, rgba(86, 47, 17, 0.76), rgba(18, 9, 4, 0.66)),
    radial-gradient(circle at 18% 20%, rgba(255, 218, 142, 0.18), transparent 0 44%);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.24), inset 0 1px 0 rgba(255, 235, 180, 0.1);
  cursor: pointer;
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}

.review-desk-entry strong {
  color: #ffe2a0;
  font-size: 0.9rem;
  letter-spacing: 0.02em;
}

.review-desk-entry:hover,
.review-desk-entry:focus-visible {
  border-color: rgba(255, 224, 157, 0.78);
  background:
    linear-gradient(135deg, rgba(105, 59, 22, 0.82), rgba(28, 14, 6, 0.72)),
    radial-gradient(circle at 18% 20%, rgba(255, 218, 142, 0.24), transparent 0 46%);
  transform: translateY(-1px);
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.32), inset 0 1px 0 rgba(255, 235, 180, 0.14);
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

.primary-action:disabled,
.quiet-action:disabled {
  cursor: not-allowed;
  opacity: 0.52;
  transform: none;
}

.detail-actions {
  align-content: start;
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
    "link link"
    "actions actions"
    "sync operation";
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
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

  .repository-shortcuts {
    grid-template-columns: 1fr;
  }

  .panel-head.inline {
    align-items: start;
    flex-direction: column;
  }
}
</style>
