import { request } from './httpClient'

export const growthApi = {
  summary() {
    return request('/users/me/growth')
  },
  summaryFor(userId) {
    return request(`/users/${userId}/growth`)
  },
  badges() {
    return request('/users/me/badges')
  },
  badgesFor(userId) {
    return request(`/users/${userId}/badges`)
  },
  // 真实「贡献历程」：返回 { items: [...], repoCount }，无贡献时为空列表 + 0。
  contributions() {
    return request('/users/me/contributions')
  },
  contributionsFor(userId) {
    return request(`/users/${userId}/contributions`)
  },
}
