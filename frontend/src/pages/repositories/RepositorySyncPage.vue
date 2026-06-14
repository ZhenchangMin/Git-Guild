<script setup>
import { useRouter } from 'vue-router'

import deskImg from '../../assets/desk.webp'
import RepositorySyncDesk from '../../components/RepositorySyncDesk.vue'

const router = useRouter()

function backToHall() {
  router.push({ name: 'hall' })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene desk-mode" :style="{ backgroundImage: `url(${deskImg})` }">
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回公会大厅</span>
      </button>

      <RepositorySyncDesk />
    </section>
  </main>
</template>
