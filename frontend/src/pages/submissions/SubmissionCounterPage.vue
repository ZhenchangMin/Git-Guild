<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import submissionCounterImg from '../../assets/submission-counter-clerk-v0.png'
import SubmissionCounter from '../../components/SubmissionCounter.vue'
import { questDetails } from '../../data/quests'
import { sessionStore } from '../../stores/sessionStore'

const route = useRoute()
const router = useRouter()

// Lightweight catalog used as fallback when the user lands here without a
// full quest record. The richer fields (description, repository, PR) come
// from quests.js via `questDetails`.
const questCatalog = {
  'QST-0412': { title: 'Issue 同步状态页', difficulty: 'C', stack: 'Vue / Spring Boot', reward: '180 XP' },
  'QST-0427': { title: '重构成果提交流程', difficulty: 'D', stack: 'Vue / REST API', reward: '240 XP' },
  'QST-0431': { title: '仓库导入失败提示页', difficulty: 'C', stack: 'Vue / Gitea', reward: '160 XP' },
  'QST-0438': { title: '新手执行清单', difficulty: 'B', stack: 'Markdown / Vue', reward: '120 XP' },
  'QST-0440': { title: '任务筛选条件保持', difficulty: 'C', stack: 'Vue / Local State', reward: '150 XP' },
  'QST-0444': { title: '审核反馈归档', difficulty: 'D', stack: 'Spring Boot / Vue', reward: '260 XP' },
}

const activeQuest = computed(() => {
  const questId = String(route.query.questId || 'QST-0427')
  const summary = questCatalog[questId] ?? questCatalog['QST-0427']
  return {
    id: questId,
    ...summary,
    detail: questDetails[questId] ?? questDetails['QST-0427'],
  }
})

// After a successful submission we surface a small persistent banner on the
// scene itself, so even if the user closes the modal the page still shows
// "已交付柜台" instead of inviting another submit.
const lastReceipt = ref(null)

function handleSubmitted(receipt) {
  lastReceipt.value = receipt
}

// Adventurers come from the workbench; everyone else (admin previewing, etc.)
// goes back to the hall. We don't trust intent guards here — routes already
// gate the page on ADVENTURER.
const backTarget = computed(() => {
  if (sessionStore.role === 'ADVENTURER') return { name: 'adventurer-workbench' }
  return { name: 'hall' }
})

const backLabel = computed(() =>
  sessionStore.role === 'ADVENTURER' ? '返回冒险者工作台' : '返回公会大厅',
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
