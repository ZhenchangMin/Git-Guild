<script setup>
import { useRouter } from 'vue-router'

import workbenchImg from '../../assets/workbench.png'
import Workbench from '../../components/Workbench.vue'

const router = useRouter()

function backToHall() {
  router.push({ name: 'hall' })
}

function openSubmission(questId) {
  router.push({
    name: 'submission-counter',
    query: questId ? { questId } : {},
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
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回公会大厅</span>
      </button>

      <Workbench initial-view="guild-master" @open-submission="openSubmission" @open-id-card="openProfile" />
    </section>
  </main>
</template>
