import { request } from './httpClient'

function withQuery(endpoint, params = {}) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    query.set(key, String(value))
  })
  const queryString = query.toString()
  return queryString ? `${endpoint}?${queryString}` : endpoint
}

export const submissionApi = {
  list(params) {
    return request(withQuery('/submissions', params))
  },
  create(payload) {
    return request('/submissions', { method: 'POST', body: payload })
  },
  detail(submissionId) {
    return request(`/submissions/${submissionId}`)
  },
  reviewQueue() {
    return request('/submissions/review-queue')
  },
  // Get draft data for a quest (repository, branch, PR candidates)
  getDraft(questId) {
    return request(`/quests/${questId}/submission-draft`)
  },
}
