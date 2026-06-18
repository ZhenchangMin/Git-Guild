<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import deskImg from '../../assets/desk.webp'
import { growthApi } from '../../api/growthApi'
import { questApi } from '../../api/questApi'
import { submissionApi } from '../../api/submissionApi'
import { sessionStore } from '../../stores/sessionStore'
import { useAssistantChat } from '../../composables/useAssistantChat'

const router = useRouter()
const pageEnteredAt = Date.now()

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
  deleteMessage,
  formatMessageTime,
} = useAssistantChat({ page: 'front-desk' })

const sceneRef = ref(null)
const welcomeBubbleRef = ref(null)
const userBubbleRef = ref(null)
const historyOpen = ref(false)
const sceneMetrics = ref(null)
const welcomeBubbleSize = ref({ width: 420, height: 120 })
const userBubbleSize = ref({ width: 260, height: 74 })
let sceneResizeObserver = null
let bubbleResizeObserver = null
const observedBubbleElements = new Set()

const DESK_IMAGE = {
  width: 1672,
  height: 941,
  positionX: 0.5,
  positionY: 0.2,
}

const CHARACTER_BOX = {
  x: 636,
  y: 160,
  width: 430.5,
  height: 502,
}

const CHARACTER_SAFE_MARGIN = 42
const SCREEN_EDGE = 32
const BUBBLE_RADIUS = 22
const NPC_BUBBLE_TAIL_WIDTH = 18
const NPC_BUBBLE_HOTZONE_GAP = -50
const USER_BUBBLE_TAIL_HEIGHT = 16

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
const clamp = (value, min, max) => Math.min(Math.max(value, min), max)

const displayName = computed(() => {
  if (isAdventurer.value) return sessionStore.user?.displayName || sessionStore.user?.username || '冒险家'
  if (isMaintainer.value) return sessionStore.user?.displayName || sessionStore.user?.username || '委托人'
  return sessionStore.user?.displayName || sessionStore.user?.username || '来访者'
})

const defaultWelcomeBubbleText = computed(() => {
  if (isVisitor.value) return welcomeMessage.value
  return `欢迎回来，${displayName.value}！我是艾丽丝，GitGuild AI 向导。有什么可以帮你的？`
})

const currentVisitMessages = computed(() => messages.value.filter((message) => {
  const createdAt = Date.parse(message.createdAt)
  return Number.isFinite(createdAt) && createdAt >= pageEnteredAt
}))

const latestAssistantMessage = computed(() => {
  for (let index = currentVisitMessages.value.length - 1; index >= 0; index -= 1) {
    const message = currentVisitMessages.value[index]
    if (message.role === 'npc') return message
  }
  return null
})

const latestUserMessage = computed(() => {
  for (let index = currentVisitMessages.value.length - 1; index >= 0; index -= 1) {
    const message = currentVisitMessages.value[index]
    if (message.role === 'user') return message
  }
  return null
})

const welcomeBubbleText = computed(() => {
  if (loading.value) return '艾丽丝正在翻阅公会手册'
  return latestAssistantMessage.value?.text || defaultWelcomeBubbleText.value
})

const welcomeBubbleActions = computed(() => {
  if (loading.value) return []
  return latestAssistantMessage.value?.actions ?? []
})

const promptBubbles = computed(() => {
  const suggestedQuestions = latestAssistantMessage.value?.suggestedQuestions ?? []
  return (suggestedQuestions.length ? suggestedQuestions : quickQuestions.value).slice(0, 4)
})

const historyButtonLabel = computed(() => {
  const count = messages.value.length
  return count > 0 ? `历史 ${count}` : '历史'
})

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

const welcomeBubbleStyle = computed(() => {
  const metrics = sceneMetrics.value
  if (!metrics) return {}

  const { character, width, height } = metrics
  const leftSpace = character.left - SCREEN_EDGE
  const rightSpace = width - character.right - SCREEN_EDGE
  const useLeftSide = leftSpace >= 340 || leftSpace >= rightSpace
  const sideGap = useLeftSide ? NPC_BUBBLE_TAIL_WIDTH + NPC_BUBBLE_HOTZONE_GAP : 24
  const maxWidth = useLeftSide ? leftSpace - sideGap : rightSpace - sideGap
  const bubbleWidth = clamp(Math.min(520, maxWidth), 300, 520)
  const left = useLeftSide
    ? clamp(character.left - bubbleWidth - sideGap, SCREEN_EDGE, width - bubbleWidth - SCREEN_EDGE)
    : clamp(character.right + sideGap, SCREEN_EDGE, width - bubbleWidth - SCREEN_EDGE)
  const top = clamp(character.top + 34, 104, height - 320)

  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${bubbleWidth}px`,
  }
})

const userBubbleStyle = computed(() => {
  const metrics = sceneMetrics.value
  if (!metrics) return {}

  const { width } = metrics
  const inputDockWidth = clamp(Math.min(860, width - 84), 260, width - SCREEN_EDGE * 2)
  const inputDockRight = width / 2 + inputDockWidth / 2
  const right = clamp(width - inputDockRight, SCREEN_EDGE, width - inputDockWidth - SCREEN_EDGE)

  return {
    right: `${right}px`,
    bottom: '126px',
    maxWidth: `${inputDockWidth}px`,
  }
})

const welcomeBubbleFrameWidth = computed(() => Math.ceil(welcomeBubbleSize.value.width + NPC_BUBBLE_TAIL_WIDTH))
const welcomeBubbleFrameHeight = computed(() => Math.ceil(welcomeBubbleSize.value.height))
const userBubbleFrameWidth = computed(() => Math.ceil(userBubbleSize.value.width))
const userBubbleFrameHeight = computed(() => Math.ceil(userBubbleSize.value.height + USER_BUBBLE_TAIL_HEIGHT))

const welcomeBubbleFrameStyle = computed(() => ({
  width: `${welcomeBubbleFrameWidth.value}px`,
  height: `${welcomeBubbleFrameHeight.value}px`,
}))

const userBubbleFrameStyle = computed(() => ({
  width: `${userBubbleFrameWidth.value}px`,
  height: `${userBubbleFrameHeight.value}px`,
}))

const welcomeBubbleViewBox = computed(() => (
  `0 0 ${welcomeBubbleFrameWidth.value} ${welcomeBubbleFrameHeight.value}`
))

const userBubbleViewBox = computed(() => (
  `0 0 ${userBubbleFrameWidth.value} ${userBubbleFrameHeight.value}`
))

const welcomeBubblePath = computed(() => {
  const width = Math.max(44, Math.ceil(welcomeBubbleSize.value.width))
  const height = Math.max(44, Math.ceil(welcomeBubbleSize.value.height))
  const radius = Math.min(BUBBLE_RADIUS, width / 2 - 1, height / 2 - 1)
  const tailWidth = NPC_BUBBLE_TAIL_WIDTH
  const tailHeight = Math.min(28, Math.max(18, height * 0.28))
  const tailCenter = clamp(height * 0.5, radius + tailHeight / 2, height - radius - tailHeight / 2)
  const tailTop = tailCenter - tailHeight / 2
  const tailBottom = tailCenter + tailHeight / 2
  const tailTip = width + tailWidth - 1

  return [
    `M ${radius} 1`,
    `H ${width - radius}`,
    `Q ${width} 1 ${width} ${radius}`,
    `V ${tailTop}`,
    `L ${tailTip} ${tailCenter}`,
    `L ${width} ${tailBottom}`,
    `V ${height - radius}`,
    `Q ${width} ${height} ${width - radius} ${height}`,
    `H ${radius}`,
    `Q 1 ${height} 1 ${height - radius}`,
    `V ${radius}`,
    `Q 1 1 ${radius} 1`,
    'Z',
  ].join(' ')
})

const userBubblePath = computed(() => {
  const width = Math.max(44, Math.ceil(userBubbleSize.value.width))
  const height = Math.max(44, Math.ceil(userBubbleSize.value.height))
  const radius = Math.min(BUBBLE_RADIUS, width / 2 - 1, height / 2 - 1)
  const tailHeight = USER_BUBBLE_TAIL_HEIGHT
  const tailWidth = 28
  const tailCenter = clamp(width - 46, radius + tailWidth / 2, width - radius - tailWidth / 2)
  const tailRight = tailCenter + tailWidth / 2
  const tailLeft = tailCenter - tailWidth / 2

  return [
    `M ${radius} 1`,
    `H ${width - radius}`,
    `Q ${width} 1 ${width} ${radius}`,
    `V ${height - radius}`,
    `Q ${width} ${height} ${width - radius} ${height}`,
    `H ${tailRight}`,
    `L ${tailCenter + 6} ${height}`,
    `L ${tailCenter} ${height + tailHeight - 1}`,
    `L ${tailCenter - 6} ${height}`,
    `H ${tailLeft}`,
    `H ${radius}`,
    `Q 1 ${height} 1 ${height - radius}`,
    `V ${radius}`,
    `Q 1 1 ${radius} 1`,
    'Z',
  ].join(' ')
})

const promptCloudStyle = computed(() => {
  const metrics = sceneMetrics.value
  if (!metrics) return {}

  const { character, width } = metrics
  const availableWidth = character.left - SCREEN_EDGE - 16
  const cloudWidth = clamp(Math.min(610, availableWidth), 360, 610)
  const left = clamp(SCREEN_EDGE + 32, SCREEN_EDGE, width - cloudWidth - SCREEN_EDGE)

  return {
    left: `${left}px`,
    width: `${cloudWidth}px`,
  }
})

function updateSceneMetrics() {
  const scene = sceneRef.value
  if (!scene) return

  const rect = scene.getBoundingClientRect()
  if (!rect.width || !rect.height) return

  const scale = Math.max(rect.width / DESK_IMAGE.width, rect.height / DESK_IMAGE.height)
  const renderedWidth = DESK_IMAGE.width * scale
  const renderedHeight = DESK_IMAGE.height * scale
  const offsetX = (rect.width - renderedWidth) * DESK_IMAGE.positionX
  const offsetY = (rect.height - renderedHeight) * DESK_IMAGE.positionY

  const character = {
    left: clamp(offsetX + CHARACTER_BOX.x * scale - CHARACTER_SAFE_MARGIN, SCREEN_EDGE, rect.width - SCREEN_EDGE),
    top: clamp(offsetY + CHARACTER_BOX.y * scale - CHARACTER_SAFE_MARGIN, SCREEN_EDGE, rect.height - SCREEN_EDGE),
    right: clamp(
      offsetX + (CHARACTER_BOX.x + CHARACTER_BOX.width) * scale + CHARACTER_SAFE_MARGIN,
      SCREEN_EDGE,
      rect.width - SCREEN_EDGE,
    ),
    bottom: clamp(
      offsetY + (CHARACTER_BOX.y + CHARACTER_BOX.height) * scale + CHARACTER_SAFE_MARGIN,
      SCREEN_EDGE,
      rect.height - SCREEN_EDGE,
    ),
  }

  sceneMetrics.value = {
    width: rect.width,
    height: rect.height,
    character,
  }
}

function updateBubbleSize(target, sizeRef) {
  if (!target) return
  const rect = target.getBoundingClientRect()
  if (!rect.width || !rect.height) return

  const width = Math.ceil(rect.width)
  const height = Math.ceil(rect.height)
  if (sizeRef.value.width === width && sizeRef.value.height === height) return
  sizeRef.value = { width, height }
}

function observeBubbleElement(target, sizeRef) {
  if (!target || !bubbleResizeObserver || observedBubbleElements.has(target)) return
  observedBubbleElements.add(target)
  bubbleResizeObserver.observe(target)
  updateBubbleSize(target, sizeRef)
}

function refreshBubbleObservers() {
  observeBubbleElement(welcomeBubbleRef.value, welcomeBubbleSize)
  observeBubbleElement(userBubbleRef.value, userBubbleSize)
}

function openHistory() {
  historyOpen.value = true
  nextTick(() => {
    const el = messageList.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function closeHistory() {
  historyOpen.value = false
}

onMounted(async () => {
  updateSceneMetrics()
  if (sceneRef.value && typeof ResizeObserver !== 'undefined') {
    sceneResizeObserver = new ResizeObserver(updateSceneMetrics)
    sceneResizeObserver.observe(sceneRef.value)
  }
  if (typeof ResizeObserver !== 'undefined') {
    bubbleResizeObserver = new ResizeObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.target === welcomeBubbleRef.value) updateBubbleSize(entry.target, welcomeBubbleSize)
        if (entry.target === userBubbleRef.value) updateBubbleSize(entry.target, userBubbleSize)
      })
    })
    await nextTick()
    refreshBubbleObservers()
  }

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

watch([latestUserMessage, welcomeBubbleText, welcomeBubbleActions], () => {
  nextTick(refreshBubbleObservers)
})

onBeforeUnmount(() => {
  sceneResizeObserver?.disconnect()
  sceneResizeObserver = null
  bubbleResizeObserver?.disconnect()
  bubbleResizeObserver = null
  observedBubbleElements.clear()
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
    <section ref="sceneRef" class="desk-scene" :style="{ backgroundImage: `url(${deskImg})` }">
      <div class="desk-scrim" aria-hidden="true"></div>

      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
        <span>返回大厅</span>
      </button>

      <button class="back-orb desk-history-orb" type="button" aria-label="打开历史记录" @click="openHistory">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 8v5l3 2" />
          <path d="M4 12a8 8 0 1 0 2.34-5.66" />
          <path d="M4 4v5h5" />
        </svg>
        <span>{{ historyButtonLabel }}</span>
      </button>

      <aside class="desk-status-dock" aria-label="当前身份状态">
        <span
          v-for="chip in statusChips"
          :key="chip.label"
          class="desk-status-chip"
          :class="{ urgent: chip.urgent }"
        >
          <i>{{ chip.label }}</i>
          <b>{{ chip.value }}</b>
        </span>
        <button
          v-if="primaryShortcut.routeName !== 'quest-board'"
          class="desk-status-cta desk-status-cta--alt"
          type="button"
          @click="navigate('quest-board')"
        >
          <span class="desk-cta-glyph" aria-hidden="true">❖</span>委托板
        </button>
        <button class="desk-status-cta" type="button" @click="navigate(primaryShortcut.routeName)">
          {{ primaryShortcut.label }}
        </button>
      </aside>

      <section
        ref="welcomeBubbleRef"
        class="desk-welcome-bubble"
        :style="welcomeBubbleStyle"
        aria-live="polite"
        aria-label="艾丽丝欢迎语"
      >
        <svg
          class="bubble-frame bubble-frame--npc"
          :style="welcomeBubbleFrameStyle"
          :viewBox="welcomeBubbleViewBox"
          aria-hidden="true"
          focusable="false"
        >
          <defs>
            <linearGradient id="deskNpcBubbleFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stop-color="#22130a" stop-opacity="0.94" />
              <stop offset="1" stop-color="#0f0905" stop-opacity="0.86" />
            </linearGradient>
          </defs>
          <path
            :d="welcomeBubblePath"
            fill="url(#deskNpcBubbleFill)"
            stroke="#eeb85b"
            stroke-opacity="0.42"
            stroke-width="1.2"
            stroke-linejoin="round"
          />
        </svg>
        <span class="desk-npc-icon" aria-hidden="true">&#9872;</span>
        <div class="desk-welcome-copy">
          <strong>艾丽丝</strong>
          <p class="desk-welcome-message" :class="{ 'is-loading': loading }">
            <span v-if="loading" class="loading-text">{{ welcomeBubbleText }}</span>
            <template v-else>{{ welcomeBubbleText }}</template>
          </p>

          <div v-if="!loading && latestAssistantMessage?.source" class="desk-source desk-welcome-meta">
            <span v-if="latestAssistantMessage.source === 'AI'" class="source-badge ai">&#9733; AI 回答</span>
            <span v-else-if="latestAssistantMessage.source === 'FAQ'" class="source-badge faq">&#9733; 帮助手册</span>
            <span v-else class="source-badge fallback">&#9888; 降级回答</span>
          </div>

          <div v-if="welcomeBubbleActions.length" class="desk-actions desk-welcome-actions">
            <button
              v-for="action in welcomeBubbleActions"
              :key="action.routeName"
              class="desk-action-btn"
              type="button"
              @click="navigate(action.routeName)"
            >{{ action.label }}</button>
          </div>
        </div>
      </section>

      <Transition name="msg-slide">
        <section
          ref="userBubbleRef"
          v-if="latestUserMessage"
          class="desk-user-echo"
          :style="userBubbleStyle"
          aria-label="你的最近发言"
        >
          <svg
            class="bubble-frame bubble-frame--user"
            :style="userBubbleFrameStyle"
            :viewBox="userBubbleViewBox"
            aria-hidden="true"
            focusable="false"
          >
            <defs>
              <linearGradient id="deskUserBubbleFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#22130a" stop-opacity="0.94" />
                <stop offset="1" stop-color="#0f0905" stop-opacity="0.86" />
              </linearGradient>
            </defs>
            <path
              :d="userBubblePath"
              fill="url(#deskUserBubbleFill)"
              stroke="#eeb85b"
              stroke-opacity="0.42"
              stroke-width="1.2"
              stroke-linejoin="round"
            />
          </svg>
          <p class="desk-bubble">{{ latestUserMessage.text }}</p>
        </section>
      </Transition>

      <section class="desk-prompt-cloud" :style="promptCloudStyle" aria-label="推荐问题">
        <button
          v-for="q in promptBubbles"
          :key="q"
          class="desk-quick-chip"
          type="button"
          @click="send(q)"
        >{{ q }}</button>
      </section>

      <footer class="desk-input-dock" aria-label="向艾丽丝提问">
        <div class="desk-input-row">
          <input
            v-model="draft"
            class="desk-input"
            type="text"
            placeholder="向艾丽丝提问..."
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

      <Transition name="history-fade">
        <section
          v-if="historyOpen"
          class="desk-history-layer"
          role="dialog"
          aria-modal="true"
          aria-label="艾丽丝历史对话"
          tabindex="-1"
          @keydown.escape="closeHistory"
        >
          <button class="history-scrim" type="button" aria-label="关闭历史记录" @click="closeHistory"></button>

          <article class="history-panel">
            <header class="history-head">
              <div>
                <span>GitGuild AI 向导</span>
                <h2>本次对话记录</h2>
              </div>
              <button class="history-close" type="button" aria-label="关闭历史记录" @click="closeHistory">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              </button>
            </header>

            <div ref="messageList" class="history-body">
              <p v-if="messages.length === 0" class="history-empty">当前页面还没有对话记录。</p>

              <div v-for="msg in messages" :key="`history-${msg.id}`" class="history-msg" :class="msg.role">
                <p class="history-bubble">{{ msg.text }}</p>

                <div class="history-meta">
                  <time v-if="msg.createdAt" :datetime="msg.createdAt">{{ formatMessageTime(msg.createdAt) }}</time>
                  <button class="history-delete" type="button" @click="deleteMessage(msg.id)">删除</button>
                </div>

                <div v-if="msg.source" class="desk-source history-source">
                  <span v-if="msg.source === 'AI'" class="source-badge ai">&#9733; AI 回答</span>
                  <span v-else-if="msg.source === 'FAQ'" class="source-badge faq">&#9733; 帮助手册</span>
                  <span v-else class="source-badge fallback">&#9888; 降级回答</span>
                </div>

                <div v-if="msg.actions && msg.actions.length" class="desk-actions history-actions">
                  <button
                    v-for="action in msg.actions"
                    :key="`history-${msg.id}-${action.routeName}`"
                    class="desk-action-btn"
                    type="button"
                    @click="navigate(action.routeName)"
                  >{{ action.label }}</button>
                </div>
              </div>

              <div v-if="loading" class="history-msg npc">
                <p class="history-bubble desk-loading">
                  <span class="loading-text">艾丽丝正在翻阅公会手册</span>
                </p>
              </div>
            </div>
          </article>
        </section>
      </Transition>
    </section>
  </main>
</template>

<style scoped>
.desk-scene {
  position: relative;
  width: 100%;
  min-height: 100svh;
  overflow: hidden;
  background-position: center 20%;
  background-size: cover;
  isolation: isolate;
}

.desk-scene::before {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  content: '';
  background:
    linear-gradient(90deg, rgba(5, 3, 1, 0.3), transparent 22%, transparent 72%, rgba(5, 3, 1, 0.28)),
    radial-gradient(ellipse at 50% 34%, transparent 18%, rgba(5, 3, 1, 0.22) 73%);
}

.desk-scrim {
  position: absolute;
  inset: auto 0 0 0;
  height: 36%;
  background: linear-gradient(to top, rgba(9, 5, 2, 0.86) 4%, rgba(9, 5, 2, 0.36) 52%, transparent);
  pointer-events: none;
  z-index: 1;
}

.back-orb {
  z-index: 20;
}

.desk-history-orb {
  top: 70px;
  z-index: 20;
}

.desk-status-dock {
  position: absolute;
  top: 28px;
  right: 32px;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  max-width: min(760px, calc(100% - 140px));
  padding: 8px;
  border: 1px solid rgba(238, 184, 91, 0.24);
  border-radius: 16px;
  background: rgba(14, 8, 4, 0.42);
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(8px);
}

.desk-status-chip {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  min-height: 32px;
  border: 1px solid rgba(238, 184, 91, 0.24);
  border-radius: 999px;
  padding: 5px 11px;
  background: rgba(10, 6, 3, 0.36);
}

.desk-status-chip i {
  color: rgba(255, 232, 190, 0.54);
  font-style: normal;
  font-size: 0.7rem;
}

.desk-status-chip b {
  color: #ffe4ad;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 0.9rem;
  font-variant-numeric: tabular-nums;
}

.desk-status-chip.urgent {
  border-color: rgba(232, 140, 100, 0.5);
  background: rgba(110, 42, 36, 0.34);
}

.desk-status-chip.urgent b {
  color: #ffb088;
}

.desk-status-cta,
.desk-action-btn {
  border: 1px solid rgba(245, 195, 99, 0.56);
  color: #1a0f06;
  font-weight: 700;
  background: linear-gradient(180deg, #ffd98a, #c07528);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.32);
  cursor: pointer;
  transition: filter 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.desk-status-cta {
  min-height: 32px;
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 0.78rem;
}

.desk-status-cta:hover,
.desk-status-cta:focus-visible,
.desk-action-btn:hover,
.desk-action-btn:focus-visible {
  filter: brightness(1.08);
  transform: translateY(-1px);
  box-shadow: 0 7px 22px rgba(0, 0, 0, 0.4), 0 0 16px rgba(255, 200, 100, 0.16);
}

.desk-status-cta:active,
.desk-action-btn:active {
  transform: scale(0.96);
}

/* 次级 CTA：翡翠实心变体，与金色主按钮同尺寸但用高对比色，一眼可辨（金+绿纹章配色）。 */
.desk-status-cta--alt {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: linear-gradient(180deg, #5fd9a4, #1c8c5e);
  color: #042a1d;
  border-color: rgba(150, 240, 200, 0.62);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.32), 0 0 14px rgba(45, 200, 140, 0.22);
}

.desk-status-cta--alt:hover,
.desk-status-cta--alt:focus-visible {
  filter: brightness(1.08);
  box-shadow: 0 7px 22px rgba(0, 0, 0, 0.4), 0 0 18px rgba(60, 220, 150, 0.36);
}

.desk-cta-glyph {
  font-size: 0.74rem;
  color: currentColor;
  opacity: 0.78;
  line-height: 1;
}

.desk-welcome-bubble,
.desk-user-echo,
.desk-prompt-cloud,
.desk-input-dock {
  position: absolute;
  z-index: 4;
}

.desk-welcome-bubble {
  box-sizing: border-box;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  min-height: 98px;
  padding: 16px 18px 17px;
  overflow: visible;
  color: rgba(255, 235, 200, 0.92);
  animation: bubbleArrive 520ms cubic-bezier(0.18, 0.86, 0.2, 1) both;
  isolation: isolate;
}

.bubble-frame {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 0;
  pointer-events: none;
  overflow: visible;
}

.bubble-frame--npc,
.bubble-frame--user {
  filter: drop-shadow(0 22px 62px rgba(0, 0, 0, 0.42));
}

.desk-npc-icon {
  position: relative;
  z-index: 3;
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

.desk-welcome-copy {
  position: relative;
  z-index: 3;
}

.desk-welcome-copy strong {
  display: block;
  color: #ffe4a8;
  font-family: var(--font-display);
  font-size: 1rem;
}

.desk-welcome-message {
  margin: 5px 0 0;
  max-height: min(34svh, 15em);
  overflow: auto;
  padding-right: 8px;
  color: rgba(255, 235, 200, 0.88);
  line-height: 1.58;
  overflow-wrap: anywhere;
  scrollbar-gutter: stable;
  text-wrap: pretty;
}

.desk-user-echo {
  box-sizing: border-box;
  pointer-events: auto;
  display: grid;
  gap: 7px;
  width: fit-content;
  padding: 14px 24px 17px 18px;
  overflow: visible;
  color: rgba(255, 235, 200, 0.92);
  animation: bubbleArrive 420ms cubic-bezier(0.18, 0.86, 0.2, 1) both;
  isolation: isolate;
}

.desk-user-echo {
  justify-items: end;
  z-index: 7;
}

.desk-bubble,
.history-bubble {
  box-sizing: border-box;
  margin: 0;
  padding: 11px 15px;
  border-radius: 17px;
  line-height: 1.58;
  text-wrap: pretty;
  word-break: break-word;
}

.desk-bubble {
  position: relative;
  z-index: 1;
  max-height: min(30svh, 13em);
  overflow: auto;
  font-size: 0.92rem;
}

.desk-user-echo .desk-bubble {
  width: fit-content;
  min-width: 0;
  max-width: 100%;
  border-radius: 0;
  padding: 0 11px 0 0;
  color: rgba(255, 235, 200, 0.88);
  background: transparent;
  font-size: 1rem;
  overflow-wrap: anywhere;
  scrollbar-gutter: stable;
  white-space: normal;
}

.history-msg.user .history-bubble {
  color: rgba(255, 235, 200, 0.94);
  background: rgba(24, 14, 8, 0.78);
  border: 1px solid rgba(238, 184, 91, 0.28);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
}

.history-msg.npc .history-bubble {
  color: rgba(255, 235, 200, 0.94);
  background: rgba(24, 14, 8, 0.78);
  border: 1px solid rgba(238, 184, 91, 0.28);
  backdrop-filter: blur(8px);
}

.desk-user-echo .desk-bubble {
  max-height: 6.32em;
  overflow-x: hidden;
  overflow-y: auto;
}

.desk-loading {
  display: flex;
  align-items: center;
  min-height: 42px;
}

.loading-text {
  color: rgba(255, 232, 190, 0.62);
  font-style: italic;
}

.loading-text::after {
  content: '';
  display: inline-block;
  width: 1em;
  text-align: left;
  animation: deskDotPulse 1.2s steps(1, end) infinite;
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
  backdrop-filter: blur(6px);
}

.source-badge.ai {
  border: 1px solid rgba(119, 160, 91, 0.56);
  color: #d7ffd8;
  background: rgba(67, 97, 58, 0.5);
}

.source-badge.faq {
  border: 1px solid rgba(238, 184, 91, 0.48);
  color: #ffe6b1;
  background: rgba(133, 76, 20, 0.46);
}

.source-badge.fallback {
  border: 1px solid rgba(202, 104, 88, 0.56);
  color: #ffe0da;
  background: rgba(110, 42, 36, 0.46);
}

.desk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.desk-welcome-meta,
.desk-welcome-actions {
  margin-top: 9px;
}

.desk-action-btn {
  border-radius: 999px;
  padding: 6px 13px;
  font-size: 0.78rem;
}

.desk-prompt-cloud {
  bottom: 118px;
  display: flex;
  flex-direction: column;
  gap: 9px;
  align-items: flex-start;
}

.desk-quick-chip {
  width: fit-content;
  max-width: 100%;
  min-height: 42px;
  border: 1px solid rgba(238, 184, 91, 0.4);
  border-radius: 14px;
  padding: 9px 16px;
  color: rgba(255, 232, 190, 0.86);
  background:
    linear-gradient(180deg, rgba(37, 21, 11, 0.68), rgba(13, 8, 5, 0.58)),
    rgba(21, 12, 7, 0.46);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(8px);
  font-size: 0.84rem;
  line-height: 1.35;
  text-align: left;
  text-wrap: pretty;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
  animation: bubbleArrive 420ms cubic-bezier(0.18, 0.86, 0.2, 1) both;
}

.desk-quick-chip:nth-child(2) {
  animation-delay: 70ms;
}

.desk-quick-chip:nth-child(3) {
  animation-delay: 120ms;
}

.desk-quick-chip:nth-child(4) {
  animation-delay: 170ms;
}

.desk-quick-chip:hover,
.desk-quick-chip:focus-visible {
  border-color: var(--gold-bright);
  color: #fff2d0;
  background: rgba(82, 45, 16, 0.66);
  box-shadow: 0 0 18px rgba(255, 200, 100, 0.16), 0 12px 28px rgba(0, 0, 0, 0.36);
  transform: translateY(-2px);
}

.desk-input-dock {
  left: 50%;
  bottom: 28px;
  z-index: 6;
  width: min(860px, calc(100% - 84px));
  transform: translateX(-50%);
}

.desk-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
  min-height: 58px;
  border: 1px solid rgba(238, 184, 91, 0.36);
  border-radius: 999px;
  padding: 6px 7px 6px 18px;
  background:
    linear-gradient(180deg, rgba(20, 12, 7, 0.78), rgba(8, 5, 3, 0.76)),
    rgba(11, 7, 4, 0.62);
  box-shadow:
    0 20px 58px rgba(0, 0, 0, 0.48),
    inset 0 1px 0 rgba(255, 238, 188, 0.12);
  backdrop-filter: blur(10px);
}

.desk-input {
  flex: 1;
  min-width: 0;
  min-height: 42px;
  border: 0;
  padding: 0 4px;
  color: #ffe8b9;
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.96rem;
}

.desk-input::placeholder {
  color: rgba(255, 232, 190, 0.4);
}

.desk-input:focus-visible {
  outline: none;
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

.desk-send svg,
.history-close svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.desk-send svg {
  width: 21px;
  height: 21px;
  stroke-width: 2.4;
}

.desk-send:hover:not(:disabled),
.desk-send:focus-visible:not(:disabled) {
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

.history-fade-enter-active,
.history-fade-leave-active {
  transition: opacity 220ms ease;
}

.history-fade-enter-from,
.history-fade-leave-to {
  opacity: 0;
}

.desk-history-layer {
  position: fixed;
  inset: 0;
  z-index: 1600;
  display: grid;
  place-items: center;
  padding: 48px;
}

.history-scrim {
  position: absolute;
  inset: 0;
  border: 0;
  padding: 0;
  background: rgba(5, 3, 1, 0.62);
  backdrop-filter: blur(9px);
}

.history-panel {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  width: min(980px, calc(100vw - 96px));
  height: min(720px, calc(100svh - 96px));
  border: 1px solid rgba(231, 177, 90, 0.44);
  border-radius: 22px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(33, 18, 9, 0.94), rgba(14, 8, 4, 0.96)),
    radial-gradient(ellipse at 12% 0%, rgba(216, 154, 50, 0.14), transparent 50%);
  box-shadow:
    0 34px 90px rgba(0, 0, 0, 0.58),
    inset 0 1px 0 rgba(255, 238, 188, 0.18);
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(238, 184, 91, 0.18);
}

.history-head span {
  color: rgba(255, 232, 190, 0.58);
  font-size: 0.78rem;
}

.history-head h2 {
  margin: 3px 0 0;
  color: #ffe4a8;
  font-family: var(--font-display);
  font-size: 1.32rem;
}

.history-close {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid rgba(238, 184, 91, 0.38);
  border-radius: 50%;
  color: #ffe4ad;
  background: rgba(17, 9, 5, 0.52);
  cursor: pointer;
  transition: background 160ms ease, border-color 160ms ease, transform 160ms ease;
}

.history-close svg {
  width: 19px;
  height: 19px;
  stroke-width: 2.2;
}

.history-close:hover,
.history-close:focus-visible {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.72);
  transform: translateY(-1px);
}

.history-body {
  overflow-y: auto;
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 22px 24px;
}

.history-empty {
  margin: auto;
  color: rgba(255, 232, 190, 0.58);
}

.history-msg {
  display: grid;
  gap: 7px;
  max-width: min(74%, 680px);
}

.history-msg.user {
  justify-self: end;
  justify-items: end;
}

.history-msg.npc {
  justify-self: start;
  justify-items: start;
}

.history-bubble {
  font-size: 0.95rem;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.3);
}

.history-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-inline: 4px;
  color: rgba(255, 232, 190, 0.48);
  font-size: 0.72rem;
  line-height: 1.4;
}

.history-msg.user .history-meta {
  justify-content: flex-end;
}

.history-delete {
  border: 1px solid rgba(238, 184, 91, 0.26);
  border-radius: 999px;
  padding: 2px 8px;
  color: rgba(255, 232, 190, 0.68);
  background: rgba(17, 9, 5, 0.36);
  font-size: 0.72rem;
  cursor: pointer;
  transition: border-color 160ms ease, color 160ms ease, background 160ms ease, transform 160ms ease;
}

.history-delete:hover,
.history-delete:focus-visible {
  border-color: rgba(232, 140, 100, 0.58);
  color: #ffe0da;
  background: rgba(110, 42, 36, 0.42);
  transform: translateY(-1px);
}

.history-source,
.history-actions {
  margin-inline: 4px;
}

.msg-slide-enter-active {
  transition: opacity 280ms ease, transform 280ms cubic-bezier(0.18, 0.86, 0.2, 1);
}

.msg-slide-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

@keyframes bubbleArrive {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes deskDotPulse {
  0%   { content: ''; }
  25%  { content: '.'; }
  50%  { content: '..'; }
  75%  { content: '...'; }
}

@media (max-width: 1180px) {
  .desk-status-dock {
    max-width: calc(100% - 112px);
  }

  .desk-input-dock {
    width: min(860px, calc(100% - 72px));
  }
}

@media (prefers-reduced-motion: reduce) {
  .desk-welcome-bubble,
  .desk-user-echo,
  .desk-quick-chip,
  .loading-text::after,
  .msg-slide-enter-active {
    animation: none;
    transition: none;
  }
}
</style>
