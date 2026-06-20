<script setup>
import { useRouter } from 'vue-router'

import workbenchImg from '../../assets/workbench.webp'
import HomeOrb from '../../components/HomeOrb.vue'
import Workbench from '../../components/Workbench.vue'

const router = useRouter()

// 返回上一页（保留用户来路）；无站内历史（直接深链进入）时兜底回大厅。
function goBack() {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push({ name: 'hall' })
  }
}

function openSubmission(quest) {
  const query = typeof quest === 'object' && quest !== null
    ? Object.fromEntries(Object.entries(quest).filter(([, value]) => value !== undefined && value !== null && value !== ''))
    : quest
      ? { questId: quest }
      : {}
  router.push({
    name: 'submission-counter',
    query,
  })
}

function openProfile() {
  router.push({ name: 'profile' })
}
</script>

<template>
  <main class="app-shell">
    <section
      class="scene work-scene workbench-mode"
      :style="{ backgroundImage: `url(${workbenchImg})` }"
    >
      <HomeOrb />
      <button class="back-orb" type="button" aria-label="返回上一页" @click="goBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回</span>
      </button>

      <Workbench @open-submission="openSubmission" @open-id-card="openProfile" />
    </section>
  </main>
</template>
