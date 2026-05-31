<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import workbenchImg from '../../assets/workbench.png'
import MaintainerReviewActions from '../../components/MaintainerReviewActions.vue'
import MaintainerReviewDetail from '../../components/MaintainerReviewDetail.vue'
import MaintainerReviewQueue from '../../components/MaintainerReviewQueue.vue'
import { reviewApi } from '../../api'
import { maintainerReviewQueue, maintainerReviewStats } from '../../data/maintainerReview'

const router = useRouter()

const reviews = ref(maintainerReviewQueue.map((review) => ({ ...review })))
const selectedReviewId = ref(reviews.value[0]?.id ?? '')
const reviewResult = ref(null)
const isSubmittingReview = ref(false)

const selectedReview = computed(
  () => reviews.value.find((review) => review.id === selectedReviewId.value) ?? reviews.value[0],
)

function backToWorkbench() {
  router.push({ name: 'maintainer-workbench' })
}

function selectReview(reviewId) {
  selectedReviewId.value = reviewId
  reviewResult.value = null
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
            <article v-for="stat in maintainerReviewStats" :key="stat.label">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <small>{{ stat.hint }}</small>
            </article>
          </div>
        </header>

        <div class="maintainer-review-workspace">
          <MaintainerReviewQueue
            :reviews="reviews"
            :selected-review-id="selectedReview?.id"
            @select="selectReview"
          />
          <MaintainerReviewDetail v-if="selectedReview" :review="selectedReview" />
          <MaintainerReviewActions
            v-if="selectedReview"
            :review="selectedReview"
            :result="reviewResult"
            :busy="isSubmittingReview"
            @submit-review="submitReview"
            @save-draft="saveDraft"
            @open-pr="openPullRequest"
          />
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
  grid-template-columns: minmax(270px, 0.82fr) minmax(420px, 1.48fr) minmax(310px, 0.95fr);
  gap: 16px;
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
  .maintainer-review-workspace {
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
