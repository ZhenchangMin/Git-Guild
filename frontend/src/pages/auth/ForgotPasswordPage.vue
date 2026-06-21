<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { authApi } from '../../api'
import { setSession } from '../../stores/sessionStore'

const RESEND_SECONDS = 60

const router = useRouter()

// step: 'request' 填邮箱发码 → 'reset' 填验证码与新密码
const step = ref('request')
const isSubmitting = ref(false)
const formMessage = ref('')
const formError = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const resendCountdown = ref(0)
let resendTimer = 0

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const RESET_ERROR_MESSAGES = {
  EMAIL_NOT_REGISTERED: '该邮箱还没注册，无法找回密码。',
  INVALID_RESET_CODE: '验证码错误或已过期，请重新获取。',
  TOO_FREQUENT: '验证码请求过于频繁，请稍后再试。',
  USER_NOT_FOUND: '该邮箱未注册，请确认后重试。',
}

const canResend = computed(() => resendCountdown.value <= 0 && !isSubmitting.value)
const sendButtonText = computed(() => {
  if (isSubmitting.value && step.value === 'request') return '正在发送验证码...'
  if (resendCountdown.value > 0) return `${resendCountdown.value}s 后可重发`
  return step.value === 'request' ? '发送验证码' : '重新发送'
})

function startResendCountdown() {
  resendCountdown.value = RESEND_SECONDS
  window.clearInterval(resendTimer)
  resendTimer = window.setInterval(() => {
    resendCountdown.value -= 1
    if (resendCountdown.value <= 0) {
      window.clearInterval(resendTimer)
    }
  }, 1000)
}

function resolveError(error) {
  if (RESET_ERROR_MESSAGES[error?.code]) {
    return RESET_ERROR_MESSAGES[error.code]
  }
  // 后端字段校验（VALIDATION_FAILED）的具体原因放在 details 里。
  if (error?.code === 'VALIDATION_FAILED' && error.details) {
    return error.details
  }
  return error?.message || '无法连接 Git Guild 后端，请确认服务已启动。'
}

async function sendCode() {
  if (!form.email.trim()) {
    formError.value = '请输入邮箱地址。'
    return
  }
  formError.value = ''
  formMessage.value = ''
  isSubmitting.value = true

  try {
    await authApi.forgotPassword({ email: form.email.trim() })
    // 进入第二步填验证码。
    formMessage.value = '验证码已发送，请查收邮件（5 分钟内有效）。'
    step.value = 'reset'
    startResendCountdown()
  } catch (error) {
    formError.value = resolveError(error)
  } finally {
    isSubmitting.value = false
  }
}

async function submitReset() {
  formError.value = ''

  if (!/^\d{6}$/.test(form.code)) {
    formError.value = '请输入 6 位数字验证码。'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    formError.value = '两次输入的密码不一致，请重新确认。'
    return
  }

  isSubmitting.value = true
  try {
    await authApi.resetPassword({
      email: form.email.trim(),
      code: form.code.trim(),
      newPassword: form.newPassword,
    })
    // 重置成功后用新密码自动登录，直接进入大厅，免去再回登录页手动登录。
    formMessage.value = '密码重置成功！正在为你登录……'
    await loginWithNewPassword()
  } catch (error) {
    formError.value = resolveError(error)
  } finally {
    isSubmitting.value = false
  }
}

// 冒险家与委托人账号已合并：登录后统一为成员（MAINTAINER），ADMIN 进审核台，其余进大厅。
function toEntryRole(apiRole) {
  return apiRole === 'ADMIN' ? 'ADMIN' : 'MAINTAINER'
}

async function loginWithNewPassword() {
  const loginResponse = await authApi.login({
    account: form.email.trim(),
    password: form.newPassword,
    remember: true,
  })
  const authData = loginResponse.data
  const entryRole = toEntryRole(authData.user?.role)
  setSession({
    token: authData.accessToken,
    refreshToken: authData.refreshToken,
    role: entryRole,
    user: {
      ...authData.user,
      displayName: authData.user?.username ?? authData.user?.email ?? 'Git Guild 用户',
      role: entryRole,
      apiRole: authData.user?.role,
    },
    persist: true,
  })
  router.push(entryRole === 'ADMIN' ? { name: 'admin-review' } : { name: 'hall' })
}

function backToLogin() {
  router.push({ name: 'login' })
}

onBeforeUnmount(() => {
  window.clearInterval(resendTimer)
})
</script>

<template>
  <main class="app-shell forgot-shell">
    <section class="guild-login-card forgot-card" aria-label="找回 Git Guild 密码">
      <header class="guild-login-head">
        <p class="kicker">Git Guild Gate</p>
        <h1>找回密码</h1>
        <p>
          {{ step === 'request'
            ? '输入注册邮箱，我们会向它发送一枚 6 位验证码。'
            : '输入邮箱收到的验证码并设置新密码。' }}
        </p>
      </header>

      <form class="guild-auth-form" novalidate @submit.prevent="step === 'request' ? sendCode() : submitReset()">
        <label class="guild-field">
          <span>邮箱</span>
          <input
            v-model.trim="form.email"
            type="email"
            autocomplete="email"
            :readonly="step === 'reset'"
            required
            placeholder="alice@example.com"
          />
        </label>

        <template v-if="step === 'reset'">
          <label class="guild-field">
            <span>验证码</span>
            <input
              v-model.trim="form.code"
              inputmode="numeric"
              maxlength="6"
              required
              placeholder="6 位数字"
            />
          </label>

          <label class="guild-field">
            <span>新密码</span>
            <div class="guild-field-control">
              <input
                v-model="form.newPassword"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                minlength="8"
                required
                placeholder="设置新的公会密钥"
              />
              <button
                class="guild-field-reveal"
                type="button"
                :aria-pressed="showPassword"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
                  stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              </button>
            </div>
            <small class="guild-field-hint">8-64 位，需同时包含字母和数字。</small>
          </label>

          <label class="guild-field">
            <span>确认新密码</span>
            <div class="guild-field-control">
              <input
                v-model="form.confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                autocomplete="new-password"
                minlength="8"
                required
                placeholder="再次输入新密码"
              />
              <button
                class="guild-field-reveal"
                type="button"
                :aria-pressed="showConfirmPassword"
                :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'"
                @click="showConfirmPassword = !showConfirmPassword"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
                  stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              </button>
            </div>
          </label>
        </template>

        <p v-if="formMessage" class="guild-auth-notice success">{{ formMessage }}</p>
        <p v-if="formError" class="guild-auth-notice error">{{ formError }}</p>

        <div class="guild-login-actions forgot-actions">
          <button
            v-if="step === 'request'"
            class="guild-submit"
            type="submit"
            :disabled="isSubmitting"
          >
            {{ sendButtonText }}
          </button>

          <template v-else>
            <button class="guild-submit" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '正在重置...' : '重置密码' }}
            </button>
            <button
              class="guild-resend"
              type="button"
              :disabled="!canResend"
              @click="sendCode"
            >
              {{ sendButtonText }}
            </button>
          </template>
        </div>

        <button class="guild-guest-link" type="button" @click="backToLogin">返回登录</button>
      </form>
    </section>
  </main>
</template>

<style scoped>
/* 复用 .guild-login-card 等全局公会主题样式，这里仅负责整页居中布局。 */
.forgot-shell {
  display: grid;
  place-items: center;
  min-height: 100svh;
  padding: 24px;
  background: radial-gradient(circle at 50% 30%, rgba(45, 24, 8, 0.4), #0c0603 70%);
}

.forgot-card {
  max-height: none;
}

.forgot-actions {
  flex-wrap: wrap;
}

.guild-resend {
  min-height: 40px;
  border: 1px solid rgba(245, 195, 99, 0.4);
  border-radius: 999px;
  padding: 0 16px;
  color: rgba(255, 232, 190, 0.82);
  background: rgba(18, 10, 5, 0.56);
  transition: color 150ms ease, border-color 150ms ease, background 150ms ease;
}

.guild-resend:hover:not(:disabled) {
  color: #fff2cd;
  border-color: rgba(255, 224, 157, 0.86);
  background: rgba(82, 45, 16, 0.6);
}

.guild-resend:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
