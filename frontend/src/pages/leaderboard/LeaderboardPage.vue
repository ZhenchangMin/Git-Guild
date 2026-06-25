<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { leaderboardApi } from '../../api/leaderboardApi'
import leaderboardImg from '../../assets/leader board wall.webp'

const router = useRouter()

const leaderboardRows = ref([])
const isLoading = ref(false)
const loadError = ref('')

function unwrap(payload) {
  return payload?.data ?? payload ?? {}
}

async function loadLeaderboard() {
  isLoading.value = true
  loadError.value = ''
  try {
    const payload = await leaderboardApi.xp({ period: 'ALL_TIME', limit: 50 })
    const items = unwrap(payload).items ?? []
    leaderboardRows.value = items.map((item) => ({
      rank: item.rank,
      name: item.username ?? '匿名冒险家',
      level: item.level,
      xp: item.totalXp,
      completed: item.completedQuestCount,
    }))
  } catch (error) {
    leaderboardRows.value = []
    loadError.value = error?.message || '排行榜加载失败，请稍后重试。'
  } finally {
    isLoading.value = false
  }
}

function backToHall() {
  router.push({ name: 'hall' })
}

onMounted(loadLeaderboard)
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene leaderboard-route-mode" :style="{ backgroundImage: `url(${leaderboardImg})` }">
      <button class="back-orb" type="button" aria-label="返回" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回</span>
      </button>

      <div class="standalone-page-panel compact">
        <section class="glass-ledger standalone-hero-card" data-tutorial="leaderboard-metrics">
          <p class="kicker">排行榜墙</p>
          <h1>成长排行榜</h1>
          <p>按等级与 XP 排名，并列时比较完成任务数量。数据实时来自公会成长档案。</p>
        </section>

        <section class="glass-ledger">
          <div class="standalone-table" data-tutorial="leaderboard-list">
            <div class="standalone-table-row head">
              <span>排名</span>
              <span>冒险家</span>
              <span>等级</span>
              <span>XP</span>
              <span>完成任务</span>
            </div>

            <p v-if="isLoading" class="leaderboard-status" role="status">正在加载排行榜…</p>
            <p v-else-if="loadError" class="leaderboard-status error" role="alert">{{ loadError }}</p>
            <p v-else-if="leaderboardRows.length === 0" class="leaderboard-status">
              暂无上榜冒险家，完成委托即可登上榜单。
            </p>

            <template v-else>
              <div
                v-for="row in leaderboardRows"
                :key="row.rank"
                class="standalone-table-row"
              >
                <strong>#{{ row.rank }}</strong>
                <span>{{ row.name }}</span>
                <span>Level {{ row.level }}</span>
                <span>{{ row.xp }}</span>
                <span>{{ row.completed }}</span>
              </div>
            </template>
          </div>

          <button
            class="primary-action leaderboard-retry"
            type="button"
            data-tutorial="leaderboard-refresh"
            :disabled="isLoading"
            @click="loadLeaderboard"
          >{{ loadError && !isLoading ? '重试' : isLoading ? '刷新中…' : '刷新榜单' }}</button>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.leaderboard-status {
  margin: 0;
  padding: 18px 8px;
  text-align: center;
  color: rgba(255, 231, 183, 0.74);
  font-size: 0.92rem;
}

.leaderboard-status.error {
  color: #f0a06d;
}

.leaderboard-retry {
  margin-top: 14px;
}
</style>
