import { request } from './httpClient'

export const userApi = {
  me() {
    return request('/users/me')
  },
  updateProfile(payload) {
    return request('/users/me', { method: 'PATCH', body: payload })
  },
  setDisplayBadge(badgeId) {
    return request('/users/me/display-badge', {
      method: 'PUT',
      body: { badgeId },
    })
  },
  uploadAvatar(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request('/users/me/avatar', {
      method: 'POST',
      body: formData,
    })
  },
}
