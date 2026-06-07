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
  create(payload) {
    return request('/quests', { method: 'POST', body: payload })
  },
  submitForReview(questId) {
    return request(`/quests/${questId}/submit`, { method: 'POST' })
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
  categories(params) {
    return request(withQuery('/quest-categories', params))
  },
  tags(params) {
    return request(withQuery('/quest-tags', params))
  },
  myAssignments() {
    return request('/quests/me/assignments')
  },
  submissionDraft(questId) {
    return request(`/quests/${questId}/submission-draft`)
  },
  // 当前维护者发布的全部委托（含 DRAFT/待审核 等所有状态）。
  myPublished() {
    return request('/quests/me/published')
  },
}
