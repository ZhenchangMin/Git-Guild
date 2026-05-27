import { reactive } from 'vue'

const STORAGE_KEY = 'git-guild-session'

function readSavedSession() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}')
  } catch {
    return {}
  }
}

const savedSession = readSavedSession()

export const sessionStore = reactive({
  token: savedSession.token ?? '',
  refreshToken: savedSession.refreshToken ?? '',
  role: savedSession.role ?? 'VISITOR',
  user: savedSession.user ?? null,
})

export function setSession({ token = '', refreshToken = '', role = 'VISITOR', user = null } = {}) {
  sessionStore.token = token
  sessionStore.refreshToken = refreshToken
  sessionStore.role = role
  sessionStore.user = user

  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      token,
      refreshToken,
      role,
      user,
    }),
  )
}

export function clearSession() {
  setSession()
  localStorage.removeItem(STORAGE_KEY)
}

export function hasLoginSession() {
  return Boolean(sessionStore.token)
}
