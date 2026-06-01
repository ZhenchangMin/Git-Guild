<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'

import {
  loadNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  notificationStore,
  startNotificationPolling,
  stopNotificationPolling,
} from '../stores/notificationStore'

const open = ref(false)
const root = ref(null)

const unreadCount = computed(() => notificationStore.unreadCount)
const badgeText = computed(() => (unreadCount.value > 99 ? '99+' : String(unreadCount.value)))
const items = computed(() => notificationStore.items)

// 通知类型 → 左侧色条语义（通过 / 待审核 中性 / 需修改 / 驳回）。
const TYPE_TONE = {
  REVIEW_APPROVED: 'tone-approved',
  SUBMISSION_RECEIVED: 'tone-pending',
  REVIEW_CHANGES_REQUESTED: 'tone-changes',
  REVIEW_REJECTED: 'tone-rejected',
}

function toneClass(type) {
  return TYPE_TONE[type] ?? 'tone-pending'
}

function toggle() {
  open.value = !open.value
  if (open.value) {
    loadNotifications()
  }
}

function onItemClick(item) {
  if (item.status === 'UNREAD') {
    markNotificationRead(item.notificationId)
  }
}

function onMarkAll() {
  markAllNotificationsRead()
}

function relativeTime(iso) {
  if (!iso) return ''
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return ''
  const diffSec = Math.round((Date.now() - then) / 1000)
  if (diffSec < 60) return '刚刚'
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)} 分钟前`
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)} 小时前`
  if (diffSec < 604800) return `${Math.floor(diffSec / 86400)} 天前`
  return new Date(iso).toLocaleDateString('zh-CN')
}

function onDocumentClick(event) {
  if (open.value && root.value && !root.value.contains(event.target)) {
    open.value = false
  }
}

function onKeydown(event) {
  if (event.key === 'Escape') open.value = false
}

onMounted(() => {
  startNotificationPolling()
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  stopNotificationPolling()
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div ref="root" class="notification-bell">
    <button
      class="back-orb bell-orb"
      :class="{ 'is-open': open }"
      type="button"
      :aria-label="`通知，${unreadCount} 条未读`"
      :aria-expanded="open"
      @click="toggle"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.7 21a2 2 0 0 1-3.4 0" />
      </svg>
      <span>通知</span>
      <em v-if="unreadCount > 0" class="bell-badge" aria-hidden="true">{{ badgeText }}</em>
    </button>

    <div v-if="open" class="notification-panel" role="dialog" aria-label="站内通知">
      <header class="panel-head">
        <strong>站内通知</strong>
        <button
          class="mark-all"
          type="button"
          :disabled="unreadCount === 0"
          @click="onMarkAll"
        >
          全部已读
        </button>
      </header>

      <p v-if="notificationStore.loading && items.length === 0" class="panel-hint">加载中…</p>
      <p v-else-if="notificationStore.error" class="panel-hint panel-error">{{ notificationStore.error }}</p>
      <p v-else-if="items.length === 0" class="panel-hint">暂无通知</p>

      <ul v-else class="panel-list">
        <li
          v-for="item in items"
          :key="item.notificationId"
          class="panel-item"
          :class="[toneClass(item.type), { unread: item.status === 'UNREAD' }]"
          @click="onItemClick(item)"
        >
          <span v-if="item.status === 'UNREAD'" class="unread-dot" aria-hidden="true"></span>
          <p class="item-content">{{ item.content }}</p>
          <time class="item-time">{{ relativeTime(item.createdAt) }}</time>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.notification-bell {
  position: relative;
}

.bell-orb {
  position: relative;
  top: auto;
  left: auto;
  z-index: auto;
}

/* 面板展开时不再显示悬停 tooltip，避免与面板重叠。 */
.bell-orb.is-open span {
  display: none;
}

.bell-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  background: #e0542f;
  color: #fff;
  font-size: 0.66rem;
  font-style: normal;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 0 0 2px rgba(17, 9, 5, 0.7);
}

.notification-panel {
  position: absolute;
  left: calc(100% + 14px);
  top: 0;
  width: 320px;
  max-height: 60vh;
  overflow-y: auto;
  z-index: 1300;
  border: 1px solid rgba(244, 190, 92, 0.5);
  border-radius: 10px;
  background: rgba(21, 11, 5, 0.96);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.56);
  backdrop-filter: blur(8px);
}

.panel-head {
  position: sticky;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(244, 190, 92, 0.24);
  background: rgba(21, 11, 5, 0.98);
  color: #ffe4ad;
}

.mark-all {
  border: 1px solid rgba(244, 190, 92, 0.5);
  border-radius: 4px;
  padding: 4px 9px;
  color: #ffe4ad;
  background: transparent;
  font-size: 0.76rem;
  cursor: pointer;
  transition: background 140ms ease, border-color 140ms ease;
}

.mark-all:hover:not(:disabled) {
  background: rgba(82, 45, 16, 0.6);
  border-color: var(--gold-bright, #ffcc69);
}

.mark-all:disabled {
  opacity: 0.4;
  cursor: default;
}

.panel-hint {
  margin: 0;
  padding: 22px 14px;
  color: rgba(255, 228, 173, 0.66);
  font-size: 0.84rem;
  text-align: center;
}

.panel-error {
  color: #ffb088;
}

.panel-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.panel-item {
  position: relative;
  padding: 11px 14px 11px 20px;
  border-bottom: 1px solid rgba(244, 190, 92, 0.12);
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: background 140ms ease;
}

.panel-item:last-child {
  border-bottom: none;
}

.panel-item:hover {
  background: rgba(82, 45, 16, 0.34);
}

.panel-item.unread {
  background: rgba(82, 45, 16, 0.2);
}

.panel-item.tone-approved {
  border-left-color: #6fc07a;
}
.panel-item.tone-pending {
  border-left-color: #f4be5c;
}
.panel-item.tone-changes {
  border-left-color: #e8b54a;
}
.panel-item.tone-rejected {
  border-left-color: #e0542f;
}

.unread-dot {
  position: absolute;
  left: 9px;
  top: 16px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ffcc69;
}

.item-content {
  margin: 0 0 4px;
  color: #ffe9c7;
  font-size: 0.85rem;
  line-height: 1.4;
}

.item-time {
  color: rgba(255, 228, 173, 0.54);
  font-size: 0.72rem;
}

@media (max-width: 540px) {
  .notification-panel {
    position: fixed;
    left: 12px;
    right: 12px;
    top: 70px;
    width: auto;
  }
}
</style>
