<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import doorImg from '../../assets/door.png'
import { setSession } from '../../stores/sessionStore'

const router = useRouter()
const selectedRole = ref('ADVENTURER')

const roles = [
  { id: 'ADVENTURER', name: '冒险家', note: '浏览悬赏任务并提交成果' },
  { id: 'MAINTAINER', name: '委托人', note: '发布悬赏任务并审核成果' },
  { id: 'ADMIN', name: '管理员', note: '审核任务上架并处理异常' },
]

function enterGuild(roleId = selectedRole.value) {
  setSession({
    token: roleId === 'VISITOR' ? '' : 'mock-token',
    role: roleId,
    user: {
      displayName: roleId === 'VISITOR' ? '访客' : 'Minerva Dawn',
      role: roleId,
    },
  })

  if (roleId === 'ADMIN') {
    router.push({ name: 'admin-review' })
    return
  }

  router.push({ name: 'hall' })
}

function enterAsVisitor() {
  enterGuild('VISITOR')
}
</script>

<template>
  <main class="app-shell">
    <section class="scene door-scene" :style="{ backgroundImage: `url(${doorImg})` }">
      <aside class="login-panel" aria-label="登录面板">
        <p class="kicker">Git Guild 入口</p>
        <h1>登录 Git Guild</h1>
        <div class="field-group">
          <label for="guild-name">账号</label>
          <input id="guild-name" value="adventurer@relay" autocomplete="username" />
        </div>
        <div class="field-group">
          <label for="guild-key">访问密钥</label>
          <input id="guild-key" value="••••••••" type="password" autocomplete="current-password" />
        </div>

        <div class="role-grid" aria-label="选择角色">
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

        <div class="login-actions">
          <button class="primary-action" type="button" @click="enterGuild()">登录</button>
          <button class="quiet-action" type="button" @click="enterAsVisitor">以访客身份进入</button>
        </div>
      </aside>
    </section>
  </main>
</template>
