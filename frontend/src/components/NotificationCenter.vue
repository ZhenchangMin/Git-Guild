<script setup>
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'

import { sessionStore } from '../stores/sessionStore'
import {
  closeNotification,
  dismissToast,
  notificationStore,
  openNotification,
  startNotificationPolling,
  stopNotificationPolling,
} from '../stores/notificationStore'
import { notificationMeta } from '../data/notificationMeta'

const router = useRouter()

const toasts = computed(() => notificationStore.toasts)
const selected = computed(() => notificationStore.selected)
const selectedMeta = computed(() => (selected.value ? notificationMeta(selected.value.type) : null))

function metaOf(type) {
  return notificationMeta(type)
}

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function onToastClick(item) {
  openNotification(item)
}

function takeAction() {
  const action = selectedMeta.value?.action
  closeNotification()
  if (action?.route) {
    router.push({ name: action.route }).catch(() => {})
  }
}

// 轮询所有权集中在全局中心：登录后开，登出后停（铃铛只读不再控制轮询）。
onMounted(() => {
  if (sessionStore.token) startNotificationPolling()
})
watch(
  () => sessionStore.token,
  (token) => {
    if (token) startNotificationPolling()
    else stopNotificationPolling()
  },
)
onUnmounted(stopNotificationPolling)
</script>

<template>
  <teleport to="body">
    <!-- 角落瞬时提示：右下角堆叠，过几秒自动消失，点击打开详情 -->
    <div class="toast-stack" aria-live="polite">
      <transition-group name="toast-pop">
        <button
          v-for="toast in toasts"
          :key="toast.notificationId"
          type="button"
          class="toast-card"
          :class="metaOf(toast.type).tone"
          @click="onToastClick(toast)"
        >
          <span class="toast-icon" aria-hidden="true">{{ metaOf(toast.type).icon }}</span>
          <span class="toast-body">
            <strong>{{ metaOf(toast.type).title }}</strong>
            <small>{{ toast.content }}</small>
          </span>
          <span
            class="toast-close"
            aria-label="关闭"
            role="button"
            @click.stop="dismissToast(toast.notificationId)"
          >×</span>
        </button>
      </transition-group>
    </div>

    <!-- 详情弹窗：展开的信笺，含细节与可执行动作 -->
    <transition name="letter-pop">
      <div
        v-if="selected"
        class="letter-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="letter-title"
        @click.self="closeNotification"
      >
        <div class="letter-card" :class="selectedMeta.tone">
          <button class="letter-close" type="button" aria-label="关闭" @click="closeNotification">×</button>
          <div class="letter-seal" aria-hidden="true">{{ selectedMeta.icon }}</div>
          <p class="letter-kicker">公会信使 · {{ selectedMeta.audience || '站内通知' }}</p>
          <h2 id="letter-title">{{ selectedMeta.title }}</h2>

          <p class="letter-content">{{ selected.content }}</p>

          <dl class="letter-meta">
            <div>
              <dt>送达时间</dt>
              <dd>{{ formatTime(selected.createdAt) }}</dd>
            </div>
            <div v-if="selected.relatedType">
              <dt>关联</dt>
              <dd>{{ selected.relatedType }} #{{ selected.relatedId }}</dd>
            </div>
          </dl>

          <div class="letter-actions">
            <button class="quiet-action" type="button" @click="closeNotification">关闭</button>
            <button
              v-if="selectedMeta.action"
              class="primary-action"
              type="button"
              @click="takeAction"
            >{{ selectedMeta.action.label }} →</button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
/* ── 角落瞬时提示 ───────────────────────────────────────── */
.toast-stack {
  position: fixed;
  left: 20px;
  top: 20px;
  z-index: 2000;
  display: grid;
  gap: 10px;
  width: min(340px, calc(100vw - 32px));
  pointer-events: none;
}

.toast-card {
  pointer-events: auto;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 10px;
  width: 100%;
  text-align: left;
  border: 1px solid rgba(238, 184, 91, 0.5);
  border-left-width: 4px;
  border-radius: 12px;
  padding: 12px 12px 12px 13px;
  color: #ffe9c4;
  cursor: pointer;
  background:
    linear-gradient(165deg, rgba(46, 26, 12, 0.97), rgba(22, 12, 6, 0.98));
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.5);
  transition: transform 140ms ease, box-shadow 140ms ease, filter 140ms ease;
}

.toast-card:hover {
  transform: translateY(-2px);
  filter: brightness(1.06);
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.58);
}

.toast-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  font-size: 1.1rem;
  background: rgba(238, 184, 91, 0.18);
  color: #ffd98a;
}

.toast-body {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.toast-body strong {
  font-family: var(--font-display);
  font-size: 0.94rem;
  color: #ffe8c8;
}

.toast-body small {
  color: rgba(255, 231, 183, 0.74);
  font-size: 0.78rem;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.toast-close {
  display: grid;
  place-items: center;
  color: rgba(255, 224, 178, 0.6);
  font-size: 1.1rem;
  line-height: 1;
  cursor: pointer;
}
.toast-close:hover {
  color: #ffe0b0;
}

/* 类型色条 */
.toast-card.tone-approved,
.letter-card.tone-approved {
  border-left-color: #a9d07b;
}
.toast-card.tone-pending,
.letter-card.tone-pending {
  border-left-color: #f0b868;
}
.toast-card.tone-changes,
.letter-card.tone-changes {
  border-left-color: #f0a06d;
}
.toast-card.tone-rejected,
.letter-card.tone-rejected {
  border-left-color: #dc826e;
}

.toast-pop-enter-active {
  transition: transform 280ms cubic-bezier(0.2, 0.9, 0.3, 1.3), opacity 280ms ease;
}
.toast-pop-leave-active {
  transition: transform 200ms ease, opacity 200ms ease;
  position: relative;
}
.toast-pop-enter-from {
  transform: translateX(-40px) scale(0.96);
  opacity: 0;
}
.toast-pop-leave-to {
  transform: translateX(-40px);
  opacity: 0;
}

/* ── 详情弹窗（信笺） ───────────────────────────────────── */
.letter-modal {
  position: fixed;
  inset: 0;
  z-index: 2100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(10, 5, 3, 0.62);
  backdrop-filter: blur(2px);
}

.letter-card {
  position: relative;
  width: min(460px, 100%);
  padding: 34px 30px 26px;
  text-align: center;
  border: 1px solid rgba(238, 184, 91, 0.5);
  border-top-width: 4px;
  border-radius: 14px;
  color: #ffe9c4;
  background:
    radial-gradient(circle at 50% 0%, rgba(238, 184, 91, 0.16), transparent 55%),
    linear-gradient(168deg, rgba(52, 30, 14, 0.97), rgba(24, 13, 7, 0.98));
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.6);
}

.letter-close {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 224, 178, 0.7);
  font-size: 1.4rem;
  line-height: 1;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease;
}
.letter-close:hover {
  background: rgba(240, 198, 118, 0.18);
  color: #ffe0b0;
}

.letter-seal {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  margin: 0 auto 10px;
  border-radius: 50%;
  font-size: 1.6rem;
  color: #2a1605;
  background: radial-gradient(circle at 50% 30%, #ffe6a6, #d89a32);
  box-shadow: 0 0 0 6px rgba(238, 184, 91, 0.16), 0 8px 20px rgba(160, 100, 18, 0.4);
}

.letter-kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 224, 178, 0.62);
}

.letter-card h2 {
  margin: 6px 0 14px;
  font-family: var(--font-display);
  color: #ffe8c8;
}

.letter-content {
  margin: 0 0 16px;
  padding: 14px 16px;
  text-align: left;
  border: 1px solid rgba(240, 198, 118, 0.26);
  border-radius: 10px;
  color: #ffe6cf;
  font-size: 0.94rem;
  line-height: 1.65;
  background: rgba(15, 8, 5, 0.5);
  white-space: pre-wrap;
  word-break: break-word;
}

.letter-meta {
  display: grid;
  gap: 8px 18px;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  margin: 0 0 22px;
  text-align: left;
}
.letter-meta div {
  display: grid;
  gap: 2px;
}
.letter-meta dt {
  color: rgba(255, 231, 183, 0.55);
  font-size: 0.74rem;
}
.letter-meta dd {
  margin: 0;
  color: #ffe2a0;
  font-size: 0.88rem;
  font-variant-numeric: tabular-nums;
}

.letter-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}
.letter-actions .quiet-action,
.letter-actions .primary-action {
  min-height: 42px;
  padding: 0 18px;
}

.letter-pop-enter-active,
.letter-pop-leave-active {
  transition: opacity 200ms ease;
}
.letter-pop-enter-active .letter-card,
.letter-pop-leave-active .letter-card {
  transition: transform 220ms cubic-bezier(0.2, 0.9, 0.3, 1.2), opacity 220ms ease;
}
.letter-pop-enter-from,
.letter-pop-leave-to {
  opacity: 0;
}
.letter-pop-enter-from .letter-card,
.letter-pop-leave-to .letter-card {
  transform: translateY(14px) scale(0.96);
  opacity: 0;
}
</style>
