<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import workbenchImg from '../../assets/workbench.png'
import { recentContributions, reviewFeedbacks, workbenchStats, workbenchUser } from '../../data/workbench'

const router = useRouter()

const xpProgress = computed(() => `${Math.round((workbenchUser.xpCurrent / workbenchUser.xpTarget) * 100)}%`)
const approvedFeedbacks = computed(() => reviewFeedbacks.filter((feedback) => feedback.status === 'approved'))
const returnedFeedbacks = computed(() => reviewFeedbacks.filter((feedback) => feedback.status === 'changes-requested'))

function backToWorkbench() {
  router.push({ name: 'adventurer-workbench' })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene workbench-mode" :style="{ backgroundImage: `url(${workbenchImg})` }">
      <button class="back-orb" type="button" aria-label="返回工作台" @click="backToWorkbench">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回工作台</span>
      </button>

      <div class="standalone-page-panel">
        <section class="glass-ledger standalone-hero-card">
          <p class="kicker">成长档案</p>
          <h1>{{ workbenchUser.name }}</h1>
          <p>记录冒险家在 Git Guild 中完成任务、获得反馈和积累贡献的阶段性结果。</p>
          <div class="standalone-progress">
            <div>
              <strong>Level {{ workbenchUser.level }}</strong>
              <span>{{ workbenchUser.xpCurrent }} / {{ workbenchUser.xpTarget }} XP</span>
            </div>
            <i :style="{ width: xpProgress }"></i>
          </div>
        </section>

        <section class="standalone-card-grid">
          <article v-for="stat in workbenchStats.slice(0, 3)" :key="stat.label" class="parchment-panel standalone-stat-card">
            <span>{{ stat.label }}</span>
            <strong>{{ stat.value }}</strong>
          </article>
          <article class="parchment-panel standalone-stat-card">
            <span>已完成任务</span>
            <strong>{{ workbenchUser.completedQuests }}</strong>
          </article>
        </section>

        <section class="standalone-two-column">
          <article class="glass-ledger">
            <p class="kicker">贡献记录</p>
            <h2>最近贡献</h2>
            <ul class="standalone-list">
              <li v-for="contribution in recentContributions" :key="contribution">{{ contribution }}</li>
            </ul>
          </article>

          <article class="glass-ledger">
            <p class="kicker">审核反馈</p>
            <h2>反馈归档</h2>
            <p>通过 {{ approvedFeedbacks.length }} 条，退回修改 {{ returnedFeedbacks.length }} 条。</p>
            <button class="quiet-action" type="button" @click="backToWorkbench">到工作台查看详情</button>
          </article>
        </section>
      </div>
    </section>
  </main>
</template>
