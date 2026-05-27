import { createRouter, createWebHistory } from 'vue-router'

import { routes } from './routes'
import { hasLoginSession, sessionStore } from '../stores/sessionStore'

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

function homeForRole(role) {
  if (role === 'ADMIN') return { name: 'admin-review' }
  if (role === 'MAINTAINER') return { name: 'maintainer-workbench' }
  if (role === 'ADVENTURER') return { name: 'hall' }
  return { name: 'quest-board' }
}

router.beforeEach((to) => {
  if (to.name === 'login' && hasLoginSession()) {
    return homeForRole(sessionStore.role)
  }

  if (to.meta.requiresAuth && !hasLoginSession()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const allowedRoles = to.meta.roles
  if (Array.isArray(allowedRoles) && !allowedRoles.includes(sessionStore.role)) {
    return homeForRole(sessionStore.role)
  }

  return true
})
