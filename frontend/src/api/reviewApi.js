import { request } from './httpClient'

export const reviewApi = {
  reviewSubmission(submissionId, payload) {
    return request(`/submissions/${submissionId}/reviews`, { method: 'POST', body: payload })
  },
  // 与「接受提交」解耦的独立动作：代理合并该提交对应的 PR。
  mergePullRequest(submissionId) {
    return request(`/submissions/${submissionId}/merge`, { method: 'POST' })
  },
}
