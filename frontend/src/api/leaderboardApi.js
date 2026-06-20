import { request } from './httpClient'

function toQuery(params) {
  const entries = Object.entries(params ?? {}).filter(([, v]) => v !== undefined && v !== null && v !== '')
  if (entries.length === 0) return ''
  return `?${entries.map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&')}`
}

export const leaderboardApi = {
  xp(params) {
    return request(`/leaderboards/xp${toQuery(params)}`)
  },
}
