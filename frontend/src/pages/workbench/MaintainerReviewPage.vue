<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import workbenchImg from '../../assets/workbench.png'
import MaintainerReviewActions from '../../components/MaintainerReviewActions.vue'
import MaintainerReviewDetail from '../../components/MaintainerReviewDetail.vue'
import MaintainerReviewQueue from '../../components/MaintainerReviewQueue.vue'
import { repositoryApi, reviewApi, submissionApi } from '../../api'
import { maintainerReviewQueue } from '../../data/maintainerReview'

const router = useRouter()

const reviews = ref(maintainerReviewQueue.map((review) => ({ ...review })))
const selectedReviewId = ref('')
const reviewResult = ref(null)
const isSubmittingReview = ref(false)
const isLoadingReviewQueue = ref(false)
const isActionPanelCollapsed = ref(false)

const selectedReview = computed(
  () => reviews.value.find((review) => review.id === selectedReviewId.value) ?? null,
)
const canReviewSelectedSubmission = computed(() => selectedReview.value?.status === '待审核')
const pendingReviewCount = computed(() => reviews.value.filter((review) => review.status === '待审核').length)
const reviewedCount = computed(() => reviews.value.length - pendingReviewCount.value)
const returnedCount = computed(() => reviews.value.filter((review) => review.status.includes('修改')).length)
const reviewStats = computed(() => [
  { label: '待审核提交', value: pendingReviewCount.value, hint: '需要委托人给出结论' },
  { label: '已审核提交', value: reviewedCount.value, hint: '已给出通过、退回或驳回' },
  { label: '需退回修改', value: returnedCount.value, hint: '存在未通过检查项' },
  { label: '队列总数', value: reviews.value.length, hint: '当前演示提交记录' },
])

function formatSubmittedAt(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getMonth() + 1}-${date.getDate()} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function submittedAtOrder(value) {
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : 0
}

function completionCriteriaItems(text = '') {
  const items = String(text)
    .split(/\r?\n|；|;/)
    .map((item) => item.trim())
    .filter(Boolean)
  const criteria = items.length > 0 ? items : ['完成标准已由任务描述确认']
  return criteria.map((checkpoint) => ({
    checkpoint,
    passed: true,
    comment: '待维护者确认。',
  }))
}

function normalizeLiveReview(item) {
  const questId = item.quest?.questId
  const externalPrId = item.pullRequest?.externalPrId ?? item.pullRequest?.pullRequestId
  return {
    id: `SUB-${item.submissionId}`,
    live: true,
    submissionId: item.submissionId,
    repositoryId: item.repository?.repositoryId,
    pullRequestId: item.pullRequest?.pullRequestId,
    pullRequestUrl: item.pullRequest?.externalUrl,
    questId: `QST-${String(questId).padStart(4, '0')}`,
    questTitle: item.quest?.title ?? '未命名任务',
    summary: item.description ?? '冒险家已提交成果，等待审核。',
    submitter: item.submitter?.username ?? 'adventurer',
    rewardXp: 0,
    status: item.status === 'PENDING_REVIEW' ? '待审核' : item.status,
    statusTone: item.status === 'PENDING_REVIEW' ? 'review' : 'approved',
    submittedAt: formatSubmittedAt(item.submittedAt),
    submittedAtOrder: submittedAtOrder(item.submittedAt),
    repository: item.repository?.name ?? '未绑定仓库',
    pullRequest: `PR #${externalPrId}`,
    pullRequestTitle: item.pullRequest?.title ?? `QST-${questId} 提交成果`,
    prState: item.pullRequest?.status ?? 'OPEN',
    branch: item.pullRequest?.sourceBranch ?? 'task branch',
    latestCommit: '已由工作台提交',
    completionCriteria: completionCriteriaItems(item.completionCriteria),
    evidence: [item.pullRequest?.externalUrl].filter(Boolean),
    suggestedSummary: 'PR 已确认，完成标准通过。',
  }
}

async function loadReviewQueue() {
  isLoadingReviewQueue.value = true
  try {
    const payload = await submissionApi.list({ status: 'PENDING_REVIEW' })
    const items = Array.isArray(payload?.data) ? payload.data : []
    if (items.length > 0) {
      reviews.value = items.map(normalizeLiveReview)
      selectedReviewId.value = reviews.value[0]?.id ?? ''
    }
  } catch (error) {
    reviewResult.value = {
      tone: 'warning',
      title: '真实审核队列暂不可用',
      body: error?.message ?? '当前保留演示队列，可稍后刷新重试。',
    }
  } finally {
    isLoadingReviewQueue.value = false
  }
}

onMounted(() => {
  loadReviewQueue()
})

function backToWorkbench() {
  router.push({ name: 'maintainer-workbench' })
}

function selectReview(reviewId) {
  selectedReviewId.value = reviewId
  reviewResult.value = null
  isActionPanelCollapsed.value = false
}

function updateReviewStatus(decision) {
  if (!selectedReview.value) return

  const nextStatus =
    decision === 'APPROVED' ? '审核通过' : decision === 'REJECTED' ? '已驳回' : '已请求修改'
  const nextTone = decision === 'APPROVED' ? 'approved' : 'warning'

  reviews.value = reviews.value.map((review) =>
    review.id === selectedReview.value.id ? { ...review, status: nextStatus, statusTone: nextTone } : review,
  )
}

async function submitReview(payload) {
  if (!selectedReview.value) return

  const submissionId = selectedReview.value.submissionId
  isSubmittingReview.value = true
  try {
    if (
      payload.decision === 'APPROVED' &&
      selectedReview.value.live &&
      selectedReview.value.repositoryId &&
      selectedReview.value.pullRequestId &&
      selectedReview.value.prState !== 'MERGED'
    ) {
      const mergePayload = await repositoryApi.mergePullRequest(
        selectedReview.value.repositoryId,
        selectedReview.value.pullRequestId,
      )
      selectedReview.value.prState = mergePayload?.data?.status ?? 'MERGED'
    }
    await reviewApi.reviewSubmission(submissionId, payload)
    updateReviewStatus(payload.decision)
    reviewResult.value = {
      tone: payload.decision === 'APPROVED' ? 'approved' : 'warning',
      title:
        payload.decision === 'APPROVED'
          ? '审核已通过'
          : payload.decision === 'REJECTED'
            ? '提交已驳回'
            : '修改请求已发送',
      body: `${selectedReview.value.questId} 的审核结论已通过后端接口记录为 ${payload.decision}，submissionId=${submissionId}。`,
    }
  } catch (error) {
    reviewResult.value = {
      tone: 'warning',
      title: '审核提交失败',
      body: error.message || `维护者审核接口提交失败，请确认 submissionId=${submissionId} 存在且当前账号有审核权限。`,
    }
  } finally {
    isSubmittingReview.value = false
  }
}

function saveDraft(payload) {
  reviewResult.value = {
    tone: 'review',
    title: '审核草稿已保存',
    body: `${selectedReview.value.questId} 的 ${payload.decision} 草稿已保存在当前页面，刷新前可继续调整。`,
  }
}

function openPullRequest(review) {
  if (review.pullRequestUrl) {
    window.open(review.pullRequestUrl, '_blank', 'noopener,noreferrer')
    reviewResult.value = {
      tone: 'review',
      title: `${review.pullRequest} 已打开`,
      body: review.pullRequestUrl,
    }
    return
  }

  reviewResult.value = {
    tone: 'review',
    title: `${review.pullRequest} 状态已定位`,
    body: `${review.pullRequestTitle} 位于 ${review.branch}，最新 commit 为 ${review.latestCommit}。课堂演示版暂不跳转 Gitea。`,
  }
}
</script>

<template>
  <main class="app-shell">
    <section
      class="scene work-scene maintainer-review-scene"
      :style="{ backgroundImage: `url(${workbenchImg})` }"
    >
      <button class="back-orb" type="button" aria-label="返回委托人工作台" @click="backToWorkbench">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回委托人工作台</span>
      </button>

      <div class="maintainer-review-shell">
        <header class="maintainer-review-header">
          <div>
            <p class="kicker">Maintainer Review Desk</p>
            <h1>委托人审核台</h1>
            <p>查看待审核成果、确认 PR 状态和完成标准，并录入维护者审核意见。</p>
          </div>
          <div class="maintainer-review-stats" aria-label="维护者审核统计">
            <article v-for="stat in reviewStats" :key="stat.label">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <small>{{ stat.hint }}</small>
            </article>
          </div>
        </header>

        <div class="maintainer-review-workspace">
          <MaintainerReviewQueue
            :reviews="reviews"
            :selected-review-id="selectedReviewId"
            @select="selectReview"
          />

          <section v-if="!selectedReview" class="review-empty-state" aria-label="待选择提交">
            <div>
              <p class="kicker">Waiting For Selection</p>
              <h2>{{ pendingReviewCount }} 份待审核委托</h2>
              <p>请点击左侧委托提交审核。</p>
            </div>
          </section>

          <section
            v-else
            class="review-focus"
            :class="{
              'readonly-review': !canReviewSelectedSubmission,
              'actions-collapsed': canReviewSelectedSubmission && isActionPanelCollapsed,
            }"
          >
            <MaintainerReviewDetail :review="selectedReview" />
            <button
              v-if="canReviewSelectedSubmission"
              class="action-panel-toggle"
              type="button"
              :aria-expanded="!isActionPanelCollapsed"
              @click="isActionPanelCollapsed = !isActionPanelCollapsed"
            >
              {{ isActionPanelCollapsed ? '展开审核操作' : '收起审核操作' }}
            </button>
            <div v-if="canReviewSelectedSubmission" class="review-action-slot">
              <MaintainerReviewActions
                v-if="!isActionPanelCollapsed"
                :review="selectedReview"
                :result="reviewResult"
                :busy="isSubmittingReview"
                @submit-review="submitReview"
                @save-draft="saveDraft"
                @open-pr="openPullRequest"
              />
            </div>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.maintainer-review-scene::before {
  background:
    radial-gradient(circle at 70% 20%, rgba(242, 190, 88, 0.16), transparent 0 28%, transparent 58%),
    linear-gradient(90deg, rgba(7, 3, 1, 0.76), transparent 32%, transparent 72%, rgba(7, 3, 1, 0.76));
}

.maintainer-review-shell {
  position: absolute;
  inset: 76px 28px 28px;
  z-index: 2;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 16px;
}

.maintainer-review-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.9fr);
  gap: 18px;
  align-items: stretch;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 12px;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.72), rgba(11, 6, 3, 0.56)),
    radial-gradient(circle at 95% 12%, rgba(255, 217, 138, 0.15), transparent 0 30%);
  box-shadow: 0 22px 64px rgba(0, 0, 0, 0.36), inset 0 1px 0 rgba(255, 235, 180, 0.14);
}

.maintainer-review-header h1 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: clamp(2.1rem, 5vw, 4.2rem);
  line-height: 0.94;
}

.maintainer-review-header p:last-child {
  max-width: 68ch;
  margin: 10px 0 0;
  color: rgba(255, 231, 183, 0.74);
  line-height: 1.55;
  text-wrap: pretty;
}

.maintainer-review-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.maintainer-review-stats article {
  display: grid;
  align-content: center;
  gap: 5px;
  border: 1px solid rgba(255, 222, 161, 0.18);
  border-radius: 10px;
  padding: 12px;
  background: rgba(7, 4, 2, 0.32);
}

.maintainer-review-stats span,
.maintainer-review-stats small {
  color: rgba(255, 231, 183, 0.62);
  line-height: 1.35;
}

.maintainer-review-stats strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.75rem;
}

.maintainer-review-workspace {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 16px;
  min-height: 0;
}

.review-empty-state,
.review-focus {
  min-width: 0;
  min-height: 0;
}

.review-empty-state {
  display: grid;
  place-items: center;
  border: 1px solid rgba(238, 184, 91, 0.34);
  border-radius: 14px;
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(34, 18, 8, 0.72), rgba(7, 4, 2, 0.56)),
    radial-gradient(circle at 50% 34%, rgba(255, 219, 145, 0.14), transparent 0 34%);
  box-shadow: 0 22px 58px rgba(0, 0, 0, 0.34), inset 0 1px 0 rgba(255, 235, 180, 0.12);
  text-align: center;
}

.review-empty-state h2 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: clamp(2.4rem, 5vw, 4.8rem);
  line-height: 0.96;
}

.review-empty-state p:last-child {
  margin: 14px 0 0;
  color: rgba(255, 231, 183, 0.72);
  font-size: 1.06rem;
}

.review-focus {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 48px minmax(300px, 0.85fr);
  gap: 12px;
}

.review-focus.readonly-review {
  grid-template-columns: minmax(0, 1fr);
}

.review-focus.actions-collapsed {
  grid-template-columns: minmax(0, 1fr) 48px;
}

.review-focus.actions-collapsed .review-action-slot {
  display: none;
}

.review-action-slot {
  display: grid;
  min-width: 0;
  min-height: 0;
}

.action-panel-toggle {
  align-self: stretch;
  border: 1px solid rgba(238, 184, 91, 0.38);
  border-radius: 12px;
  padding: 12px 9px;
  color: #ffe2a0;
  background:
    linear-gradient(135deg, rgba(83, 45, 16, 0.76), rgba(14, 7, 3, 0.66)),
    radial-gradient(circle at 22% 20%, rgba(255, 218, 142, 0.16), transparent 0 42%);
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255, 235, 180, 0.1);
  cursor: pointer;
  writing-mode: vertical-rl;
  letter-spacing: 0.08em;
  font-weight: 900;
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}

.action-panel-toggle:hover,
.action-panel-toggle:focus-visible {
  border-color: rgba(255, 224, 157, 0.76);
  transform: translateY(-1px);
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.34), inset 0 1px 0 rgba(255, 235, 180, 0.14);
}

@media (max-width: 1180px) {
  .maintainer-review-shell {
    position: relative;
    inset: auto;
    width: min(100%, calc(100vw - 32px));
    min-height: 100svh;
    margin: 0 auto;
    padding: 96px 0 28px;
  }

  .maintainer-review-header,
  .maintainer-review-workspace,
  .review-focus {
    grid-template-columns: 1fr;
  }

  .maintainer-review-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .work-scene {
    overflow: auto;
  }
}

@media (max-width: 640px) {
  .maintainer-review-stats {
    grid-template-columns: 1fr;
  }
}
</style>
