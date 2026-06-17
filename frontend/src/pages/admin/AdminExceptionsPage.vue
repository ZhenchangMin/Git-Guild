<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'

import { adminApi } from '../../api/adminApi'
import { exceptionActions, exceptionCategories } from '../../data/adminExceptions'

const statusMeta = {
  UNRESOLVED: { label: '未解决', tone: 'danger' },
  IN_REVIEW: { label: '需复核', tone: 'return' },
  RESOLVED: { label: '已解决', tone: 'approved' },
}

const exceptions = ref([])
const activeId = ref(null)
const categoryFilter = ref('ALL')
const loading = ref(false)
const loadError = ref('')

const comment = ref('')
const action = ref('')
const commentError = ref('')
const submitting = ref(false)
const commentField = ref(null)

const result = ref({
  tone: 'idle',
  title: '等待管理员处理',
  body: '从左侧选择一条异常，查看原因、影响与日志，选择处理动作并填写处理说明后执行。',
})

// 后端 ExceptionView → 视图模型（id/repository/detectedAt 适配模板既有字段名）。
function normalize(view) {
  return {
    id: view.exceptionId,
    category: view.category,
    type: view.type,
    title: view.title,
    status: view.status,
    repository: view.repositoryName ?? '—',
    relatedQuest: view.relatedQuest ?? '—',
    detectedAt: formatDateTime(view.detectedAt),
    reason: view.reason,
    impact: view.impact ?? '—',
    suggestion: view.suggestion ?? '—',
    retryable: view.retryable,
    logs: Array.isArray(view.logs) ? [...view.logs] : [],
  }
}

function formatDateTime(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hh}:${mm}`
}

function readableError(error, fallback) {
  if (error?.details) return `${fallback} ${error.details}`
  if (error?.message) return `${fallback} ${error.message}`
  return fallback
}

onMounted(loadExceptions)

async function loadExceptions() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await adminApi.listExceptions()
    const items = response?.data?.items ?? []
    exceptions.value = items.map(normalize)
    activeId.value = exceptions.value[0]?.id ?? null
    if (active.value) action.value = exceptionActions[active.value.category]?.[0]?.value ?? ''
    result.value = {
      tone: exceptions.value.length ? 'idle' : 'approved',
      title: exceptions.value.length ? '等待管理员处理' : '当前没有待处理异常',
      body: exceptions.value.length
        ? '从左侧选择一条异常，查看原因、影响与日志，选择处理动作并填写处理说明后执行。'
        : '平台暂未检测到异常。仓库同步失败等事件会自动出现在这里。',
    }
  } catch (error) {
    loadError.value = readableError(error, '异常队列读取失败。')
    result.value = { tone: 'danger', title: '异常队列读取失败', body: loadError.value }
  } finally {
    loading.value = false
  }
}

const filtered = computed(() =>
  categoryFilter.value === 'ALL'
    ? exceptions.value
    : exceptions.value.filter((item) => item.category === categoryFilter.value),
)

const active = computed(
  () => exceptions.value.find((item) => item.id === activeId.value) ?? exceptions.value[0],
)

const stats = computed(() => {
  const counts = exceptions.value.reduce((summary, item) => {
    summary[item.status] = (summary[item.status] ?? 0) + 1
    return summary
  }, {})
  return [
    { label: '未解决', value: counts.UNRESOLVED ?? 0, tone: 'danger' },
    { label: '需复核', value: counts.IN_REVIEW ?? 0, tone: 'return' },
    { label: '已解决', value: counts.RESOLVED ?? 0, tone: 'approved' },
  ]
})

const actionOptions = computed(() => (active.value ? exceptionActions[active.value.category] ?? [] : []))

function statusOf(item) {
  return statusMeta[item.status] ?? { label: item.status, tone: 'pending' }
}

// 用后端返回的最新 ExceptionView 就地替换列表中的对应项。
function replaceFromView(view) {
  const fresh = normalize(view)
  const index = exceptions.value.findIndex((item) => item.id === fresh.id)
  if (index >= 0) exceptions.value.splice(index, 1, fresh)
  return fresh
}

function selectException(item) {
  activeId.value = item.id
  comment.value = ''
  commentError.value = ''
  action.value = exceptionActions[item.category]?.[0]?.value ?? ''
  result.value = {
    tone: 'idle',
    title: `异常 #${item.id} 已调阅`,
    body: `正在处理「${item.type}」。请选择处理动作并填写处理说明。`,
  }
}

async function retrySync() {
  const item = active.value
  if (!item || !item.retryable || submitting.value) return
  submitting.value = true
  try {
    const response = await adminApi.retryException(item.id)
    const fresh = replaceFromView(response.data)
    result.value = {
      tone: statusOf(fresh).tone,
      title: `异常 #${fresh.id} · 已发起重试`,
      body: fresh.status === 'RESOLVED'
        ? '重试成功，仓库已重新同步，异常已闭环。'
        : '已对外部接口发起重试但仍失败，状态转为「需复核」。接口恢复前保留上一次成功同步的数据。',
    }
  } catch (error) {
    result.value = { tone: 'danger', title: '重试失败', body: readableError(error, '发起重试失败。') }
  } finally {
    submitting.value = false
  }
}

async function submitResolve() {
  const item = active.value
  if (!item || submitting.value) return

  const trimmed = comment.value.trim()
  if (trimmed.length < 1) {
    commentError.value = '处理说明为必填项，请填写本次处理的依据。'
    await nextTick()
    commentField.value?.focus()
    return
  }
  if (!action.value) {
    commentError.value = '请选择一个处理动作。'
    return
  }
  commentError.value = ''

  submitting.value = true
  try {
    const response = await adminApi.resolveException(item.id, { action: action.value, comment: trimmed })
    const fresh = replaceFromView(response.data)
    const chosen = actionOptions.value.find((option) => option.value === action.value)
    result.value = {
      tone: statusOf(fresh).tone,
      title: `异常 #${fresh.id} · ${chosen?.label ?? '已处理'}`,
      body: `${trimmed}（当前状态：${statusOf(fresh).label}）`,
    }
    comment.value = ''
  } catch (error) {
    result.value = { tone: 'danger', title: '处理失败', body: readableError(error, '提交处理失败。') }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="admin-exceptions-workspace">
    <aside class="admin-queue-panel" aria-label="异常队列">
      <div class="admin-panel-head">
        <div>
          <p class="kicker">Exception Docket</p>
          <h1>异常处理中心</h1>
        </div>
        <span>同步 / 关联 / 权限</span>
      </div>

      <dl class="admin-stat-grid admin-stat-grid-3">
        <div v-for="stat in stats" :key="stat.label" :class="stat.tone">
          <dt>{{ stat.label }}</dt>
          <dd>{{ stat.value }}</dd>
        </div>
      </dl>

      <div class="admin-filter-row" role="tablist" aria-label="按类型筛选">
        <button
          v-for="option in exceptionCategories"
          :key="option.key"
          type="button"
          role="tab"
          class="admin-filter-chip"
          :class="{ active: categoryFilter === option.key }"
          :aria-selected="categoryFilter === option.key"
          @click="categoryFilter = option.key"
        >
          {{ option.label }}
        </button>
      </div>

      <div class="admin-application-list">
        <button
          v-for="item in filtered"
          :key="item.id"
          class="admin-application-card"
          :class="[{ active: active?.id === item.id }, statusOf(item).tone]"
          type="button"
          @click="selectException(item)"
        >
          <span>#{{ item.id }} · {{ item.detectedAt }}</span>
          <strong>{{ item.type }} · {{ item.title }}</strong>
          <small>{{ item.repository }} · {{ item.relatedQuest }}</small>
          <em>{{ statusOf(item).label }}</em>
        </button>
        <p v-if="filtered.length === 0" class="admin-empty-queue">
          {{ loading ? '正在读取异常队列...' : loadError || '该类型下没有异常记录。' }}
        </p>
      </div>
    </aside>

    <section v-if="active" class="admin-detail-panel" aria-live="polite">
      <header class="admin-detail-hero">
        <div>
          <p class="kicker">{{ active.type }}</p>
          <h2>{{ active.title }}</h2>
          <p>{{ active.reason }}</p>
        </div>
        <span class="admin-status-seal" :class="statusOf(active).tone">{{ statusOf(active).label }}</span>
      </header>

      <div class="admin-detail-grid">
        <article class="admin-ledger-card issue-card">
          <p class="kicker">Context</p>
          <h3>异常上下文</h3>
          <dl>
            <div>
              <dt>仓库</dt>
              <dd>{{ active.repository }}</dd>
            </div>
            <div>
              <dt>关联任务</dt>
              <dd>{{ active.relatedQuest }}</dd>
            </div>
            <div>
              <dt>发现时间</dt>
              <dd>{{ active.detectedAt }}</dd>
            </div>
            <div>
              <dt>是否可重试</dt>
              <dd>{{ active.retryable ? '可重试' : '不可重试' }}</dd>
            </div>
          </dl>
        </article>

        <article class="admin-ledger-card">
          <p class="kicker">Impact</p>
          <h3>影响</h3>
          <p class="admin-ledger-text">{{ active.impact }}</p>
        </article>

        <article class="admin-ledger-card">
          <p class="kicker">Suggestion</p>
          <h3>处理建议</h3>
          <p class="admin-ledger-text">{{ active.suggestion }}</p>
        </article>

        <article class="admin-ledger-card standards-card">
          <p class="kicker">Logs</p>
          <h3>异常日志</h3>
          <ol class="admin-log-list">
            <li v-for="(line, index) in active.logs" :key="index">{{ line }}</li>
          </ol>
        </article>
      </div>
    </section>

    <aside v-if="active" class="admin-action-panel">
      <section class="parchment-panel admin-decision-card">
        <p class="kicker">处理动作</p>
        <h2>异常处置</h2>

        <template v-if="active.status !== 'RESOLVED'">
          <p class="admin-decision-lead">选择处理动作并填写说明，处理记录会写入异常日志。</p>

          <label class="admin-select-field">
            <span class="admin-reason-label">处理动作</span>
            <select v-model="action">
              <option v-for="option in actionOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <label class="admin-reason-field" :class="{ invalid: commentError }">
            <span class="admin-reason-label">处理说明</span>
            <textarea
              ref="commentField"
              v-model="comment"
              rows="3"
              placeholder="说明本次处理的依据与后续要求。"
              @input="commentError = ''"
            ></textarea>
            <span v-if="commentError" class="admin-reason-error">{{ commentError }}</span>
          </label>

          <div class="admin-action-buttons">
            <button type="button" class="primary-action" :disabled="submitting" @click="submitResolve">
              提交处理
            </button>
            <button
              v-if="active.retryable"
              type="button"
              class="quiet-action"
              :disabled="submitting"
              @click="retrySync"
            >
              重试同步
            </button>
          </div>
        </template>

        <p v-else class="admin-decision-lead muted">该异常已解决，无需进一步处理。</p>
      </section>

      <section class="glass-ledger admin-result-ledger" :class="result.tone">
        <p class="kicker">操作结果</p>
        <h2>{{ result.title }}</h2>
        <p>{{ result.body }}</p>
      </section>
    </aside>
  </div>
</template>
