import { request, requestMock } from './httpClient'

export const growthApi = {
  summary() {
    return request('/users/me/growth')
  },
  badges() {
    return request('/users/me/badges')
  },
  contributions(params) {
    return requestMock('/users/me/contributions', { params })
  },
}
