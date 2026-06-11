<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'

import { adminApi } from '../../api/adminApi'
import { decisionMeta, questStatusMeta } from '../../data/admin'

const ADMIN_NAME = '管理员 · 审核台'
const REASON_MAX = 500

const DEFAULT_PAGE_SIZE = 20

function cloneApplication(application) {
  return {
    ...application,
    // verdict 由管理员现场判定（null=待核验 / 'pass'=通过 / 'fail'=存疑），不再由数据预设结论。
    clarityChecks: application.clarityChecks.map((check) => ({ ...check, verdict: null })),
    complianceChecks: application.complianceChecks.map((check) => ({ ...check, verdict: null })),
    completionStandards: [...application.completionStandards],
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

const statusFilterOptions = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING_ADMIN_REVIEW', label: '待审核' },
  { key: 'PUBLISHED', label: '已上架' },
  { key: 'REJECTED', label: '已退回' },
  { key: 'CLOSED', label: '已下架' },
]

const statusHint = {
  DRAFT: '任务已退回，等待发布者补充完整后重新提交审核。',
  REJECTED: '任务已退回，等待发布者补充完整后重新提交审核。',
  CLOSED: '任务已下架关闭，如需重新发布请走后台管理流程单独处理。',
  PUBLISHED: '任务已通过管理员审核并上架，当前不需要继续处理。',
}

onMounted(() => {
  loadAdminQuests()
})

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
        : '真实后端当前没有 PENDING_ADMIN_REVIEW 状态的任务。请先用委托人账号发布并提交审核。',
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
  const issue = source.issue
    ? `#${source.issue.externalIssueId ?? source.issue.issueId} · ${source.issue.title ?? '未命名 Issue'}`
    : '未关联 Issue'
  const publisher = source.publisher?.username ?? summary.publisher?.username ?? '未知委托人'
  const status = source.status ?? summary.status ?? 'PENDING_ADMIN_REVIEW'

  return {
    id: `APP-${source.questId ?? summary.questId}`,
    questId: source.questId ?? summary.questId,
    questCode: `QST-${source.questId ?? summary.questId}`,
    title,
    publisher: `委托人 · ${publisher}`,
    repository,
    issue,
    submittedAt: formatDateTime(source.createdAt ?? summary.createdAt),
    questStatus: status,
    summary: description,
    targetAudience: 'P4-029 端到端联调',
    reward: `${source.rewardXp ?? summary.rewardXp ?? 0} XP`,
    difficulty: source.difficulty ?? summary.difficulty ?? '未标注',
    clarityChecks: buildClarityChecks(title, description, completionCriteria),
    complianceChecks: buildComplianceChecks(source),
    completionStandards: splitCompletionStandards(completionCriteria),
    risks: [],
    reviewRecords: [
      {
        decision: 'SUBMITTED',
        adminName: `委托人 · ${publisher}`,
        reviewedAt: formatDateTime(source.updatedAt ?? source.createdAt ?? summary.createdAt),
        reason: '发布者提交任务发布申请，等待管理员审核。',
      },
    ],
  }
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
      label: '奖励与难度已设置',
      note: quest?.rewardXp && quest?.difficulty ? `${quest.difficulty} 难度，奖励 ${quest.rewardXp} XP。` : '奖励或难度缺失。',
    },
    {
      label: '未绕过管理员上架流程',
      note: '任务处于管理员审核队列，审核通过后才会出现在任务板。',
    },
  ]
}

function splitCompletionStandards(value) {
  return String(value ?? '')
    .split(/\r?\n|[；;]/)
    .map((item) => item.trim())
    .filter(Boolean)
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

const filteredApplications = computed(() =>
  statusFilter.value === 'ALL'
    ? applications.value
    : applications.value.filter((application) => application.questStatus === statusFilter.value),
)

const activeApplication = computed(
  () =>
    applications.value.find((application) => application.id === activeId.value) ??
    applications.value[0],
)

const queueStats = computed(() => {
  const counts = applications.value.reduce(
    (summary, application) => {
      summary[application.questStatus] = (summary[application.questStatus] ?? 0) + 1
      return summary
    },
    {},
  )
  return [
    { label: '待审核', value: counts.PENDING_ADMIN_REVIEW ?? 0, tone: 'pending' },
    { label: '已上架', value: counts.PUBLISHED ?? 0, tone: 'approved' },
    { label: '已退回', value: counts.DRAFT ?? 0, tone: 'return' },
    { label: '已下架', value: counts.CLOSED ?? 0, tone: 'danger' },
  ]
})

// 单组检查的现场核验进度（清晰度 / 合规性分别统计，避免计数与显示项数不一致）。
function progressOf(checks) {
  return { reviewed: checks.filter((check) => check.verdict).length, total: checks.length }
}

// 设定单项核验结论；再次点击同一结论则取消，回到待核验。
function setVerdict(check, verdict) {
  check.verdict = check.verdict === verdict ? null : verdict
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
    await nextTick()
    reasonField.value?.focus()
    return
  }
  if (trimmed.length > REASON_MAX) {
    reasonError.value = `审核原因不能超过 ${REASON_MAX} 字。`
    return
  }
  reasonError.value = ''

  submitting.value = true
  try {
    const payload = {
      decision,
      reason: trimmed,
      visibleToPublisher: visibleToPublisher.value,
    }
    const response = await adminApi.submitAdminReview(application.questId, payload)
    const reviewed = response?.data

    application.questStatus = reviewed?.questStatus ?? meta.nextStatus
    application.reviewRecords = [
      ...application.reviewRecords,
      {
        decision,
        adminName: ADMIN_NAME,
        reviewedAt: reviewed?.reviewedAt ? formatDateTime(reviewed.reviewedAt) : formatNow(),
        reason: trimmed,
        visibleToPublisher: visibleToPublisher.value,
      },
    ]

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
              <em>{{ statusOf(application).label }}</em>
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
            <article class="admin-ledger-card issue-card">
              <p class="kicker">Issue</p>
              <h3>Issue 关联</h3>
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
                  <dt>难度 · 奖励</dt>
                  <dd>{{ activeApplication.difficulty }} · {{ activeApplication.reward }}</dd>
                </div>
              </dl>
            </article>

            <article class="admin-ledger-card check-card">
              <div class="admin-card-title">
                <div>
                  <p class="kicker">Clarity Check</p>
                  <h3>清晰度检查</h3>
                </div>
                <span>已核验 {{ progressOf(activeApplication.clarityChecks).reviewed }} / {{ activeApplication.clarityChecks.length }}</span>
              </div>
              <div class="admin-check-list">
                <section
                  v-for="check in activeApplication.clarityChecks"
                  :key="check.label"
                  class="admin-check-row"
                  :class="check.verdict === 'pass' ? 'passed' : check.verdict === 'fail' ? 'failed' : 'pending'"
                >
                  <div class="admin-check-head">
                    <strong>{{ check.label }}</strong>
                    <div class="admin-check-verdict" role="group" :aria-label="`${check.label} 审核结论`">
                      <button
                        type="button"
                        :class="{ on: check.verdict === 'pass' }"
                        @click="setVerdict(check, 'pass')"
                      >
                        通过
                      </button>
                      <button
                        type="button"
                        class="fail"
                        :class="{ on: check.verdict === 'fail' }"
                        @click="setVerdict(check, 'fail')"
                      >
                        存疑
                      </button>
                    </div>
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
                <span>已核验 {{ progressOf(activeApplication.complianceChecks).reviewed }} / {{ activeApplication.complianceChecks.length }}</span>
              </div>
              <div class="admin-check-list">
                <section
                  v-for="check in activeApplication.complianceChecks"
                  :key="check.label"
                  class="admin-check-row"
                  :class="check.verdict === 'pass' ? 'passed' : check.verdict === 'fail' ? 'failed' : 'pending'"
                >
                  <div class="admin-check-head">
                    <strong>{{ check.label }}</strong>
                    <div class="admin-check-verdict" role="group" :aria-label="`${check.label} 审核结论`">
                      <button
                        type="button"
                        :class="{ on: check.verdict === 'pass' }"
                        @click="setVerdict(check, 'pass')"
                      >
                        通过
                      </button>
                      <button
                        type="button"
                        class="fail"
                        :class="{ on: check.verdict === 'fail' }"
                        @click="setVerdict(check, 'fail')"
                      >
                        存疑
                      </button>
                    </div>
                  </div>
                  <p>{{ check.note }}</p>
                </section>
              </div>
            </article>

            <article class="admin-ledger-card standards-card">
              <p class="kicker">Acceptance</p>
              <h3>完成标准</h3>
              <ol v-if="activeApplication.completionStandards.length > 0">
                <li v-for="standard in activeApplication.completionStandards" :key="standard">
                  {{ standard }}
                </li>
              </ol>
              <p v-else class="admin-empty-note">维护者尚未提供可验收完成标准，建议退回补充。</p>
            </article>

            <!-- 审核记录（AdminReviewRecord） -->
            <article class="admin-ledger-card timeline-card">
              <p class="kicker">Audit Trail</p>
              <h3>审核记录</h3>
              <ol class="admin-timeline">
                <li
                  v-for="(record, index) in activeApplication.reviewRecords"
                  :key="index"
                  :class="recordTone(record.decision)"
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
