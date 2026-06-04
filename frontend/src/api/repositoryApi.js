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
  importRepository(payload) {
    return request('/repositories/import', { method: 'POST', body: payload })
  },
  sync(repositoryId, payload) {
    return request(`/repositories/${repositoryId}/sync`, { method: 'POST', body: payload })
  },
  issues(repositoryId, params) {
    const query = new URLSearchParams()
    Object.entries(params ?? {}).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') return
      query.set(key, String(value))
    })
    const suffix = query.toString() ? `?${query}` : ''
    return request(`/repositories/${repositoryId}/issues${suffix}`)
  },
}
