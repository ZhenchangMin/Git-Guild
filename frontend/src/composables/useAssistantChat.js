import { computed, nextTick, ref, watch } from 'vue'

import { assistantApi } from '../api/assistantApi'
import { sessionStore } from '../stores/sessionStore'

let idCounter = 0
let deleteConfirmUserKey = ''
let singleDeleteConfirmed = false

const HISTORY_STORAGE_PREFIX = 'gitguild:assistant:history'
const HISTORY_TTL_MS = 30 * 24 * 60 * 60 * 1000
const HISTORY_LIMIT = 1000

function resolveMaybeRef(value) {
  const nextValue = typeof value === 'function' ? value() : value
  if (nextValue && typeof nextValue === 'object' && 'value' in nextValue) return nextValue.value
  return nextValue
}

function userHistorySegment() {
  const user = sessionStore.user ?? {}
  if (user.userId) return `user:${user.userId}`
  if (user.id) return `user:${user.id}`
  if (user.username) return `username:${user.username}`
  if (user.email) return `email:${user.email}`
  return sessionStore.role ? `role:${sessionStore.role}` : 'visitor'
}

function buildStorageKey(userKey) {
  return `${HISTORY_STORAGE_PREFIX}:${userKey}`
}

function getHistoryStorage() {
  if (typeof window === 'undefined') return null

  try {
    return window.localStorage
  } catch {
    return null
  }
}

function nowIso() {
  return new Date().toISOString()
}

function normalizeCreatedAt(value, fallback = nowIso()) {
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? fallback : new Date(parsed).toISOString()
}

function normalizeStoredMessage(message, fallbackCreatedAt = nowIso()) {
  const numericId = Number(message?.id)
  const id = Number.isFinite(numericId) && numericId > 0 ? numericId : ++idCounter
  idCounter = Math.max(idCounter, id)

  const normalized = {
    id,
    role: message?.role === 'user' ? 'user' : 'npc',
    text: typeof message?.text === 'string' ? message.text : '',
    createdAt: normalizeCreatedAt(message?.createdAt, fallbackCreatedAt),
    suggestedQuestions: Array.isArray(message?.suggestedQuestions) ? message.suggestedQuestions : [],
    actions: Array.isArray(message?.actions) ? message.actions : [],
  }

  if (message?.source) normalized.source = message.source
  return normalized
}

function pruneMessages(items) {
  const cutoff = Date.now() - HISTORY_TTL_MS
  return items
    .filter((message) => {
      const createdAt = Date.parse(message.createdAt)
      return Number.isFinite(createdAt) && createdAt >= cutoff
    })
    .slice(-HISTORY_LIMIT)
}

function loadSavedMessages(storageKey) {
  const storage = getHistoryStorage()
  if (!storage) return []

  try {
    const raw = storage.getItem(storageKey)
    if (!raw) return []

    const payload = JSON.parse(raw)
    const savedAt = Number(payload?.savedAt)
    if (Number.isFinite(savedAt) && Date.now() - savedAt > HISTORY_TTL_MS) {
      storage.removeItem(storageKey)
      return []
    }

    const rawMessages = Array.isArray(payload) ? payload : payload?.messages
    if (!Array.isArray(rawMessages)) return []

    const fallbackCreatedAt = Number.isFinite(savedAt) ? new Date(savedAt).toISOString() : nowIso()
    return pruneMessages(rawMessages.map((message) => normalizeStoredMessage(message, fallbackCreatedAt)))
  } catch {
    return []
  }
}

function saveMessages(storageKey, items) {
  const storage = getHistoryStorage()
  if (!storage) return

  try {
    if (!items.length) {
      storage.removeItem(storageKey)
      return
    }

    storage.setItem(storageKey, JSON.stringify({
      version: 1,
      savedAt: Date.now(),
      messages: items,
    }))
  } catch {
    // Ignore storage quota or privacy-mode failures; chat still works for the current page.
  }
}

function resetDeleteConfirmationFor(userKey) {
  deleteConfirmUserKey = userKey
  singleDeleteConfirmed = false
}

function confirmSingleDelete(userKey) {
  if (deleteConfirmUserKey !== userKey) resetDeleteConfirmationFor(userKey)
  if (singleDeleteConfirmed) return true
  if (typeof window === 'undefined' || typeof window.confirm !== 'function') {
    singleDeleteConfirmed = true
    return true
  }

  const confirmed = window.confirm('删除这条对话记录？本次登录期间后续删除单条记录将不再确认。')
  if (confirmed) singleDeleteConfirmed = true
  return confirmed
}

function padTime(value) {
  return String(value).padStart(2, '0')
}

export function formatAssistantMessageTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''

  const now = new Date()
  const dateKey = date.toDateString()
  const nowKey = now.toDateString()
  const time = `${padTime(date.getHours())}:${padTime(date.getMinutes())}`

  if (dateKey === nowKey) return `今天 ${time}`

  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (dateKey === yesterday.toDateString()) return `昨天 ${time}`

  return `${date.getFullYear()}-${padTime(date.getMonth() + 1)}-${padTime(date.getDate())} ${time}`
}

const QUICK_QUESTIONS = {
  VISITOR: [
    'Git Guild 是什么？',
    '冒险家如何接取委托？',
    '委托人如何发布委托？',
    '有哪些演示账号？',
  ],
  ADVENTURER: [
    '我的委托下一步该做什么？',
    '如何提交成果？',
    '如何查看我的 PR？',
    '我被退回后怎么修改？',
  ],
  MAINTAINER: [
    '哪些提交需要我审核？',
    '如何发布新委托？',
    '如何合并 PR？',
    '我发布了哪些委托？',
  ],
}

export function useAssistantChat({ page }) {
  const messages = ref([])
  const draft = ref('')
  const loading = ref(false)
  const messageList = ref(null)
  const currentHistoryUserKey = computed(userHistorySegment)
  const currentHistoryStorageKey = computed(() => buildStorageKey(currentHistoryUserKey.value))

  const pageValue = () => resolveMaybeRef(page)

  const quickQuestions = computed(() => {
    const role = sessionStore.role
    if (role === 'ADVENTURER') return QUICK_QUESTIONS.ADVENTURER
    if (role === 'MAINTAINER') return QUICK_QUESTIONS.MAINTAINER
    return QUICK_QUESTIONS.VISITOR
  })

  const canSend = computed(() => draft.value.trim().length > 0 && !loading.value)

  const welcomeMessage = computed(() => (
    '欢迎来到 GitGuild！我是艾丽丝，GitGuild AI 向导，可以帮你快速了解GitGuild。'
  ))

  function scrollToBottom() {
    nextTick(() => {
      const el = messageList.value
      if (el) el.scrollTop = el.scrollHeight
    })
  }

  function createMessage(payload) {
    return {
      id: ++idCounter,
      createdAt: nowIso(),
      ...payload,
    }
  }

  function loadHistory() {
    messages.value = loadSavedMessages(currentHistoryStorageKey.value)
    scrollToBottom()
  }

  function persistHistory() {
    const pruned = pruneMessages(messages.value)
    if (pruned.length !== messages.value.length) {
      messages.value = pruned
      return
    }
    saveMessages(currentHistoryStorageKey.value, pruned)
  }

  async function send(messageText) {
    const text = (messageText ?? draft.value).trim()
    if (!text || loading.value) return

    messages.value.push(createMessage({ role: 'user', text }))
    draft.value = ''
    loading.value = true
    scrollToBottom()

    try {
      const res = await assistantApi.chat({ message: text, page: pageValue() })
      const data = res?.data ?? res
      messages.value.push(createMessage({
        role: 'npc',
        text: data.answer ?? '',
        source: data.source ?? 'FALLBACK',
        suggestedQuestions: data.suggestedQuestions ?? [],
        actions: data.actions ?? [],
      }))
    } catch {
      messages.value.push(createMessage({
        role: 'npc',
        text: '艾丽丝暂时无法回答。你可以查看帮助页面了解平台使用方法，或稍后再试。',
        source: 'FALLBACK',
        suggestedQuestions: ['冒险家怎么接任务？', '委托人怎么审核提交？'],
        actions: [],
      }))
    } finally {
      loading.value = false
      scrollToBottom()
    }
  }

  function onKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      send()
    }
  }

  function reset() {
    draft.value = ''
    loading.value = false
  }

  function deleteMessage(messageId) {
    if (!confirmSingleDelete(currentHistoryUserKey.value)) return false

    const targetId = String(messageId)
    const before = messages.value.length
    messages.value = messages.value.filter((message) => String(message.id) !== targetId)
    return messages.value.length < before
  }

  resetDeleteConfirmationFor(currentHistoryUserKey.value)
  loadHistory()

  watch(currentHistoryUserKey, (userKey, oldUserKey) => {
    if (userKey === oldUserKey) return
    resetDeleteConfirmationFor(userKey)
    loadHistory()
  })

  watch(messages, persistHistory, { deep: true })

  return {
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
    deleteMessage,
    formatMessageTime: formatAssistantMessageTime,
    scrollToBottom,
  }
}
