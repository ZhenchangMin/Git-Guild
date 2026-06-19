<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'

import { adminApi } from '../../api/adminApi'
import { decisionMeta, questStatusMeta } from '../../data/admin'

const ADMIN_NAME = '管理员 · 审核台'
const REASON_MAX = 500

const DEFAULT_PAGE_SIZE = 20

function cloneApplication(application) {
  return {
    ...application,
    clarityChecks: application.clarityChecks.map((check) => ({ ...check })),
    complianceChecks: application.complianceChecks.map((check) => ({ ...check })),
    risks: [...application.risks],
    reviewRecords: application.reviewRecords.map((record) => ({ ...record })),
  }
}

const applications = ref([])
const activeId = ref(null)
const statusFilter = ref('ALL')

const reason = ref('')
const visibleToPublisher = ref(true)
const reasonError = ref('')
const submitting = ref(false)
const loading = ref(false)
const loadError = ref('')
const reasonField = ref(null)

const actionResult = ref({
  tone: 'idle',
  title: '等待管理员审核',
  body: '从左侧队列选择任务发布申请，核对清晰度、合规性、Issue 关联与完成标准，填写审核原因后再做决定。',
})

// 通过 / 退回 / 下架（有接取时）提交前的二次确认；保存当前等待确认的决策，null 表示未在确认态。
const confirmingDecision = ref(null)

// 三种决策各自的确认弹窗文案与按钮标签，颜色由 recordTone(decision) 驱动以区分通过(绿)/退回(橙)/下架(红)。
const confirmDialogMeta = {
  APPROVE_PUBLISH: {
    confirmLabel: '确认通过上架',
    text: (application) =>
      `确认通过「${application.questCode}」的上架审核吗？通过后将立即进入悬赏任务板，供冒险家接取。`,
  },
  REJECT_PUBLISH: {
    confirmLabel: '确认退回补充',
    text: (application) =>
      `确认退回「${application.questCode}」要求补充吗？发布者需要根据审核原因修改后重新提交审核。`,
  },
  TAKE_DOWN: {
    confirmLabel: '确认下架',
    text: (application) =>
      `确认下架「${application.questCode}」吗？${
        application.assignmentActive ? '接取者的进度将被取消，且无法恢复。' : '该任务将不再展示于悬赏任务板。'
      }`,
  },
}

function confirmButtonClass(decision) {
  const intent = decisionMeta[decision]?.intent
  if (intent === 'primary') return 'primary-action'
  if (intent === 'danger') return 'quiet-action danger'
  return 'quiet-action warn'
}

function cancelConfirm() {
  confirmingDecision.value = null
}

const statusFilterOptions = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING_ADMIN_REVIEW', label: '待审核' },
  { key: 'PUBLISHED', label: '已上架' },
  { key: 'TAKEN', label: '已接取' },
  { key: 'REJECTED', label: '已退回' },
  { key: 'CLOSED', label: '已下架' },
]

const statusHint = {
  DRAFT: '任务已退回，等待发布者补充完整后重新提交审核。',
  REJECTED: '任务已退回，等待发布者补充完整后重新提交审核。',
  PUBLISHED: '任务已通过管理员审核并上架，当前不需要继续处理。',
}

// 队列自动刷新：避免管理员必须手动刷新页面才能看到新提交的待审核委托。
const REFRESH_INTERVAL_MS = 15000
let refreshTimer = null
const isRefreshing = ref(false)

onMounted(() => {
  loadAdminQuests()
  refreshTimer = window.setInterval(refreshQueue, REFRESH_INTERVAL_MS)
  document.addEventListener('visibilitychange', handleVisibility)
  window.addEventListener('focus', refreshQueue)
})

onUnmounted(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
  document.removeEventListener('visibilitychange', handleVisibility)
  window.removeEventListener('focus', refreshQueue)
})

function handleVisibility() {
  if (document.visibilityState === 'visible') refreshQueue()
}

// 增量刷新：只把"新出现的委托"并入队列（置顶），保留管理员当前正在核验的条目与现场进度，
// 不整页重载、不打断填写中的审核结论。轮询失败保持静默，下次再试。
async function refreshQueue() {
  if (loading.value || submitting.value || isRefreshing.value) return
  isRefreshing.value = true
  try {
    const response = await adminApi.listAdminQuests({ page: 1, size: DEFAULT_PAGE_SIZE })
    const items = response?.data?.items ?? []
    const existingById = new Map(applications.value.map((app) => [String(app.questId), app]))

    // 已在队列中的：同步最新状态（如别处已处理），但保留现场核验进度。
    items.forEach((item) => {
      const existing = existingById.get(String(item.questId))
      if (existing && item.status && existing.questStatus !== item.status) {
        existing.questStatus = item.status
      }
    })

    const newSummaries = items.filter((item) => !existingById.has(String(item.questId)))
    if (newSummaries.length === 0) return

    const mapped = await Promise.all(newSummaries.map(toApplication))
    const fresh = mapped.map(cloneApplication)
    applications.value = [...fresh, ...applications.value]
    if (!activeId.value) activeId.value = applications.value[0]?.id ?? null
    // 此前为空态提示时，恢复为正常待审核引导。
    if (actionResult.value.tone === 'return') {
      actionResult.value = {
        tone: 'idle',
        title: '等待管理员审核',
        body: '从左侧队列选择任务发布申请，核对清晰度、合规性、Issue 关联与完成标准，填写审核原因后再做决定。',
      }
    }
  } catch {
    // 静默：轮询失败不打断管理员当前操作。
  } finally {
    isRefreshing.value = false
  }
}

async function loadAdminQuests() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await adminApi.listAdminQuests({ page: 1, size: DEFAULT_PAGE_SIZE })
    const items = response?.data?.items ?? []
    const mapped = await Promise.all(items.map(toApplication))
    applications.value = mapped.map(cloneApplication)
    activeId.value = applications.value[0]?.id ?? null
    actionResult.value = {
      tone: applications.value.length ? 'idle' : 'return',
      title: applications.value.length ? '等待管理员审核' : '暂无待审核委托',
      body: applications.value.length
        ? '从左侧队列选择任务发布申请，核对清晰度、合规性、Issue 关联与完成标准，填写审核原因后再做决定。'
        : '真实后端当前没有可处理的任务（待审核 / 已上架 / 已退回 / 已下架）。请先用委托人账号发布并提交审核。',
    }
  } catch (error) {
    loadError.value = readableError(error, '管理员审核队列读取失败。')
    actionResult.value = {
      tone: 'danger',
      title: '审核队列读取失败',
      body: loadError.value,
    }
  } finally {
    loading.value = false
  }
}

async function toApplication(summary) {
  let detail = null
  try {
    const detailResponse = await adminApi.getAdminQuest(summary.questId)
    detail = detailResponse?.data ?? null
  } catch {
    detail = null
  }

  const source = detail ?? summary
  const title = source.title ?? summary.title ?? '未命名委托'
  const description = source.description ?? summary.descriptionPreview ?? '暂无描述'
  const completionCriteria = source.completionCriteria ?? ''
  const repository = source.repository?.name ?? '未关联仓库'
  const repositoryUrl = source.repository?.webUrl ?? null
  const issue = source.issue
    ? `#${source.issue.externalIssueId ?? source.issue.issueId} · ${source.issue.title ?? '未命名 Issue'}`
    : '未关联 Issue'
  const publisher = source.publisher?.username ?? summary.publisher?.username ?? '未知委托人'
  const status = source.status ?? summary.status ?? 'PENDING_ADMIN_REVIEW'
  const questId = source.questId ?? summary.questId
  const assignmentActive = Boolean(source.assignment?.assigned)
  const assigneeName = source.assignment?.assignee?.username ?? null

  return {
    id: `APP-${questId}`,
    questId,
    questCode: `QST-${questId}`,
    title,
    publisher: `委托人 · ${publisher}`,
    repository,
    repositoryUrl,
    issue,
    submittedAt: formatDateTime(source.createdAt ?? summary.createdAt),
    questStatus: status,
    summary: description,
    targetAudience: 'P4-029 端到端联调',
    reward: `${source.rewardXp ?? summary.rewardXp ?? 0} XP`,
    difficulty: source.difficulty ?? summary.difficulty ?? '未标注',
    completionCriteria,
    assignmentActive,
    assigneeName,
    clarityChecks: buildClarityChecks(title, description, completionCriteria),
    complianceChecks: buildComplianceChecks(source),
    risks: [],
    reviewRecords: await loadReviewHistory(questId, publisher, source.createdAt ?? summary.createdAt),
  }
}

// 拉取该 Quest 的全部历史审核记录（真实落库数据），而不是只在本地拼一条临时 SUBMITTED 记录——
// 这样下架 / 退回之后审核记录时间线才会真正反映状态变化。
async function loadReviewHistory(questId, publisher, createdAt) {
  const submittedRecord = {
    decision: 'SUBMITTED',
    adminName: `委托人 · ${publisher}`,
    reviewedAt: formatDateTime(createdAt),
    reason: '发布者提交任务发布申请，等待管理员审核。',
  }
  try {
    const response = await adminApi.listReviewHistory(questId)
    const items = response?.data ?? []
    const mapped = items.map((item) => ({
      decision: item.decision,
      adminName: `管理员 · ${item.admin?.username ?? '审核台'}`,
      reviewedAt: formatDateTime(item.reviewedAt),
      reason: item.reason,
      visibleToPublisher: item.visibleToPublisher,
      checklist: item.checklist ?? [],
    }))
    return annotateSuperseded([submittedRecord, ...mapped])
  } catch {
    return [submittedRecord]
  }
}

// 把被「再次申请且最终通过」覆盖的旧退回记录标记为 superseded，时间线上做弱化展示，
// 而不是让管理员误以为退回记录从未被处理过。
function annotateSuperseded(records) {
  let lastApproveIndex = -1
  records.forEach((record, index) => {
    if (record.decision === 'APPROVE_PUBLISH' || record.decision === 'REOPEN') {
      lastApproveIndex = index
    }
  })
  return records.map((record, index) => ({
    ...record,
    superseded: record.decision === 'REJECT_PUBLISH' && index < lastApproveIndex,
  }))
}

function buildClarityChecks(title, description, completionCriteria) {
  return [
    {
      label: '任务目标明确',
      note: title ? `标题为「${title}」，可作为任务目标核验依据。` : '任务标题缺失，建议退回补充。',
    },
    {
      label: '背景说明充分',
      note: description ? preview(description, 90) : '任务描述为空，建议退回补充。',
    },
    {
      label: '完成标准可验收',
      note: completionCriteria ? preview(completionCriteria, 90) : '完成标准为空，建议退回补充。',
    },
  ]
}

function buildComplianceChecks(quest) {
  return [
    {
      label: '仓库与 Issue 已关联',
      note: quest?.repository && quest?.issue ? '仓库和 Issue 信息已随任务提交。' : '仓库或 Issue 信息不完整。',
    },
    {
      label: '奖励与难度设置合理',
      note: quest?.rewardXp && quest?.difficulty ? `${quest.difficulty} 难度，奖励 ${quest.rewardXp} XP。` : '奖励或难度缺失。',
    },
    {
      label: '仓库文件内容合规',
      note: '请前往 Gitea 仓库核对文件内容，确认不含敏感信息或违规内容。',
      link: quest?.repository?.webUrl ?? null,
    },
  ]
}

function preview(value, maxLength) {
  const text = String(value ?? '')
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
}

function formatDateTime(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hh}:${mm}`
}

function readableError(error, fallback) {
  if (error?.details) return `${fallback} ${error.details}`
  if (error?.message) return `${fallback} ${error.message}`
  return fallback
}

// TAKEN 不是后端的真实 questStatus，而是「已上架且已被接取」这一交叉维度的虚拟筛选项。
const filteredApplications = computed(() => {
  if (statusFilter.value === 'ALL') return applications.value
  if (statusFilter.value === 'TAKEN') {
    return applications.value.filter(
      (application) => application.questStatus === 'PUBLISHED' && application.assignmentActive,
    )
  }
  return applications.value.filter((application) => application.questStatus === statusFilter.value)
})

const activeApplication = computed(
  () =>
    applications.value.find((application) => application.id === activeId.value) ??
    applications.value[0],
)

const queueStats = computed(() => {
  const counts = applications.value.reduce(
    (summary, application) => {
      summary[application.questStatus] = (summary[application.questStatus] ?? 0) + 1
      if (application.questStatus === 'PUBLISHED' && application.assignmentActive) {
        summary.TAKEN = (summary.TAKEN ?? 0) + 1
      }
      return summary
    },
    {},
  )
  return [
    { label: '待审核', value: counts.PENDING_ADMIN_REVIEW ?? 0, tone: 'pending' },
    { label: '已上架', value: counts.PUBLISHED ?? 0, tone: 'approved' },
    { label: '已接取', value: counts.TAKEN ?? 0, tone: 'approved' },
    { label: '已退回', value: (counts.REJECTED ?? 0) + (counts.DRAFT ?? 0), tone: 'return' },
    { label: '已下架', value: counts.CLOSED ?? 0, tone: 'danger' },
  ]
})

// 提交上架审核时随附的 5 项检查清单（清晰度 3 项 + 合规性 2 项），仅供管理员审阅时关注，不需要逐项操作。
function checklistFor(application) {
  return [...application.clarityChecks, ...application.complianceChecks]
}

// 当前任务状态允许的管理员决策（业务规则 2-5）。
const availableDecisions = computed(() => {
  const application = activeApplication.value
  if (!application) return []
  return Object.keys(decisionMeta).filter(
    (decision) => decisionMeta[decision].requires === application.questStatus,
  )
})

function statusOf(application) {
  return questStatusMeta[application.questStatus] ?? { label: application.questStatus, tone: 'pending' }
}

function recordLabel(decision) {
  if (decision === 'SUBMITTED') return '提交申请'
  return decisionMeta[decision]?.label ?? decision
}

function recordTone(decision) {
  if (decision === 'SUBMITTED') return 'pending'
  return decisionMeta[decision]?.intent === 'primary'
    ? 'approved'
    : decisionMeta[decision]?.intent === 'danger'
      ? 'danger'
      : 'return'
}

function formatNow() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  return `今天 ${hh}:${mm}`
}

function selectApplication(application) {
  activeId.value = application.id
  reason.value = ''
  reasonError.value = ''
  visibleToPublisher.value = true
  confirmingDecision.value = null
  actionResult.value = {
    tone: 'idle',
    title: `${application.questCode} 已调阅`,
    body: `正在审核 ${application.questCode} 的任务发布申请。`,
  }
}

async function submitDecision(decision) {
  const application = activeApplication.value
  if (!application || submitting.value) return

  const meta = decisionMeta[decision]
  if (!meta || meta.requires !== application.questStatus) return

  const trimmed = reason.value.trim()
  if (trimmed.length < 1) {
    reasonError.value = '审核原因为必填项，请填写处理说明（1-500 字）。'
    confirmingDecision.value = null
    await nextTick()
    reasonField.value?.focus()
    return
  }
  if (trimmed.length > REASON_MAX) {
    reasonError.value = `审核原因不能超过 ${REASON_MAX} 字。`
    confirmingDecision.value = null
    return
  }
  reasonError.value = ''

  // 通过上架 / 退回补充始终二次确认；下架仅在任务存在 ACTIVE 接取记录时才拦截确认。
  const needsConfirm =
    decision === 'APPROVE_PUBLISH' ||
    decision === 'REJECT_PUBLISH' ||
    (decision === 'TAKE_DOWN' && application.assignmentActive)
  if (needsConfirm && confirmingDecision.value !== decision) {
    confirmingDecision.value = decision
    return
  }

  submitting.value = true
  try {
    const payload = {
      decision,
      reason: trimmed,
      visibleToPublisher: visibleToPublisher.value,
    }
    if (decision === 'APPROVE_PUBLISH') {
      payload.checklist = checklistFor(application).map((check) => ({
        label: check.label,
        passed: true,
      }))
    }
    const response = await adminApi.submitAdminReview(application.questId, payload)
    const reviewed = response?.data

    application.questStatus = reviewed?.questStatus ?? meta.nextStatus
    application.reviewRecords = annotateSuperseded([
      ...application.reviewRecords,
      {
        decision,
        adminName: ADMIN_NAME,
        reviewedAt: reviewed?.reviewedAt ? formatDateTime(reviewed.reviewedAt) : formatNow(),
        reason: trimmed,
        visibleToPublisher: visibleToPublisher.value,
        checklist: payload.checklist ?? [],
      },
    ])
    if (decision === 'TAKE_DOWN') {
      application.assignmentActive = false
    }
    confirmingDecision.value = null

    const notify = visibleToPublisher.value
      ? '审核原因已同步给发布者。'
      : '审核原因仅内部留档，未展示给发布者。'
    actionResult.value = {
      tone: statusOf(application).tone,
      title: `${application.questCode} · ${meta.label}`,
      body: `${meta.message}${notify}`,
    }
    reason.value = ''
  } catch (error) {
    actionResult.value = {
      tone: 'danger',
      title: '审核提交失败',
      body: readableError(error, '管理员审核提交失败。'),
    }
  } finally {
    submitting.value = false
  }
}

</script>

<template>
  <div class="admin-review-route">
      <div class="session-action-stack" aria-label="账号操作">
        <button class="back-orb logout-orb logout-orb-admin" type="button" aria-label="退出登录" @click="logout">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10 7V5a2 2 0 0 1 2-2h6v18h-6a2 2 0 0 1-2-2v-2" />
            <path d="M3 12h10" />
            <path d="m6 9-3 3 3 3" />
          </svg>
          <span>退出登录</span>
        </button>
      </div>

      <div class="admin-review-workspace">
        <aside class="admin-queue-panel" aria-label="任务发布申请队列">
          <div class="admin-panel-head">
            <div>
              <p class="kicker">Admin Clearance</p>
              <h1>任务上架审核</h1>
            </div>
            <span>发布 / 下架</span>
          </div>

          <dl class="admin-stat-grid">
            <div v-for="stat in queueStats" :key="stat.label" :class="stat.tone">
              <dt>{{ stat.label }}</dt>
              <dd>{{ stat.value }}</dd>
            </div>
          </dl>

          <div class="admin-filter-row" role="tablist" aria-label="按状态筛选">
            <button
              v-for="option in statusFilterOptions"
              :key="option.key"
              type="button"
              role="tab"
              class="admin-filter-chip"
              :class="{ active: statusFilter === option.key }"
              :aria-selected="statusFilter === option.key"
              @click="statusFilter = option.key"
            >
              {{ option.label }}
            </button>
          </div>

          <div class="admin-application-list">
            <button
              v-for="application in filteredApplications"
              :key="application.id"
              class="admin-application-card"
              :class="[{ active: activeApplication?.id === application.id }, statusOf(application).tone]"
              type="button"
              @click="selectApplication(application)"
            >
              <span>{{ application.id }} · {{ application.submittedAt }}</span>
              <strong>{{ application.questCode }} · {{ application.title }}</strong>
              <small>{{ application.publisher }}</small>
              <em>
                {{ statusOf(application).label }}
                <template v-if="application.questStatus === 'PUBLISHED' && application.assignmentActive"> · 已接取</template>
              </em>
            </button>
            <p v-if="filteredApplications.length === 0" class="admin-empty-queue">
              {{ loading ? '正在读取真实审核队列...' : loadError || '当前筛选下没有任务发布申请。' }}
            </p>
          </div>
        </aside>

        <!-- 详情 -->
        <section v-if="activeApplication" class="admin-detail-panel" aria-live="polite">
          <header class="admin-detail-hero">
            <div>
              <p class="kicker">Quest Publishing Application</p>
              <h2>{{ activeApplication.questCode }} · {{ activeApplication.title }}</h2>
              <p>{{ activeApplication.summary }}</p>
            </div>
            <span class="admin-status-seal" :class="statusOf(activeApplication).tone">
              {{ statusOf(activeApplication).label }}
            </span>
          </header>

          <div class="admin-detail-grid">
            <!-- 委托内容：把发布者填写的完整任务内容前置展示，方便管理员逐项核对再决定是否上架。 -->
            <article class="admin-ledger-card content-card">
              <div class="admin-card-title">
                <div>
                  <p class="kicker">Quest Content</p>
                  <h3>委托内容</h3>
                </div>
              </div>
              <dl>
                <div>
                  <dt>目标对象</dt>
                  <dd>{{ activeApplication.targetAudience }}</dd>
                </div>
                <div>
                  <dt>难度 · 奖励</dt>
                  <dd>{{ activeApplication.difficulty }} · {{ activeApplication.reward }}</dd>
                </div>
                <div class="admin-dl-wide">
                  <dt>任务描述</dt>
                  <dd>{{ activeApplication.summary }}</dd>
                </div>
                <div class="admin-dl-wide">
                  <dt>完成标准</dt>
                  <dd>{{ activeApplication.completionCriteria || '维护者尚未填写完成标准。' }}</dd>
                </div>
              </dl>
            </article>

            <article class="admin-ledger-card issue-card">
              <div class="admin-card-title">
                <div>
                  <p class="kicker">Issue</p>
                  <h3>Issue 关联</h3>
                </div>
                <a
                  v-if="activeApplication.repositoryUrl"
                  class="admin-gitea-link"
                  :href="activeApplication.repositoryUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  前往 Gitea 查看仓库 ↗
                </a>
              </div>
              <dl>
                <div>
                  <dt>发布者</dt>
                  <dd>{{ activeApplication.publisher }}</dd>
                </div>
                <div>
                  <dt>仓库</dt>
                  <dd>{{ activeApplication.repository }}</dd>
                </div>
                <div>
                  <dt>Issue</dt>
                  <dd>{{ activeApplication.issue }}</dd>
                </div>
                <div>
                  <dt>当前接取状态</dt>
                  <dd>
                    {{ activeApplication.assignmentActive ? `已被接取 · ${activeApplication.assigneeName ?? '冒险家'}` : '尚无人接取' }}
                  </dd>
                </div>
              </dl>
            </article>

            <article class="admin-ledger-card check-card">
              <div class="admin-card-title">
                <div>
                  <p class="kicker">Clarity Check</p>
                  <h3>清晰度检查</h3>
                </div>
                <span>提示关注点 · {{ activeApplication.clarityChecks.length }} 项</span>
              </div>
              <div class="admin-check-list">
                <section
                  v-for="check in activeApplication.clarityChecks"
                  :key="check.label"
                  class="admin-check-row"
                >
                  <div class="admin-check-head">
                    <strong>{{ check.label }}</strong>
                  </div>
                  <p>{{ check.note }}</p>
                </section>
              </div>
            </article>

            <article class="admin-ledger-card check-card">
              <div class="admin-card-title">
                <div>
                  <p class="kicker">Compliance</p>
                  <h3>合规性检查</h3>
                </div>
                <span>提示关注点 · {{ activeApplication.complianceChecks.length }} 项</span>
              </div>
              <div class="admin-check-list">
                <section
                  v-for="check in activeApplication.complianceChecks"
                  :key="check.label"
                  class="admin-check-row"
                >
                  <div class="admin-check-head">
                    <strong>{{ check.label }}</strong>
                  </div>
                  <p>{{ check.note }}</p>
                  <a
                    v-if="check.link"
                    class="admin-gitea-link"
                    :href="check.link"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    前往 Gitea 仓库查看 ↗
                  </a>
                </section>
              </div>
            </article>

            <!-- 审核记录（AdminReviewRecord） -->
            <article class="admin-ledger-card timeline-card">
              <p class="kicker">Audit Trail</p>
              <h3>审核记录</h3>
              <ol class="admin-timeline">
                <li
                  v-for="(record, index) in activeApplication.reviewRecords"
                  :key="index"
                  :class="[recordTone(record.decision), { superseded: record.superseded }]"
                >
                  <div class="timeline-head">
                    <strong>{{ recordLabel(record.decision) }}</strong>
                    <time>{{ record.reviewedAt }}</time>
                  </div>
                  <p class="timeline-reason">{{ record.reason }}</p>
                  <small>
                    {{ record.adminName }}
                    <em v-if="record.decision !== 'SUBMITTED' && record.visibleToPublisher === false">
                      · 仅内部可见
                    </em>
                    <em v-if="record.superseded" class="superseded-badge">· 已被覆盖</em>
                  </small>
                </li>
              </ol>
            </article>
          </div>
        </section>

        <!-- 操作 -->
        <aside v-if="activeApplication" class="admin-action-panel">
          <section class="parchment-panel admin-decision-card">
            <p class="kicker">审核决定</p>
            <h2>管理员操作</h2>

            <template v-if="availableDecisions.length > 0">
              <p class="admin-decision-lead">
                通过后进入悬赏任务板；退回或下架都不会展示给冒险家。审核原因为必填项。
              </p>

              <p v-if="activeApplication.assignmentActive && !confirmingDecision" class="admin-assignment-warning">
                ⚠ 该任务已被「{{ activeApplication.assigneeName ?? '某位冒险家' }}」接取。下架会同步取消其接取记录，对方需重新接取才能继续。
              </p>

              <div v-if="confirmingDecision" class="admin-confirm-dialog" :class="recordTone(confirmingDecision)">
                <p>{{ confirmDialogMeta[confirmingDecision].text(activeApplication) }}</p>
                <div class="admin-confirm-actions">
                  <button
                    type="button"
                    :class="confirmButtonClass(confirmingDecision)"
                    :disabled="submitting"
                    @click="submitDecision(confirmingDecision)"
                  >
                    {{ confirmDialogMeta[confirmingDecision].confirmLabel }}
                  </button>
                  <button type="button" class="quiet-action" @click="cancelConfirm">取消</button>
                </div>
              </div>

              <template v-else>
                <label class="admin-reason-field" :class="{ invalid: reasonError }">
                  <span class="admin-reason-label">
                    审核原因
                    <small>{{ reason.length }} / {{ REASON_MAX }}</small>
                  </span>
                  <textarea
                    ref="reasonField"
                    v-model="reason"
                    :maxlength="REASON_MAX"
                    rows="3"
                    placeholder="说明本次审核结论的依据，将记入审核记录。"
                    @input="reasonError = ''"
                  ></textarea>
                  <span v-if="reasonError" class="admin-reason-error">{{ reasonError }}</span>
                </label>

                <label class="admin-visibility-toggle">
                  <input v-model="visibleToPublisher" type="checkbox" />
                  <span>将审核原因展示给任务发布者</span>
                </label>

                <div class="admin-action-buttons">
                  <button
                    v-for="decision in availableDecisions"
                    :key="decision"
                    type="button"
                    :class="
                      decisionMeta[decision].intent === 'primary'
                        ? 'primary-action'
                        : decisionMeta[decision].intent === 'danger'
                          ? 'quiet-action danger'
                          : 'quiet-action'
                    "
                    :disabled="submitting"
                    @click="submitDecision(decision)"
                  >
                    {{ decisionMeta[decision].label }}
                  </button>
                </div>
              </template>
            </template>

            <p v-else class="admin-decision-lead muted">{{ statusHint[activeApplication.questStatus] }}</p>
          </section>

          <section class="glass-ledger admin-result-ledger" :class="actionResult.tone">
            <p class="kicker">操作结果</p>
            <h2>{{ actionResult.title }}</h2>
            <p>{{ actionResult.body }}</p>
          </section>
        </aside>
      </div>
  </div>
</template>
