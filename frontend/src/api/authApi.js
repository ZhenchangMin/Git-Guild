import { request } from './httpClient'

export const authApi = {
  register(payload) {
    return request('/auth/register', { method: 'POST', body: payload })
  },
  login(payload) {
    return request('/auth/login', { method: 'POST', body: payload })
  },
  forgotPassword(payload) {
    return request('/auth/forgot-password', { method: 'POST', body: payload })
  },
  resetPassword(payload) {
    return request('/auth/reset-password', { method: 'POST', body: payload })
  },
  me() {
    return request('/users/me')
  },
  logout() {
    return request('/auth/logout', { method: 'POST' })
  },
}
