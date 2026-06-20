<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import workbenchImg from '../../assets/workbench.webp'
import HomeOrb from '../../components/HomeOrb.vue'
import MaintainerReviewActions from '../../components/MaintainerReviewActions.vue'
import MaintainerReviewDetail from '../../components/MaintainerReviewDetail.vue'
import MaintainerReviewQueue from '../../components/MaintainerReviewQueue.vue'
import { reviewApi, submissionApi } from '../../api'
import { toBrowsableGiteaUrl } from '../../utils/giteaUrl'

const router = useRouter()

const reviews = ref([])
const selectedReviewId = ref('')
const reviewResult = ref(null)
const reviewAlert = ref(null)
const reviewAlertRef = ref(null)
const loadError = ref('')
const isLoadingReviews = ref(false)
const isSubmittingReview = ref(false)
const isMerging = ref(false)

const selectedReview = computed(
  () => reviews.value.find((review) => review.id === selectedReviewId.value) ?? null,
)
const canReviewSelectedSubmission = computed(() => selectedReview.value?.status === '待审核')
const pendingReviewCount = computed(() => reviews.value.filter((review) => review.status === '待审核').length)
const reviewedCount = computed(() => reviews.value.length - pendingReviewCount.value)
const returnedCount = computed(() => reviews.value.filter((review) => review.status.includes('修改')).length)
const reviewStats = computed(() => [
  { label: '待审核提交', value: pendingReviewCount.value },
  { label: '已审核提交', value: reviewedCount.value },
  { label: '需退回修改', value: returnedCount.value },
  { label: '队列总数', value: reviews.value.length },
])

function padId(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return '????'
  return String(numeric).padStart(4, '0')
}

function formatDateTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function submittedAtOrder(value) {
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

const PR_STATUS_LABELS = {
  OPEN: '待合并',
  MERGED: '已合并',
  CLOSED: '已关闭',
  DRAFT: '草稿',
  UNKNOWN: '未知',
}

function prStatusLabel(status) {
  return PR_STATUS_LABELS[status] ?? status ?? '未知'
}

function mapSubmissionStatus(status) {
  return {
    PENDING_REVIEW: { label: '待审核', tone: 'review' },
    APPROVED: { label: '审核通过', tone: 'approved' },
    CHANGES_REQUESTED: { label: '已请求修改', tone: 'warning' },
    REJECTED: { label: '已驳回', tone: 'danger' },
  }[status] ?? { label: status || '未知状态', tone: 'review' }
}

function splitCompletionCriteria(criteria) {
  return String(criteria || '')
    .split(/\r?\n|[;；]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function buildCompletionCriteria(submission) {
  const prStatus = submission.pullRequest?.status || 'UNKNOWN'
  const base = [
    {
      checkpoint: 'PR 已关联到当前任务仓库',
      passed: Boolean(submission.pullRequest?.pullRequestId),
      comment: submission.pullRequest?.externalPrId
        ? `已关联 PR #${submission.pullRequest.externalPrId}。`
        : '提交记录缺少 PR 关联。',
    },
    {
      checkpoint: 'PR 合并状态',
      passed: prStatus === 'MERGED',
      comment: prStatus === 'MERGED'
        ? 'PR 已合并。'
        : `当前 PR 状态为「${prStatusLabel(prStatus)}」。「接受提交」只完成任务并发放 XP，不会自动合并；如需合并请点「合并 PR」按钮，或在 Gitea 手动合并。`,
    },
    {
      checkpoint: '成果说明已提交',
      passed: Boolean(submission.description?.trim()),
      comment: submission.description || '未填写成果说明。',
    },
  ]

  const taskCriteria = splitCompletionCriteria(submission.completionCriteria).map((checkpoint) => ({
    checkpoint,
    passed: true,
    comment: '请结合 PR 变更和提交说明人工确认。',
  }))

  return [...base, ...taskCriteria]
}

function mapSubmissionToReview(submission) {
  const status = mapSubmissionStatus(submission.status)
  const quest = submission.quest ?? {}
  const repository = submission.repository ?? {}
  const pullRequest = submission.pullRequest ?? {}
  const externalPrId = pullRequest.externalPrId ? String(pullRequest.externalPrId) : ''
  const prLabel = externalPrId ? `PR #${externalPrId}` : `PR ${pullRequest.pullRequestId ?? '—'}`
  const branch =
    pullRequest.sourceBranch && pullRequest.targetBranch
      ? `${pullRequest.sourceBranch} -> ${pullRequest.targetBranch}`
      : pullRequest.sourceBranch || repository.defaultBranch || '—'
  const completionCriteria = buildCompletionCriteria(submission)
  const repositoryBranchName = pullRequest.sourceBranch || repository.defaultBranch || 'main'
  const repositoryUrl = toBrowsableGiteaUrl(repository.sourceUrl) || null
  // 分支源码视图：跳到该分支下的文件树，而不是仓库默认分支首页。
  const repositoryBranchUrl = repositoryUrl
    ? `${repositoryUrl}/src/branch/${encodeURIComponent(repositoryBranchName).replace(/%2F/g, '/')}/`
    : null
  // PR 详情页：优先用后端回传的真实 PR 链接（同样要把内网回环地址换成可浏览地址），
  // 缺失时按仓库地址 + externalPrId 兜底拼接。
  const pullRequestUrl =
    toBrowsableGiteaUrl(pullRequest.externalUrl) ||
    (repositoryUrl && externalPrId ? `${repositoryUrl}/pulls/${externalPrId}` : '')

  return {
    id: `SUB-${padId(submission.submissionId)}`,
    submissionId: submission.submissionId,
    questId: `QST-${padId(quest.questId)}`,
    questTitle: quest.title || '未命名委托',
    submitter: submission.submitter?.username || `用户 ${submission.submitter?.userId ?? '—'}`,
    repository: repository.name || repository.sourceUrl || '未关联仓库',
    repositoryBranch: repositoryBranchName,
    repositoryBranchUrl,
    pullRequest: prLabel,
    pullRequestUrl,
    pullRequestTitle: pullRequest.title || '未命名 PR',
    prState: pullRequest.status || 'UNKNOWN',
    branch,
    latestCommit: '请打开 PR 查看',
    status: status.label,
    statusTone: status.tone,
    submittedAt: formatDateTime(submission.submittedAt),
    submittedAtOrder: submittedAtOrder(submission.submittedAt),
    rewardXp: submission.rewardXp ?? 0,
    summary: submission.description || '该提交没有填写成果说明。',
    completionCriteria,
    // 佐证材料：只放真正的外部佐证链接（成果说明已单独展示，不再重复塞进来）。
    evidence: [
      pullRequestUrl ? `PR 链接：${pullRequestUrl}` : '',
    ].filter(Boolean),
    // 冒险家上传的佐证文件（{name, mimeType, content=base64 dataURL}），供委托人在审核台预览/下载。
    evidenceFiles: Array.isArray(submission.evidence) ? submission.evidence : [],
    suggestedSummary: completionCriteria.every((item) => item.passed)
      ? 'PR 与成果说明已核对，符合通过条件。'
      : 'PR 尚未满足全部通过条件，请根据未通过项给出修改意见。',
  }
}

async function loadReviewQueue() {
  isLoadingReviews.value = true
  loadError.value = ''
  try {
    const response = await submissionApi.reviewQueue()
    const items = Array.isArray(response?.data) ? response.data : []
    reviews.value = items.map(mapSubmissionToReview)
    if (!reviews.value.some((review) => review.id === selectedReviewId.value)) {
      selectedReviewId.value =
        reviews.value.find((review) => review.status === '待审核')?.id ||
        reviews.value[0]?.id ||
        ''
    }
  } catch (error) {
    reviews.value = []
    selectedReviewId.value = ''
    loadError.value = error.message || '审核队列加载失败，请确认当前账号有维护者权限。'
  } finally {
    isLoadingReviews.value = false
  }
}

// 返回上一页：有站内历史就弹出（通常是事务所），避免用 push 反复压栈造成
// review ↔ maintainer 历史来回 ping-pong 死循环；无历史（深链）时兜底回事务所。
function backToWorkbench() {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push({ name: 'maintainer-workbench' })
  }
}

function selectReview(reviewId) {
  selectedReviewId.value = reviewId
  reviewResult.value = null
  reviewAlert.value = null
}

function updateReviewStatus(decision) {
  if (!selectedReview.value) return

  const nextStatus =
    decision === 'APPROVED' ? '审核通过' : decision === 'REJECTED' ? '已驳回' : '已请求修改'
  const nextTone = decision === 'APPROVED' ? 'approved' : decision === 'REJECTED' ? 'danger' : 'warning'

  reviews.value = reviews.value.map((review) =>
    review.id === selectedReview.value.id ? { ...review, status: nextStatus, statusTone: nextTone } : review,
  )
}

async function submitReview(payload) {
  if (!selectedReview.value) return

  const submissionId = selectedReview.value.submissionId
  const currentReview = selectedReview.value
  isSubmittingReview.value = true
  reviewAlert.value = null
  try {
    await reviewApi.reviewSubmission(submissionId, payload)
    updateReviewStatus(payload.decision)
    await loadReviewQueue()
    const successTitle =
      payload.decision === 'APPROVED'
        ? '审核已通过'
        : payload.decision === 'REJECTED'
          ? '提交已驳回'
          : '修改请求已发送'
    const successBody =
      payload.decision === 'APPROVED'
        ? `「${currentReview.questTitle}」已接受，任务标记完成并发放 ${currentReview.rewardXp} XP。PR 未自动合并，如需合并请点「合并 PR」。`
        : payload.decision === 'REJECTED'
          ? `已驳回「${currentReview.questTitle}」的提交。`
          : `已将「${currentReview.questTitle}」退回给 ${currentReview.submitter}，等待修改后重新提交。`
    reviewAlert.value = {
      tone: payload.decision === 'APPROVED' ? 'success' : 'warning',
      title: successTitle,
      body: successBody,
    }
    reviewResult.value = {
      tone: payload.decision === 'APPROVED' ? 'approved' : 'warning',
      title: successTitle,
      body: successBody,
    }
  } catch (error) {
    const errorInfo = buildReviewErrorMessage(error, currentReview)
    reviewAlert.value = {
      tone: 'error',
      title: errorInfo.title,
      body: errorInfo.body,
    }
    reviewResult.value = {
      tone: 'warning',
      title: errorInfo.title,
      body: errorInfo.body,
    }
    await nextTick()
    reviewAlertRef.value?.focus()
  } finally {
    isSubmittingReview.value = false
  }
}

async function mergeSelectedPullRequest() {
  if (!selectedReview.value) return
  const review = selectedReview.value
  const submissionId = review.submissionId
  isMerging.value = true
  reviewAlert.value = null
  try {
    await reviewApi.mergePullRequest(submissionId)
    // 本地即时反映合并结果，再以队列刷新对齐后端。
    reviews.value = reviews.value.map((item) =>
      item.id === review.id ? { ...item, prState: 'MERGED' } : item,
    )
    await loadReviewQueue()
    reviewAlert.value = {
      tone: 'success',
      title: 'PR 已合并',
      body: `${review.pullRequest} 已合并到目标分支。`,
    }
  } catch (error) {
    const body =
      error?.code === 'PR_MERGE_CONFLICT'
        ? `${review.pullRequest} 存在冲突，无法自动合并。请先在 Gitea 解决冲突后再试。`
        : `${error?.code ? `${error.code}：` : ''}${error?.message || '合并失败，请稍后再试。'}`
    reviewAlert.value = { tone: 'error', title: 'PR 合并失败', body }
    await nextTick()
    reviewAlertRef.value?.focus()
  } finally {
    isMerging.value = false
  }
}

function buildReviewErrorMessage(error, review) {
  if (error?.code === 'TASK_BRANCH_EMPTY') {
    return {
      title: '任务分支没有改动',
      body: '冒险家的任务分支与基线分支无差异，无法形成 PR。请退回让其推送提交后重新提交。',
    }
  }
  if (error?.code) {
    return {
      title: '审核提交失败',
      body: `${error.code}：${error.message || '后端拒绝了本次审核请求'}。`,
    }
  }
  return {
    title: '审核提交失败',
    body: error?.message || '提交失败，请确认当前账号有审核权限，或稍后重试。',
  }
}

function saveDraft(payload) {
  reviewResult.value = {
    tone: 'review',
    title: '审核草稿已保存',
    body: `「${selectedReview.value.questTitle}」的审核草稿已保存在当前页面，刷新前可继续调整。`,
  }
}

function openPullRequest(review) {
  if (review.pullRequestUrl) {
    window.open(review.pullRequestUrl, '_blank', 'noopener,noreferrer')
    return
  }
  reviewResult.value = {
    tone: 'review',
    title: `${review.pullRequest} 状态已定位`,
    body: `${review.pullRequestTitle} 位于 ${review.branch}，当前提交记录没有可打开的 PR URL。`,
  }
}

onMounted(loadReviewQueue)
</script>

<template>
  <main class="app-shell">
    <section
      class="scene work-scene maintainer-review-scene"
      :style="{ backgroundImage: `url(${workbenchImg})` }"
    >
      <HomeOrb />
      <button class="back-orb" type="button" aria-label="返回委托人工作台" @click="backToWorkbench">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回委托人工作台</span>
      </button>

      <div class="maintainer-review-shell" :class="{ 'has-review-alert': reviewAlert }">
        <header class="maintainer-review-header">
          <div>
            <h1>委托人审核台</h1>
          </div>
          <div class="maintainer-review-stats" aria-label="维护者审核统计">
            <article v-for="stat in reviewStats" :key="stat.label">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
            </article>
          </div>
        </header>

        <section
          v-if="reviewAlert"
          ref="reviewAlertRef"
          class="maintainer-review-alert"
          :class="reviewAlert.tone"
          tabindex="-1"
          role="alert"
          aria-live="assertive"
        >
          <strong>{{ reviewAlert.title }}</strong>
          <p>{{ reviewAlert.body }}</p>
        </section>

        <div class="maintainer-review-workspace">
          <MaintainerReviewQueue
            :reviews="reviews"
            :selected-review-id="selectedReviewId"
            @select="selectReview"
          />

          <section v-if="isLoadingReviews" class="review-empty-state" aria-label="审核队列加载中">
            <div>
              <h2>正在读取审核队列</h2>
              <p>请稍候。</p>
            </div>
          </section>

          <section v-else-if="loadError" class="review-empty-state" aria-label="审核队列加载失败">
            <div>
              <h2>审核队列加载失败</h2>
              <p>{{ loadError }}</p>
            </div>
          </section>

          <section v-else-if="!selectedReview" class="review-empty-state" aria-label="待选择提交">
            <div>
              <h2>{{ pendingReviewCount }} 份待审核提交</h2>
              <p>请点击左侧提交开始审核。</p>
            </div>
          </section>

          <section
            v-else
            class="review-focus"
            :class="{ 'readonly-review': !canReviewSelectedSubmission }"
          >
            <MaintainerReviewDetail
              :review="selectedReview"
              :merging="isMerging"
              @merge-pr="mergeSelectedPullRequest"
            />
            <div v-if="canReviewSelectedSubmission" class="review-action-slot">
              <MaintainerReviewActions
                :review="selectedReview"
                :result="reviewResult"
                :busy="isSubmittingReview"
                @submit-review="submitReview"
                @save-draft="saveDraft"
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

.maintainer-review-shell.has-review-alert {
  grid-template-rows: auto auto minmax(0, 1fr);
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

.maintainer-review-header > div {
  display: grid;
  align-content: center;
  gap: 8px;
}

.maintainer-review-header h1 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: clamp(2rem, 3.4vw, 2.9rem);
  line-height: 1.02;
  letter-spacing: -0.015em;
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
  gap: 10px;
  border: 1px solid rgba(255, 222, 161, 0.18);
  border-radius: 10px;
  padding: 16px 16px 18px;
  background: rgba(7, 4, 2, 0.32);
}

.maintainer-review-stats span {
  color: rgba(255, 231, 183, 0.66);
  font-size: 0.84rem;
  line-height: 1.35;
}

.maintainer-review-stats strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 2.4rem;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.maintainer-review-alert {
  display: grid;
  gap: 6px;
  border: 1px solid rgba(255, 196, 145, 0.62);
  border-radius: 10px;
  padding: 13px 16px;
  color: #ffe8b9;
  background:
    linear-gradient(135deg, rgba(88, 28, 18, 0.86), rgba(30, 9, 5, 0.78)),
    radial-gradient(circle at 6% 20%, rgba(255, 180, 130, 0.18), transparent 0 34%);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.34), inset 0 1px 0 rgba(255, 235, 180, 0.14);
}

.maintainer-review-alert:focus {
  outline: 2px solid rgba(255, 224, 157, 0.82);
  outline-offset: 3px;
}

.maintainer-review-alert.success {
  border-color: rgba(169, 208, 123, 0.62);
  background:
    linear-gradient(135deg, rgba(43, 74, 28, 0.84), rgba(12, 30, 10, 0.72)),
    radial-gradient(circle at 6% 20%, rgba(169, 208, 123, 0.2), transparent 0 34%);
}

.maintainer-review-alert.warning {
  border-color: rgba(240, 184, 104, 0.64);
  background:
    linear-gradient(135deg, rgba(91, 54, 16, 0.86), rgba(32, 17, 5, 0.76)),
    radial-gradient(circle at 6% 20%, rgba(255, 210, 130, 0.18), transparent 0 34%);
}

.maintainer-review-alert strong {
  color: #ffe2a0;
  font-size: 1rem;
}

.maintainer-review-alert p {
  margin: 0;
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.48;
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
  font-size: clamp(1.5rem, 3vw, 2.1rem);
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.review-empty-state p:last-child {
  margin: 12px 0 0;
  color: rgba(255, 231, 183, 0.72);
  font-size: 0.98rem;
}

.review-focus {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.85fr);
  gap: 12px;
}

.review-focus.readonly-review {
  grid-template-columns: minmax(0, 1fr);
}

.review-action-slot {
  display: grid;
  min-width: 0;
  min-height: 0;
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
