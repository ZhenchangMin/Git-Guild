<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import deskImg from '../../assets/desk.webp'
import doorImg from '../../assets/door.webp'
import hallImg from '../../assets/hall.webp'
import leaderboardImg from '../../assets/leader board wall.webp'
import operationRoomImg from '../../assets/operation room.webp'
import profileArchiveBg from '../../assets/profile-archive-bg.webp'
import questBoardImg from '../../assets/quest board.webp'
import submissionCounterImg from '../../assets/submission-counter-clerk-v0.webp'
import workbenchImg from '../../assets/workbench.webp'
import { TUTORIAL_LAUNCH_QUERY, tutorials } from '../../data/tutorials'

const route = useRoute()
const router = useRouter()

const HELP_BACKGROUNDS = {
  login: doorImg,
  'forgot-password': doorImg,
  hall: hallImg,
  'quest-board': questBoardImg,
  'quest-detail': questBoardImg,
  'front-desk': deskImg,
  'adventurer-workbench': workbenchImg,
  'maintainer-workbench': deskImg,
  'repository-sync': deskImg,
  'maintainer-publish': workbenchImg,
  'maintainer-review': workbenchImg,
  'submission-counter': submissionCounterImg,
  leaderboard: leaderboardImg,
  profile: profileArchiveBg,
  'growth-profile': profileArchiveBg,
  'admin-review': operationRoomImg,
  'admin-exceptions': operationRoomImg,
  'admin-taxonomy': operationRoomImg,
}

const HELP_BACKGROUND_BY_PATH = [
  [/^\/login$/, 'login'],
  [/^\/forgot-password$/, 'forgot-password'],
  [/^\/hall$/, 'hall'],
  [/^\/quests\/[^/]+$/, 'quest-detail'],
  [/^\/quests$/, 'quest-board'],
  [/^\/front-desk$/, 'front-desk'],
  [/^\/workbench$/, 'adventurer-workbench'],
  [/^\/maintainer$/, 'maintainer-workbench'],
  [/^\/maintainer\/publish$/, 'maintainer-publish'],
  [/^\/maintainer\/reviews$/, 'maintainer-review'],
  [/^\/repositories\/sync$/, 'repository-sync'],
  [/^\/submissions$/, 'submission-counter'],
  [/^\/leaderboard$/, 'leaderboard'],
  [/^\/profile$/, 'profile'],
  [/^\/growth$/, 'growth-profile'],
  [/^\/admin(?:\/review)?$/, 'admin-review'],
  [/^\/admin\/exceptions$/, 'admin-exceptions'],
  [/^\/admin\/taxonomy$/, 'admin-taxonomy'],
]

function inferSourceRouteFromReturnTo(path) {
  const pathname = path.split('?')[0].split('#')[0]
  const match = HELP_BACKGROUND_BY_PATH.find(([pattern]) => pattern.test(pathname))
  return match?.[1] ?? ''
}

// 账号已合并：一个公会成员同时能做两件事，无需切换身份。
const memberCan = [
  { icon: '⚔', name: '接任务', desc: '接取别人发布的委托，完成开发并提交成果，赚取经验值与徽章。' },
  { icon: '📜', name: '发委托', desc: '把自己仓库的 Issue 发布成悬赏，并审核他人提交的成果。' },
]

// 接任务主线：从接取到结算。
const acceptSteps = [
  { where: '任务板', text: '筛选并打开一个委托，确认完成标准后接取。' },
  { where: '工作台', text: '创建任务分支，改完后提交 commit 并发起 PR。' },
  { where: '提交柜台', text: '关联 PR、填写成果说明并上传佐证，提交审核。' },
  { where: '成长档案', text: '通过后自动结算经验值与徽章。' },
]

// 发委托主线：发布与验收。
const publishSteps = [
  { where: '仓库接入', text: '先导入或同步要发布的 Gitea 仓库，让平台读取其 Issue。' },
  { where: '发布委托', text: '选定 Issue，填写完成标准与奖励后提交管理员审核。' },
  { where: '审核台', text: '收到成果后核对 PR 与佐证，给出通过 / 退回 / 驳回。' },
]

// 常见问题：覆盖最高频的几类。
const faqs = [
  {
    q: '提交时提示「请求参数不合法 / PR 失败」？',
    a: '多半是任务分支还没有改动或未发起 PR。先在工作台完成 commit 并创建 PR，再回提交柜台提交。',
  },
  {
    q: '委托被退回（已要求修改）怎么办？',
    a: '先读反馈，在工作台更新分支与 PR，再回提交柜台重新提交即可。',
  },
  {
    q: '能接取自己发布的委托吗？',
    a: '不能。成员虽然同时能发布和接取，但不能接自己发的委托。请把它留给其他成员，你可以在委托人工作台跟踪进度。',
  },
  {
    q: '点「接取委托」提示已被别人接取？',
    a: '委托刚被他人抢先接取、任务板还没刷新。返回任务板即可看到最新可接取的委托。',
  },
]

const returnTo = computed(() => {
  const value = route.query.returnTo
  return typeof value === 'string' ? value : ''
})

const sourceRouteName = computed(() => {
  const value = route.query.sourceRoute
  if (typeof value === 'string' && HELP_BACKGROUNDS[value]) return value
  return inferSourceRouteFromReturnTo(returnTo.value)
})

const helpBackground = computed(() => HELP_BACKGROUNDS[sourceRouteName.value] ?? hallImg)

const contextualTutorialId = computed(() => {
  const value = route.query.tutorialId
  return typeof value === 'string' && tutorials[value] ? value : ''
})

const canStartTutorial = computed(() => Boolean(returnTo.value && contextualTutorialId.value))

function createReturnLocation(extraQuery = {}) {
  if (!returnTo.value) return null
  const target = new URL(returnTo.value, window.location.origin)
  if (target.origin !== window.location.origin) return null

  Object.entries(extraQuery).forEach(([key, value]) => {
    target.searchParams.set(key, value)
  })

  return {
    path: target.pathname,
    query: Object.fromEntries(target.searchParams.entries()),
    hash: target.hash,
  }
}

function isPreviousHistoryEntry(location) {
  const previousPath = window.history.state?.back
  return typeof previousPath === 'string' && previousPath === router.resolve(location).fullPath
}

function backToPreviousPage() {
  const target = createReturnLocation()
  if (!target) {
    router.replace({ name: 'hall' })
    return
  }

  if (isPreviousHistoryEntry(target)) {
    router.back()
    return
  }

  router.replace(target)
}

function startCurrentPageTutorial() {
  if (!canStartTutorial.value) return
  const target = createReturnLocation({ [TUTORIAL_LAUNCH_QUERY]: contextualTutorialId.value })
  if (target) router.replace(target)
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene help-route-mode" :style="{ backgroundImage: `url(${helpBackground})` }">
      <button class="back-orb" type="button" aria-label="返回" @click="backToPreviousPage">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回</span>
      </button>

      <div class="standalone-page-panel compact">
        <section class="glass-ledger standalone-hero-card">
          <p class="kicker">公会指南</p>
          <h1>Git Guild 使用手册</h1>
          <p>一个账号，既能接任务也能发委托。沿对应主线走，卡住了看常见问题。</p>
        </section>

        <section class="parchment-panel help-body">
          <!-- 成员能做什么 -->
          <div class="help-block">
            <p class="kicker">一个账号，两件事</p>
            <h2>成员可以做什么？</h2>
            <div class="role-grid">
              <article v-for="item in memberCan" :key="item.name" class="role-card">
                <span class="role-icon" aria-hidden="true">{{ item.icon }}</span>
                <h3>{{ item.name }}</h3>
                <p>{{ item.desc }}</p>
              </article>
            </div>
          </div>

          <!-- 接任务主线 -->
          <div class="help-block">
            <p class="kicker">接任务主线</p>
            <h2>从接取到结算</h2>
            <ol class="route-list">
              <li v-for="(step, idx) in acceptSteps" :key="idx">
                <span class="route-where">{{ step.where }}</span>
                <span class="route-text">{{ step.text }}</span>
              </li>
            </ol>
          </div>

          <!-- 发委托主线 -->
          <div class="help-block">
            <p class="kicker">发委托主线</p>
            <h2>从发布到验收</h2>
            <ol class="route-list">
              <li v-for="(step, idx) in publishSteps" :key="idx">
                <span class="route-where">{{ step.where }}</span>
                <span class="route-text">{{ step.text }}</span>
              </li>
            </ol>
          </div>

          <!-- 常见问题 -->
          <div class="help-block">
            <p class="kicker">常见问题</p>
            <h2>遇到卡点先看这里</h2>
            <dl class="faq-list">
              <div v-for="(faq, idx) in faqs" :key="idx" class="faq-item">
                <dt>{{ faq.q }}</dt>
                <dd>{{ faq.a }}</dd>
              </div>
            </dl>
          </div>

          <div class="help-actions">
            <button
              class="quiet-action"
              type="button"
              :disabled="!canStartTutorial"
              title="返回来源页面并打开对应教程"
              @click="startCurrentPageTutorial"
            >
              查看本页面教程
            </button>
            <button class="quiet-action" type="button" @click="router.push({ name: 'front-desk' })">
              咨询 AI 向导
            </button>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.help-body {
  display: grid;
  gap: 26px;
}

.help-block h2 {
  margin: 2px 0 0;
  color: #4c250c;
  font-size: 1.3rem;
}

/* 身份卡片 */
.role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.role-card {
  display: grid;
  gap: 6px;
  border: 1px solid rgba(90, 48, 20, 0.3);
  border-radius: 8px;
  padding: 14px;
  background: rgba(255, 248, 228, 0.5);
}

.role-icon {
  font-size: 1.5rem;
}

.role-card h3 {
  margin: 0;
  color: #4c250c;
  font-size: 1.05rem;
}

.role-card p {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.88rem;
  line-height: 1.5;
}

/* 路线步骤 */
.route-list {
  display: grid;
  gap: 10px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  counter-reset: route;
}

.route-list li {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  border-left: 3px solid rgba(135, 80, 18, 0.55);
  border-radius: 0 6px 6px 0;
  padding: 11px 14px 11px 44px;
  background: rgba(255, 248, 228, 0.46);
  counter-increment: route;
}

.route-list li::before {
  position: absolute;
  left: 10px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(180deg, #c98b3c, #8a5012);
  color: #fff6e2;
  font-size: 0.82rem;
  font-weight: 700;
  content: counter(route);
}

.route-where {
  flex: 0 0 auto;
  border: 1px solid rgba(135, 80, 18, 0.4);
  border-radius: 999px;
  padding: 3px 10px;
  color: #6e3a10;
  font-size: 0.74rem;
  font-weight: 700;
  white-space: nowrap;
  background: rgba(214, 142, 56, 0.16);
}

.route-text {
  color: #3d2515;
  font-size: 0.92rem;
  line-height: 1.5;
}

/* 常见问题 */
.faq-list {
  display: grid;
  gap: 12px;
  margin: 14px 0 0;
}

.faq-item {
  border-bottom: 1px solid rgba(90, 48, 20, 0.24);
  padding-bottom: 12px;
}

.faq-item dt {
  color: #4c250c;
  font-weight: 700;
  font-size: 0.96rem;
}

.faq-item dd {
  margin: 6px 0 0;
  color: var(--ink-soft);
  font-size: 0.9rem;
  line-height: 1.55;
}

.help-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 4px;
}

.help-actions .quiet-action:disabled {
  opacity: 0.48;
  cursor: default;
}

@media (max-width: 720px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
