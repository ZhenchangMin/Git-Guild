<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import hallImg from '../../assets/hall.png'

const router = useRouter()
const hallViewport = ref(null)
const hallTrack = ref(null)
const hallOffset = ref(0)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartOffset = ref(0)

const rooms = [
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

function openRoom(room) {
  router.push({ name: room.routeName })
}

function backToLogin() {
  router.push({ name: 'login' })
}

function openHelp() {
  router.push({ name: 'help' })
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
    <button class="help-orb" type="button" aria-label="打开 Git Guild 使用教程" @click="openHelp">?</button>

    <section class="hall-scene">
      <button class="back-orb" type="button" aria-label="返回登录入口" @click="backToLogin">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回登录入口</span>
      </button>

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
          <img class="hall-image" :src="hallImg" alt="Git Guild Hall" draggable="false" @load="centerHall" />
          <button
            v-for="room in rooms"
            :key="room.id"
            class="hotspot"
            type="button"
            :aria-label="room.label"
            :style="{ left: `${room.left}%`, top: `${room.top}%`, width: `${room.width}%`, height: `${room.height}%` }"
            @click="openRoom(room)"
          >
            <span class="tooltip">
              <strong>{{ room.label }}</strong>
              <small>{{ room.hint }}</small>
            </span>
          </button>
        </div>
      </div>
    </section>
  </main>
</template>
