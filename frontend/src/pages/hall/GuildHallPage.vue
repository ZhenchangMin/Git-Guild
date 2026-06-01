<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import hallImg from '../../assets/hall.png'
import NotificationBell from '../../components/NotificationBell.vue'
import { questCommissions } from '../../data/questBoard'
import { clearSession, hasLoginSession, sessionStore } from '../../stores/sessionStore'

const router = useRouter()

// 仅登录且非访客时展示通知中心——访客没有可投递的关键状态变化。
const showNotificationBell = computed(() => hasLoginSession() && sessionStore.role !== 'VISITOR')

const openQuestCount = computed(
  () => questCommissions.filter((quest) => quest.status === '可接取').length || questCommissions.length,
)
const hallViewport = ref(null)
const hallTrack = ref(null)
const hallOffset = ref(0)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartOffset = ref(0)

const baseRooms = [
  {
    id: 'submission',
    label: '提交柜台',
    hint: '登记成果并提交审核',
    routeName: 'submission-counter',
    left: 7.2,
    top: 17,
    width: 11.8,
    height: 56,
  },
  {
    id: 'quest',
    label: '悬赏任务板',
    hint: '浏览可接取的 Issue 关联任务',
    routeName: 'quest-board',
    left: 23.6,
    top: 16,
    width: 13.8,
    height: 45,
  },
  {
    id: 'desk',
    label: '前台向导',
    hint: '新手引导、仓库接入和异常求助',
    routeName: 'repository-sync',
    left: 43.8,
    top: 37,
    width: 16.2,
    height: 50,
  },
  {
    id: 'workbench',
    label: '工作台',
    hint: '任务、仓库、分支、commit 和 PR',
    routeName: 'adventurer-workbench',
    left: 64.3,
    top: 18,
    width: 14.5,
    height: 53,
  },
  {
    id: 'leaderboard',
    label: '排行榜墙',
    hint: '展示成长记录和阶段性成果',
    routeName: 'leaderboard',
    left: 82.2,
    top: 17,
    width: 11.4,
    height: 56,
  },
]

const rooms = computed(() =>
  baseRooms.map((room) =>
    room.id === 'workbench'
      ? {
          ...room,
          routeName: sessionStore.role === 'MAINTAINER' ? 'maintainer-workbench' : 'adventurer-workbench',
          hint:
            sessionStore.role === 'MAINTAINER'
              ? '接取任务、提交成果，并进入成果审核台'
              : room.hint,
        }
      : room,
  ),
)

function clampHallOffset(offset) {
  const viewport = hallViewport.value
  const track = hallTrack.value
  if (!viewport || !track) return offset
  const min = Math.min(0, viewport.clientWidth - track.offsetWidth)
  return Math.max(min, Math.min(0, offset))
}

function centerHall() {
  const viewport = hallViewport.value
  const track = hallTrack.value
  if (!viewport || !track) return
  hallOffset.value = clampHallOffset((viewport.clientWidth - track.offsetWidth) / 2)
}

function beginHallDrag(event) {
  if (event.target.closest('.hotspot')) return
  isDragging.value = true
  dragStartX.value = event.clientX
  dragStartOffset.value = hallOffset.value
  hallViewport.value?.setPointerCapture(event.pointerId)
}

function dragHall(event) {
  if (!isDragging.value) return
  const delta = event.clientX - dragStartX.value
  hallOffset.value = clampHallOffset(dragStartOffset.value + delta)
}

function endHallDrag(event) {
  if (!isDragging.value) return
  isDragging.value = false
  hallViewport.value?.releasePointerCapture(event.pointerId)
}

function openRoute(routeName) {
  router.push({ name: routeName })
}

function openRoom(room) {
  openRoute(room.routeName)
}

function switchAccount() {
  clearSession()
  router.push({ name: 'login' })
}

function logout() {
  clearSession()
  router.push({ name: 'login' })
}

onMounted(() => {
  window.addEventListener('resize', centerHall)
  nextTick(centerHall)
})

onUnmounted(() => {
  window.removeEventListener('resize', centerHall)
})
</script>

<template>
  <main class="app-shell">
    <button class="help-orb" type="button" aria-label="打开 Git Guild 使用教程" @click="openRoute('help')">?</button>

    <section class="hall-scene">
      <div class="session-action-stack" aria-label="账号与成长入口">
        <NotificationBell v-if="showNotificationBell" />
        <button class="back-orb" type="button" aria-label="切换账号" @click="switchAccount">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M15 6 9 12l6 6" />
          </svg>
          <span>切换账号</span>
        </button>
        <button class="back-orb growth-orb" type="button" aria-label="打开成长档案" @click="openRoute('growth-profile')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 3 15 9l6 .8-4.5 4.3 1.1 6.1L12 17.2 6.4 20.2l1.1-6.1L3 9.8 9 9z" />
          </svg>
          <span>成长档案</span>
        </button>
        <button class="back-orb logout-orb" type="button" aria-label="退出登录" @click="logout">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10 7V5a2 2 0 0 1 2-2h6v18h-6a2 2 0 0 1-2-2v-2" />
            <path d="M3 12h10" />
            <path d="m6 9-3 3 3 3" />
          </svg>
          <span>退出登录</span>
        </button>
      </div>

      <div
        ref="hallViewport"
        class="hall-viewport"
        :class="{ dragging: isDragging }"
        @pointerdown="beginHallDrag"
        @pointermove="dragHall"
        @pointerup="endHallDrag"
        @pointercancel="endHallDrag"
      >
        <div ref="hallTrack" class="hall-track" :style="{ transform: `translateX(${hallOffset}px)` }">
          <img class="hall-image" :src="hallImg" alt="Git Guild 公会大厅" draggable="false" @load="centerHall" />
          <button
            v-for="room in rooms"
            :key="room.id"
            class="hotspot"
            :class="{ 'has-shape': room.id === 'quest' }"
            type="button"
            :aria-label="room.label"
            :style="{ left: `${room.left}%`, top: `${room.top}%`, width: `${room.width}%`, height: `${room.height}%` }"
            @click="openRoom(room)"
          >
            <svg
              v-if="room.id === 'quest'"
              class="hotspot-shape"
              viewBox="0 0 100 100"
              preserveAspectRatio="none"
              aria-hidden="true"
            >
              <path d="M2.5 13 L6 13 L6 4 Q31 1 56 4 L56 13 L60 13 L60 84 L2.5 84 Z" />
            </svg>

            <span class="tooltip" :class="{ 'tooltip-quest': room.id === 'quest' }">
              <strong>{{ room.label }}</strong>
              <small>{{ room.hint }}</small>
              <em v-if="room.id === 'quest'" class="tooltip-cta">{{ openQuestCount }} 份委托待接取</em>
            </span>
          </button>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.growth-orb {
  border-color: rgba(255, 219, 145, 0.56);
  background:
    linear-gradient(135deg, rgba(88, 48, 18, 0.86), rgba(20, 10, 3, 0.78)),
    radial-gradient(circle at 22% 20%, rgba(255, 219, 145, 0.22), transparent 0 42%);
}

.growth-orb span {
  color: #ffe2a0;
}

.growth-orb:hover,
.growth-orb:focus-visible {
  border-color: rgba(255, 226, 160, 0.88);
  box-shadow: 0 0 26px rgba(255, 197, 89, 0.34);
}

.hotspot.has-shape::after {
  content: none;
}

.hotspot-shape {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
  pointer-events: none;
}

.hotspot-shape path {
  fill: rgba(255, 220, 132, 0.04);
  stroke: rgba(255, 211, 116, 0.32);
  stroke-width: 2;
  vector-effect: non-scaling-stroke;
  stroke-linejoin: round;
  stroke-linecap: round;
  filter: drop-shadow(0 0 5px rgba(255, 190, 82, 0.18));
  transition: stroke 220ms ease, fill 220ms ease, filter 220ms ease;
  animation: quest-board-breathe 4.2s ease-in-out infinite;
}

.hotspot.has-shape:hover .hotspot-shape path,
.hotspot.has-shape:focus-visible .hotspot-shape path {
  fill: rgba(255, 220, 132, 0.12);
  stroke: rgba(255, 217, 128, 0.92);
  filter: drop-shadow(0 0 10px rgba(255, 190, 82, 0.55));
  animation: none;
}

@keyframes quest-board-breathe {
  0%,
  100% {
    stroke: rgba(255, 211, 116, 0.3);
    filter: drop-shadow(0 0 4px rgba(255, 190, 82, 0.14));
  }
  50% {
    stroke: rgba(255, 217, 128, 0.6);
    filter: drop-shadow(0 0 9px rgba(255, 190, 82, 0.4));
  }
}

.tooltip-quest {
  min-width: 218px;
}

.tooltip-cta {
  margin-top: 4px;
  padding-top: 7px;
  border-top: 1px solid rgba(244, 190, 92, 0.32);
  color: #ffd98a;
  font-size: 0.8rem;
  font-style: normal;
  font-weight: 700;
}

@media (prefers-reduced-motion: reduce) {
  .hotspot-shape path {
    animation: none;
  }
}
</style>
