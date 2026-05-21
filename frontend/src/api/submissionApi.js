import { requestMock } from './httpClient'

export const submissionApi = {
  create(payload) {
    return requestMock('/submissions', { method: 'POST', body: payload })
  },
  detail(submissionId) {
    return requestMock(`/submissions/${submissionId}`)
  },
}
