import { request } from './httpClient'

export const growthApi = {
  summary() {
    return request('/users/me/growth')
  },
  badges() {
    return request('/users/me/badges')
  },
  // 真实「贡献历程」：返回 { items: [...], repoCount }，无贡献时为空列表 + 0。
  contributions() {
    return request('/users/me/contributions')
  },
}
