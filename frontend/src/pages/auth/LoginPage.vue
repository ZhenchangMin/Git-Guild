<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { authApi } from '../../api'
import doorImg from '../../assets/door.png'
import { setSession } from '../../stores/sessionStore'

const ENTRY_ANIMATION_MS = 760

const router = useRouter()
const route = useRoute()
const mode = ref('login')
const selectedRole = ref('BEGINNER')
const isSubmitting = ref(false)
// When a guarded action (e.g. a guest tapping "接取委托") bounces the user
// here with a `redirect` query, skip the closed-gate intro and open the login
// modal immediately so they don't have to click the door first.
const isGateOpen = ref(typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/'))
const isEntering = ref(false)
const formMessage = ref('')
const formError = ref('')
const accountInput = ref(null)
let entryTimer = 0

const form = reactive({
  username: '',
  account: '',
  password: '',
  remember: true,
})

const roles = [
  {
    id: 'BEGINNER',
    entryRole: 'ADVENTURER',
    label: 'Adventurer',
    caption: '接取任务',
  },
  {
    id: 'MAINTAINER',
    entryRole: 'MAINTAINER',
    label: 'Maintainer',
    caption: '发布悬赏',
  },
]

const isRegisterMode = computed(() => mode.value === 'register')
const activeRole = computed(() => roles.find((role) => role.id === selectedRole.value) ?? roles[0])
const submitButtonText = computed(() => {
  if (isSubmitting.value) return isRegisterMode.value ? '正在登记身份...' : '正在开启大门...'
  return isRegisterMode.value ? `注册为 ${activeRole.value.label}` : `以 ${activeRole.value.label} 身份进入`
})

function toEntryRole(apiRole) {
  if (apiRole === 'BEGINNER') return 'ADVENTURER'
  if (apiRole === 'ADMIN') return 'ADMIN'
  if (apiRole === 'MAINTAINER') return 'MAINTAINER'
  return activeRole.value.entryRole
}

function routeForRole(entryRole) {
  if (entryRole === 'ADMIN') return { name: 'admin-review' }
  if (entryRole === 'MAINTAINER') return { name: 'maintainer-workbench' }
  return { name: 'hall' }
}

function routeAfterEntry(entryRole) {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && entryRole !== 'VISITOR') {
    return redirect
  }
  return routeForRole(entryRole)
}

function normalizeUser(user) {
  const entryRole = toEntryRole(user?.role)
  return {
    ...user,
    displayName: user?.username ?? user?.email ?? 'Git Guild 用户',
    role: entryRole,
    apiRole: user?.role,
  }
}

function openGate() {
  if (isEntering.value) return
  isGateOpen.value = true
  formError.value = ''
  formMessage.value = ''
  nextTick(() => accountInput.value?.focus())
}

function closeGate() {
  if (isSubmitting.value || isEntering.value) return
  isGateOpen.value = false
}

function switchMode(nextMode) {
  mode.value = nextMode
  formError.value = ''
  formMessage.value = ''
  nextTick(() => accountInput.value?.focus())
}

function beginEntryAnimation(targetRoute) {
  isEntering.value = true
  isGateOpen.value = true
  window.clearTimeout(entryTimer)
  entryTimer = window.setTimeout(() => {
    router.push(targetRoute)
  }, ENTRY_ANIMATION_MS)
}

function saveAuthSession(authData) {
  const user = normalizeUser(authData.user)
  setSession({
    token: authData.accessToken,
    refreshToken: authData.refreshToken,
    role: user.role,
    user,
  })
  beginEntryAnimation(routeAfterEntry(user.role))
}

async function submitAuth() {
  formError.value = ''
  formMessage.value = ''
  isSubmitting.value = true

  try {
    if (isRegisterMode.value) {
      await authApi.register({
        username: form.username.trim(),
        email: form.account.trim(),
        password: form.password,
        role: selectedRole.value,
      })
      formMessage.value = '账号已创建，正在开启公会大门。'
    }

    const loginResponse = await authApi.login({
      email: form.account.trim(),
      password: form.password,
      remember: form.remember,
    })
    saveAuthSession(loginResponse.data)
  } catch (error) {
    formError.value = error.message || '无法连接 Git Guild 后端，请确认服务已启动。'
  } finally {
    isSubmitting.value = false
  }
}

function enterAsVisitor() {
  setSession({
    role: 'VISITOR',
    user: {
      displayName: '游客',
      role: 'VISITOR',
    },
  })
  beginEntryAnimation({ name: 'quest-board' })
}

onMounted(() => {
  // Match openGate()'s focus handoff so an auto-opened modal lands focus on
  // the email field instead of the page body.
  if (isGateOpen.value) {
    nextTick(() => accountInput.value?.focus())
  }
})

onBeforeUnmount(() => {
  window.clearTimeout(entryTimer)
})
</script>

<template>
  <main class="app-shell guild-login-shell">
    <section
      class="guild-gate-scene"
      :class="{ 'is-awake': isGateOpen, 'is-entering': isEntering }"
      aria-label="Git Guild 公会入口"
    >
      <div class="guild-gate-art" :style="{ backgroundImage: `url(${doorImg})` }" aria-hidden="true"></div>

      <button
        v-if="!isGateOpen && !isEntering"
        class="guild-gate-hotspot"
        type="button"
        aria-label="点击进入公会"
        @click="openGate"
      >
        <span>点击进入公会</span>
      </button>

      <Transition name="guild-login-modal">
        <div v-if="isGateOpen && !isEntering" class="guild-login-layer" @click.self="closeGate">
          <aside class="guild-login-card" aria-label="登录或注册 Git Guild">
            <header class="guild-login-head">
              <p class="kicker">Git Guild Gate</p>
              <h1>{{ isRegisterMode ? '登记公会身份' : '进入公会大厅' }}</h1>
            </header>

            <div class="guild-auth-tabs" aria-label="登录或注册切换">
              <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">
                登录
              </button>
              <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">
                注册
              </button>
            </div>

            <form class="guild-auth-form" @submit.prevent="submitAuth">
              <label v-if="isRegisterMode" class="guild-field">
                <span>用户名</span>
                <input
                  v-model.trim="form.username"
                  autocomplete="username"
                  minlength="3"
                  maxlength="32"
                  required
                  placeholder="alice"
                />
              </label>

              <label class="guild-field">
                <span>账号 / 邮箱</span>
                <input
                  ref="accountInput"
                  v-model.trim="form.account"
                  type="email"
                  autocomplete="email"
                  required
                  placeholder="alice@example.com"
                />
              </label>

              <label class="guild-field">
                <span>密码</span>
                <input
                  v-model="form.password"
                  type="password"
                  :autocomplete="isRegisterMode ? 'new-password' : 'current-password'"
                  :minlength="isRegisterMode ? 8 : undefined"
                  required
                  placeholder="输入你的公会密钥"
                />
              </label>

              <div class="guild-role-block">
                <span>选择入口身份</span>
                <div class="guild-role-pills" aria-label="选择角色">
                  <button
                    v-for="role in roles"
                    :key="role.id"
                    class="guild-role-pill"
                    :class="{ active: selectedRole === role.id }"
                    type="button"
                    @click="selectedRole = role.id"
                  >
                    <strong>{{ role.label }}</strong>
                    <small>{{ role.caption }}</small>
                  </button>
                </div>
              </div>

              <label v-if="!isRegisterMode" class="guild-remember">
                <input v-model="form.remember" type="checkbox" />
                <span>保持登录状态</span>
              </label>

              <p v-if="formMessage" class="guild-auth-notice success">{{ formMessage }}</p>
              <p v-if="formError" class="guild-auth-notice error">{{ formError }}</p>

              <div class="guild-login-actions">
                <button class="guild-submit" type="submit" :disabled="isSubmitting || isEntering">
                  {{ submitButtonText }}
                </button>
                <button class="guild-guest-link" type="button" :disabled="isSubmitting" @click="enterAsVisitor">
                  游客参观
                </button>
              </div>
            </form>
          </aside>
        </div>
      </Transition>
    </section>
  </main>
</template>
