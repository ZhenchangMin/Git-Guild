<script setup>
import { useRouter } from 'vue-router'

import hallImg from '../../assets/hall.webp'

const router = useRouter()

const helpSteps = [
  '从悬赏任务板筛选适合自己的任务。',
  '进入任务详情确认完成标准、技术栈和预计工作量。',
  '接取任务后进入工作台创建分支、上传 commit 并发起 PR。',
  '到提交柜台登记成果，等待委托人审核。',
  '审核通过后查看成长档案和贡献记录。',
]

function backToHall() {
  router.push({ name: 'hall' })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene help-route-mode" :style="{ backgroundImage: `url(${hallImg})` }">
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回公会大厅</span>
      </button>

      <div class="standalone-page-panel compact">
        <section class="glass-ledger standalone-hero-card">
          <p class="kicker">帮助说明</p>
          <h1>Git Guild 新手路径</h1>
          <p>帮助页用于解释平台主流程。后续可继续拆成 FAQ、角色说明和异常处理说明。</p>
        </section>

        <section class="parchment-panel">
          <p class="kicker">Quick Start</p>
          <h2>推荐操作顺序</h2>
          <ol class="standalone-list ordered">
            <li v-for="step in helpSteps" :key="step">{{ step }}</li>
          </ol>
          <button class="primary-action" type="button" @click="router.push({ name: 'quest-board' })">
            前往悬赏任务板
          </button>
        </section>
      </div>
    </section>
  </main>
</template>
