import { request } from './httpClient'

export const assistantApi = {
  chat({ message, page }) {
    return request('/assistant/chat', {
      method: 'POST',
      body: { message, page },
    })
  },
}
