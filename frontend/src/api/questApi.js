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
  recommendations(params) {
    return request(withQuery('/recommendations/quests', params))
  },
}
