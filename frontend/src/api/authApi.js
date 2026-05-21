import { requestMock } from './httpClient'

export const authApi = {
  login(payload) {
    return requestMock('/auth/login', { method: 'POST', body: payload })
  },
  me() {
    return requestMock('/auth/me')
  },
}
