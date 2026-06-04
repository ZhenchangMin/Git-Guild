import { request } from './httpClient'

export const submissionApi = {
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
