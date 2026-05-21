import { reactive } from 'vue'

export const sessionStore = reactive({
  token: '',
  role: 'VISITOR',
  user: null,
})

export function setSession({ token = '', role = 'VISITOR', user = null } = {}) {
  sessionStore.token = token
  sessionStore.role = role
  sessionStore.user = user
}

export function clearSession() {
  setSession()
}
