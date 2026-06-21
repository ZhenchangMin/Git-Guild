<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { authApi } from '../../api'
import doorImg from '../../assets/door.webp'
import hallImg from '../../assets/hall.webp'
import { setSession } from '../../stores/sessionStore'

const ENTRY_ANIMATION_MS = 980
// 注册成功后，先让"账号已创建"的提示停留一会，用户看清后再进入开门动画。
const REGISTER_SUCCESS_HOLD_MS = 1600
const HALL_ENTRY_FROM_GATE_KEY = 'gitguild.hallEntryFromGate'
const REMEMBER_PASSWORD_PREF_KEY = 'gitguild.rememberPassword'
const SAVED_LOGIN_ACCOUNT_KEY = 'gitguild.savedLoginAccount'

const router = useRouter()
const route = useRoute()
const mode = ref('login')
const isSubmitting = ref(false)
const isGateOpen = ref(false)
const isEntering = ref(false)
const formMessage = ref('')
const formError = ref('')
const accountInput = ref(null)
const authFormRef = ref(null)
const confirmPasswordInput = ref(null)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const rememberPassword = ref(false)
let entryTimer = 0
let holdTimer = 0
let hallPreloadImage = null

// 预加载大门背景图：图就绪前整个入口场景保持透明，就绪后按钮与背景一起淡入，
// 消除"按钮先冒出来、背景图随后才慢慢铺上"的割裂感。
const bgReady = ref(false)
const doorPreloader = new Image()
doorPreloader.onload = () => {
  bgReady.value = true
}
doorPreloader.onerror = () => {
  // 加载失败也放行，避免页面永久停在空白。
  bgReady.value = true
}
doorPreloader.src = doorImg

const form = reactive({
  username: '',
  account: '',
  password: '',
  confirmPassword: '',
  remember: false,
})

const AUTH_ERROR_MESSAGES = {
  USERNAME_ALREADY_TAKEN: '这个用户名已被使用，请换一个。',
  EMAIL_ALREADY_REGISTERED: '这个邮箱已经注册过，请直接登录或换一个邮箱。',
  INVALID_CREDENTIALS: '账号或密码不正确，请重新输入。',
  ACCOUNT_DISABLED: '这个账号已被禁用，请联系管理员。',
  FORBIDDEN: '当前注册入口不允许创建管理员账号。',
}

const isRegisterMode = computed(() => mode.value === 'register')
const submitButtonText = computed(() => {
  if (isSubmitting.value) return isRegisterMode.value ? '正在登记身份...' : '正在开启大门...'
  return isRegisterMode.value ? '注册并加入公会' : '进入公会'
})

// 冒险家与委托人账号已合并：登录后统一为成员（MAINTAINER）。
// 兼容历史 BEGINNER 账号——同样映射为成员。
function toEntryRole(apiRole) {
  if (apiRole === 'ADMIN') return 'ADMIN'
  return 'MAINTAINER'
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
  nextTick(async () => {
    await restorePasswordCredential()
    accountInput.value?.focus()
  })
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
  form.confirmPassword = ''
  confirmPasswordInput.value?.setCustomValidity('')
  nextTick(() => accountInput.value?.focus())
}

function beginEntryAnimation(targetRoute) {
  isEntering.value = true
  isGateOpen.value = true
  rememberHallEntrySource(targetRoute)
  preloadHallImage(targetRoute)
  window.clearTimeout(entryTimer)
  entryTimer = window.setTimeout(() => {
    router.push(targetRoute)
  }, ENTRY_ANIMATION_MS)
}

function isHallRoute(targetRoute) {
  return (
    targetRoute?.name === 'hall' ||
    (typeof targetRoute === 'string' && targetRoute.startsWith('/hall'))
  )
}

function rememberHallEntrySource(targetRoute) {
  if (!isHallRoute(targetRoute)) return

  try {
    window.sessionStorage.setItem(HALL_ENTRY_FROM_GATE_KEY, 'true')
  } catch {
    // 过门厅淡出只是视觉增强；存储不可用时不影响登录跳转。
  }
}

function preloadHallImage(targetRoute) {
  const shouldPreloadHall = isHallRoute(targetRoute)

  if (!shouldPreloadHall || hallPreloadImage) return
  hallPreloadImage = new Image()
  hallPreloadImage.decoding = 'async'
  hallPreloadImage.src = hallImg
}

// 注册成功后短暂停留，让用户看清"账号已创建"提示，再继续登录与开门动画。
function holdForRegisterSuccess() {
  return new Promise((resolve) => {
    window.clearTimeout(holdTimer)
    holdTimer = window.setTimeout(resolve, REGISTER_SUCCESS_HOLD_MS)
  })
}

function restoreSavedLoginPreference() {
  try {
    rememberPassword.value = window.localStorage.getItem(REMEMBER_PASSWORD_PREF_KEY) === 'true'
    const savedAccount = window.localStorage.getItem(SAVED_LOGIN_ACCOUNT_KEY)
    if (savedAccount && !form.account) {
      form.account = savedAccount
    }
  } catch {
    rememberPassword.value = false
  }
}

function persistLoginPreference() {
  try {
    if (!rememberPassword.value) {
      window.localStorage.removeItem(REMEMBER_PASSWORD_PREF_KEY)
      window.localStorage.removeItem(SAVED_LOGIN_ACCOUNT_KEY)
      return
    }

    window.localStorage.setItem(REMEMBER_PASSWORD_PREF_KEY, 'true')
    window.localStorage.setItem(SAVED_LOGIN_ACCOUNT_KEY, form.account.trim())
  } catch {
    // 密码不写入网页存储；浏览器密码管理器会基于标准 autocomplete 接管保存。
  }
}

async function storePasswordCredential() {
  if (!rememberPassword.value || isRegisterMode.value || !authFormRef.value) return
  if (!window.PasswordCredential || !navigator.credentials?.store) return

  try {
    const credential = new window.PasswordCredential(authFormRef.value)
    await navigator.credentials.store(credential)
  } catch {
    // 浏览器可能因非 HTTPS、隐私设置或用户拒绝而不允许保存，保留原生密码管理器兜底。
  }
}

async function restorePasswordCredential() {
  if (!rememberPassword.value || isRegisterMode.value) return
  if (!window.PasswordCredential || !navigator.credentials?.get) return

  try {
    const credential = await navigator.credentials.get({
      password: true,
      mediation: 'optional',
    })
    if (credential?.type !== 'password') return
    if (credential.id && !form.account) form.account = credential.id
    if (credential.password && !form.password) form.password = credential.password
  } catch {
    // 自动回填完全取决于浏览器能力和用户授权；失败时不打断登录流程。
  }
}

function saveAuthSession(authData) {
  const user = normalizeUser(authData.user)
  setSession({
    token: authData.accessToken,
    refreshToken: authData.refreshToken,
    role: user.role,
    user,
    persist: form.remember,
  })
  beginEntryAnimation(routeAfterEntry(user.role))
}

function syncPasswordConfirmationValidity() {
  const input = confirmPasswordInput.value
  if (!input) return true

  const mismatch = isRegisterMode.value && form.confirmPassword && form.password !== form.confirmPassword
  input.setCustomValidity(mismatch ? '两次输入的密码不一致' : '')
  return !mismatch
}

async function submitAuth(event) {
  // 关闭浏览器在输入时自动弹出的原生校验气泡（表单已加 novalidate），
  // 改为仅在用户点击提交时主动触发——此时"邮箱需包含 @"等提示才会出现。
  const formEl = event?.target
  syncPasswordConfirmationValidity()
  if (formEl instanceof HTMLFormElement && !formEl.reportValidity()) {
    return
  }

  formError.value = ''
  formMessage.value = ''

  if (isRegisterMode.value && form.password !== form.confirmPassword) {
    formError.value = '两次输入的密码不一致，请重新确认。'
    confirmPasswordInput.value?.focus()
    return
  }

  isSubmitting.value = true

  try {
    if (isRegisterMode.value) {
      await authApi.register({
        username: form.username.trim(),
        email: form.account.trim(),
        password: form.password,
      })
      formMessage.value = '账号已创建成功！稍候将为你开启公会大门……'
      await holdForRegisterSuccess()
    }

    const loginResponse = await authApi.login({
      account: form.account.trim(),
      password: form.password,
      remember: form.remember,
    })
    persistLoginPreference()
    await storePasswordCredential()
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
  if (AUTH_ERROR_MESSAGES[error?.code]) {
    return AUTH_ERROR_MESSAGES[error.code]
  }
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
    persist: false,
  })
  beginEntryAnimation({ name: 'quest-board' })
}

onMounted(restoreSavedLoginPreference)

onBeforeUnmount(() => {
  window.clearTimeout(entryTimer)
  window.clearTimeout(holdTimer)
})
</script>

<template>
  <main class="app-shell guild-login-shell">
    <section
      class="guild-gate-scene"
      :class="{ 'is-awake': isGateOpen, 'is-entering': isEntering, 'is-ready': bgReady }"
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

            <form
              id="guild-auth-form"
              ref="authFormRef"
              class="guild-auth-form"
              autocomplete="on"
              method="post"
              :action="isRegisterMode ? '/api/v1/auth/register' : '/api/v1/auth/login'"
              novalidate
              @submit.prevent="submitAuth"
            >
              <label v-if="isRegisterMode" class="guild-field">
                <span>用户名</span>
                <input
                  v-model.trim="form.username"
                  autocomplete="username"
                  minlength="1"
                  maxlength="32"
                  required
                  placeholder="alice"
                />
              </label>

              <label class="guild-field">
                <span>{{ isRegisterMode ? '邮箱' : '用户名 / 邮箱' }}</span>
                <input
                  ref="accountInput"
                  v-model.trim="form.account"
                  :type="isRegisterMode ? 'email' : 'text'"
                  :name="isRegisterMode ? 'email' : 'username'"
                  :autocomplete="isRegisterMode ? 'email' : 'username'"
                  required
                  :placeholder="isRegisterMode ? 'alice@example.com' : '用户名或邮箱'"
                />
              </label>

              <label class="guild-field">
                <span>密码</span>
                <div class="guild-field-control">
                  <input
                    v-model="form.password"
                    :type="showPassword ? 'text' : 'password'"
                    name="password"
                    :autocomplete="isRegisterMode ? 'new-password' : 'current-password'"
                    :minlength="isRegisterMode ? 8 : undefined"
                    required
                    placeholder="输入你的公会密钥"
                    @input="syncPasswordConfirmationValidity"
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
                <small v-if="isRegisterMode" class="guild-field-hint">8-64 位，需同时包含字母和数字。</small>
              </label>

              <label v-if="isRegisterMode" class="guild-field">
                <span>再次输入密码</span>
                <div class="guild-field-control">
                  <input
                    ref="confirmPasswordInput"
                    v-model="form.confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    name="confirmPassword"
                    autocomplete="new-password"
                    minlength="8"
                    required
                    placeholder="再次输入你的公会密钥"
                    @input="syncPasswordConfirmationValidity"
                  />
                  <button
                    class="guild-field-reveal"
                    type="button"
                    :aria-pressed="showConfirmPassword"
                    :aria-label="showConfirmPassword ? '隐藏确认密码' : '显示确认密码'"
                    :title="showConfirmPassword ? '隐藏确认密码' : '显示确认密码'"
                    @click="showConfirmPassword = !showConfirmPassword"
                  >
                    <svg
                      v-if="!showConfirmPassword"
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

              <div v-if="!isRegisterMode" class="guild-login-options">
                <label class="guild-remember">
                  <input v-model="form.remember" type="checkbox" />
                  <span>保持登录状态</span>
                </label>

                <label class="guild-remember">
                  <input v-model="rememberPassword" type="checkbox" autocomplete="off" />
                  <span>让浏览器记住密码</span>
                </label>
              </div>

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

<style scoped>
/* 大门背景图就绪前，整个入口场景保持透明（露出 .guild-login-shell 的深色底），
   就绪后按钮与背景一起淡入，避免割裂感。只加 opacity/transition，不覆盖
   .guild-gate-art 自身的 transform/filter 过渡（开门动画仍正常）。 */
.guild-gate-scene {
  opacity: 0;
  transition: opacity 560ms ease;
}

.guild-gate-scene.is-ready {
  opacity: 1;
}
</style>
