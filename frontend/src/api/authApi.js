import { request } from './httpClient'

export const authApi = {
  register(payload) {
    return request('/auth/register', { method: 'POST', body: payload })
  },
  login(payload) {
    return request('/auth/login', { method: 'POST', body: payload })
  },
  me() {
    return request('/users/me')
  },
  logout() {
    return request('/auth/logout', { method: 'POST' })
  },
}
