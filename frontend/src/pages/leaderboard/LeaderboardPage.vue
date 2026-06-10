<script setup>
import { useRouter } from 'vue-router'

import leaderboardImg from '../../assets/leader board wall.webp'

const router = useRouter()

const leaderboardRows = [
  { rank: 1, name: 'Minerva Dawn', level: 3, xp: 720, completed: 6 },
  { rank: 2, name: 'Aster Vale', level: 3, xp: 680, completed: 5 },
  { rank: 3, name: 'Rowan Byte', level: 2, xp: 540, completed: 4 },
  { rank: 4, name: 'Nova Patch', level: 2, xp: 420, completed: 3 },
]

function backToHall() {
  router.push({ name: 'hall' })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene leaderboard-route-mode" :style="{ backgroundImage: `url(${leaderboardImg})` }">
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回公会大厅</span>
      </button>

      <div class="standalone-page-panel compact">
        <section class="glass-ledger standalone-hero-card">
          <p class="kicker">排行榜墙</p>
          <h1>成长排行榜</h1>
          <p>P2 展示项，当前使用演示数据。后续可接入 `/leaderboards/xp`。</p>
        </section>

        <section class="glass-ledger">
          <div class="standalone-table">
            <div class="standalone-table-row head">
              <span>排名</span>
              <span>冒险家</span>
              <span>等级</span>
              <span>XP</span>
              <span>完成任务</span>
            </div>
            <div v-for="row in leaderboardRows" :key="row.rank" class="standalone-table-row">
              <strong>#{{ row.rank }}</strong>
              <span>{{ row.name }}</span>
              <span>Level {{ row.level }}</span>
              <span>{{ row.xp }}</span>
              <span>{{ row.completed }}</span>
            </div>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>
