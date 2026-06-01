import { request } from './httpClient'

/**
 * 站内通知接口（P4-024）。全部作用于"当前登录用户"，无需传 userId——
 * 后端从 JWT 解析接收者，与成长档案 /me 端点风格一致。
 */
export const notificationApi = {
  list(limit = 20) {
    return request(`/users/me/notifications?limit=${limit}`)
  },
  markRead(notificationId) {
    return request(`/users/me/notifications/${notificationId}/read`, { method: 'POST' })
  },
  markAllRead() {
    return request('/users/me/notifications/read-all', { method: 'POST' })
  },
}
