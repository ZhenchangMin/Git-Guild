<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { useAssistantChat } from '../composables/useAssistantChat'

const props = defineProps({
  visible: { type: Boolean, default: false },
  page: { type: String, default: 'hall' },
})

const emit = defineEmits(['close'])

const router = useRouter()

const {
  messages,
  draft,
  loading,
  messageList,
  quickQuestions,
  canSend,
  welcomeMessage,
  send,
  onKeydown,
  reset,
} = useAssistantChat({ page: computed(() => props.page) })

function navigate(routeName) {
  emit('close')
  router.push({ name: routeName })
}

watch(() => props.visible, (val) => {
  if (!val) reset()
})
</script>

<template>
  <Transition name="assistant-fade">
    <div v-if="visible" class="assistant-backdrop" @click.self="emit('close')">
      <Transition name="assistant-panel" appear>
        <div v-if="visible" class="assistant-panel">
          <header class="assistant-head">
            <div class="assistant-head-label">
              <span class="assistant-npc-icon" aria-hidden="true">&#9872;</span>
              <div>
                <strong>公会前台向导</strong>
                <small>新手引导 & 委托答疑</small>
              </div>
            </div>
            <button class="assistant-close" type="button" aria-label="关闭前台向导" @click="emit('close')">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M18 6 6 18M6 6l12 12" />
              </svg>
            </button>
          </header>

          <div ref="messageList" class="assistant-body">
            <div v-if="messages.length === 0" class="assistant-welcome">
              <p>{{ welcomeMessage }}</p>
              <div class="assistant-quick-row">
                <button
                  v-for="q in quickQuestions"
                  :key="q"
                  class="assistant-quick-chip"
                  type="button"
                  @click="send(q)"
                >{{ q }}</button>
              </div>
            </div>

            <TransitionGroup name="msg-slide" tag="div" class="assistant-msg-list">
              <div
                v-for="msg in messages"
                :key="msg.id"
                class="assistant-msg"
                :class="msg.role"
              >
                <p class="assistant-bubble">{{ msg.text }}</p>

                <div v-if="msg.source" class="assistant-source">
                  <span v-if="msg.source === 'AI'" class="source-badge ai">&#9733; AI 回答</span>
                  <span v-else-if="msg.source === 'FAQ'" class="source-badge faq">&#9733; 帮助手册</span>
                  <span v-else class="source-badge fallback">&#9888; 降级回答</span>
                </div>

                <div v-if="msg.actions && msg.actions.length" class="assistant-actions">
                  <button
                    v-for="action in msg.actions"
                    :key="action.routeName"
                    class="assistant-action-btn"
                    type="button"
                    @click="navigate(action.routeName)"
                  >{{ action.label }}</button>
                </div>

                <div v-if="msg.suggestedQuestions && msg.suggestedQuestions.length && msg === messages[messages.length - 1]" class="assistant-quick-row">
                  <button
                    v-for="q in msg.suggestedQuestions"
                    :key="q"
                    class="assistant-quick-chip"
                    type="button"
                    @click="send(q)"
                  >{{ q }}</button>
                </div>
              </div>
            </TransitionGroup>

            <div v-if="loading" class="assistant-msg npc">
              <p class="assistant-bubble assistant-loading">
                <span class="loading-dot">前台正在翻阅公会手册</span>
              </p>
            </div>
          </div>

          <footer class="assistant-foot">
            <div class="assistant-input-row">
              <input
                v-model="draft"
                class="assistant-input"
                type="text"
                placeholder="输入你的问题..."
                maxlength="500"
                :disabled="loading"
                @keydown="onKeydown"
              />
              <button
                class="assistant-send"
                type="button"
                aria-label="发送"
                :disabled="!canSend"
                @click="send()"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M5 12h13M12 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          </footer>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<style scoped>
.assistant-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1500;
  display: grid;
  place-items: end center;
  padding: 28px;
  background: radial-gradient(circle at 50% 36%, rgba(74, 43, 16, 0.2), rgba(5, 3, 1, 0.78));
  backdrop-filter: blur(6px);
}

.assistant-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  width: min(480px, calc(100vw - 56px));
  height: min(540px, 60svh);
  border: 1px solid rgba(231, 177, 90, 0.56);
  border-radius: 20px 20px 0 0;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(31, 17, 9, 0.92), rgba(15, 8, 4, 0.9)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.14), transparent 58%);
  box-shadow:
    0 -4px 40px rgba(0, 0, 0, 0.48),
    0 28px 80px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 232, 176, 0.16);
  backdrop-filter: blur(8px);
}

.assistant-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px;
  border-bottom: 1px solid rgba(238, 184, 91, 0.22);
}

.assistant-head-label {
  display: flex;
  align-items: center;
  gap: 10px;
}

.assistant-npc-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid rgba(245, 195, 99, 0.48);
  border-radius: 50%;
  color: #ffd98a;
  font-size: 1.1rem;
  background:
    radial-gradient(circle at 35% 28%, rgba(255, 226, 151, 0.28), transparent 42%),
    rgba(41, 22, 9, 0.62);
}

.assistant-head-label strong {
  font-family: var(--font-display);
  color: #ffe2a0;
  font-size: 1.05rem;
}

.assistant-head-label small {
  display: block;
  color: rgba(255, 232, 190, 0.58);
  font-size: 0.74rem;
  margin-top: 1px;
}

.assistant-close {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(238, 184, 91, 0.38);
  border-radius: 50%;
  padding: 0;
  color: #ffe4ad;
  background: rgba(17, 9, 5, 0.5);
  cursor: pointer;
  transition: background 160ms ease, border-color 160ms ease;
}

.assistant-close svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
}

.assistant-close:hover,
.assistant-close:focus-visible {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.72);
}

.assistant-body {
  overflow-y: auto;
  padding: 18px;
  display: grid;
  align-content: start;
  gap: 14px;
}

.assistant-welcome {
  display: grid;
  gap: 16px;
}

.assistant-welcome p {
  margin: 0;
  color: rgba(255, 232, 190, 0.82);
  line-height: 1.55;
  text-wrap: pretty;
}

.assistant-quick-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.assistant-quick-chip {
  border: 1px solid rgba(238, 184, 91, 0.44);
  border-radius: 999px;
  padding: 7px 14px;
  color: rgba(255, 232, 190, 0.82);
  background: rgba(21, 12, 7, 0.52);
  font-size: 0.84rem;
  cursor: pointer;
  transition: border-color 160ms ease, background 160ms ease, color 160ms ease, transform 160ms ease;
}

.assistant-quick-chip:hover,
.assistant-quick-chip:focus-visible {
  border-color: var(--gold-bright);
  color: #ffe8b8;
  background: rgba(82, 45, 16, 0.56);
  transform: translateY(-1px);
}

.assistant-msg {
  display: grid;
  gap: 8px;
  max-width: 88%;
}

.assistant-msg.user {
  justify-self: end;
  justify-items: end;
}

.assistant-msg.npc {
  justify-self: start;
  justify-items: start;
}

.assistant-bubble {
  margin: 0;
  padding: 10px 14px;
  border-radius: 14px;
  line-height: 1.55;
  text-wrap: pretty;
  word-break: break-word;
}

.assistant-msg.user .assistant-bubble {
  color: #1a0f06;
  background: linear-gradient(135deg, #ffd98a, #d89a32);
  border-bottom-right-radius: 4px;
}

.assistant-msg.npc .assistant-bubble {
  color: rgba(255, 232, 190, 0.92);
  background: rgba(21, 12, 7, 0.68);
  border: 1px solid rgba(238, 184, 91, 0.28);
  border-bottom-left-radius: 4px;
}

.assistant-loading {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 40px;
}

.loading-dot {
  color: rgba(255, 232, 190, 0.62);
  font-style: italic;
}

.loading-dot::after {
  content: '';
  display: inline-block;
  width: 1em;
  text-align: left;
  animation: assistantDotPulse 1.2s steps(1, end) infinite;
}

@keyframes assistantDotPulse {
  0%   { content: ''; }
  25%  { content: '.'; }
  50%  { content: '..'; }
  75%  { content: '...'; }
}

.assistant-source {
  display: flex;
  gap: 6px;
}

.source-badge {
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 0.68rem;
  line-height: 1.4;
}

.source-badge.ai {
  border: 1px solid rgba(119, 160, 91, 0.56);
  color: #d7ffd8;
  background: rgba(67, 97, 58, 0.48);
}

.source-badge.faq {
  border: 1px solid rgba(238, 184, 91, 0.48);
  color: #ffe6b1;
  background: rgba(133, 76, 20, 0.42);
}

.source-badge.fallback {
  border: 1px solid rgba(202, 104, 88, 0.56);
  color: #ffe0da;
  background: rgba(110, 42, 36, 0.42);
}

.assistant-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.assistant-action-btn {
  border: 1px solid rgba(245, 195, 99, 0.54);
  border-radius: 6px;
  padding: 6px 14px;
  color: #1a0f06;
  font-weight: 700;
  font-size: 0.82rem;
  background: linear-gradient(180deg, #ffd98a, #b56c22);
  cursor: pointer;
  transition: filter 150ms ease, transform 150ms ease;
}

.assistant-action-btn:hover,
.assistant-action-btn:focus-visible {
  filter: brightness(1.08);
  transform: translateY(-1px);
}

.assistant-foot {
  padding: 12px 18px;
  border-top: 1px solid rgba(238, 184, 91, 0.22);
}

.assistant-input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.assistant-input {
  flex: 1;
  min-height: 42px;
  border: 1px solid rgba(224, 163, 72, 0.52);
  border-radius: 10px;
  padding: 0 14px;
  color: #ffe8b9;
  background: rgba(11, 7, 4, 0.62);
  box-shadow: inset 0 2px 12px rgba(0, 0, 0, 0.36);
  font-family: var(--font-body);
  font-size: 0.92rem;
}

.assistant-input::placeholder {
  color: rgba(255, 232, 190, 0.38);
}

.assistant-input:focus-visible {
  outline: 2px solid rgba(255, 217, 128, 0.82);
  outline-offset: 2px;
}

.assistant-input:disabled {
  opacity: 0.55;
}

.assistant-send {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  border: 1px solid #f3ca75;
  border-radius: 50%;
  padding: 0;
  color: #221205;
  background: linear-gradient(180deg, #ffd98a, #b56c22);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.36);
  cursor: pointer;
  transition: filter 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}

.assistant-send svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.assistant-send:hover:not(:disabled) {
  filter: brightness(1.09);
  transform: translateY(-1px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.44), 0 0 20px rgba(255, 197, 89, 0.2);
}

.assistant-send:active:not(:disabled) {
  transform: scale(0.94);
  filter: brightness(0.92);
}

.assistant-send:disabled {
  cursor: not-allowed;
  filter: grayscale(0.35);
  opacity: 0.55;
  transform: none;
}

/* Message entrance animation */
.msg-slide-enter-active {
  transition: opacity 260ms ease, transform 260ms cubic-bezier(0.18, 0.86, 0.2, 1);
}

.msg-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.assistant-msg-list {
  display: grid;
  gap: 14px;
}

/* Transitions */
.assistant-fade-enter-active,
.assistant-fade-leave-active {
  transition: opacity 220ms ease;
}

.assistant-fade-enter-from,
.assistant-fade-leave-to {
  opacity: 0;
}

.assistant-panel-enter-active {
  transition: transform 320ms cubic-bezier(0.18, 0.86, 0.2, 1), opacity 240ms ease;
}

.assistant-panel-leave-active {
  transition: transform 200ms cubic-bezier(0.4, 0, 0.2, 1), opacity 180ms ease;
}

.assistant-panel-enter-from {
  transform: translateY(40px);
  opacity: 0;
}

.assistant-panel-leave-to {
  transform: translateY(30px);
  opacity: 0;
}

@media (max-width: 560px) {
  .assistant-backdrop {
    place-items: end center;
    padding: 12px;
  }

  .assistant-panel {
    width: 100%;
    height: 55svh;
    border-radius: 18px 18px 0 0;
  }
}
</style>
