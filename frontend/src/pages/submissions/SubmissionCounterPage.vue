<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import submissionCounterImg from '../../assets/submission-counter-clerk-v0.png'
import SubmissionCounter from '../../components/SubmissionCounter.vue'
import { questDetails } from '../../data/quests'

const route = useRoute()
const router = useRouter()

const questCatalog = {
  'QST-0412': {
    title: 'Issue 同步状态页',
    difficulty: 'C',
    stack: 'Vue / Spring Boot',
    reward: '180 XP',
  },
  'QST-0427': {
    title: '重构成果提交流程',
    difficulty: 'D',
    stack: 'Vue / REST API',
    reward: '240 XP',
  },
  'QST-0431': {
    title: '仓库导入失败提示页',
    difficulty: 'C',
    stack: 'Vue / Gitea',
    reward: '160 XP',
  },
  'QST-0438': {
    title: '新手执行清单',
    difficulty: 'B',
    stack: 'Markdown / Vue',
    reward: '120 XP',
  },
  'QST-0440': {
    title: '任务筛选条件保持',
    difficulty: 'C',
    stack: 'Vue / Local State',
    reward: '150 XP',
  },
  'QST-0444': {
    title: '审核反馈归档',
    difficulty: 'D',
    stack: 'Spring Boot / Vue',
    reward: '260 XP',
  },
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

function backToHall() {
  router.push({ name: 'hall' })
}
</script>

<template>
  <main class="app-shell">
    <section
      class="scene work-scene submission-mode"
      :style="{ backgroundImage: `url(${submissionCounterImg})` }"
    >
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回公会大厅</span>
      </button>

      <SubmissionCounter :quest="activeQuest" />
    </section>
  </main>
</template>
