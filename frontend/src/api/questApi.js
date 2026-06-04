import { request } from './httpClient'

function withQuery(endpoint, params = {}) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    query.set(key, Array.isArray(value) ? value.join(',') : String(value))
  })
  const queryString = query.toString()
  return queryString ? `${endpoint}?${queryString}` : endpoint
}

export const questApi = {
  list(params) {
    return request(withQuery('/quests', params))
  },
  detail(questId) {
    return request(`/quests/${questId}`)
  },
  assign(questId) {
    return request(`/quests/${questId}/assignments`, { method: 'POST' })
  },
  // 为当前用户在该 Quest 的 active 接取记录确保 task branch（幂等，Issue #12）
  ensureTaskBranch(questId) {
    return request(`/quests/${questId}/task-branch`, { method: 'POST' })
  },
  myAssignments() {
    return request('/users/me/quest-assignments')
  },
  recommendations(params) {
    return request(withQuery('/recommendations/quests', params))
  },
}
