import { requestMock } from './httpClient'

export const adminApi = {
  listQuestReviewApplications(params) {
    return requestMock('/admin/quest-review-applications', { params })
  },
  approveApplication(applicationId, payload) {
    return requestMock(`/admin/quest-review-applications/${applicationId}/approve`, { method: 'POST', body: payload })
  },
}
