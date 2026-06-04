<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { questApi } from '../../api/questApi'
import questBoardImg from '../../assets/quest board.png'
import QuestDetail from '../../components/QuestDetail.vue'
import { questCommissions } from '../../data/questBoard'
import { sessionStore } from '../../stores/sessionStore'

const route = useRoute()
const router = useRouter()
const liveQuestDetail = ref(null)
const isLoadingDetail = ref(false)
const detailError = ref('')

const routeQuestId = computed(() => {
  const id = Number(route.params.questId)
  return Number.isInteger(id) && id > 0 ? id : null
})

const STATUS_LABELS = {
  PUBLISHED: '可接取',
  IN_PROGRESS: '进行中',
  PENDING_ADMIN_REVIEW: '待审核',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
  DRAFT: '草稿',
}

const WORKFLOW_STATES = {
  PUBLISHED: 'available',
  IN_PROGRESS: 'in-progress',
  PENDING_ADMIN_REVIEW: 'in-review',
  COMPLETED: 'completed',
  CLOSED: 'completed',
  DRAFT: 'available',
}

function normalizeCriteria(value) {
  if (Array.isArray(value)) return value
  if (!value) return ['查看任务详情', '提交关联 PR', '等待委托人审核']
  return String(value)
    .split(/\r?\n|；|;/)
    .map((line) => line.trim())
    .filter(Boolean)
}

function normalizeIssueNumber(issue) {
  const id = issue?.externalIssueId ?? issue?.issueId
  if (!id) return '#?'
  return String(id).startsWith('#') ? String(id) : `#${id}`
}

function normalizeQuestDetail(detail) {
  const techStack = Array.isArray(detail.techStack) ? detail.techStack : []
  const tagNames = Array.isArray(detail.tags)
    ? detail.tags.map((tag) => tag.name ?? tag).filter(Boolean)
    : []
  const status = STATUS_LABELS[detail.status] ?? detail.status ?? '可接取'

  return {
    id: String(detail.questId),
    routeId: String(detail.questId),
    title: detail.title ?? '未命名委托',
    issuer: detail.publisher?.username ? `委托人 · ${detail.publisher.username}` : '委托人 · Git Guild',
    category: detail.category?.name ?? '任务',
    difficulty: detail.difficulty ?? 'C',
    stack: techStack.length > 0 ? techStack.join(' / ') : '待补充',
    techStack,
    status,
    tags: tagNames,
    reward: detail.rewardXp ? `${detail.rewardXp} XP` : '待定 XP',
    summary: detail.description ?? '查看详情了解这份委托的背景和完成要求。',
    criteria: normalizeCriteria(detail.completionCriteria),
    assignment: detail.assignment,
    detail: {
      workflowState: WORKFLOW_STATES[detail.status] ?? 'available',
      estimatedHours: detail.estimatedHours ?? 6,
      description: detail.description,
      repository: {
        name: detail.repository?.name ?? '未绑定仓库',
        branch: detail.repository?.defaultBranch ?? 'main',
        syncStatus: detail.repository?.syncStatus ?? 'UNKNOWN',
      },
      issue: {
        number: normalizeIssueNumber(detail.issue),
        title: detail.issue?.title ?? detail.title ?? '未绑定 Issue',
        status: detail.issue?.status ?? 'UNKNOWN',
        url: detail.issue?.externalUrl,
      },
    },
  }
}

const activeQuest = computed(() => {
  if (liveQuestDetail.value) return normalizeQuestDetail(liveQuestDetail.value)
  const questId = String(route.params.questId || 'QST-0427')
  return questCommissions.find((quest) => quest.id === questId) ?? questCommissions[0]
})

const activeQuestIntent = computed(() => (route.query.intent === 'accept' ? 'accept' : 'view'))

// Default to returning to whatever page the adventurer came from (quest board,
// hall, workbench…). Fall back to the quest board when there's no history entry
// to go back to (e.g. the detail page was opened directly via URL).
function goBack() {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push({ name: 'quest-board' })
  }
}

function openWorkbench() {
  router.push({ name: sessionStore.role === 'MAINTAINER' ? 'maintainer-workbench' : 'adventurer-workbench' })
}

function openSubmission(questId = activeQuest.value.id) {
  router.push({
    name: 'submission-counter',
    query: { questId },
  })
}

async function loadQuestDetail() {
  liveQuestDetail.value = null
  detailError.value = ''
  if (!routeQuestId.value) return

  isLoadingDetail.value = true
  try {
    const payload = await questApi.detail(routeQuestId.value)
    liveQuestDetail.value = payload?.data ?? null
  } catch (error) {
    detailError.value = error?.message ?? '真实任务详情暂时不可用，当前显示演示数据。'
  } finally {
    isLoadingDetail.value = false
  }
}

watch(() => route.params.questId, loadQuestDetail)
onMounted(loadQuestDetail)
</script>

<template>
  <main class="app-shell">
    <section class="scene quest-detail-mode" :style="{ backgroundImage: `url(${questBoardImg})` }">
      <button class="back-orb" type="button" aria-label="返回上一页" @click="goBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回上一页</span>
      </button>

      <QuestDetail
        :quest="activeQuest"
        :quest-id="String(route.params.questId || '')"
        :intent="activeQuestIntent"
        @open-workbench="openWorkbench"
        @open-submission="openSubmission"
      />
      <p v-if="isLoadingDetail" class="quest-detail-live-note">正在加载真实任务详情...</p>
      <p v-else-if="detailError" class="quest-detail-live-note warning">{{ detailError }}</p>
    </section>
  </main>
</template>

<style scoped>
.quest-detail-live-note {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 12;
  max-width: min(360px, calc(100vw - 48px));
  margin: 0;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 8px;
  background: rgba(18, 23, 33, 0.88);
  color: #f8fafc;
  font-size: 0.9rem;
}

.quest-detail-live-note.warning {
  border-color: rgba(245, 158, 11, 0.55);
  color: #fde68a;
}
</style>
