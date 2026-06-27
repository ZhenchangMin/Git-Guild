<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { leaderboardApi } from '../../api/leaderboardApi'
import leaderboardImg from '../../assets/leaderboard-wall-stone-v4.png'
import { TUTORIAL_COMPLETE_STEP_EVENT } from '../../data/tutorials'
import { getMockLeaderboard } from '../../mocks/leaderboardMockData'

const router = useRouter()

const periodOptions = [
  { value: 'WEEKLY', label: '本周' },
  { value: 'MONTHLY', label: '本月' },
  { value: 'ALL_TIME', label: '全部' },
]

const selectedPeriod = ref('WEEKLY')
const leaderboardRows = ref([])
const generatedAt = ref('')
const isLoading = ref(false)
const loadError = ref('')
const scrollableList = ref(null)
const fixedRows = computed(() => leaderboardRows.value.slice(0, 3))
const scrollableRows = computed(() => leaderboardRows.value.slice(3, 50))

let scrollAlignmentTimer

function unwrap(payload) {
  return payload?.data ?? payload ?? {}
}

function titleForLevel(level) {
  if (level >= 12) return '公会大师'
  if (level >= 8) return '公会精英'
  if (level >= 5) return '代码工匠'
  if (level >= 3) return '前端协作学徒'
  if (level >= 2) return '协作学徒'
  return '见习冒险者'
}

function medalClass(rank) {
  if (rank === 1) return 'medal-gold'
  if (rank === 2) return 'medal-silver'
  if (rank === 3) return 'medal-bronze'
  return ''
}

function formatUpdatedAt(value) {
  if (!value) return '更新时间 --'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '更新时间 --'
  const formatted = new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
  return `更新时间 ${formatted.replaceAll('/', '-')}`
}

function mapLeaderboardItem(item) {
  return {
    rank: item.rank,
    userId: item.userId,
    name: item.username ?? item.name ?? '匿名冒险家',
    level: item.level,
    title: item.title ?? titleForLevel(item.level ?? 1),
    completed: item.completedQuestCount ?? item.completed ?? 0,
  }
}

function useMockLeaderboard(generatedAtValue = '') {
  leaderboardRows.value = getMockLeaderboard(selectedPeriod.value).map(mapLeaderboardItem)
  generatedAt.value = generatedAtValue || new Date().toISOString()
}

async function loadLeaderboard() {
  isLoading.value = true
  loadError.value = ''
  try {
    const payload = await leaderboardApi.xp({ period: selectedPeriod.value, limit: 50 })
    const data = unwrap(payload)
    const items = data.items ?? []
    if (items.length > 0) {
      generatedAt.value = data.generatedAt ?? ''
      leaderboardRows.value = items.slice(0, 50).map(mapLeaderboardItem)
    } else {
      useMockLeaderboard(data.generatedAt)
    }
  } catch {
    useMockLeaderboard()
  } finally {
    isLoading.value = false
    await nextTick()
    resetScrollableRows()
  }
}

function completeTutorialStep(stepId) {
  window.dispatchEvent(
    new CustomEvent(TUTORIAL_COMPLETE_STEP_EVENT, {
      detail: {
        tutorialId: 'leaderboard',
        stepId,
      },
    }),
  )
}

async function selectPeriod(period) {
  if (isLoading.value) return
  if (selectedPeriod.value === period) {
    completeTutorialStep('leaderboard-period')
    return
  }
  selectedPeriod.value = period
  await loadLeaderboard()
  completeTutorialStep('leaderboard-period')
}

async function refreshLeaderboard() {
  if (isLoading.value) return
  await loadLeaderboard()
  completeTutorialStep('leaderboard-refresh')
}

function backToHall() {
  router.push({ name: 'hall' })
}

function resetScrollableRows() {
  if (!scrollableList.value) return
  scrollableList.value.scrollTop = 0
  window.requestAnimationFrame(updateScrollableRowWidths)
}

function updateScrollableRowWidths() {
  const list = scrollableList.value
  if (!list) return

  const listRect = list.getBoundingClientRect()
  if (!listRect.height) return

  list.querySelectorAll('.rank-entry').forEach((row) => {
    const rowRect = row.getBoundingClientRect()
    const rowCenter = rowRect.top + rowRect.height / 2
    const progress = Math.min(1, Math.max(0, (rowCenter - listRect.top) / listRect.height))
    const width = 98.2 + progress * 1.8
    row.style.setProperty('--row-width', `${width.toFixed(3)}%`)
  })
}

function alignScrollableRows() {
  const list = scrollableList.value
  const firstRow = list?.querySelector('.rank-entry')
  if (!list || !firstRow) return

  const rowHeight = firstRow.getBoundingClientRect().height
  if (!rowHeight) return

  const target = Math.round(list.scrollTop / rowHeight) * rowHeight
  if (Math.abs(list.scrollTop - target) < 1) return
  list.scrollTo({ top: target, behavior: 'smooth' })
}

function scheduleScrollAlignment() {
  updateScrollableRowWidths()
  window.clearTimeout(scrollAlignmentTimer)
  scrollAlignmentTimer = window.setTimeout(alignScrollableRows, 120)
}

function handleLeaderboardWheel(event) {
  const list = scrollableList.value
  if (!list || list.scrollHeight <= list.clientHeight) return
  list.scrollTop += event.deltaY
  scheduleScrollAlignment()
}

function setPageScrollLock(locked) {
  document.documentElement.classList.toggle('leaderboard-page-active', locked)
  document.body.classList.toggle('leaderboard-page-active', locked)
}

onMounted(() => {
  setPageScrollLock(true)
  window.addEventListener('resize', updateScrollableRowWidths)
  loadLeaderboard()
})

onBeforeUnmount(() => {
  window.clearTimeout(scrollAlignmentTimer)
  window.removeEventListener('resize', updateScrollableRowWidths)
  setPageScrollLock(false)
})
</script>

<template>
  <main class="app-shell">
    <section
      class="scene work-scene leaderboard-route-mode leaderboard-scene"
      :style="{ backgroundImage: `url(${leaderboardImg})` }"
      @wheel.prevent="handleLeaderboardWheel"
    >
      <button class="back-orb" type="button" aria-label="返回" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回</span>
      </button>

      <section class="leaderboard-inscription" data-tutorial="leaderboard-metrics">
        <header class="leaderboard-heading">
          <h1>公会荣誉榜</h1>
          <div class="leaderboard-toolbar">
            <div class="period-switch" data-tutorial="leaderboard-period" aria-label="排行榜时间范围">
              <button
                v-for="option in periodOptions"
                :key="option.value"
                class="period-option"
                type="button"
                :class="{ active: selectedPeriod === option.value }"
                :disabled="isLoading"
                @click="selectPeriod(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <div class="leaderboard-meta">
              <span class="updated-mark">{{ formatUpdatedAt(generatedAt) }}</span>
              <button
                class="leaderboard-retry"
                type="button"
                data-tutorial="leaderboard-refresh"
                :disabled="isLoading"
                @click="refreshLeaderboard"
              >
                {{ isLoading ? '刷新中' : loadError ? '重试' : '刷新榜单' }}
              </button>
            </div>
          </div>
        </header>

        <section class="engraved-board" data-tutorial="leaderboard-list" aria-label="公会荣誉排行榜">
          <div class="rank-entry rank-head">
            <span>排名</span>
            <span>冒险家</span>
            <span>等级与称号</span>
            <span>完成任务</span>
          </div>

          <p v-if="isLoading" class="leaderboard-status" role="status">正在刷新荣誉榜...</p>
          <p v-else-if="loadError" class="leaderboard-status error" role="alert">{{ loadError }}</p>
          <p v-else-if="leaderboardRows.length === 0" class="leaderboard-status">
            暂无上榜冒险家，完成委托即可刻入荣誉榜。
          </p>

          <template v-else>
            <div class="fixed-ranks" aria-label="排行榜前三名">
              <article
                v-for="row in fixedRows"
                :key="row.rank"
                class="rank-entry"
                :class="medalClass(row.rank)"
                :data-user-id="row.userId"
              >
                <strong class="rank-mark">#{{ row.rank }}</strong>
                <span class="rank-name">{{ row.name }}</span>
                <span class="rank-title">Level {{ row.level }} · {{ row.title }}</span>
                <span class="rank-completed">{{ row.completed }} 项</span>
              </article>
            </div>

            <div
              ref="scrollableList"
              class="scrollable-ranks"
              data-tutorial="leaderboard-scroll"
              aria-label="排行榜第四至第五十名"
              tabindex="0"
              @scroll="scheduleScrollAlignment"
            >
              <article
                v-for="row in scrollableRows"
                :key="row.rank"
                class="rank-entry"
                :data-user-id="row.userId"
              >
                <strong class="rank-mark">#{{ row.rank }}</strong>
                <span class="rank-name">{{ row.name }}</span>
                <span class="rank-title">Level {{ row.level }} · {{ row.title }}</span>
                <span class="rank-completed">{{ row.completed }} 项</span>
              </article>
            </div>
          </template>
        </section>
      </section>
    </section>
  </main>
</template>

<style scoped>
:global(html.leaderboard-page-active),
:global(body.leaderboard-page-active) {
  height: 100%;
  overflow: hidden;
  overscroll-behavior: none;
}

.app-shell {
  height: 100svh;
  overflow: hidden;
}

.leaderboard-scene {
  --art-height: max(100svh, 56.28vw);
  --art-top: min(0px, calc(50svh - 28.14vw));
  --board-width: max(35.5vw, 63.1svh);
  --heading-top: calc(var(--art-top) + max(29.97svh, 16.87vw));
  --heading-height: max(6.16svh, 3.47vw);
  --fixed-row-height: max(5.34svh, 3vw);
  --scroll-row-height: max(5.67svh, 3.19vw);
  --scroll-window-height: max(28.37svh, 15.96vw);
  --ranking-content-height: max(44.39svh, 24.98vw);
  display: block;
  width: 100vw;
  height: 100svh;
  min-height: 100svh;
  overflow: hidden;
  overscroll-behavior: none;
  padding: 0;
  background-position: center;
  background-size: cover;
}

.leaderboard-scene::before {
  background:
    linear-gradient(90deg, rgba(16, 8, 3, 0.2), transparent 25%, transparent 75%, rgba(16, 8, 3, 0.2)),
    linear-gradient(180deg, rgba(10, 5, 2, 0.08), transparent 25%, transparent 82%, rgba(10, 5, 2, 0.18));
}

.leaderboard-inscription {
  position: absolute;
  top: var(--heading-top);
  left: 50%;
  z-index: 1;
  width: var(--board-width);
  padding: 0;
  color: #080808;
  transform: translateX(-50%);
  text-shadow: 0 1px 1px rgba(255, 255, 255, 0.32);
}

.leaderboard-heading {
  display: grid;
  align-content: center;
  gap: 3px;
  height: var(--heading-height);
  margin: 0;
}

.leaderboard-heading h1 {
  margin: 0;
  color: #050505;
  font-family: var(--font-display), 'Songti SC', serif;
  font-size: 2.08rem;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
  pointer-events: none;
  text-align: center;
  transform: translateY(24px);
  text-shadow:
    0 1px 0 rgba(255, 255, 255, 0.42),
    0 2px 5px rgba(25, 21, 20, 0.2);
}

.leaderboard-toolbar {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 95.5%;
  min-height: 27px;
  margin-inline: auto;
}

.period-switch {
  display: inline-flex;
  gap: 2px;
  border: 1px solid rgba(107, 66, 15, 0.42);
  padding: 2px;
  background: rgba(168, 112, 29, 0.08);
}

.period-option {
  min-width: 48px;
  border: 0;
  padding: 4px 9px;
  color: #080808;
  background: transparent;
  font-family: var(--font-title, inherit);
  font-size: 0.82rem;
  font-weight: 800;
  cursor: pointer;
  transition: color 160ms ease, background 160ms ease, box-shadow 160ms ease;
}

.period-option:hover,
.period-option.active {
  color: #fff1c4;
  background: linear-gradient(180deg, #ad7627, #75460e);
  box-shadow: inset 0 0 0 1px rgba(255, 230, 164, 0.34);
}

.period-option:disabled {
  cursor: wait;
  opacity: 0.62;
}

.leaderboard-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.updated-mark {
  color: #080808;
  font-family: var(--font-title, inherit);
  font-size: 0.82rem;
  font-weight: 800;
  white-space: nowrap;
}

.engraved-board {
  display: block;
}

.rank-entry {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) minmax(168px, 1.18fr) 64px;
  align-items: center;
  gap: 6px;
  min-height: 0;
  width: var(--row-width, 100%);
  border: 0;
  margin-inline: auto;
  padding: 0 5px;
  color: #080808;
  background: transparent;
}

.rank-head {
  --row-width: 95.5%;
  height: var(--fixed-row-height);
  color: #080808;
  font-family: var(--font-display);
  font-size: 0.82rem;
  font-weight: 800;
}

.fixed-ranks .rank-entry {
  height: var(--fixed-row-height);
}

.fixed-ranks .rank-entry,
.scrollable-ranks .rank-entry {
  transform: translateX(4px);
}

.fixed-ranks .rank-entry:nth-child(1) {
  --row-width: 96.2%;
}

.fixed-ranks .rank-entry:nth-child(2) {
  --row-width: 97%;
}

.fixed-ranks .rank-entry:nth-child(3) {
  --row-width: 97.8%;
}

.scrollable-ranks {
  height: var(--scroll-window-height);
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: none;
  scroll-snap-type: y mandatory;
}

.scrollable-ranks::-webkit-scrollbar {
  display: none;
}

.scrollable-ranks:focus-visible {
  outline: 1px solid rgba(115, 69, 15, 0.58);
  outline-offset: 2px;
}

.scrollable-ranks .rank-entry {
  --row-width: 98.4%;
  height: var(--scroll-row-height);
  scroll-snap-align: start;
  scroll-snap-stop: always;
}

.rank-mark {
  color: #080808;
  font-family: var(--font-display);
  font-size: 1.05rem;
}

.rank-name {
  overflow: hidden;
  color: #080808;
  font-family: var(--font-title, inherit);
  font-size: 1.06rem;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-title {
  color: #080808;
  font-family: var(--font-title, inherit);
  font-size: 0.96rem;
  font-weight: 800;
  white-space: nowrap;
}

.rank-completed {
  justify-self: end;
  color: #080808;
  font-size: 0.96rem;
  font-weight: 800;
}

.rank-entry.medal-gold {
  color: #986100;
  background: linear-gradient(90deg, rgba(208, 148, 28, 0.28), rgba(208, 148, 28, 0.08) 68%, transparent);
}

.rank-entry.medal-gold .rank-mark,
.rank-entry.medal-gold .rank-name {
  color: #986100;
  font-size: 1.24rem;
  text-shadow: 0 1px 0 rgba(255, 232, 157, 0.56);
}

.rank-entry.medal-gold .rank-title,
.rank-entry.medal-gold .rank-completed {
  color: #704706;
  font-size: 1.08rem;
  font-weight: 900;
}

.rank-entry.medal-silver {
  background: linear-gradient(90deg, rgba(108, 121, 130, 0.2), rgba(108, 121, 130, 0.06) 68%, transparent);
}

.rank-entry.medal-silver .rank-mark,
.rank-entry.medal-silver .rank-name {
  color: #4d5c68;
  font-size: 1.17rem;
  text-shadow: 0 1px 0 rgba(236, 242, 242, 0.64);
}

.rank-entry.medal-silver .rank-title,
.rank-entry.medal-silver .rank-completed {
  color: #46535d;
  font-size: 1.03rem;
  font-weight: 900;
}

.rank-entry.medal-bronze {
  background: linear-gradient(90deg, rgba(174, 82, 39, 0.22), rgba(174, 82, 39, 0.06) 68%, transparent);
}

.rank-entry.medal-bronze .rank-mark,
.rank-entry.medal-bronze .rank-name {
  color: #99461f;
  font-size: 1.1rem;
  text-shadow: 0 1px 0 rgba(238, 174, 132, 0.5);
}

.rank-entry.medal-bronze .rank-title,
.rank-entry.medal-bronze .rank-completed {
  color: #75371c;
  font-size: 0.98rem;
  font-weight: 900;
}

.leaderboard-status {
  margin: 0;
  height: var(--ranking-content-height);
  padding: 24px 8px;
  text-align: center;
  color: #080808;
  font-size: 1.02rem;
  font-weight: 700;
}

.leaderboard-status.error {
  color: #8c3d28;
}

.leaderboard-retry {
  border: 1px solid rgba(103, 61, 10, 0.72);
  padding: 4px 9px;
  color: #fff1c4;
  background: linear-gradient(180deg, #b87d2b, #75450e);
  font-family: var(--font-display);
  font-size: 0.78rem;
  font-weight: 900;
  cursor: pointer;
  transition: filter 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.leaderboard-retry:hover {
  filter: brightness(1.08);
  transform: translateY(-1px);
  box-shadow: 0 0 12px rgba(126, 75, 14, 0.24);
}

.leaderboard-retry:disabled {
  cursor: wait;
  filter: grayscale(0.2);
  opacity: 0.66;
  transform: none;
}

@media (max-height: 800px) {
  .leaderboard-heading {
    gap: 2px;
  }

  .leaderboard-heading h1 {
    font-size: 1.45rem;
  }

  .leaderboard-toolbar {
    min-height: 19px;
  }

  .period-option {
    min-width: 43px;
    padding: 3px 7px;
    font-size: 0.74rem;
  }

  .updated-mark {
    font-size: 0.7rem;
  }

  .leaderboard-retry,
  .rank-head {
    font-size: 0.72rem;
  }

  .rank-name,
  .rank-mark {
    font-size: 0.94rem;
  }

  .rank-title,
  .rank-completed {
    font-size: 0.85rem;
  }
}
</style>
