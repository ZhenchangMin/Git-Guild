<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

// 直达公会大厅的"逃生出口"：无论当前在哪个深层页面，一键回到 hall，
// 避免来回点"返回"时在历史栈里打转。固定显示在左上角返回键的正下方。
function goHome() {
  router.push({ name: 'hall' })
}
</script>

<template>
  <button class="back-orb home-orb" type="button" aria-label="返回公会大厅" @click="goHome">
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 11.5 12 5l8 6.5" />
      <path d="M6 10.5V19h12v-8.5" />
      <path d="M10 19v-4.5h4V19" />
    </svg>
    <span>返回大厅</span>
  </button>
</template>
