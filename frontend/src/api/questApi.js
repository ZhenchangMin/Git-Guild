import { requestMock } from './httpClient'

export const questApi = {
  list(params) {
    return requestMock('/quests', { params })
  },
  detail(questId) {
    return requestMock(`/quests/${questId}`)
  },
  assign(questId) {
    return requestMock(`/quests/${questId}/assignments`, { method: 'POST' })
  },
}
