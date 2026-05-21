import { requestMock } from './httpClient'

export const repositoryApi = {
  list(params) {
    return requestMock('/repositories', { params })
  },
  importRepository(payload) {
    return requestMock('/repositories/import', { method: 'POST', body: payload })
  },
  sync(repositoryId, payload) {
    return requestMock(`/repositories/${repositoryId}/sync`, { method: 'POST', body: payload })
  },
}
