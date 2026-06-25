<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

import MessageEntryButton from './messages/MessageEntryButton.vue'
import QuestStatusFlow from './QuestStatusFlow.vue'
import { questApi } from '../api/questApi'
import { growthApi } from '../api/growthApi'
import { repositoryApi } from '../api/repositoryApi'
import { submissionApi } from '../api/submissionApi'
import { TUTORIAL_CLOSE_EVENT, TUTORIAL_STEP_EVENT } from '../data/tutorials'
import { workbenchStats, workbenchUser } from '../data/workbench'
import { toBrowsableGiteaUrl } from '../utils/giteaUrl'

// 已清空所有 mock 业务数据：工作台只展示后端真实数据，无数据即呈现真实空态。
// 这些空数组保留原变量名，下游 reduce/filter/find/v-for 均对空数组安全。
const notifications = []
const fallbackPullRequests = []
const recentContributions = []
const fallbackRepositories = []
const reviewFeedbacks = []
const fallbackTaskGroups = []
const workbenchEmails = []
const ADVENTURER_WORKBENCH_TUTORIAL_ID = 'adventurerWorkbench'
const tutorialWorkbenchRepository = {
  name: 'gitguild-web',
  syncStatus: 'Synced',
  defaultBranch: 'main',
  branches: 3,
  issues: 12,
  pullRequests: 4,
  lastCommit: 'c7a42f1',
  repositoryId: 1042,
  hostType: 'GITEA',
  sourceUrl: 'http://127.0.0.1:3000/gitguild/gitguild-web.git',
  giteaUrl: 'http://127.0.0.1:3000/gitguild/gitguild-web',
}
const tutorialWorkbenchTaskGroups = [
  {
    id: 'in-progress',
    label: '进行中',
    tasks: [
      {
        id: 'QST-1042',
        questId: 1042,
        title: '修复站内信未读角标同步异常',
        repository: tutorialWorkbenchRepository.name,
        repositoryId: tutorialWorkbenchRepository.repositoryId,
        issue: '#128',
        prStatus: '未发起',
        branch: 'feature/qst-1042-message-badge-sync',
        recentCommit: '待上传',
        prNumber: '',
        prState: '未创建',
        checkResult: '等待提交',
        counterLink: '未登记',
        counterDetail: '分支已准备好；完成修改并推送后，再到提交柜台登记成果。',
        nextStep: '克隆仓库，切换任务分支并完成修复',
        externalUrl: '',
        changeRequests: [],
        tutorialMock: true,
        actions: [
          { label: '同步 PR 状态', type: 'sync-pr' },
          { label: '去提交柜台', type: 'submit', primary: true },
        ],
      },
      {
        id: 'QST-1036',
        questId: 1036,
        title: '补齐任务详情页验收标准展示',
        repository: tutorialWorkbenchRepository.name,
        repositoryId: tutorialWorkbenchRepository.repositoryId,
        issue: '#121',
        prStatus: '未发起',
        branch: 'feature/qst-1036-criteria-panel',
        recentCommit: '待上传',
        prNumber: '',
        prState: '未创建',
        checkResult: '等待提交',
        counterLink: '未登记',
        counterDetail: '任务已接取，完成页面补充后再登记成果。',
        nextStep: '确认详情页数据结构并补齐验收标准区域',
        externalUrl: '',
        changeRequests: [],
        tutorialMock: true,
        actions: [
          { label: '同步 PR 状态', type: 'sync-pr' },
          { label: '去提交柜台', type: 'submit', primary: true },
        ],
      },
    ],
  },
  {
    id: 'changes-requested',
    label: '待修改',
    tasks: [
      {
        id: 'QST-1028',
        questId: 1028,
        title: '调整排行榜移动端文字换行',
        repository: tutorialWorkbenchRepository.name,
        repositoryId: tutorialWorkbenchRepository.repositoryId,
        issue: '#115',
        prStatus: 'PR #42 退回修改',
        branch: 'feature/qst-1028-mobile-ranking',
        recentCommit: '已同步',
        prNumber: 'PR #42',
        prState: '退回修改',
        checkResult: '复审前待修改',
        counterLink: '需重新提交',
        counterDetail: '维护者要求补充小屏宽度下的换行处理。',
        nextStep: '按反馈修改样式后重新提交成果',
        externalUrl: '',
        changeRequests: [
          {
            reviewedAt: '06-24 15:20',
            reviewerName: '维护者',
            comment: '排行榜用户名过长时会挤压等级信息，请补充换行或截断处理。',
            checklist: ['小屏用户名不遮挡等级', '刷新按钮仍可点击'],
          },
        ],
        tutorialMock: true,
        actions: [
          { label: '同步 PR 状态', type: 'sync-pr' },
          { label: '去提交柜台', type: 'submit', primary: true },
        ],
      },
    ],
  },
  {
    id: 'in-review',
    label: '审核中',
    tasks: [
      {
        id: 'QST-1019',
        questId: 1019,
        title: '优化提交柜台证据上传提示',
        repository: tutorialWorkbenchRepository.name,
        repositoryId: tutorialWorkbenchRepository.repositoryId,
        issue: '#109',
        prStatus: 'PR #39 待审核',
        branch: 'feature/qst-1019-evidence-hint',
        recentCommit: '已同步',
        prNumber: 'PR #39',
        prState: '待审核',
        checkResult: '基础检查通过',
        counterLink: '待审核',
        counterDetail: '成果已提交，等待维护者审核。',
        nextStep: '等待审核结果或查看站内信通知',
        externalUrl: '',
        changeRequests: [],
        tutorialMock: true,
        actions: [
          { label: '同步 PR 状态', type: 'sync-pr' },
          { label: '去提交柜台', type: 'submit', primary: true },
        ],
      },
    ],
  },
  {
    id: 'completed',
    label: '已完成',
    tasks: [
      {
        id: 'QST-1007',
        questId: 1007,
        title: '修复帮助页快捷入口跳转',
        repository: tutorialWorkbenchRepository.name,
        repositoryId: tutorialWorkbenchRepository.repositoryId,
        issue: '#97',
        prStatus: 'PR #31 已合并',
        branch: 'feature/qst-1007-help-links',
        recentCommit: '已同步',
        prNumber: 'PR #31',
        prState: '已合并',
        checkResult: '审核通过',
        counterLink: '已登记',
        counterDetail: '任务成果已通过审核并写入成长记录。',
        nextStep: '查看成长档案中的贡献记录',
        externalUrl: '',
        changeRequests: [],
        tutorialMock: true,
        actions: [
          { label: '同步 PR 状态', type: 'sync-pr' },
          { label: '去提交柜台', type: 'submit', primary: true },
        ],
      },
    ],
  },
]
import { sessionStore } from '../stores/sessionStore'
import {
  loadNotifications,
  markNotificationRead,
  notificationStore,
  openNotification,
} from '../stores/notificationStore'
import { loadMessageThreads, messageUnreadCount, openQuestMessages } from '../stores/messageStore'
import { notificationMeta } from '../data/notificationMeta'

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
  avatarUrl: sessionStore.user?.avatarUrl || '',
  // 成长数值初始为 null，避免真实数据到达前闪现演示用户（Minerva Dawn / 720 XP）的等级与 XP
  level: null,
  xpCurrent: null,
  xpTarget: null,
  completedQuests: null,
})
const liveAssignments = ref([])       // MyAssignmentItem[]
const liveRepositories = ref([])      // Repo[]
const livePullRequests = ref([])      // PR[]
// 初始展示占位符 '-'，避免真实数据到达前闪现 workbenchStats 演示数值（2/1/1/3）
const liveStats = ref(workbenchStats.map((stat) => ({ ...stat, value: '-' })))
const isAdventurerWorkbenchTutorialActive = ref(false)
const liveLoading = ref(true) // 起始即加载态：成长数据由 onMounted 拉取，首帧先显示占位符而非演示值

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
        avatarUrl: sessionStore.user?.avatarUrl || '',
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
          { label: '未读信笺', value: 0 },
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
  if (items.length === 0 && isAdventurerWorkbenchTutorialActive.value) return tutorialWorkbenchTaskGroups
  if (items.length === 0) return fallbackTaskGroups

  // 顺序：进行中 → 待修改 → 审核中 → 已完成（Object.values 保留插入顺序）
  const groups = {
    'in-progress': { id: 'in-progress', label: '进行中', tasks: [] },
    'changes-requested': { id: 'changes-requested', label: '待修改', tasks: [] },
    'in-review': { id: 'in-review', label: '审核中', tasks: [] },
    completed: { id: 'completed', label: '已完成', tasks: [] },
  }
  for (const item of items) {
    // 「待修改」看 Submission 状态——Quest 状态退回修改时仍停留在 IN_REVIEW，不会单独转出一个状态。
    const status = item.questStatus ?? item.assignmentStatus
    let groupId = 'in-progress'
    if (item.latestSubmissionStatus === 'CHANGES_REQUESTED') groupId = 'changes-requested'
    else if (status === 'IN_REVIEW') groupId = 'in-review'
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
    // 维护者每次退回时留下的意见，按时间升序——QuestStatusFlow 用它判断是否允许查看「退回修改」节点并渲染历史。
    changeRequests: item.changeRequests ?? [],
    actions: [
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
  sessionStore.user?.displayName || sessionStore.user?.username || liveUser.value.name || '冒险家',
)
const effectiveAvatarUrl = computed(() => sessionStore.user?.avatarUrl || liveUser.value.avatarUrl || '')
const effectiveAvatarInitial = computed(() => effectiveUserName.value.trim().charAt(0).toUpperCase() || '冒')
const avatarLoadFailed = ref(false)
watch(effectiveAvatarUrl, () => {
  avatarLoadFailed.value = false
})
const effectiveLevel = computed(() => liveUser.value.level || 1)
const effectiveXpCurrent = computed(() => liveUser.value.xpCurrent || 0)
const effectiveXpTarget = computed(() => liveUser.value.xpTarget || 1000)
const effectiveCompletedQuests = computed(() => liveUser.value.completedQuests || 0)
// 成长数据加载完成前以占位符渲染等级 / XP，避免出现"别人的 XP"再跳变
const levelText = computed(() => (liveLoading.value ? '—' : effectiveLevel.value))
const xpText = computed(() =>
  liveLoading.value ? '— / —' : `${effectiveXpCurrent.value} / ${effectiveXpTarget.value}`,
)
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
// 信箱未读数直接取真实站内通知的未读数（信箱现在只展示真实通知）。
const totalUnreadCount = computed(() => notificationStore.unreadCount)
const displayStats = computed(() =>
  liveStats.value.map((stat) =>
    stat.label === '未读邮件' || stat.label === '未读信笺'
      ? { ...stat, label: '未读信笺', value: messageUnreadCount.value }
      : stat,
  ),
)
const selectedTaskRepository = computed(() =>
  repositoryList.value.find((repository) => repository.name === selectedTask.value?.repository),
)
const selectedTaskPullRequests = computed(() => {
  if (!selectedTask.value) return []

  const prId = selectedTask.value.prNumber || selectedTask.value.prStatus.match(/PR #\d+/)?.[0]
  return pullRequestList.value.filter((pr) => pr.id === prId || pr.taskId === selectedTask.value.id)
})

// ── 冒险家「克隆并推送」实路径 ────────────────────────────────────────────
// 自动取真实仓库地址（repository.sourceUrl）+ 调后端 ensureTaskBranch 拿平台自动
// 创建的任务分支；冒险家只需 clone → 切到该分支 → 改完 push，再回提交柜台一键提交。
const realTaskBranch = ref('')
const branchLoading = ref(false)
const branchError = ref('')
const copiedKey = ref('')
const copiedOk = ref(true)
const taskRepoSourceUrl = ref('')
const taskCloneUrlAuth = ref('') // 带 admin 凭据的克隆地址（来自 ensureTaskBranch），供命令直接 clone+push
let copiedTimer = 0

// 冒险家不是仓库 owner，GET /repositories 列表对他为空；改为按 repositoryId 取仓库详情拿 sourceUrl。
const taskCloneUrl = computed(
  () => toBrowsableGiteaUrl(taskRepoSourceUrl.value) || getRepositoryUrl(selectedTaskRepository.value),
)
const taskRepoFolder = computed(() => {
  const url = taskCloneUrl.value
  if (!url) return 'repo'
  const last = url.replace(/\/+$/, '').split('/').pop() || 'repo'
  return last.replace(/\.git$/i, '')
})
const taskCloneSteps = computed(() => {
  const cloneUrl = taskCloneUrlAuth.value || taskCloneUrl.value || '<仓库地址>'
  const branch = realTaskBranch.value || '<任务分支>'
  const commitMsg = `${selectedTask.value?.id || 'task'}: 完成任务`
  return [
    {
      title: '克隆仓库',
      body: '地址已内置凭据，clone 后 push 不会再要账号密码（已克隆可跳过）。',
      command: `git clone ${cloneUrl}`,
    },
    {
      title: '切到任务分支',
      body: '平台已自动创建该分支，直接切过去即可，无需自己新建。',
      command: `cd ${taskRepoFolder.value} && git checkout ${branch}`,
    },
    { title: '改完推送', body: '完成修改后提交并推送到任务分支。', command: `git add . && git commit -m "${commitMsg}" && git push` },
  ]
})

async function ensureRealTaskBranch(questId) {
  if (!questId) return
  branchLoading.value = true
  branchError.value = ''
  try {
    const res = await questApi.ensureTaskBranch(questId)
    const branch = res?.data?.taskBranch || ''
    realTaskBranch.value = branch
    taskCloneUrlAuth.value = toBrowsableGiteaUrl(res?.data?.cloneUrl || '')
    if (branch && selectedTask.value) selectedTask.value.branch = branch
  } catch (error) {
    realTaskBranch.value = ''
    branchError.value = error?.message || '任务分支创建失败，请稍后重试。'
  } finally {
    branchLoading.value = false
  }
}

async function fetchTaskRepoSourceUrl(repositoryId) {
  if (!repositoryId) {
    taskRepoSourceUrl.value = ''
    return
  }
  try {
    const res = await repositoryApi.detail(repositoryId)
    taskRepoSourceUrl.value = res?.data?.sourceUrl || ''
  } catch {
    taskRepoSourceUrl.value = ''
  }
}

// 选中任务时：取仓库地址（按 repositoryId），并对进行中 / 待修改任务自动确保任务分支。
watch(
  () => selectedTask.value?.questId,
  (questId) => {
    realTaskBranch.value = ''
    branchError.value = ''
    taskRepoSourceUrl.value = ''
    taskCloneUrlAuth.value = ''
    if (selectedTask.value?.tutorialMock) {
      realTaskBranch.value = selectedTask.value.branch
      taskRepoSourceUrl.value = tutorialWorkbenchRepository.sourceUrl
      return
    }
    fetchTaskRepoSourceUrl(selectedTask.value?.repositoryId)
    const state = selectedTask.value?.workflowState
    if (questId && (state === 'in-progress' || state === 'changes-requested')) {
      ensureRealTaskBranch(questId)
    }
  },
  { immediate: true },
)

// 非 HTTPS / 非 localhost 时 navigator.clipboard 不可用（不是安全上下文），需要
// document.execCommand('copy') 兜底，否则线上 http:// 部署点「复制」会悄悄什么都不做。
function copyViaFallback(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  let ok = false
  try {
    ok = document.execCommand('copy')
  } catch {
    ok = false
  }
  document.body.removeChild(textarea)
  return ok
}

async function copyText(text, key) {
  if (!text) return
  let ok = false
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text)
      ok = true
    } catch {
      ok = copyViaFallback(text)
    }
  } else {
    ok = copyViaFallback(text)
  }
  copiedKey.value = key
  copiedOk.value = ok
  window.clearTimeout(copiedTimer)
  copiedTimer = window.setTimeout(() => (copiedKey.value = ''), 1600)
}

function openTaskRepository() {
  if (taskCloneUrl.value) window.open(taskCloneUrl.value, '_blank', 'noopener,noreferrer')
}

function goSubmissionCounter() {
  emit('open-submission', selectedTask.value?.questId ?? selectedTask.value?.id)
}
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
    // 维护者每次退回时写的建议，按时间升序；QuestStatusFlow 据此渲染「退回修改」节点的历史记录。
    feedbackHistory: selectedTask.value.changeRequests ?? [],
    syncStatus: selectedTaskRepository.value?.syncStatus,
    viewer: effectiveUserName.value,
  }
})
const linkPanelTask = computed(() => selectedTask.value ?? null)
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
const xpBarWidth = computed(() => (liveLoading.value ? '0%' : xpProgress.value))
const activeRoleLabel = computed(() => effectiveRole.value)

function selectTask(task) {
  selectedTaskId.value = task.id
  selectedEmailId.value = null
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

function selectNotification(notification) {
  selectedNotificationText.value = notification.text
  selectedTaskId.value = null
  selectedEmailId.value = null
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

// 点信箱里的某条通知：关掉信箱，弹出与公会大厅同款的「信笺」详情弹窗（含动作按钮，自动标记已读）。
function openMailNotification(item) {
  isMailboxOpen.value = false
  openNotification(item)
}

function openSelectedTaskMessages() {
  if (!selectedTask.value?.questId) return
  isMailboxOpen.value = false
  openQuestMessages(selectedTask.value.questId)
}

function mailTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function handleAdventurerWorkbenchTutorialStep(event) {
  if (event.detail?.tutorialId === ADVENTURER_WORKBENCH_TUTORIAL_ID) {
    isAdventurerWorkbenchTutorialActive.value = true
  }
}

function handleAdventurerWorkbenchTutorialClose(event) {
  if (event.detail?.tutorialId === ADVENTURER_WORKBENCH_TUTORIAL_ID) {
    isAdventurerWorkbenchTutorialActive.value = false
  }
}

onMounted(() => {
  // 轮询由全局 NotificationCenter 统一负责；这里只主动拉一次，保证信箱首屏未读数最新。
  loadNotifications()
  loadMessageThreads()
  fetchWorkbenchData()
  window.addEventListener(TUTORIAL_STEP_EVENT, handleAdventurerWorkbenchTutorialStep)
  window.addEventListener(TUTORIAL_CLOSE_EVENT, handleAdventurerWorkbenchTutorialClose)
})

onUnmounted(() => {
  window.removeEventListener(TUTORIAL_STEP_EVENT, handleAdventurerWorkbenchTutorialStep)
  window.removeEventListener(TUTORIAL_CLOSE_EVENT, handleAdventurerWorkbenchTutorialClose)
})

function showGrowthDetails(source = '个人成长档案') {
  isGrowthDetailOpen.value = true
  selectedTaskId.value = null
  selectedEmailId.value = null
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

function findRepositoryForTask(task) {
  if (!task) return null
  return (
    repositoryList.value.find((repository) => repository.repositoryId && repository.repositoryId === task.repositoryId) ??
    findRepositoryRecord(task.repository) ??
    null
  )
}

function getRepositoryUrl(repository) {
  return toBrowsableGiteaUrl(repository?.giteaUrl || repository?.sourceUrl || repository?.url || '')
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

function findActionTaskRecord() {
  return selectedTask.value ? findTaskRecord(selectedTask.value.id) : null
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

  if (action.type === 'exception') {
    const repository = selectedTaskRepository.value
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
          <span class="user-avatar" :class="{ 'has-img': effectiveAvatarUrl && !avatarLoadFailed }" aria-hidden="true">
            <img
              v-if="effectiveAvatarUrl && !avatarLoadFailed"
              :src="effectiveAvatarUrl"
              alt=""
              @error="avatarLoadFailed = true"
            />
            <span v-else>{{ effectiveAvatarInitial }}</span>
          </span>
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
            <strong>Level {{ levelText }}</strong>
            <span>{{ xpText }} XP</span>
          </div>
          <button class="quiet-action detail-link" type="button" @click="openIdCard">
            查看详细信息
          </button>
        </div>
        <div class="xp-track" aria-label="XP 进度">
          <span :style="{ width: xpBarWidth }"></span>
        </div>
      </div>

      <dl class="workbench-stat-grid">
        <div v-for="stat in displayStats" :key="stat.label">
          <dt>{{ stat.label }}</dt>
          <dd>{{ stat.value }}</dd>
        </div>
      </dl>

      <div class="mailbox-area">
        <MessageEntryButton variant="circle" />
        <button
          class="mail-button"
          type="button"
          aria-label="打开系统通知"
          :aria-expanded="isMailboxOpen"
          @click="toggleMailbox"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.7 21a2 2 0 0 1-3.4 0" />
          </svg>
          <span v-if="totalUnreadCount > 0" class="mail-alert" aria-label="有未读消息">!</span>
        </button>

        <section v-if="isMailboxOpen" class="mailbox-popover" aria-label="站内通知">
          <div class="mailbox-head">
            <div>
              <p class="kicker">Mailbox</p>
              <h2>站内通知</h2>
            </div>
            <span>{{ totalUnreadCount }} 未读</span>
          </div>

          <p v-if="notificationStore.items.length === 0" class="mailbox-empty">暂无通知</p>
          <button
            v-for="item in notificationStore.items"
            :key="item.notificationId"
            class="mail-preview"
            :class="[notificationMeta(item.type).tone, { unread: item.status === 'UNREAD' }]"
            type="button"
            @click="openMailNotification(item)"
          >
            <span>{{ notificationMeta(item.type).title }} · {{ mailTime(item.createdAt) }}</span>
            <strong>{{ item.content }}</strong>
          </button>
        </section>
      </div>
    </header>

    <aside class="workbench-panel todo-panel" data-tutorial="workbench-task-list">
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
          <div class="task-detail-actions">
            <button
              class="quiet-action message-task-action"
              type="button"
              data-tutorial="workbench-contact-client"
              @click="openSelectedTaskMessages"
            >
              联系任务对方
            </button>
            <span class="status-pill">{{ selectedTask.statusLabel }}</span>
          </div>
        </div>

        <div class="detail-grid">
          <QuestStatusFlow
            class="task-flow-card"
            data-tutorial="workbench-task-overview"
            :status="selectedTask.workflowState"
            :context="selectedTaskFlowContext"
            compact
          />

          <!-- 「已提交/审核中」阶段不再需要克隆并推送的命令提示，隐藏整张卡片 -->
          <article
            v-if="selectedTask.workflowState !== 'in-review'"
            class="detail-card wide task-clone-card"
            data-tutorial="workbench-clone-card"
          >
            <div class="clone-head">
              <div>
                <p class="kicker">Clone &amp; Push</p>
                <h3>克隆并推送</h3>
              </div>
              <span class="status-pill" :class="{ warning: !!branchError }">
                {{ branchLoading ? '准备分支中…' : branchError ? '分支未就绪' : realTaskBranch ? '分支已就绪' : '准备中' }}
              </span>
            </div>
            <p class="clone-helper">
              平台已自动创建任务分支，你只需克隆仓库、切到该分支、改完推送，再回提交柜台一键提交（提交时自动创建 PR，无需手动建 PR）。
            </p>

            <div class="clone-field-grid">
              <div class="clone-field">
                <span class="clone-label">仓库地址</span>
                <code>{{ taskCloneUrl || '—' }}</code>
                <button
                  class="quiet-action copy-btn"
                  :class="{ copied: copiedKey === 'clone-url' }"
                  type="button"
                  :disabled="!taskCloneUrl"
                  @click="copyText(taskCloneUrl, 'clone-url')"
                ><span v-if="copiedKey === 'clone-url'" class="copy-btn-result"><svg v-if="copiedOk" class="copy-check-icon" viewBox="0 0 16 16" width="13" height="13" aria-hidden="true"><path d="M3 8.5l3.2 3.2L13 4.4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>{{ copiedOk ? '已复制' : '复制失败' }}</span><span v-else>复制</span></button>
              </div>
              <div class="clone-field">
                <span class="clone-label">任务分支</span>
                <code>{{ realTaskBranch || (branchLoading ? '创建中…' : '—') }}</code>
                <button
                  class="quiet-action copy-btn"
                  :class="{ copied: copiedKey === 'task-branch' }"
                  type="button"
                  :disabled="!realTaskBranch"
                  @click="copyText(realTaskBranch, 'task-branch')"
                ><span v-if="copiedKey === 'task-branch'" class="copy-btn-result"><svg v-if="copiedOk" class="copy-check-icon" viewBox="0 0 16 16" width="13" height="13" aria-hidden="true"><path d="M3 8.5l3.2 3.2L13 4.4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>{{ copiedOk ? '已复制' : '复制失败' }}</span><span v-else>复制</span></button>
              </div>
            </div>

            <p v-if="branchError" class="clone-error">
              {{ branchError }}
              <button class="clone-retry" type="button" @click="ensureRealTaskBranch(selectedTask.questId)">重试</button>
            </p>

            <ol class="clone-steps" aria-label="克隆与推送步骤" data-tutorial="workbench-copy-command">
              <li v-for="step in taskCloneSteps" :key="step.title">
                <div class="step-head">
                  <strong>{{ step.title }}</strong>
                  <button
                    class="quiet-action copy-btn"
                    :class="{ copied: copiedKey === `step-${step.title}` }"
                    type="button"
                    @click="copyText(step.command, `step-${step.title}`)"
                  ><span v-if="copiedKey === `step-${step.title}`" class="copy-btn-result"><svg v-if="copiedOk" class="copy-check-icon" viewBox="0 0 16 16" width="13" height="13" aria-hidden="true"><path d="M3 8.5l3.2 3.2L13 4.4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>{{ copiedOk ? '已复制' : '复制失败' }}</span><span v-else>复制命令</span></button>
                </div>
                <span>{{ step.body }}</span>
                <code>{{ step.command }}</code>
              </li>
            </ol>

            <div class="clone-actions">
              <button class="quiet-action" type="button" :disabled="!taskCloneUrl" @click="openTaskRepository">
                在 Gitea 打开仓库
              </button>
              <button
                class="primary-action"
                type="button"
                data-tutorial="workbench-submit-counter"
                @click="goSubmissionCounter"
              >去提交柜台</button>
            </div>
          </article>
        </div>
      </section>

      <section v-else-if="isGrowthDetailOpen" class="workbench-panel task-detail-panel growth-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Growth Detail</p>
            <h2>成长详细信息</h2>
          </div>
          <span class="status-pill">Level {{ levelText }}</span>
        </div>

        <div class="growth-detail-grid">
          <article class="growth-hero-card">
            <div>
              <span>当前等级</span>
              <strong>Level {{ levelText }}</strong>
              <p>{{ xpText }} XP</p>
            </div>
            <div class="xp-track large">
              <span :style="{ width: xpBarWidth }"></span>
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
  overflow: hidden;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.3rem;
  background:
    radial-gradient(circle at 38% 28%, rgba(255, 232, 176, 0.3), transparent 0 26%, transparent 50%),
    linear-gradient(145deg, #173154, #0d1a2e 48%, #44220e);
  box-shadow: inset 0 0 0 2px rgba(255, 225, 160, 0.18), 0 10px 24px rgba(0, 0, 0, 0.28);
}

.user-avatar.has-img {
  background: rgba(34, 18, 8, 0.5);
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-avatar span {
  line-height: 1;
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
  display: flex;
  align-items: center;
  gap: 10px;
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

.task-detail-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.message-task-action {
  min-height: 34px;
  padding: 0 12px;
  white-space: nowrap;
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
  border-left-width: 3px;
  border-radius: 5px;
  margin-top: 8px;
  padding: 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(11, 6, 3, 0.38);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

/* 通知类型色条，与公会大厅通知保持一致 */
.mail-preview.tone-approved { border-left-color: #a9d07b; }
.mail-preview.tone-pending { border-left-color: #f0b868; }
.mail-preview.tone-changes { border-left-color: #f0a06d; }
.mail-preview.tone-rejected { border-left-color: #dc826e; }

.mailbox-empty {
  margin: 12px 0 4px;
  color: rgba(255, 231, 183, 0.6);
  font-size: 0.84rem;
  text-align: center;
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
  /* 显式行轨：头部按内容高度贴顶，列表占据剩余空间并滚动。
     否则两条隐式 auto 行会因 align-content 默认 stretch 被平分，
     把头部撑高、再把 kicker 与「我的待办」拉开，导致标题悬在中间。 */
  grid-template-rows: auto minmax(0, 1fr);
  align-content: start;
  overflow: auto;
}

/* 头部内部同理：保持 kicker 与标题紧贴顶部，不被纵向拉伸。 */
.panel-head {
  align-content: start;
}

.todo-group-list {
  display: grid;
  align-content: start;
  gap: 14px;
}

.todo-group {
  display: grid;
  align-content: start;
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
  align-content: start;
  justify-items: start;
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

.todo-task .status-pill {
  align-self: start;
  justify-self: start;
  min-height: 0;
  white-space: nowrap;
  line-height: 1.1;
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
  max-width: 100%;
  color: #ffe8b9;
  line-height: 1.22;
  overflow-wrap: anywhere;
}

.todo-task small,
.operation-panel p,
.notice-row p,
.detail-card p {
  margin: 0;
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.36;
  overflow-wrap: anywhere;
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

  .panel-head.inline {
    align-items: start;
    flex-direction: column;
  }
}

/* ── 冒险家「克隆并推送」卡片 ───────────────────────────────────────── */
.task-clone-card {
  gap: 12px;
  border-color: rgba(238, 184, 91, 0.42);
  background:
    linear-gradient(135deg, rgba(93, 49, 16, 0.6), rgba(11, 6, 3, 0.44)),
    radial-gradient(circle at 8% 0%, rgba(255, 217, 138, 0.14), transparent 0 28%, transparent 58%);
}
.clone-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}
.clone-helper {
  margin: 0;
  max-width: 70ch;
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.5;
}
.clone-field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.clone-field {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(238, 184, 91, 0.22);
  border-radius: 5px;
  padding: 8px 10px;
  background: rgba(7, 4, 2, 0.3);
}
.clone-label {
  color: var(--gold-bright);
  font-size: 0.72rem;
  letter-spacing: 0.06em;
  white-space: nowrap;
}
.clone-field code {
  overflow-x: auto;
  color: #ffe4ad;
  font-size: 0.8rem;
  font-family: 'Consolas', 'SFMono-Regular', monospace;
  white-space: nowrap;
}
.copy-btn {
  min-height: 28px;
  padding: 0 10px;
  font-size: 0.74rem;
}
.clone-error {
  margin: 0;
  color: #ffd7c9;
  font-size: 0.84rem;
}
.clone-retry {
  margin-left: 6px;
  border: none;
  background: transparent;
  color: var(--gold-bright);
  text-decoration: underline;
  cursor: pointer;
}
.clone-steps {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
  counter-reset: clone-step;
}
.clone-steps li {
  display: grid;
  gap: 5px;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 10px;
  background: rgba(7, 4, 2, 0.28);
}
.step-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.step-head strong {
  color: #ffe8b9;
  line-height: 1.25;
}
.step-head strong::before {
  counter-increment: clone-step;
  content: counter(clone-step) '. ';
  color: var(--gold-bright);
}
.clone-steps li span {
  color: rgba(255, 231, 183, 0.74);
  line-height: 1.4;
}
.clone-steps li code {
  overflow-x: auto;
  border: 1px solid rgba(238, 184, 91, 0.22);
  border-radius: 4px;
  padding: 7px 8px;
  color: #ffe4ad;
  background: rgba(12, 7, 4, 0.55);
  font-size: 0.78rem;
  font-family: 'Consolas', 'SFMono-Regular', monospace;
  white-space: nowrap;
}
.clone-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.copy-btn.copied {
  border-color: rgba(182, 227, 160, 0.6);
  background: rgba(46, 90, 32, 0.55);
  color: #d9f5c8;
  animation: copy-btn-pop 220ms ease-out;
}
.copy-btn-result {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.copy-check-icon {
  flex: none;
  color: #9be37f;
  animation: copy-check-draw 220ms ease-out;
}
@keyframes copy-btn-pop {
  0% {
    transform: scale(0.92);
  }
  60% {
    transform: scale(1.04);
  }
  100% {
    transform: scale(1);
  }
}
@keyframes copy-check-draw {
  from {
    opacity: 0;
    transform: scale(0.5);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
@media (max-width: 640px) {
  .clone-field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
