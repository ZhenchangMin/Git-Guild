<script setup>
import { useRouter } from 'vue-router'

import profileImg from '../../assets/user profile.png'
import { workbenchUser } from '../../data/workbench'

const router = useRouter()

function backToWorkbench() {
  router.push({ name: 'adventurer-workbench' })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene profile-route-mode" :style="{ backgroundImage: `url(${profileImg})` }">
      <button class="back-orb" type="button" aria-label="返回工作台" @click="backToWorkbench">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回工作台</span>
      </button>

      <div class="standalone-page-panel compact">
        <section class="glass-ledger standalone-hero-card">
          <p class="kicker">个人档案</p>
          <h1>{{ workbenchUser.name }}</h1>
          <p>这里后续承载头像、昵称、技术栈、简介和可展示项目经历。当前先保留为 P1 独立页面。</p>
        </section>

        <section class="standalone-two-column">
          <article class="parchment-panel">
            <p class="kicker">身份</p>
            <h2>{{ workbenchUser.role }}</h2>
            <dl class="standalone-dl">
              <div>
                <dt>等级</dt>
                <dd>Level {{ workbenchUser.level }}</dd>
              </div>
              <div>
                <dt>经验</dt>
                <dd>{{ workbenchUser.xpCurrent }} / {{ workbenchUser.xpTarget }} XP</dd>
              </div>
              <div>
                <dt>已完成任务</dt>
                <dd>{{ workbenchUser.completedQuests }}</dd>
              </div>
            </dl>
          </article>

          <article class="glass-ledger">
            <p class="kicker">后续编辑项</p>
            <h2>可扩展信息</h2>
            <ul class="standalone-list">
              <li>头像上传与裁剪</li>
              <li>技术栈标签</li>
              <li>个人简介</li>
              <li>公开贡献展示</li>
            </ul>
          </article>
        </section>
      </div>
    </section>
  </main>
</template>
