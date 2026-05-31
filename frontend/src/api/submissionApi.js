import { request } from './httpClient'

export const submissionApi = {
  create(payload) {
    return request('/submissions', { method: 'POST', body: payload })
  },
  detail(submissionId) {
    return request(`/submissions/${submissionId}`)
  },
}
