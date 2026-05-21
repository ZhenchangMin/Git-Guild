import { requestMock } from './httpClient'

export const leaderboardApi = {
  xp(params) {
    return requestMock('/leaderboards/xp', { params })
  },
}
