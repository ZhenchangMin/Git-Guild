import { reactive } from 'vue'

const STORAGE_KEY = 'git-guild-session'

function readSavedSession() {
  try {
    const sessionValue = sessionStorage.getItem(STORAGE_KEY)
    if (sessionValue) return { ...JSON.parse(sessionValue), persistence: 'session' }

    const localValue = localStorage.getItem(STORAGE_KEY)
    if (localValue) return { ...JSON.parse(localValue), persistence: 'local' }
    return {}
  } catch {
    return {}
  }
}

const savedSession = readSavedSession()
let activePersistence = savedSession.persistence === 'session' ? 'session' : 'local'

export const sessionStore = reactive({
  token: savedSession.token ?? '',
  refreshToken: savedSession.refreshToken ?? '',
  role: savedSession.role ?? 'VISITOR',
  user: savedSession.user ?? null,
})

function writeSession(storage) {
  storage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      token: sessionStore.token,
      refreshToken: sessionStore.refreshToken,
      role: sessionStore.role,
      user: sessionStore.user,
    }),
  )
}

function persistSession() {
  localStorage.removeItem(STORAGE_KEY)
  sessionStorage.removeItem(STORAGE_KEY)

  const storage = activePersistence === 'local' ? localStorage : sessionStorage
  writeSession(storage)
}

export function setSession({ token = '', refreshToken = '', role = 'VISITOR', user = null, persist = true } = {}) {
  sessionStore.token = token
  sessionStore.refreshToken = refreshToken
  sessionStore.role = role
  sessionStore.user = user
  activePersistence = persist ? 'local' : 'session'

  persistSession()
}

export function updateSessionUser(patch = {}) {
  sessionStore.user = {
    ...(sessionStore.user ?? {}),
    ...patch,
  }
  persistSession()
}

export function clearSession() {
  sessionStore.token = ''
  sessionStore.refreshToken = ''
  sessionStore.role = 'VISITOR'
  sessionStore.user = null
  localStorage.removeItem(STORAGE_KEY)
  sessionStorage.removeItem(STORAGE_KEY)
}

export function hasLoginSession() {
  return Boolean(sessionStore.token)
}
