import { requestMock } from './httpClient'

export const growthApi = {
  summary() {
    return requestMock('/me/growth')
  },
  contributions(params) {
    return requestMock('/me/contributions', { params })
  },
}
