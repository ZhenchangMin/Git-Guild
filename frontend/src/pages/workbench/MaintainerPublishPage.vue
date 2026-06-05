<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { questApi } from '../../api/questApi'
import { repositoryApi } from '../../api/repositoryApi'

const router = useRouter()

// ── 选项数据 ────────────────────────────────────────────────────────────────
const repositories = ref([])
const categories = ref([])
const issues = ref([])
const loadingMeta = ref(true)
const loadingIssues = ref(false)
const metaError = ref('')

// ── 表单状态 ────────────────────────────────────────────────────────────────
const form = ref({
  repositoryId: '',
  issueMode: 'existing', // existing | new
  issueId: '',
  giteaIssueTitle: '',
  giteaIssueBody: '',
  title: 'Hello World',
  description: '在仓库中新增一个 hello world 文件，作为最短 MVP 演示任务。',
  completionCriteria: '新增 hello.md（或 hello world 文件），内容包含 "Hello, Git-Guild!"，并通过 PR 合并。',
  categoryId: '',
  difficulty: 'A',
  techStack: 'Markdown',
  estimatedHours: 1,
  rewardXp: 50,
})

const submitting = ref(false)
const submitError = ref('')
const submitOk = ref(null) // { questId }

const DIFFICULTIES = ['A', 'B', 'C', 'D']

const selectedRepo = computed(
  () => repositories.value.find((r) => String(r.repositoryId) === String(form.value.repositoryId)) ?? null,
)

// ── 加载仓库 + 分类 ─────────────────────────────────────────────────────────
async function loadMeta() {
  loadingMeta.value = true
  metaError.value = ''
  try {
    const [repoPayload, catPayload] = await Promise.all([
      repositoryApi.list(),
      questApi.categories(),
    ])
    repositories.value = unwrapList(repoPayload)
    categories.value = unwrapList(catPayload)
    if (repositories.value.length > 0) {
      form.value.repositoryId = String(repositories.value[0].repositoryId)
    }
    const mvp = categories.value.find((c) => c.name === 'MVP') ?? categories.value[0]
    if (mvp) form.value.categoryId = String(mvp.categoryId)
  } catch (error) {
    metaError.value = error?.message ?? '加载仓库/分类失败，请确认后端已启动。'
  } finally {
    loadingMeta.value = false
  }
}

// 选定仓库后拉取其 OPEN Issue 列表
async function loadIssues(repositoryId) {
  issues.value = []
  form.value.issueId = ''
  if (!repositoryId) return
  loadingIssues.value = true
  try {
    const payload = await repositoryApi.issues(repositoryId, { status: 'OPEN' })
    issues.value = unwrapItems(payload)
    if (issues.value.length > 0) {
      form.value.issueId = String(issues.value[0].issueId)
    } else {
      // 没有可选 Issue 时自动切到"新建 Issue"模式
      form.value.issueMode = 'new'
    }
  } catch {
    issues.value = []
    form.value.issueMode = 'new'
  } finally {
    loadingIssues.value = false
  }
}

watch(
  () => form.value.repositoryId,
  (id) => loadIssues(id),
)

onMounted(async () => {
  await loadMeta()
  if (form.value.repositoryId) await loadIssues(form.value.repositoryId)
})

// ── 校验 + 提交 ─────────────────────────────────────────────────────────────
const errors = computed(() => {
  const e = {}
  if (!form.value.repositoryId) e.repositoryId = '请选择仓库。'
  if (form.value.issueMode === 'existing' && !form.value.issueId) e.issue = '请选择已有 Issue，或切换为新建 Issue。'
  if (form.value.issueMode === 'new' && !form.value.giteaIssueTitle.trim()) e.issue = '请填写新建 Issue 的标题。'
  if (!form.value.title.trim()) e.title = '请填写任务标题。'
  if (!form.value.description.trim()) e.description = '请填写任务描述。'
  if (!form.value.completionCriteria.trim()) e.completionCriteria = '请填写完成标准。'
  if (!form.value.categoryId) e.categoryId = '请选择分类。'
  if (!techStackArray.value.length) e.techStack = '请填写至少一个技术栈（逗号分隔）。'
  if (!(Number(form.value.estimatedHours) > 0)) e.estimatedHours = '预估工时需大于 0。'
  if (!(Number(form.value.rewardXp) > 0)) e.rewardXp = '奖励 XP 需大于 0。'
  return e
})

const techStackArray = computed(() =>
  form.value.techStack
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean),
)

const canSubmit = computed(() => Object.keys(errors.value).length === 0 && !submitting.value)

async function publish() {
  submitError.value = ''
  submitOk.value = null
  if (!canSubmit.value) {
    submitError.value = Object.values(errors.value)[0] ?? '请完成必填项。'
    return
  }
  submitting.value = true
  try {
    const payload = {
      repositoryId: Number(form.value.repositoryId),
      title: form.value.title.trim(),
      description: form.value.description.trim(),
      completionCriteria: form.value.completionCriteria.trim(),
      difficulty: form.value.difficulty,
      techStack: techStackArray.value,
      estimatedHours: Number(form.value.estimatedHours),
      rewardXp: Number(form.value.rewardXp),
      categoryId: Number(form.value.categoryId),
      tagIds: [],
    }
    if (form.value.issueMode === 'existing') {
      payload.issueId = Number(form.value.issueId)
    } else {
      payload.giteaIssueTitle = form.value.giteaIssueTitle.trim()
      if (form.value.giteaIssueBody.trim()) payload.giteaIssueBody = form.value.giteaIssueBody.trim()
    }

    const createRes = await questApi.create(payload)
    const questId = createRes?.data?.questId
    if (!questId) throw new Error('创建任务成功但未返回 questId。')
    await questApi.submitForReview(questId)
    submitOk.value = { questId }
  } catch (error) {
    submitError.value = error?.message ?? '发布失败，请稍后再试。'
  } finally {
    submitting.value = false
  }
}

function backToWorkbench() {
  router.push({ name: 'maintainer-workbench' })
}

// ── 工具：解包 ApiResponse ───────────────────────────────────────────────────
function unwrapList(payload) {
  const data = payload?.data
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.items)) return data.items
  return []
}
function unwrapItems(payload) {
  const data = payload?.data
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data)) return data
  return []
}
</script>

<template>
  <main class="app-shell publish-shell">
    <section class="publish-panel">
      <header class="publish-head">
        <button class="quiet-action" type="button" @click="backToWorkbench">← 返回工作台</button>
        <h1>发布任务</h1>
        <p class="publish-sub">委托人创建任务并提交管理员审核（DRAFT → 待审核）。</p>
      </header>

      <p v-if="metaError" class="publish-banner error">{{ metaError }}</p>
      <p v-if="loadingMeta" class="publish-banner">正在加载仓库与分类…</p>

      <form v-else class="publish-form" @submit.prevent="publish">
        <label class="field">
          <span>目标仓库</span>
          <select v-model="form.repositoryId">
            <option v-for="r in repositories" :key="r.repositoryId" :value="String(r.repositoryId)">
              {{ r.name }}（{{ r.defaultBranch || 'main' }}）
            </option>
          </select>
          <small v-if="errors.repositoryId" class="field-error">{{ errors.repositoryId }}</small>
          <small v-else-if="selectedRepo" class="field-hint">{{ selectedRepo.sourceUrl }}</small>
        </label>

        <fieldset class="field issue-field">
          <span>关联 Issue</span>
          <div class="issue-modes">
            <label class="radio"><input v-model="form.issueMode" type="radio" value="existing" /> 选择已有</label>
            <label class="radio"><input v-model="form.issueMode" type="radio" value="new" /> 新建 Gitea Issue</label>
          </div>
          <template v-if="form.issueMode === 'existing'">
            <select v-model="form.issueId" :disabled="loadingIssues">
              <option v-if="loadingIssues" value="">加载中…</option>
              <option v-for="i in issues" :key="i.issueId" :value="String(i.issueId)">
                #{{ i.externalIssueId }} · {{ i.title }}
              </option>
            </select>
            <small v-if="!loadingIssues && !issues.length" class="field-hint">该仓库暂无 OPEN Issue，请切换为新建。</small>
          </template>
          <template v-else>
            <input v-model.trim="form.giteaIssueTitle" placeholder="Issue 标题，如：Add hello world file" />
            <textarea v-model="form.giteaIssueBody" rows="2" placeholder="Issue 描述（可选）"></textarea>
          </template>
          <small v-if="errors.issue" class="field-error">{{ errors.issue }}</small>
        </fieldset>

        <label class="field">
          <span>任务标题</span>
          <input v-model.trim="form.title" placeholder="Hello World" />
          <small v-if="errors.title" class="field-error">{{ errors.title }}</small>
        </label>

        <label class="field wide">
          <span>任务描述</span>
          <textarea v-model="form.description" rows="3"></textarea>
          <small v-if="errors.description" class="field-error">{{ errors.description }}</small>
        </label>

        <label class="field wide">
          <span>完成标准</span>
          <textarea v-model="form.completionCriteria" rows="2"></textarea>
          <small v-if="errors.completionCriteria" class="field-error">{{ errors.completionCriteria }}</small>
        </label>

        <label class="field">
          <span>分类</span>
          <select v-model="form.categoryId">
            <option v-for="c in categories" :key="c.categoryId" :value="String(c.categoryId)">{{ c.name }}</option>
          </select>
          <small v-if="errors.categoryId" class="field-error">{{ errors.categoryId }}</small>
        </label>

        <label class="field">
          <span>难度</span>
          <select v-model="form.difficulty">
            <option v-for="d in DIFFICULTIES" :key="d" :value="d">{{ d }}</option>
          </select>
        </label>

        <label class="field">
          <span>技术栈（逗号分隔）</span>
          <input v-model.trim="form.techStack" placeholder="Markdown, Git" />
          <small v-if="errors.techStack" class="field-error">{{ errors.techStack }}</small>
        </label>

        <label class="field">
          <span>预估工时（小时）</span>
          <input v-model="form.estimatedHours" type="number" min="1" />
          <small v-if="errors.estimatedHours" class="field-error">{{ errors.estimatedHours }}</small>
        </label>

        <label class="field">
          <span>奖励 XP</span>
          <input v-model="form.rewardXp" type="number" min="1" />
          <small v-if="errors.rewardXp" class="field-error">{{ errors.rewardXp }}</small>
        </label>

        <div class="publish-actions">
          <button class="primary-action" type="submit" :disabled="!canSubmit">
            {{ submitting ? '发布中…' : '发布并提交审核' }}
          </button>
        </div>

        <p v-if="submitError" class="publish-banner error">{{ submitError }}</p>
        <div v-if="submitOk" class="publish-banner ok">
          任务 #{{ submitOk.questId }} 已创建并提交管理员审核。请用 admin 账号到审核台通过后上架。
        </div>
      </form>
    </section>
  </main>
</template>

<style scoped>
.publish-shell {
  display: flex;
  justify-content: center;
  padding: 32px 16px;
  background: #f4ecd8;
  min-height: 100vh;
}
.publish-panel {
  width: min(760px, 100%);
  background: rgba(255, 250, 236, 0.92);
  border: 1px solid rgba(98, 55, 20, 0.2);
  border-radius: 10px;
  padding: 24px 28px;
  box-shadow: 0 10px 30px rgba(70, 34, 10, 0.12);
}
.publish-head h1 {
  margin: 10px 0 4px;
  color: #4a250f;
}
.publish-sub {
  margin: 0 0 12px;
  color: rgba(69, 37, 15, 0.6);
  font-size: 0.88rem;
}
.publish-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.field {
  display: grid;
  gap: 6px;
  min-width: 0;
  color: rgba(69, 37, 15, 0.78);
  font-size: 0.86rem;
}
.field.wide,
.issue-field {
  grid-column: 1 / -1;
}
.field span {
  font-weight: 600;
}
.field input,
.field select,
.field textarea {
  width: 100%;
  border: 1px solid rgba(98, 55, 20, 0.26);
  border-radius: 4px;
  padding: 9px 10px;
  color: #3b210f;
  background: rgba(255, 244, 210, 0.58);
}
.issue-modes {
  display: flex;
  gap: 16px;
  margin-bottom: 4px;
}
.radio {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 400;
}
.field-error {
  color: #a23b2c;
}
.field-hint {
  color: rgba(69, 37, 15, 0.5);
  word-break: break-all;
}
.publish-actions {
  grid-column: 1 / -1;
}
.primary-action {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  background: #7a4a18;
  color: #fff5e0;
  font-weight: 600;
  cursor: pointer;
}
.primary-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.quiet-action {
  border: none;
  background: transparent;
  color: #7a4a18;
  cursor: pointer;
  padding: 0;
}
.publish-banner {
  grid-column: 1 / -1;
  margin: 0;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(98, 55, 20, 0.08);
  color: rgba(69, 37, 15, 0.8);
  font-size: 0.88rem;
}
.publish-banner.error {
  background: rgba(162, 59, 44, 0.12);
  color: #a23b2c;
}
.publish-banner.ok {
  background: rgba(38, 122, 60, 0.14);
  color: #1f7a3c;
}
</style>
