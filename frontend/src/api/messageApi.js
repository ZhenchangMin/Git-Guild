import { request } from './httpClient'

export const messageApi = {
  listThreads() {
    return request('/message-threads')
  },
  detail(threadId) {
    return request(`/message-threads/${threadId}`)
  },
  openQuestThread(questId) {
    return request(`/message-threads/by-quest/${questId}`)
  },
  send(threadId, content) {
    return request(`/message-threads/${threadId}/messages`, {
      method: 'POST',
      body: { content },
    })
  },
  markRead(threadId) {
    return request(`/message-threads/${threadId}/read`, { method: 'POST' })
  },
}
