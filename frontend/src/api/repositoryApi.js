import { request } from './httpClient'

export const repositoryApi = {
  list(params) {
    const query = new URLSearchParams()
    Object.entries(params ?? {}).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') return
      query.set(key, String(value))
    })
    const suffix = query.toString() ? `?${query}` : ''
    return request(`/repositories${suffix}`)
  },
  detail(repositoryId) {
    return request(`/repositories/${repositoryId}`)
  },
  // 直接在平台本地 Gitea 新建仓库（委托人 / 管理员）。
  create(payload) {
    return request('/repositories', { method: 'POST', body: payload })
  },
  importRepository(payload, options = {}) {
    return request('/repositories/import', { method: 'POST', body: payload, signal: options.signal })
  },
  sync(repositoryId, payload, options = {}) {
    return request(`/repositories/${repositoryId}/sync`, { method: 'POST', body: payload, signal: options.signal })
  },
  issues(repositoryId, params, options = {}) {
    const query = new URLSearchParams()
    Object.entries(params ?? {}).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') return
      query.set(key, String(value))
    })
    const suffix = query.toString() ? `?${query}` : ''
    return request(`/repositories/${repositoryId}/issues${suffix}`, { signal: options.signal })
  },
  // Get all repositories for the current user (for workbench)
  myRepositories() {
    return request('/repositories')
  },
  // 删除已接入的仓库（级联清理其委托/提交/审核及 Gitea 副本）
  remove(repositoryId) {
    return request(`/repositories/${repositoryId}`, { method: 'DELETE' })
  },
}
