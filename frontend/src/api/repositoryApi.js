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

export const repositoryApi = {
  list(params) {
    return request(withQuery('/repositories', params))
  },
  importRepository(payload) {
    return request('/repositories/import', { method: 'POST', body: payload })
  },
  sync(repositoryId, payload) {
    return request(`/repositories/${repositoryId}/sync`, { method: 'POST', body: payload })
  },
  createCommit(repositoryId, payload) {
    return request(`/repositories/${repositoryId}/commits`, { method: 'POST', body: payload })
  },
  createPullRequest(repositoryId, payload) {
    return request(`/repositories/${repositoryId}/pull-requests`, { method: 'POST', body: payload })
  },
  getPullRequest(repositoryId, pullRequestId) {
    return request(`/repositories/${repositoryId}/pull-requests/${pullRequestId}`)
  },
  mergePullRequest(repositoryId, pullRequestId) {
    return request(`/repositories/${repositoryId}/pull-requests/${pullRequestId}/merge`, { method: 'POST' })
  },
}
