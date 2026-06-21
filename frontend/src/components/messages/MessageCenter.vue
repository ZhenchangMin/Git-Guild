<script setup>
import { computed, nextTick, ref, watch } from 'vue'

import {
  closeMessageCenter,
  messageStore,
  openQuestMessages,
  selectMessageThread,
  sendMessage,
} from '../../stores/messageStore'
import { overlayStore } from '../../stores/overlayStore'

const draft = ref('')
const messageListRef = ref(null)

const isOpen = computed(() => overlayStore.activeOverlay === 'message')
const activeThread = computed(() => messageStore.activeThread)
const threads = computed(() => messageStore.threads)
const canSend = computed(() => draft.value.trim().length > 0 && !messageStore.sending && activeThread.value)

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function relativeTime(iso) {
  if (!iso) return '暂无信笺'
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return ''
  const diffSec = Math.round((Date.now() - then) / 1000)
  if (diffSec < 60) return '刚刚'
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)} 分钟前`
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)} 小时前`
  if (diffSec < 604800) return `${Math.floor(diffSec / 86400)} 天前`
  return new Date(iso).toLocaleDateString('zh-CN')
}

async function submitMessage() {
  if (!canSend.value) return
  const sent = await sendMessage(draft.value)
  if (sent) draft.value = ''
}

function onKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault()
    submitMessage()
  }
}

async function scrollToBottom() {
  await nextTick()
  const list = messageListRef.value
  if (list) list.scrollTop = list.scrollHeight
}

watch(
  () => activeThread.value?.messages?.length,
  scrollToBottom,
)

watch(isOpen, (open) => {
  if (open) scrollToBottom()
})

defineExpose({ openQuestMessages })
</script>

<template>
  <teleport to="body">
    <transition name="message-shell">
      <div
        v-if="isOpen"
        class="message-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="message-center-title"
        @click.self="closeMessageCenter"
      >
        <section class="message-center">
          <button class="center-close" type="button" aria-label="关闭信笺" @click="closeMessageCenter">×</button>
          <aside class="thread-sidebar" aria-label="信笺会话列表">
            <header class="thread-head">
              <div>
                <p class="kicker">Mailbox</p>
                <h2 id="message-center-title">站内信笺</h2>
              </div>
            </header>

            <p v-if="messageStore.loadingThreads && threads.length === 0" class="thread-empty">正在整理信笺...</p>
            <p v-else-if="threads.length === 0" class="thread-empty">暂无可联系的任务会话</p>

            <div v-else class="thread-list">
              <button
                v-for="thread in threads"
                :key="thread.threadId"
                class="thread-item"
                :class="{ active: activeThread?.threadId === thread.threadId, unread: thread.unreadCount > 0 }"
                type="button"
                @click="selectMessageThread(thread.threadId)"
              >
                <span class="thread-code">{{ thread.questCode }}</span>
                <strong>{{ thread.questTitle }}</strong>
                <small>
                  {{ thread.latestMessage?.content || `与 ${thread.counterpart?.username || '对方'} 开始通信` }}
                </small>
                <em>{{ relativeTime(thread.latestMessage?.createdAt || thread.lastMessageAt) }}</em>
                <b v-if="thread.unreadCount > 0">{{ thread.unreadCount > 9 ? '9+' : thread.unreadCount }}</b>
              </button>
            </div>
          </aside>

          <main class="thread-content">
            <section v-if="messageStore.loadingThread && !activeThread" class="thread-placeholder">
              <h3>正在展开信笺</h3>
              <p>请稍候。</p>
            </section>

            <section v-else-if="!activeThread" class="thread-placeholder">
              <h3>选择一条信笺会话</h3>
              <p>只有已经建立任务关系的委托人与冒险家可以互发信笺。</p>
            </section>

            <template v-else>
              <header class="conversation-head">
                <div>
                  <p class="kicker">{{ activeThread.questCode }}</p>
                  <h2>{{ activeThread.questTitle }}</h2>
                </div>
                <div class="recipient-chip">
                  <span>收件人</span>
                  <strong>{{ activeThread.counterpart?.username || '对方' }}</strong>
                </div>
              </header>

              <div ref="messageListRef" class="message-list" aria-label="信笺内容">
                <p v-if="activeThread.messages.length === 0" class="no-messages">
                  还没有信笺。写下第一封，确认任务细节或同步进度。
                </p>

                <article
                  v-for="message in activeThread.messages"
                  :key="message.messageId"
                  class="message-bubble"
                  :class="{ mine: message.mine }"
                >
                  <header>
                    <strong>{{ message.mine ? '我' : message.senderName }}</strong>
                    <time>{{ formatTime(message.createdAt) }}</time>
                  </header>
                  <p>{{ message.content }}</p>
                </article>
              </div>

              <form class="composer" @submit.prevent="submitMessage">
                <div class="composer-field">
                  <label for="message-draft">写信笺</label>
                  <textarea
                    id="message-draft"
                    v-model="draft"
                    maxlength="1000"
                    rows="5"
                    placeholder="写下要同步的任务问题、进度或审核说明..."
                    :disabled="messageStore.sending"
                    @keydown="onKeydown"
                  ></textarea>
                </div>

                <footer class="composer-actions">
                  <span>{{ draft.trim().length }}/1000</span>
                  <button class="primary-action" type="submit" :disabled="!canSend">
                    {{ messageStore.sending ? '投递中...' : '发送信笺' }}
                  </button>
                </footer>
                <p v-if="messageStore.error" class="message-error">{{ messageStore.error }}</p>
              </form>
            </template>
          </main>
        </section>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.message-modal {
  position: fixed;
  inset: 0;
  z-index: 2200;
  display: grid;
  place-items: center;
  padding: 28px;
  background: rgba(8, 4, 2, 0.66);
  backdrop-filter: blur(3px);
}

.message-center {
  position: relative;
  width: min(1120px, calc(100vw - 64px));
  height: min(720px, calc(100vh - 64px));
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid rgba(238, 184, 91, 0.62);
  border-radius: 14px;
  color: #ffe7b5;
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.98), rgba(13, 7, 4, 0.98)),
    radial-gradient(circle at 72% 18%, rgba(255, 206, 118, 0.14), transparent 34%);
  box-shadow: 0 32px 82px rgba(0, 0, 0, 0.62), inset 0 1px 0 rgba(255, 230, 176, 0.14);
}

.thread-sidebar {
  min-width: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  border-right: 1px solid rgba(238, 184, 91, 0.22);
  background: rgba(12, 6, 3, 0.34);
}

.thread-head,
.conversation-head,
.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.thread-head {
  padding: 18px;
  border-bottom: 1px solid rgba(238, 184, 91, 0.2);
}

.thread-head h2,
.conversation-head h2 {
  margin: 2px 0 0;
  font-family: var(--font-display);
  font-size: 1.25rem;
  line-height: 1.2;
}

/* 关闭按钮悬浮在整个站内信浮层的右上角（而非侧栏标题处）。 */
.center-close {
  position: absolute;
  top: 12px;
  right: 14px;
  z-index: 5;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(238, 184, 91, 0.36);
  border-radius: 9px;
  color: rgba(255, 224, 178, 0.82);
  background: rgba(13, 7, 4, 0.62);
  font-size: 1.4rem;
  line-height: 1;
  cursor: pointer;
  transition: color 140ms ease, background 140ms ease, border-color 140ms ease, transform 140ms ease;
}

.center-close:hover {
  color: #ffe0b0;
  background: rgba(240, 198, 118, 0.2);
  border-color: rgba(255, 204, 105, 0.7);
}

.center-close:active {
  transform: scale(0.92);
}

.thread-list {
  min-height: 0;
  overflow-y: auto;
  padding: 10px;
}

.thread-empty,
.thread-placeholder,
.no-messages {
  margin: 0;
  color: rgba(255, 231, 183, 0.7);
}

.thread-empty {
  padding: 24px 18px;
  text-align: center;
}

.thread-item {
  position: relative;
  width: 100%;
  display: grid;
  gap: 5px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 12px 34px 12px 12px;
  color: #ffe7b5;
  text-align: left;
  background: transparent;
  cursor: pointer;
  transition: background 140ms ease, border-color 140ms ease;
}

.thread-item:hover,
.thread-item.active {
  border-color: rgba(238, 184, 91, 0.42);
  background: rgba(82, 45, 16, 0.34);
}

.thread-item.unread {
  background: rgba(82, 45, 16, 0.22);
}

.thread-code,
.thread-item em {
  color: rgba(255, 231, 183, 0.58);
  font-size: 0.76rem;
  font-style: normal;
}

.thread-item strong,
.thread-item small {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.thread-item strong {
  color: #ffe2a0;
  font-size: 0.92rem;
}

.thread-item small {
  color: rgba(255, 231, 183, 0.72);
}

.thread-item b {
  position: absolute;
  top: 12px;
  right: 10px;
  min-width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  color: #fff;
  background: #d84d35;
  font-size: 0.68rem;
}

.thread-content {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.thread-placeholder {
  place-self: center;
  max-width: 420px;
  text-align: center;
}

.thread-placeholder h3 {
  margin: 0 0 8px;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.4rem;
}

.conversation-head {
  /* 右侧多留出空间，避免“收件人”信息被右上角关闭按钮压住。 */
  padding: 18px 64px 18px 22px;
  border-bottom: 1px solid rgba(238, 184, 91, 0.2);
}

.conversation-head h2 {
  max-width: 560px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipient-chip {
  display: grid;
  gap: 2px;
  border: 1px solid rgba(238, 184, 91, 0.34);
  border-radius: 8px;
  padding: 8px 12px;
  text-align: right;
  background: rgba(17, 9, 5, 0.42);
}

.recipient-chip span {
  color: rgba(255, 231, 183, 0.58);
  font-size: 0.72rem;
}

.recipient-chip strong {
  color: #ffe2a0;
  font-size: 0.92rem;
}

.message-list {
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px;
  background:
    linear-gradient(rgba(255, 232, 184, 0.045) 1px, transparent 1px),
    rgba(10, 5, 3, 0.24);
  background-size: 100% 32px;
}

.no-messages {
  align-self: center;
  margin-top: 80px;
}

.message-bubble {
  width: min(520px, 82%);
  display: grid;
  gap: 7px;
  border: 1px solid rgba(238, 184, 91, 0.28);
  border-radius: 8px;
  padding: 12px 14px;
  background: rgba(30, 16, 8, 0.76);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.22);
}

.message-bubble.mine {
  align-self: flex-end;
  border-color: rgba(167, 204, 125, 0.38);
  background: rgba(39, 61, 34, 0.64);
}

.message-bubble header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.message-bubble strong {
  color: #ffe2a0;
  font-size: 0.86rem;
}

.message-bubble time {
  color: rgba(255, 231, 183, 0.54);
  font-size: 0.72rem;
}

.message-bubble p {
  margin: 0;
  color: #fff0d5;
  line-height: 1.58;
  white-space: pre-wrap;
  word-break: break-word;
}

.composer {
  position: relative;
  display: grid;
  gap: 10px;
  padding: 16px 22px 18px;
  border-top: 1px solid rgba(238, 184, 91, 0.2);
  background: rgba(13, 7, 4, 0.7);
}

.composer-field {
  position: relative;
  display: grid;
  gap: 8px;
}

.composer-field label {
  color: rgba(255, 231, 183, 0.72);
  font-size: 0.82rem;
}

.composer textarea {
  width: 100%;
  resize: none;
  border: 1px solid rgba(238, 184, 91, 0.36);
  border-radius: 8px;
  padding: 12px 14px;
  color: #ffe7b5;
  background: rgba(7, 3, 1, 0.48);
  font: inherit;
  line-height: 1.5;
  outline: none;
  transition: border-color 140ms ease, box-shadow 140ms ease;
}

.composer textarea:focus {
  border-color: rgba(255, 204, 105, 0.78);
  box-shadow: 0 0 0 3px rgba(255, 204, 105, 0.12);
}

.composer-actions span {
  color: rgba(255, 231, 183, 0.58);
  font-size: 0.8rem;
}

.composer-actions .primary-action {
  min-height: 40px;
  padding: 0 18px;
}

.message-error {
  margin: 0;
  color: #ffb088;
  font-size: 0.84rem;
}

.message-shell-enter-active,
.message-shell-leave-active {
  transition: opacity 180ms ease;
}

.message-shell-enter-active .message-center,
.message-shell-leave-active .message-center {
  transition: transform 220ms ease, opacity 220ms ease;
}

.message-shell-enter-from,
.message-shell-leave-to {
  opacity: 0;
}

.message-shell-enter-from .message-center,
.message-shell-leave-to .message-center {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}
</style>
