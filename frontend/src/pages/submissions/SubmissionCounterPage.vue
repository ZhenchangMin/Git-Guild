<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { questApi } from '../../api/questApi'
import submissionCounterImg from '../../assets/submission-counter-clerk-v0.webp'
import SubmissionCounter from '../../components/SubmissionCounter.vue'
import { sessionStore } from '../../stores/sessionStore'

const route = useRoute()
const router = useRouter()

const quest = ref(null)
const questLoading = ref(false)

function extractQuestNumericId(value) {
  const match = String(value ?? '').match(/\d+/)
  return match ? Number(match[0]) : null
}

// 只展示后端真实委托；无 questId / 加载失败时为 null，
// SubmissionCounter 组件自身已处理“未关联任务”空态。
const activeQuest = computed(() => quest.value)

async function fetchQuest(rawQuestId) {
  const questId = extractQuestNumericId(rawQuestId)
  if (!questId) {
    quest.value = null
    return
  }
  questLoading.value = true
  try {
    const response = await questApi.detail(questId)
    const detail = response?.data
    if (detail) {
      quest.value = mapQuestDetail(detail)
    }
  } catch {
    quest.value = null
  } finally {
    questLoading.value = false
  }
}

function mapQuestDetail(detail) {
  return {
    id: `QST-${String(detail.questId).padStart(4, '0')}`,
    numericId: detail.questId,
    title: detail.title,
    difficulty: detail.difficulty,
    stack: (detail.techStack || []).join(' / ') || '未指定',
    reward: detail.rewardXp ? `${detail.rewardXp} XP` : '0 XP',
    detail: {
      description: detail.description,
      repository: {
        name: detail.repository?.name || '',
        branch: detail.repository?.defaultBranch || 'main',
        syncStatus: detail.repository?.syncStatus || 'Synced',
      },
      issue: {
        number: detail.issue?.externalIssueId ? `#${detail.issue.externalIssueId}` : '',
        title: detail.issue?.title || '',
        status: detail.issue?.status || '',
      },
      pr: {
        number: 'Not created',
        status: '尚未开始',
      },
    },
  }
}

watch(
  () => route.query.questId,
  (questId) => fetchQuest(questId),
  { immediate: true },
)

// After a successful submission we surface a small persistent banner on the
// scene itself, so even if the user closes the modal the page still shows
// "已交付柜台" instead of inviting another submit.
const lastReceipt = ref(null)

function handleSubmitted(receipt) {
  lastReceipt.value = receipt
}

// Adventurers and maintainers can both use the adventurer-style workbench to
// complete accepted quests; admins preview from the hall.
const backTarget = computed(() => {
  if (sessionStore.role === 'ADVENTURER' || sessionStore.role === 'MAINTAINER') {
    return { name: 'adventurer-workbench' }
  }
  return { name: 'hall' }
})

const backLabel = computed(() =>
  sessionStore.role === 'ADVENTURER' || sessionStore.role === 'MAINTAINER'
    ? '返回工作台'
    : '返回公会大厅',
)

function navigateBack() {
  router.push(backTarget.value)
}

function formatReceiptTime(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<template>
  <main class="app-shell">
    <section
      class="scene work-scene submission-mode"
      :style="{ backgroundImage: `url(${submissionCounterImg})` }"
    >
      <button class="back-orb" type="button" :aria-label="backLabel" @click="navigateBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>{{ backLabel }}</span>
      </button>

      <Transition name="submission-toast">
        <aside v-if="lastReceipt" class="submission-scene-receipt" aria-live="polite">
          <span class="scene-receipt-seal" aria-hidden="true">審</span>
          <div>
            <p class="kicker">柜台已收讫</p>
            <strong>{{ lastReceipt.id }}</strong>
            <small>{{ formatReceiptTime(lastReceipt.submittedAt) }} · 等待维护者审核</small>
          </div>
        </aside>
      </Transition>

      <SubmissionCounter :quest="activeQuest" @submitted="handleSubmitted" />
    </section>
  </main>
</template>

<style scoped>
/* Page-only floating receipt chip — sits on the scene next to the back orb
   so the user has a persistent "yes, it's submitted" anchor even after the
   parchment modal closes. */
.submission-scene-receipt {
  position: absolute;
  right: 24px;
  top: 24px;
  z-index: 3;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 10px 16px 10px 12px;
  border: 1px solid rgba(255, 215, 145, 0.34);
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(60, 32, 12, 0.78), rgba(36, 18, 6, 0.86));
  color: #ffe7b8;
  box-shadow: 0 18px 36px rgba(8, 4, 2, 0.5);
  max-width: min(320px, 60vw);
}

.scene-receipt-seal {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 30% 26%, rgba(255, 196, 178, 0.6), transparent 50%),
    #a31e20;
  color: #fff1cf;
  font-family: 'KaiTi', 'STKaiti', serif;
  font-size: 1.04rem;
  font-weight: 700;
  box-shadow: inset 0 -2px 4px rgba(40, 4, 4, 0.6);
}

.submission-scene-receipt .kicker {
  color: rgba(255, 231, 184, 0.7);
  font-size: 0.62rem;
  letter-spacing: 0.18em;
  margin: 0 0 1px;
}

.submission-scene-receipt strong {
  display: block;
  font-size: 0.96rem;
  letter-spacing: 0.04em;
  font-variant-numeric: tabular-nums;
}

.submission-scene-receipt small {
  display: block;
  margin-top: 2px;
  color: rgba(255, 231, 184, 0.66);
  font-size: 0.7rem;
}
</style>
