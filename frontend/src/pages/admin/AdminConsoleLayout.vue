<script setup>
import { useRouter } from 'vue-router'

import operationRoomImg from '../../assets/operation room.png'
import { clearSession } from '../../stores/sessionStore'

const router = useRouter()

const sections = [
  { routeName: 'admin-review', label: '任务审核', hint: '上架 · 退回 · 下架', glyph: '✦' },
  { routeName: 'admin-exceptions', label: '异常处理', hint: '同步 · 关联 · 权限', glyph: '⚠' },
  { routeName: 'admin-taxonomy', label: '平台配置', hint: '分类 · 标签 · 难度', glyph: '❖' },
]

function logout() {
  clearSession()
  router.push({ name: 'login' })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene admin-scene" :style="{ backgroundImage: `url(${operationRoomImg})` }">
      <div class="session-action-stack" aria-label="账号操作">
        <button class="back-orb logout-orb logout-orb-admin" type="button" aria-label="退出登录" @click="logout">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10 7V5a2 2 0 0 1 2-2h6v18h-6a2 2 0 0 1-2-2v-2" />
            <path d="M3 12h10" />
            <path d="m6 9-3 3 3 3" />
          </svg>
          <span>退出登录</span>
        </button>
      </div>

      <div class="admin-console">
        <aside class="admin-console-rail" aria-label="管理员控制台导航">
          <div class="admin-console-brand">
            <p class="kicker">Admin Clearance</p>
            <h1>管理员控制台</h1>
          </div>

          <nav class="admin-console-nav">
            <RouterLink
              v-for="section in sections"
              :key="section.routeName"
              :to="{ name: section.routeName }"
              class="admin-console-link"
              active-class="active"
            >
              <span class="admin-console-glyph" aria-hidden="true">{{ section.glyph }}</span>
              <span class="admin-console-link-text">
                <strong>{{ section.label }}</strong>
                <small>{{ section.hint }}</small>
              </span>
            </RouterLink>
          </nav>

          <p class="admin-console-footnote">ADMIN</p>
        </aside>

        <div class="admin-console-stage">
          <RouterView />
        </div>
      </div>
    </section>
  </main>
</template>
