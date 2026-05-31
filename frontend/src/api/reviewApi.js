import { request } from './httpClient'

export const reviewApi = {
  reviewSubmission(submissionId, payload) {
    return request(`/submissions/${submissionId}/reviews`, { method: 'POST', body: payload })
  },
}
