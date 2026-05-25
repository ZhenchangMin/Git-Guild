<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import operationRoomImg from '../../assets/operation room.png'
import { adminExceptionCases, adminQuestApplications } from '../../data/admin'
import { clearSession } from '../../stores/sessionStore'

const router = useRouter()
const adminApplications = ref(adminQuestApplications.map((application) => ({ ...application })))
const adminExceptions = ref(adminExceptionCases.map((exception) => ({ ...exception })))
const activeAdminApplicationId = ref(adminQuestApplications[0]?.id ?? null)
const adminActionResult = ref({
  title: '等待管理员审核',
  body: '选择维护者提交的任务发布申请，检查清晰度、合规性、Issue 关联和完成标准后再决定是否上架。',
})

const activeAdminApplication = computed(
  () =>
    adminApplications.value.find((application) => application.id === activeAdminApplicationId.value) ??
    adminApplications.value[0],
)

const adminQueueStats = computed(() => {
  const counts = adminApplications.value.reduce(
    (summary, application) => {
      if (application.status === '已通过上架') summary.approved += 1
      else if (application.status === '退回补充') summary.returned += 1
      else if (application.status === '标记异常') summary.flagged += 1
      else summary.pending += 1
      return summary
    },
    { pending: 0, approved: 0, returned: 0, flagged: 0 },
  )
  return [
    { label: '待处理', value: counts.pending },
    { label: '已上架', value: counts.approved },
    { label: '退回补充', value: counts.returned },
    { label: '异常', value: counts.flagged },
  ]
})

const adminExceptionStats = computed(() => {
  const pending = adminExceptions.value.filter((exception) => exception.status === '待处理').length
  const review = adminExceptions.value.filter((exception) => exception.status === '需复核').length
  return { pending, review }
})

const activeAdminPassedChecks = computed(() => {
  const application = activeAdminApplication.value
  if (!application) return { passed: 0, total: 0 }
  const checks = [...application.clarityChecks, ...application.complianceChecks]
  return {
    passed: checks.filter((check) => check.passed).length,
    total: checks.length,
  }
})

function selectAdminApplication(application) {
  activeAdminApplicationId.value = application.id
  adminActionResult.value = {
    title: `${application.id} 已调阅`,
    body: `正在审核 ${application.questId} 的任务发布申请。这里检查的是上架申请，不是冒险家成果提交。`,
  }
}

function runAdminApplicationAction(action) {
  const application = activeAdminApplication.value
  if (!application) return

  const statusMap = {
    approve: {
      status: '已通过上架',
      statusTone: 'approved',
      title: '通过上架',
      body: `${application.questId} 已通过管理员审核，可进入悬赏任务板供冒险家接取。`,
    },
    return: {
      status: '退回补充',
      statusTone: 'return',
      title: '已退回维护者补充',
      body: `${application.questId} 已退回维护者，需要补齐完成标准、奖励或边界说明后重新提交管理员审核。`,
    },
    flag: {
      status: '标记异常',
      statusTone: 'danger',
      title: '已标记异常',
      body: `${application.questId} 已进入异常处理队列，上架前需要管理员确认合规风险和 Issue 关联。`,
    },
  }
  const result = statusMap[action]
  if (!result) return

  application.status = result.status
  application.statusTone = result.statusTone
  adminActionResult.value = {
    title: result.title,
    body: result.body,
  }
}

function resolveAdminException(exception) {
  exception.status = exception.resultStatus
  exception.statusTone = exception.resultTone
  adminActionResult.value = {
    title: `${exception.type}：${exception.status}`,
    body: exception.resultMessage,
  }
}

function switchAccount() {
  clearSession()
  router.push({ name: 'login' })
}

function logout() {
  clearSession()
  router.push({ name: 'login' })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene" :style="{ backgroundImage: `url(${operationRoomImg})` }">
      <div class="session-action-stack" aria-label="账号操作">
        <button class="back-orb" type="button" aria-label="切换账号" @click="switchAccount">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M15 6 9 12l6 6" />
          </svg>
          <span>切换账号</span>
        </button>
        <button class="back-orb logout-orb" type="button" aria-label="退出登录" @click="logout">
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
            <span>发布申请</span>
          </div>

          <dl class="admin-stat-grid">
            <div v-for="stat in adminQueueStats" :key="stat.label">
              <dt>{{ stat.label }}</dt>
              <dd>{{ stat.value }}</dd>
            </div>
          </dl>

          <div class="admin-application-list">
            <button
              v-for="application in adminApplications"
              :key="application.id"
              class="admin-application-card"
              :class="[{ active: activeAdminApplication?.id === application.id }, application.statusTone]"
              type="button"
              @click="selectAdminApplication(application)"
            >
              <span>{{ application.id }} · {{ application.submittedAt }}</span>
              <strong>{{ application.questId }} · {{ application.title }}</strong>
              <small>{{ application.maintainer }}</small>
              <em>{{ application.status }}</em>
            </button>
          </div>
        </aside>

        <section v-if="activeAdminApplication" class="admin-detail-panel" aria-live="polite">
          <header class="admin-detail-hero">
            <div>
              <p class="kicker">Quest Publishing Application</p>
              <h2>{{ activeAdminApplication.questId }} · {{ activeAdminApplication.title }}</h2>
              <p>{{ activeAdminApplication.summary }}</p>
            </div>
            <span class="admin-status-seal" :class="activeAdminApplication.statusTone">
              {{ activeAdminApplication.status }}
            </span>
          </header>

          <div class="admin-detail-grid">
            <article class="admin-ledger-card issue-card">
              <p class="kicker">Issue 关联</p>
              <h3>Issue 关联</h3>
              <dl>
                <div>
                  <dt>维护者</dt>
                  <dd>{{ activeAdminApplication.maintainer }}</dd>
                </div>
                <div>
                  <dt>仓库</dt>
                  <dd>{{ activeAdminApplication.repository }}</dd>
                </div>
                <div>
                  <dt>Issue</dt>
                  <dd>{{ activeAdminApplication.issue }}</dd>
                </div>
                <div>
                  <dt>难度</dt>
                  <dd>{{ activeAdminApplication.difficulty }}</dd>
                </div>
              </dl>
            </article>

            <article class="admin-ledger-card check-card">
              <div class="admin-card-title">
                <div>
                  <p class="kicker">Clarity Check</p>
                  <h3>清晰度检查</h3>
                </div>
                <span>{{ activeAdminPassedChecks.passed }} / {{ activeAdminPassedChecks.total }}</span>
              </div>
              <div class="admin-check-list">
                <section
                  v-for="check in activeAdminApplication.clarityChecks"
                  :key="check.label"
                  class="admin-check-row"
                  :class="{ passed: check.passed, failed: !check.passed }"
                >
                  <strong>{{ check.label }}</strong>
                  <span>{{ check.passed ? '通过' : '待补' }}</span>
                  <p>{{ check.note }}</p>
                </section>
              </div>
            </article>

            <article class="admin-ledger-card check-card">
              <p class="kicker">Compliance</p>
              <h3>合规性检查</h3>
              <div class="admin-check-list">
                <section
                  v-for="check in activeAdminApplication.complianceChecks"
                  :key="check.label"
                  class="admin-check-row"
                  :class="{ passed: check.passed, failed: !check.passed }"
                >
                  <strong>{{ check.label }}</strong>
                  <span>{{ check.passed ? '通过' : '风险' }}</span>
                  <p>{{ check.note }}</p>
                </section>
              </div>
            </article>

            <article class="admin-ledger-card standards-card">
              <p class="kicker">Acceptance</p>
              <h3>完成标准</h3>
              <ol v-if="activeAdminApplication.completionStandards.length > 0">
                <li v-for="standard in activeAdminApplication.completionStandards" :key="standard">{{ standard }}</li>
              </ol>
              <p v-else class="admin-empty-note">维护者尚未提供可验收完成标准，建议退回补充。</p>
            </article>
          </div>
        </section>

        <aside v-if="activeAdminApplication" class="admin-action-panel">
          <section class="glass-ledger admin-risk-ledger">
            <p class="kicker">Risk Notice</p>
            <h2>风险提示</h2>
            <ul>
              <li v-for="risk in activeAdminApplication.risks" :key="risk">
                <span>{{ risk }}</span>
              </li>
            </ul>
          </section>

          <section class="glass-ledger admin-exception-ledger">
            <div class="admin-card-title">
              <div>
                <p class="kicker">Exception Docket</p>
                <h2>异常处理演示</h2>
              </div>
              <span>{{ adminExceptionStats.pending }} 待处理 · {{ adminExceptionStats.review }} 复核</span>
            </div>

            <div class="admin-exception-list">
              <article
                v-for="exception in adminExceptions"
                :key="exception.id"
                class="admin-exception-card"
                :class="exception.statusTone"
              >
                <div class="exception-card-head">
                  <span>{{ exception.id }} · {{ exception.type }}</span>
                  <em>{{ exception.status }}</em>
                </div>
                <h3>{{ exception.title }}</h3>
                <dl>
                  <div>
                    <dt>原因</dt>
                    <dd>{{ exception.reason }}</dd>
                  </div>
                  <div>
                    <dt>影响</dt>
                    <dd>{{ exception.impact }}</dd>
                  </div>
                </dl>
                <button
                  class="quiet-action"
                  type="button"
                  :disabled="exception.status !== '待处理'"
                  @click="resolveAdminException(exception)"
                >
                  {{ exception.status === '待处理' ? exception.actionLabel : '处理完成' }}
                </button>
              </article>
            </div>
          </section>

          <section class="parchment-panel admin-decision-card">
            <p class="kicker">审核决定</p>
            <h2>管理员操作</h2>
            <p>通过后进入悬赏任务板；退回或异常都不会上架。</p>
            <div class="admin-action-buttons">
              <button class="primary-action" type="button" @click="runAdminApplicationAction('approve')">
                通过上架
              </button>
              <button class="quiet-action" type="button" @click="runAdminApplicationAction('return')">
                退回补充
              </button>
              <button class="quiet-action danger" type="button" @click="runAdminApplicationAction('flag')">
                标记异常
              </button>
            </div>
          </section>

          <section class="glass-ledger admin-result-ledger">
            <p class="kicker">操作结果</p>
            <h2>{{ adminActionResult.title }}</h2>
            <p>{{ adminActionResult.body }}</p>
          </section>
        </aside>
      </div>
    </section>
  </main>
</template>
