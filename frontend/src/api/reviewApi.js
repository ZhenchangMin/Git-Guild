import { requestMock } from './httpClient'

export const reviewApi = {
  reviewSubmission(submissionId, payload) {
    return requestMock(`/submissions/${submissionId}/reviews`, { method: 'POST', body: payload })
  },
}
