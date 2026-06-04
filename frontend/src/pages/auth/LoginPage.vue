<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { authApi } from '../../api'
import doorImg from '../../assets/door.png'
import { setSession } from '../../stores/sessionStore'

const ENTRY_ANIMATION_MS = 980
// 注册成功后，先让"账号已创建"的提示停留一会，用户看清后再进入开门动画。
const REGISTER_SUCCESS_HOLD_MS = 1600

const router = useRouter()
const route = useRoute()
const mode = ref('login')
const selectedRole = ref('BEGINNER')
const isSubmitting = ref(false)
const isGateOpen = ref(false)
const isEntering = ref(false)
const formMessage = ref('')
const formError = ref('')
const accountInput = ref(null)
const showPassword = ref(false)
let entryTimer = 0
let holdTimer = 0

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
    label: '冒险家',
    caption: '接取任务',
  },
  {
    id: 'MAINTAINER',
    entryRole: 'MAINTAINER',
    label: '委托人',
    caption: '发布悬赏',
  },
]

const isRegisterMode = computed(() => mode.value === 'register')
const activeRole = computed(() => roles.find((role) => role.id === selectedRole.value) ?? roles[0])
const submitButtonText = computed(() => {
  if (isSubmitting.value) return isRegisterMode.value ? '正在登记身份...' : '正在开启大门...'
  return isRegisterMode.value ? `注册为${activeRole.value.label}` : '进入公会'
})

function toEntryRole(apiRole) {
  if (apiRole === 'BEGINNER') return 'ADVENTURER'
  if (apiRole === 'ADMIN') return 'ADMIN'
  if (apiRole === 'MAINTAINER') return 'MAINTAINER'
  return 'ADVENTURER'
}

function routeForRole(entryRole) {
  if (entryRole === 'ADMIN') return { name: 'admin-review' }
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

// 仅当"按下"和"松开"都发生在遮罩层本身时才关闭登录框。
// 这样从输入框内按下、拖拽到框外松开（click 会冒泡到遮罩层）不会误触关闭。
const backdropPressed = ref(false)

function onLayerPointerDown(event) {
  backdropPressed.value = event.target === event.currentTarget
}

function onLayerPointerUp(event) {
  if (backdropPressed.value && event.target === event.currentTarget) {
    closeGate()
  }
  backdropPressed.value = false
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

// 注册成功后短暂停留，让用户看清"账号已创建"提示，再继续登录与开门动画。
function holdForRegisterSuccess() {
  return new Promise((resolve) => {
    window.clearTimeout(holdTimer)
    holdTimer = window.setTimeout(resolve, REGISTER_SUCCESS_HOLD_MS)
  })
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

async function submitAuth(event) {
  // 关闭浏览器在输入时自动弹出的原生校验气泡（表单已加 novalidate），
  // 改为仅在用户点击提交时主动触发——此时"邮箱需包含 @"等提示才会出现。
  const formEl = event?.target
  if (formEl instanceof HTMLFormElement && !formEl.reportValidity()) {
    return
  }

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
      formMessage.value = '账号已创建成功！稍候将为你开启公会大门……'
      await holdForRegisterSuccess()
    }

    const loginResponse = await authApi.login({
      email: form.account.trim(),
      password: form.password,
      remember: form.remember,
    })
    saveAuthSession(loginResponse.data)
  } catch (error) {
    formError.value = resolveAuthError(error)
  } finally {
    isSubmitting.value = false
  }
}

// 后端校验类错误（VALIDATION_FAILED）的 message 是笼统的"请求参数不合法"，
// 真正可操作的原因（如"password 必须同时包含字母和数字"）放在 details 里，
// 所以优先展示 details；业务类错误（邮箱已注册等）的 message 本身就具体。
function resolveAuthError(error) {
  if (error?.code === 'VALIDATION_FAILED' && error.details) {
    return error.details
  }
  return error?.message || '无法连接 Git Guild 后端，请确认服务已启动。'
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

onBeforeUnmount(() => {
  window.clearTimeout(entryTimer)
  window.clearTimeout(holdTimer)
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
      <div class="guild-gate-flare" aria-hidden="true"></div>

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
        <div
          v-if="isGateOpen && !isEntering"
          class="guild-login-layer"
          @pointerdown="onLayerPointerDown"
          @pointerup="onLayerPointerUp"
        >
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

            <form class="guild-auth-form" novalidate @submit.prevent="submitAuth">
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
                <div class="guild-field-control">
                  <input
                    v-model="form.password"
                    :type="showPassword ? 'text' : 'password'"
                    :autocomplete="isRegisterMode ? 'new-password' : 'current-password'"
                    :minlength="isRegisterMode ? 8 : undefined"
                    required
                    placeholder="输入你的公会密钥"
                  />
                  <button
                    class="guild-field-reveal"
                    type="button"
                    :aria-pressed="showPassword"
                    :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                    :title="showPassword ? '隐藏密码' : '显示密码'"
                    @click="showPassword = !showPassword"
                  >
                    <svg
                      v-if="!showPassword"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.6"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      aria-hidden="true"
                    >
                      <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    <svg
                      v-else
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.6"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      aria-hidden="true"
                    >
                      <path d="M3 3l18 18" />
                      <path d="M10.6 6.1A9.6 9.6 0 0 1 12 6c6 0 9.5 6 9.5 6a16 16 0 0 1-2.8 3.4" />
                      <path d="M6.3 7.6A15.7 15.7 0 0 0 2.5 12S6 18 12 18a9.3 9.3 0 0 0 3.4-.65" />
                      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
                    </svg>
                  </button>
                </div>
              </label>

              <div v-if="isRegisterMode" class="guild-role-block">
                <span>选择注册身份</span>
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
