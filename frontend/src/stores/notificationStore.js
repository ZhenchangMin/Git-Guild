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
  // 角落里的瞬时提示队列（新到通知时入队，过几秒自动出队）。
  toasts: [],
  // 当前在详情弹窗里打开的通知；null = 弹窗关闭。
  selected: null,
})

let pollTimer = null
// 已经"见过"的通知 ID 基线：首屏加载只记录、不弹 toast，之后才把新增的弹成角落提示。
let seenIds = new Set()
let baselineReady = false
const TOAST_TTL_MS = 6000

function canFetch() {
  return hasLoginSession() && sessionStore.role !== 'VISITOR'
}

function applyFeed(feed) {
  const items = feed?.items ?? []
  notificationStore.items = items
  notificationStore.unreadCount = feed?.unreadCount ?? 0
  if (feed) {
    detectNewToasts(items)
  } else {
    // 登出/访客：清空基线与瞬时提示，避免下次登录串号。
    seenIds = new Set()
    baselineReady = false
    notificationStore.toasts = []
    notificationStore.selected = null
  }
}

// 对比基线，找出本轮新出现的未读通知并弹成角落 toast。
function detectNewToasts(items) {
  if (!baselineReady) {
    items.forEach((item) => seenIds.add(item.notificationId))
    baselineReady = true
    return
  }
  for (const item of items) {
    if (seenIds.has(item.notificationId)) continue
    seenIds.add(item.notificationId)
    if (item.status === 'UNREAD') pushToast(item)
  }
}

function pushToast(item) {
  if (notificationStore.toasts.some((t) => t.notificationId === item.notificationId)) return
  notificationStore.toasts = [...notificationStore.toasts, item]
  window.setTimeout(() => dismissToast(item.notificationId), TOAST_TTL_MS)
}

export function dismissToast(notificationId) {
  notificationStore.toasts = notificationStore.toasts.filter((t) => t.notificationId !== notificationId)
}

// 打开通知详情弹窗：同时收起对应 toast，并标记为已读。
export function openNotification(item) {
  notificationStore.selected = item
  dismissToast(item.notificationId)
  if (item.status === 'UNREAD') markNotificationRead(item.notificationId)
}

export function closeNotification() {
  notificationStore.selected = null
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
