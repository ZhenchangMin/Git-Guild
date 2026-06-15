<script setup>
import { useRouter } from 'vue-router'

import deskImg from '../../assets/desk.webp'
import RepositorySyncDesk from '../../components/RepositorySyncDesk.vue'

const router = useRouter()

// 返回上一页（保留用户来路，如从工作台进入）；无站内历史时兜底回大厅。
function goBack() {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push({ name: 'hall' })
  }
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene desk-mode" :style="{ backgroundImage: `url(${deskImg})` }">
      <button class="back-orb" type="button" aria-label="返回上一页" @click="goBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回</span>
      </button>

      <RepositorySyncDesk />
    </section>
  </main>
</template>
