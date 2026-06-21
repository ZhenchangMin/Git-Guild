import { computed, reactive } from 'vue'

import { messageApi } from '../api/messageApi'
import { ApiError } from '../api/httpClient'
import { hasLoginSession, sessionStore } from './sessionStore'
import { closeOverlay, openOverlay } from './overlayStore'

export const messageStore = reactive({
  threads: [],
  activeThread: null,
  loadingThreads: false,
  loadingThread: false,
  sending: false,
  error: '',
})

export const messageUnreadCount = computed(() =>
  messageStore.threads.reduce((total, thread) => total + Number(thread.unreadCount || 0), 0),
)

function canFetchMessages() {
  return hasLoginSession() && sessionStore.role !== 'VISITOR'
}

function applyThreadList(items) {
  messageStore.threads = Array.isArray(items) ? items : []
}

function upsertThreadSummary(detail) {
  if (!detail?.threadId) return
  const summary = {
    threadId: detail.threadId,
    questId: detail.questId,
    questCode: detail.questCode,
    questTitle: detail.questTitle,
    counterpart: detail.counterpart,
    publisher: detail.publisher,
    assignee: detail.assignee,
    latestMessage: detail.messages?.at?.(-1)
      ? {
          content: detail.messages.at(-1).content,
          senderName: detail.messages.at(-1).senderName,
          createdAt: detail.messages.at(-1).createdAt,
          mine: detail.messages.at(-1).mine,
        }
      : null,
    unreadCount: detail.unreadCount ?? 0,
    lastMessageAt: detail.lastMessageAt,
  }
  const rest = messageStore.threads.filter((thread) => thread.threadId !== detail.threadId)
  messageStore.threads = [summary, ...rest].sort(
    (a, b) => new Date(b.lastMessageAt || 0).getTime() - new Date(a.lastMessageAt || 0).getTime(),
  )
}

function readError(error, fallback) {
  return error instanceof ApiError ? error.message : fallback
}

export async function loadMessageThreads() {
  if (!canFetchMessages()) {
    applyThreadList([])
    messageStore.activeThread = null
    return
  }

  messageStore.loadingThreads = true
  messageStore.error = ''
  try {
    const response = await messageApi.listThreads()
    applyThreadList(response?.data)
  } catch (error) {
    messageStore.error = readError(error, '信笺列表加载失败')
  } finally {
    messageStore.loadingThreads = false
  }
}

export async function openMessageCenter(threadId = null) {
  if (!canFetchMessages()) return
  openOverlay('message')
  await loadMessageThreads()
  const targetId = threadId ?? messageStore.activeThread?.threadId ?? messageStore.threads[0]?.threadId
  if (targetId) {
    await selectMessageThread(targetId)
  }
}

export function closeMessageCenter() {
  closeOverlay('message')
}

export async function openQuestMessages(questId) {
  if (!questId || !canFetchMessages()) return

  openOverlay('message')
  messageStore.loadingThread = true
  messageStore.error = ''
  try {
    const response = await messageApi.openQuestThread(questId)
    messageStore.activeThread = response?.data ?? null
    upsertThreadSummary(messageStore.activeThread)
    await messageApi.markRead(messageStore.activeThread.threadId).catch(() => null)
    await loadMessageThreads()
  } catch (error) {
    messageStore.error = readError(error, '当前委托暂时无法发起信笺')
  } finally {
    messageStore.loadingThread = false
  }
}

export async function selectMessageThread(threadId) {
  if (!threadId || !canFetchMessages()) return

  messageStore.loadingThread = true
  messageStore.error = ''
  try {
    const detail = await messageApi.detail(threadId)
    messageStore.activeThread = detail?.data ?? null
    if (messageStore.activeThread?.threadId) {
      const read = await messageApi.markRead(messageStore.activeThread.threadId).catch(() => null)
      if (read?.data) messageStore.activeThread = read.data
      upsertThreadSummary(messageStore.activeThread)
      await loadMessageThreads()
    }
  } catch (error) {
    messageStore.error = readError(error, '信笺内容加载失败')
  } finally {
    messageStore.loadingThread = false
  }
}

export async function sendMessage(content) {
  const normalized = String(content || '').trim()
  const threadId = messageStore.activeThread?.threadId
  if (!normalized || !threadId || messageStore.sending) return false

  messageStore.sending = true
  messageStore.error = ''

  try {
    const response = await messageApi.send(threadId, normalized)
    messageStore.activeThread = response?.data ?? messageStore.activeThread
    upsertThreadSummary(messageStore.activeThread)
    await loadMessageThreads()
    return true
  } catch (error) {
    messageStore.error = readError(error, '信笺发送失败')
    return false
  } finally {
    messageStore.sending = false
  }
}
