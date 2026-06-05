<script setup>
import { useRouter } from 'vue-router'

import workbenchImg from '../../assets/workbench.png'
import Workbench from '../../components/Workbench.vue'

const router = useRouter()

function backToHall() {
  router.push({ name: 'hall' })
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

function openReviewDesk() {
  router.push({ name: 'maintainer-review' })
}

function openPublish() {
  router.push({ name: 'maintainer-publish' })
}
</script>

<template>
  <main class="app-shell">
    <section
      class="scene work-scene workbench-mode maintainer-workbench-mode"
      :style="{ backgroundImage: `url(${workbenchImg})` }"
    >
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回公会大厅</span>
      </button>

      <button class="publish-entry-orb" type="button" @click="openPublish">
        <span aria-hidden="true">＋</span>
        <span>发布新任务</span>
      </button>

      <Workbench
        show-review-desk-entry
        @open-submission="openSubmission"
        @open-id-card="openProfile"
        @open-review-desk="openReviewDesk"
      />
    </section>
  </main>
</template>

<style scoped>
.publish-entry-orb {
  position: absolute;
  top: 1.5rem;
  right: 1.5rem;
  z-index: 5;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.1rem;
  border: 1px solid rgba(255, 236, 190, 0.55);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(120, 36, 38, 0.95), rgba(86, 22, 24, 0.95));
  color: #ffe9c4;
  font-size: 0.95rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.publish-entry-orb:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.42);
}
.publish-entry-orb span[aria-hidden='true'] {
  font-size: 1.1rem;
  line-height: 1;
}
</style>
