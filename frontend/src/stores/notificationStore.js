import { reactive } from 'vue'

import { notificationApi } from '../api/notificationApi'
import { ApiError } from '../api/httpClient'
import { hasLoginSession, sessionStore } from './sessionStore'

/**
 * 站内通知前端状态（P4-024，轻量通知）。
 *
 * 设计取向：保持"轻"。不引入 Pinia，沿用 sessionStore 的 reactive 单例模式；
 * 用一个 setInterval 轮询代替 WebSocket/SSE——满足"关键状态变化在页面中可见"即可，
 * 不追求实时推送。访客（未登录）一律不拉取。
 */
const POLL_INTERVAL_MS = 30_000

export const notificationStore = reactive({
  items: [],
  unreadCount: 0,
  loading: false,
  error: '',
})

let pollTimer = null

function canFetch() {
  return hasLoginSession() && sessionStore.role !== 'VISITOR'
}

function applyFeed(feed) {
  notificationStore.items = feed?.items ?? []
  notificationStore.unreadCount = feed?.unreadCount ?? 0
}

/** 拉取最近通知与未读数。未登录时清空并直接返回。 */
export async function loadNotifications(limit = 20) {
  if (!canFetch()) {
    applyFeed(null)
    return
  }
  notificationStore.loading = true
  notificationStore.error = ''
  try {
    const response = await notificationApi.list(limit)
    applyFeed(response?.data)
  } catch (error) {
    notificationStore.error = error instanceof ApiError ? error.message : '通知加载失败'
  } finally {
    notificationStore.loading = false
  }
}

/** 标记单条已读，后端回传刷新后的 Feed。失败时静默保留旧状态。 */
export async function markNotificationRead(notificationId) {
  if (!canFetch()) return
  try {
    const response = await notificationApi.markRead(notificationId)
    applyFeed(response?.data)
  } catch (error) {
    notificationStore.error = error instanceof ApiError ? error.message : '操作失败'
  }
}

/** 全部标记已读。 */
export async function markAllNotificationsRead() {
  if (!canFetch()) return
  try {
    const response = await notificationApi.markAllRead()
    applyFeed(response?.data)
  } catch (error) {
    notificationStore.error = error instanceof ApiError ? error.message : '操作失败'
  }
}

/** 启动轮询：立即拉一次，随后每 30s 刷新。重复调用安全（先清旧定时器）。 */
export function startNotificationPolling() {
  stopNotificationPolling()
  if (!canFetch()) return
  loadNotifications()
  pollTimer = window.setInterval(loadNotifications, POLL_INTERVAL_MS)
}

/** 停止轮询并清空（如退出登录时调用）。 */
export function stopNotificationPolling() {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}
