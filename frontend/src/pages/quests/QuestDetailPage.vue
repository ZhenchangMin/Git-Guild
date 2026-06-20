<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { questApi } from '../../api/questApi'
import questBoardImg from '../../assets/quest board.webp'
import HomeOrb from '../../components/HomeOrb.vue'
import QuestDetail from '../../components/QuestDetail.vue'
import { sessionStore } from '../../stores/sessionStore'
import { toBrowsableGiteaUrl } from '../../utils/giteaUrl'

const route = useRoute()
const router = useRouter()
const activeQuest = ref(null)
const isLoading = ref(false)
const loadError = ref('')

const STATUS_LABELS = {
  DRAFT: '草稿',
  PENDING_ADMIN_REVIEW: '待管理员审核',
  PUBLISHED: '可接取',
  IN_PROGRESS: '进行中',
  IN_REVIEW: '等待维护者审核',
  COMPLETED: '已完成',
  REJECTED: '需要修改',
  CLOSED: '已关闭',
}

const WORKFLOW_STATES = {
  DRAFT: 'available',
  PENDING_ADMIN_REVIEW: 'in-review',
  PUBLISHED: 'available',
  IN_PROGRESS: 'in-progress',
  IN_REVIEW: 'in-review',
  COMPLETED: 'completed',
  REJECTED: 'changes-requested',
  CLOSED: 'completed',
}

const activeQuestIntent = computed(() => (route.query.intent === 'accept' ? 'accept' : 'view'))
const routeQuestId = computed(() => String(route.params.questId || '').trim())

function unwrapApiData(payload) {
  return payload?.data ?? payload ?? {}
}

function parseQuestApiId(value) {
  const raw = String(value ?? '').trim()
  if (/^\d+$/.test(raw)) return raw
  const qstMatch = raw.match(/^QST-0*(\d+)$/i)
  return qstMatch ? qstMatch[1] : ''
}

function formatQuestCode(questId) {
  if (questId === undefined || questId === null || questId === '') return 'QST-????'
  return `QST-${String(questId).padStart(4, '0')}`
}

function normalizeReward(rewardXp) {
  if (rewardXp === undefined || rewardXp === null || rewardXp === '') return '待定 XP'
  return `${rewardXp} XP`
}

function normalizeCriteria(value) {
  if (!value) return ['查看任务详情', '提交关联 PR', '等待维护者审核']
  if (Array.isArray(value)) return value.filter(Boolean)
  return String(value)
    .split(/\r?\n|[；;]/)
    .map((line) => line.trim())
    .filter(Boolean)
}

function normalizeIssueNumber(issue) {
  const externalId = issue?.externalIssueId ?? issue?.issueId
  if (!externalId) return '未关联'
  const text = String(externalId)
  return text.startsWith('#') ? text : `#${text}`
}

function normalizeQuestDetail(quest) {
  const questId = quest.questId ?? quest.id
  const techStack = Array.isArray(quest.techStack) ? quest.techStack : []
  const tags = Array.isArray(quest.tags)
    ? quest.tags.map((tag) => tag.name ?? tag).filter(Boolean)
    : []

  return {
    id: formatQuestCode(questId),
    questId,
    code: formatQuestCode(questId),
    routeId: String(questId ?? ''),
    title: quest.title ?? '未命名委托',
    issuer: quest.publisher?.username ? `委托人 · ${quest.publisher.username}` : '委托人 · 未知',
    category: quest.category?.name ?? '未分类',
    difficulty: quest.difficulty ?? 'C',
    stack: techStack.length > 0 ? techStack.join(' / ') : '待补充',
    techStack,
    rawStatus: quest.status,
    status: STATUS_LABELS[quest.status] ?? quest.status ?? '未知状态',
    tags,
    reward: normalizeReward(quest.rewardXp),
    summary: quest.description ?? '暂无任务说明。',
    description: quest.description ?? '暂无任务说明。',
    criteria: normalizeCriteria(quest.completionCriteria),
    estimatedHours: quest.estimatedHours,
    workflowState: WORKFLOW_STATES[quest.status] ?? 'available',
    assignment: quest.assignment ?? null,
    repository: {
      repositoryId: quest.repository?.repositoryId ?? null,
      name: quest.repository?.name ?? '未关联仓库',
      branch: quest.repository?.defaultBranch ?? '未提供',
      syncStatus: quest.repository?.syncStatus ?? '未知',
      webUrl: quest.repository?.webUrl ?? null,
    },
    issue: {
      issueId: quest.issue?.issueId ?? null,
      number: normalizeIssueNumber(quest.issue),
      title: quest.issue?.title ?? '未关联 Issue',
      status: quest.issue?.status ?? '未知',
      webUrl: toBrowsableGiteaUrl(quest.issue?.externalUrl ?? null),
    },
    pr: {
      number: 'Not created',
      status: 'Not started',
    },
  }
}

async function loadQuestDetail() {
  const apiQuestId = parseQuestApiId(routeQuestId.value)
  if (!apiQuestId) {
    activeQuest.value = null
    loadError.value = '任务编号无效，请从悬赏任务板重新打开详情。'
    return
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const payload = await questApi.detail(apiQuestId)
    activeQuest.value = normalizeQuestDetail(unwrapApiData(payload))
  } catch (error) {
    activeQuest.value = null
    loadError.value = error?.message || '任务详情加载失败，请稍后重试。'
  } finally {
    isLoading.value = false
  }
}

// 有站内历史就弹回真正的来路（任务板 / 工作台等），无历史（深链直入）时兜底到任务板。
function goBack() {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push({ name: 'quest-board' })
  }
}

function openWorkbench() {
  // 游客点「进入工作台」：先跳登录，登录后直达工作台（redirect 指向受保护的工作台路径）。
  if (sessionStore.role === 'VISITOR') {
    router.push({
      name: 'login',
      query: { redirect: router.resolve({ name: 'adventurer-workbench' }).fullPath },
    })
    return
  }
  router.push({
    name: sessionStore.role === 'ADVENTURER' || sessionStore.role === 'MAINTAINER'
      ? 'adventurer-workbench'
      : 'hall',
  })
}

function openSubmission(questId = activeQuest.value?.questId ?? activeQuest.value?.id) {
  router.push({
    name: 'submission-counter',
    query: { questId },
  })
}

watch(routeQuestId, loadQuestDetail)
onMounted(loadQuestDetail)
</script>

<template>
  <main class="app-shell">
    <section class="scene quest-detail-mode" :style="{ backgroundImage: `url(${questBoardImg})` }">
      <HomeOrb />
      <button class="back-orb" type="button" aria-label="返回上一页" @click="goBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回上一页</span>
      </button>

      <QuestDetail
        v-if="activeQuest"
        :quest="activeQuest"
        :intent="activeQuestIntent"
        @open-workbench="openWorkbench"
        @open-submission="openSubmission"
      />

      <div v-else class="quest-detail-message" role="status" aria-live="polite">
        <p class="kicker">{{ isLoading ? '正在读取' : '无法打开' }}</p>
        <h1>{{ isLoading ? '正在加载委托详情' : '委托详情加载失败' }}</h1>
        <p>{{ isLoading ? '正在从后端接口同步委托详情，请稍候。' : loadError }}</p>
        <button v-if="!isLoading" class="primary-action" type="button" @click="loadQuestDetail">重试</button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.quest-detail-message {
  width: min(620px, calc(100vw - 42px));
  margin: clamp(98px, 12svh, 136px) auto 34px;
  border: 1px solid rgba(238, 184, 91, 0.58);
  border-radius: var(--radius);
  padding: 24px;
  color: #ffe7b5;
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.86), rgba(15, 8, 4, 0.8)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.16), transparent 58%);
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.36), inset 0 1px 0 rgba(255, 229, 163, 0.14);
  backdrop-filter: blur(6px);
}

.quest-detail-message h1 {
  margin: 6px 0 0;
  font-size: clamp(1.5rem, 3vw, 2.2rem);
}

.quest-detail-message p:not(.kicker) {
  margin: 12px 0 0;
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.5;
}

.quest-detail-message button {
  margin-top: 18px;
}
</style>
