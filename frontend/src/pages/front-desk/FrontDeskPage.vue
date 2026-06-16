<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import deskImg from '../../assets/desk.webp'
import { growthApi } from '../../api/growthApi'
import { questApi } from '../../api/questApi'
import { submissionApi } from '../../api/submissionApi'
import { sessionStore } from '../../stores/sessionStore'
import { useAssistantChat } from '../../composables/useAssistantChat'

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
} = useAssistantChat({ page: 'front-desk' })

// ── Role-aware status data ──
const openQuestCount = ref(0)
const growthSummary = ref(null)
const activeAssignments = ref([])
const pendingSubmissions = ref([])
const publishedQuests = ref([])
const reviewQueue = ref([])
const statusReady = ref(false)

const role = computed(() => sessionStore.role)
const isVisitor = computed(() => role.value === 'VISITOR')
const isAdventurer = computed(() => role.value === 'ADVENTURER' || role.value === 'BEGINNER')
const isMaintainer = computed(() => role.value === 'MAINTAINER')

const len = (v) => (Array.isArray(v) ? v.length : 0)

// Status chips shown in the dialogue header, per role.
const statusChips = computed(() => {
  if (isAdventurer.value) {
    return [
      { label: '等级', value: `Lv${growthSummary.value?.level ?? 1}` },
      { label: '经验', value: `${growthSummary.value?.totalXp ?? 0} XP` },
      { label: '进行中', value: `${len(activeAssignments.value)}` },
      { label: '待审核', value: `${len(pendingSubmissions.value)}`, urgent: len(pendingSubmissions.value) > 0 },
    ]
  }
  if (isMaintainer.value) {
    return [
      { label: '待审提交', value: `${len(reviewQueue.value)}`, urgent: len(reviewQueue.value) > 0 },
      { label: '已发布', value: `${len(publishedQuests.value)}` },
      { label: '可接委托', value: `${openQuestCount.value}` },
    ]
  }
  return [{ label: '委托板', value: `${openQuestCount.value} 份可接` }]
})

// One contextual shortcut per role.
const primaryShortcut = computed(() => {
  if (isAdventurer.value) return { label: '前往工作台', routeName: 'adventurer-workbench' }
  if (isMaintainer.value) return { label: '前往审核台', routeName: 'maintainer-review' }
  return { label: '浏览悬赏任务板', routeName: 'quest-board' }
})

onMounted(async () => {
  try {
    const questRes = await questApi.list({ status: 'PUBLISHED', page: 1, size: 1 }).catch(() => null)
    openQuestCount.value = questRes?.data?.totalItems ?? 0
  } catch {
    openQuestCount.value = 0
  }

  if (isAdventurer.value || isMaintainer.value) {
    try {
      const tasks = [growthApi.summary().catch(() => null)]
      if (isAdventurer.value) {
        tasks.push(questApi.myAssignments().catch(() => null))
        tasks.push(submissionApi.list({ status: 'PENDING_REVIEW' }).catch(() => null))
      } else {
        tasks.push(questApi.myPublished().catch(() => null))
        tasks.push(submissionApi.reviewQueue().catch(() => null))
      }
      const results = await Promise.all(tasks)
      growthSummary.value = results[0]?.data ?? null
      if (isAdventurer.value) {
        activeAssignments.value = results[1]?.data?.items ?? results[1]?.data ?? []
        pendingSubmissions.value = results[2]?.data?.items ?? results[2]?.data ?? []
      } else {
        publishedQuests.value = results[1]?.data?.items ?? results[1]?.data ?? []
        reviewQueue.value = results[2]?.data?.items ?? results[2]?.data ?? []
      }
    } catch {
      // graceful fallback
    }
  }
  statusReady.value = true
})

function navigate(routeName) {
  router.push({ name: routeName })
}

function backToHall() {
  router.push({ name: 'hall' })
}
</script>

<template>
  <main class="app-shell">
    <section class="desk-scene" :style="{ backgroundImage: `url(${deskImg})` }">
      <div class="desk-scrim" aria-hidden="true"></div>

      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
        <span>返回大厅</span>
      </button>

      <!-- Visual-novel dialogue bar -->
      <section class="desk-dialogue" aria-label="公会前台向导对话">
        <header class="desk-dialogue-head">
          <div class="desk-identity">
            <span class="desk-npc-icon" aria-hidden="true">&#9872;</span>
            <div class="desk-identity-text">
              <strong>公会前台向导</strong>
              <small>有什么可以帮你，尽管开口</small>
            </div>
          </div>

          <div class="desk-status">
            <span
              v-for="chip in statusChips"
              :key="chip.label"
              class="desk-status-chip"
              :class="{ urgent: chip.urgent }"
            >
              <i>{{ chip.label }}</i>
              <b>{{ chip.value }}</b>
            </span>
            <button class="desk-status-cta" type="button" @click="navigate(primaryShortcut.routeName)">
              {{ primaryShortcut.label }}
            </button>
          </div>
        </header>

        <div ref="messageList" class="desk-dialogue-body">
          <p v-if="messages.length === 0" class="desk-welcome">{{ welcomeMessage }}</p>

          <TransitionGroup name="msg-slide" tag="div" class="desk-msg-list">
            <div v-for="msg in messages" :key="msg.id" class="desk-msg" :class="msg.role">
              <p class="desk-bubble">{{ msg.text }}</p>

              <div v-if="msg.source === 'FAQ' || msg.source === 'FALLBACK'" class="desk-source">
                <span v-if="msg.source === 'FAQ'" class="source-badge faq">&#9733; 帮助手册</span>
                <span v-else class="source-badge fallback">&#9888; 降级回答</span>
              </div>

              <div v-if="msg.actions && msg.actions.length" class="desk-actions">
                <button
                  v-for="action in msg.actions"
                  :key="action.routeName"
                  class="desk-action-btn"
                  type="button"
                  @click="navigate(action.routeName)"
                >{{ action.label }}</button>
              </div>
            </div>
          </TransitionGroup>

          <div v-if="loading" class="desk-msg npc">
            <p class="desk-bubble desk-loading">
              <span class="loading-text">前台正在翻阅公会手册</span>
            </p>
          </div>
        </div>

        <footer class="desk-dialogue-foot">
          <div v-if="quickQuestions.length" class="desk-quick-row">
            <button
              v-for="q in quickQuestions"
              :key="q"
              class="desk-quick-chip"
              type="button"
              @click="send(q)"
            >{{ q }}</button>
          </div>

          <div class="desk-input-row">
            <input
              v-model="draft"
              class="desk-input"
              type="text"
              placeholder="向前台向导提问..."
              maxlength="500"
              :disabled="loading"
              @keydown="onKeydown"
            />
            <button
              class="desk-send"
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
      </section>
    </section>
  </main>
</template>

<style scoped>
/* ── Scene ── */
.desk-scene {
  position: relative;
  width: 100%;
  min-height: 100svh;
  overflow: hidden;
  /* Pull the character toward the upper area so the dialogue bar covers the
     counter (where conversation naturally happens), not the NPC's face. */
  background-position: center 20%;
  background-size: cover;
  isolation: isolate;
}

/* Bottom scrim blends the scene art into the dialogue bar */
.desk-scrim {
  position: absolute;
  inset: auto 0 0 0;
  height: 62%;
  background: linear-gradient(to top, rgba(9, 5, 2, 0.92) 12%, rgba(9, 5, 2, 0.5) 48%, transparent);
  pointer-events: none;
  z-index: 0;
}

.back-orb {
  position: fixed;
  top: 22px;
  left: 22px;
  z-index: 10;
}

/* ── Dialogue bar ── */
.desk-dialogue {
  position: absolute;
  left: 50%;
  bottom: clamp(20px, 4svh, 40px);
  transform: translateX(-50%);
  z-index: 2;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  width: min(1040px, calc(100vw - 48px));
  max-height: min(52svh, 460px);
  border-radius: 20px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(33, 18, 9, 0.93), rgba(16, 9, 4, 0.95)),
    radial-gradient(ellipse at 12% 0%, rgba(216, 154, 50, 0.16), transparent 50%);
  border: 1px solid rgba(231, 177, 90, 0.4);
  box-shadow:
    0 -2px 0 rgba(255, 222, 150, 0.1),
    0 30px 80px rgba(0, 0, 0, 0.6),
    inset 0 1px 0 rgba(255, 238, 188, 0.18);
  backdrop-filter: blur(12px);
  animation: deskBarIn 420ms cubic-bezier(0.18, 0.86, 0.2, 1) both;
}

@keyframes deskBarIn {
  from { opacity: 0; transform: translate(-50%, 24px); }
  to { opacity: 1; transform: translate(-50%, 0); }
}

/* ── Header: identity + role status ── */
.desk-dialogue-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px 18px;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(238, 184, 91, 0.16);
  background: linear-gradient(180deg, rgba(255, 220, 132, 0.05), transparent);
}

.desk-identity {
  display: flex;
  align-items: center;
  gap: 12px;
}

.desk-npc-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  border: 1px solid rgba(245, 195, 99, 0.55);
  border-radius: 50%;
  color: #ffd98a;
  font-size: 1.25rem;
  background:
    radial-gradient(circle at 35% 28%, rgba(255, 226, 151, 0.34), transparent 42%),
    rgba(41, 22, 9, 0.55);
  box-shadow: 0 0 20px rgba(255, 200, 100, 0.16);
}

.desk-identity-text strong {
  display: block;
  font-family: var(--font-display);
  color: #ffe4a8;
  font-size: 1.1rem;
  letter-spacing: 0.02em;
}

.desk-identity-text small {
  display: block;
  color: rgba(255, 232, 190, 0.55);
  font-size: 0.76rem;
  margin-top: 2px;
}

.desk-status {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.desk-status-chip {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  border: 1px solid rgba(238, 184, 91, 0.22);
  border-radius: 10px;
  padding: 5px 11px;
  background: rgba(10, 6, 3, 0.4);
}

.desk-status-chip i {
  color: rgba(255, 232, 190, 0.5);
  font-style: normal;
  font-size: 0.7rem;
}

.desk-status-chip b {
  color: #ffe4ad;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 0.92rem;
  font-variant-numeric: tabular-nums;
}

.desk-status-chip.urgent {
  border-color: rgba(232, 140, 100, 0.5);
  background: rgba(110, 42, 36, 0.32);
}

.desk-status-chip.urgent b {
  color: #ffb088;
}

.desk-status-cta {
  border: 1px solid rgba(245, 195, 99, 0.5);
  border-radius: 10px;
  padding: 7px 15px;
  color: #1a0f06;
  font-weight: 700;
  font-size: 0.82rem;
  background: linear-gradient(180deg, #ffd98a, #c07528);
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: filter 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.desk-status-cta:hover,
.desk-status-cta:focus-visible {
  filter: brightness(1.08);
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.4), 0 0 14px rgba(255, 200, 100, 0.18);
}

.desk-status-cta:active {
  transform: scale(0.96);
}

/* ── Body: messages ── */
.desk-dialogue-body {
  overflow-y: auto;
  padding: 18px 20px;
  display: grid;
  align-content: start;
  gap: 14px;
}

.desk-welcome {
  margin: 0;
  color: rgba(255, 235, 200, 0.86);
  font-size: 1rem;
  line-height: 1.65;
  text-wrap: pretty;
  max-width: 60ch;
}

.desk-msg-list {
  display: grid;
  gap: 14px;
}

.msg-slide-enter-active {
  transition: opacity 280ms ease, transform 280ms cubic-bezier(0.18, 0.86, 0.2, 1);
}

.msg-slide-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.desk-msg {
  display: grid;
  gap: 8px;
  max-width: 78%;
}

.desk-msg.user {
  justify-self: end;
  justify-items: end;
}

.desk-msg.npc {
  justify-self: start;
  justify-items: start;
}

.desk-bubble {
  margin: 0;
  padding: 11px 16px;
  border-radius: 15px;
  font-size: 0.92rem;
  line-height: 1.6;
  text-wrap: pretty;
  word-break: break-word;
}

.desk-msg.user .desk-bubble {
  color: #1a0f06;
  background: linear-gradient(135deg, #ffe09d, #d48a28);
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
}

.desk-msg.npc .desk-bubble {
  color: rgba(255, 235, 200, 0.94);
  background: rgba(24, 14, 8, 0.78);
  border: 1px solid rgba(238, 184, 91, 0.24);
  border-bottom-left-radius: 4px;
}

.desk-loading {
  display: flex;
  align-items: center;
  min-height: 42px;
}

.loading-text {
  color: rgba(255, 232, 190, 0.58);
  font-style: italic;
}

.loading-text::after {
  content: '';
  display: inline-block;
  width: 1em;
  text-align: left;
  animation: deskDotPulse 1.2s steps(1, end) infinite;
}

@keyframes deskDotPulse {
  0%   { content: ''; }
  25%  { content: '.'; }
  50%  { content: '..'; }
  75%  { content: '...'; }
}

.desk-source {
  display: flex;
  gap: 5px;
}

.source-badge {
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 0.68rem;
  line-height: 1.4;
  letter-spacing: 0.03em;
}

.source-badge.faq {
  border: 1px solid rgba(238, 184, 91, 0.48);
  color: #ffe6b1;
  background: rgba(133, 76, 20, 0.45);
}

.source-badge.fallback {
  border: 1px solid rgba(202, 104, 88, 0.56);
  color: #ffe0da;
  background: rgba(110, 42, 36, 0.45);
}

.desk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.desk-action-btn {
  border: 1px solid rgba(245, 195, 99, 0.56);
  border-radius: 10px;
  padding: 7px 16px;
  color: #1a0f06;
  font-weight: 700;
  font-size: 0.8rem;
  background: linear-gradient(180deg, #ffd98a, #c07528);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: filter 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.desk-action-btn:hover,
.desk-action-btn:focus-visible {
  filter: brightness(1.08);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.38), 0 0 12px rgba(255, 200, 100, 0.15);
}

.desk-action-btn:active {
  transform: scale(0.96);
}

/* ── Footer: quick questions + input ── */
.desk-dialogue-foot {
  display: grid;
  gap: 12px;
  padding: 14px 20px 16px;
  border-top: 1px solid rgba(238, 184, 91, 0.16);
  background: linear-gradient(180deg, transparent, rgba(255, 220, 132, 0.03));
}

.desk-quick-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.desk-quick-chip {
  border: 1px solid rgba(238, 184, 91, 0.38);
  border-radius: 999px;
  padding: 8px 16px;
  color: rgba(255, 232, 190, 0.84);
  background: rgba(21, 12, 7, 0.46);
  font-size: 0.84rem;
  line-height: 1.35;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.desk-quick-chip:hover,
.desk-quick-chip:focus-visible {
  border-color: var(--gold-bright);
  color: #fff2d0;
  background: rgba(82, 45, 16, 0.6);
  box-shadow: 0 0 16px rgba(255, 200, 100, 0.12);
  transform: translateY(-1px);
}

.desk-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.desk-input {
  flex: 1;
  min-height: 46px;
  border: 1px solid rgba(224, 163, 72, 0.5);
  border-radius: 12px;
  padding: 0 16px;
  color: #ffe8b9;
  background: rgba(11, 7, 4, 0.62);
  box-shadow: inset 0 2px 14px rgba(0, 0, 0, 0.38);
  font-family: var(--font-body);
  font-size: 0.95rem;
  transition: border-color 180ms ease, box-shadow 180ms ease;
}

.desk-input::placeholder {
  color: rgba(255, 232, 190, 0.36);
}

.desk-input:focus-visible {
  outline: none;
  border-color: rgba(255, 217, 128, 0.85);
  box-shadow: inset 0 2px 14px rgba(0, 0, 0, 0.38), 0 0 0 3px rgba(255, 200, 100, 0.1);
}

.desk-input:disabled {
  opacity: 0.5;
}

.desk-send {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  border: 1px solid #f3ca75;
  border-radius: 50%;
  padding: 0;
  color: #221205;
  background: linear-gradient(180deg, #ffd98a, #c07528);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.38), 0 0 14px rgba(255, 200, 100, 0.08);
  cursor: pointer;
  transition: filter 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.desk-send svg {
  width: 21px;
  height: 21px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.desk-send:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.46), 0 0 22px rgba(255, 200, 100, 0.16);
}

.desk-send:active:not(:disabled) {
  transform: scale(0.93);
  filter: brightness(0.9);
}

.desk-send:disabled {
  cursor: not-allowed;
  filter: grayscale(0.4);
  opacity: 0.5;
  transform: none;
}

/* ── Reduced motion ── */
@media (prefers-reduced-motion: reduce) {
  .desk-dialogue,
  .loading-text::after,
  .msg-slide-enter-active {
    animation: none;
    transition: none;
  }
}

/* ── Mobile ── */
@media (max-width: 720px) {
  .desk-scene {
    background-position: center 14%;
  }

  .desk-dialogue {
    left: 0;
    right: 0;
    bottom: 0;
    transform: none;
    width: 100%;
    max-height: 64svh;
    border-radius: 20px 20px 0 0;
    animation: deskBarInMobile 360ms cubic-bezier(0.18, 0.86, 0.2, 1) both;
  }

  @keyframes deskBarInMobile {
    from { opacity: 0; transform: translateY(28px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .desk-dialogue-head {
    gap: 10px 12px;
  }

  .desk-status {
    width: 100%;
  }

  .desk-msg {
    max-width: 88%;
  }
}
</style>
