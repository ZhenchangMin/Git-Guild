<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { authApi } from '../../api'
import doorImg from '../../assets/door.png'
import { setSession } from '../../stores/sessionStore'

const router = useRouter()
const mode = ref('login')
const selectedRole = ref('BEGINNER')
const isSubmitting = ref(false)
const formMessage = ref('')
const formError = ref('')

const form = reactive({
  username: '',
  email: '',
  password: '',
  remember: true,
})

const roles = [
  { id: 'BEGINNER', entryRole: 'ADVENTURER', name: '冒险家', note: '浏览悬赏任务并提交成果' },
  { id: 'MAINTAINER', entryRole: 'MAINTAINER', name: '委托人', note: '发布悬赏任务并审核成果' },
]

const isRegisterMode = computed(() => mode.value === 'register')
const activeRole = computed(() => roles.find((role) => role.id === selectedRole.value) ?? roles[0])

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

function normalizeUser(user) {
  const entryRole = toEntryRole(user?.role)
  return {
    ...user,
    displayName: user?.username ?? user?.email ?? 'Git Guild 用户',
    role: entryRole,
    apiRole: user?.role,
  }
}

function saveAuthSession(authData) {
  const user = normalizeUser(authData.user)
  setSession({
    token: authData.accessToken,
    refreshToken: authData.refreshToken,
    role: user.role,
    user,
  })
  router.push(routeForRole(user.role))
}

function switchMode(nextMode) {
  mode.value = nextMode
  formError.value = ''
  formMessage.value = ''
}

async function submitAuth() {
  formError.value = ''
  formMessage.value = ''
  isSubmitting.value = true

  try {
    if (isRegisterMode.value) {
      await authApi.register({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        role: selectedRole.value,
      })
      formMessage.value = '账号创建成功，正在为你登录。'
    }

    const loginResponse = await authApi.login({
      email: form.email.trim(),
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
      displayName: '访客',
      role: 'VISITOR',
    },
  })
  router.push({ name: 'quest-board' })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene door-scene" :style="{ backgroundImage: `url(${doorImg})` }">
      <aside class="login-panel" aria-label="登录面板">
        <p class="kicker">Git Guild 入口</p>
        <h1>{{ isRegisterMode ? '创建公会身份' : '登录 Git Guild' }}</h1>
        <p class="login-copy">
          {{ isRegisterMode ? '注册平台自有账号，选择初始身份后进入对应工作区。' : '使用平台账号登录，系统会根据角色打开对应入口。' }}
        </p>

        <div class="auth-mode-tabs" aria-label="选择登录或注册">
          <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
        </div>

        <form class="auth-form" @submit.prevent="submitAuth">
          <div v-if="isRegisterMode" class="field-group">
            <label for="guild-username">用户名</label>
            <input
              id="guild-username"
              v-model.trim="form.username"
              autocomplete="username"
              minlength="3"
              maxlength="32"
              required
              placeholder="例如 alice"
            />
          </div>

          <div class="field-group">
            <label for="guild-email">邮箱</label>
            <input
              id="guild-email"
              v-model.trim="form.email"
              type="email"
              autocomplete="email"
              required
              placeholder="alice@example.com"
            />
          </div>

          <div class="field-group">
            <label for="guild-password">密码</label>
            <input
              id="guild-password"
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              minlength="8"
              required
              placeholder="至少 8 位，包含字母和数字"
            />
          </div>

          <div v-if="isRegisterMode" class="role-grid" aria-label="选择初始角色">
            <button
              v-for="role in roles"
              :key="role.id"
              class="role-choice"
              :class="{ active: selectedRole === role.id }"
              type="button"
              @click="selectedRole = role.id"
            >
              <strong>{{ role.name }}</strong>
              <span>{{ role.note }}</span>
            </button>
          </div>

          <label v-if="!isRegisterMode" class="remember-row">
            <input v-model="form.remember" type="checkbox" />
            <span>保持登录状态</span>
          </label>

          <p v-if="formMessage" class="auth-notice success">{{ formMessage }}</p>
          <p v-if="formError" class="auth-notice error">{{ formError }}</p>

          <div class="login-actions">
            <button class="primary-action" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '正在处理...' : isRegisterMode ? '注册并登录' : '登录' }}
            </button>
            <button class="quiet-action" type="button" @click="enterAsVisitor">以访客身份浏览</button>
          </div>
        </form>
      </aside>
    </section>
  </main>
</template>
