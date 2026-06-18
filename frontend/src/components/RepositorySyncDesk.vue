<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { repositoryApi } from '../api/repositoryApi'

const router = useRouter()

function goToWorkbench() {
  router.push({ name: 'maintainer-workbench' })
}

// 'create' = 在平台直接新建空仓库；'import' = 迁入已有外部仓库。
const mode = ref('create')

const createForm = ref({
  name: 'gitguild-demo',
  description: '',
})

const repositoryForm = ref({
  sourceUrl: '',
  name: 'gitguild-demo',
  hostType: 'GITEA',
})

const loading = ref(false)
const issueLoading = ref(false)
const stage = ref('ready')
const lastRepository = ref(null)
const repositoryIssues = ref([])
const errorMessage = ref('')

function switchMode(next) {
  if (mode.value === next) return
  mode.value = next
  errorMessage.value = ''
}

const notice = computed(() => {
  if (errorMessage.value) {
    return {
      tone: 'warn',
      title: mode.value === 'create' ? '仓库暂时无法创建' : '仓库暂时无法接入',
      body: errorMessage.value,
      steps:
        mode.value === 'create'
          ? ['确认仓库名未被占用', '仅委托人或管理员可创建仓库', '修正后重新创建']
          : ['确认仓库地址可访问', '确认仓库为公开仓库或平台有权限', '修正后重新导入'],
    }
  }

  if (stage.value === 'created') {
    return {
      tone: 'success',
      title: '仓库已创建',
      body: `${lastRepository.value?.name ?? '新仓库'} 已在平台本地 Gitea 创建完成。现在可以回到委托人工作台发布委托。`,
      steps: ['空仓库已就绪', '可在工作台基于该仓库发布委托', '冒险家接取后会自动建任务分支'],
    }
  }

  if (stage.value === 'imported') {
    return {
      tone: 'success',
      title: '仓库已接入',
      body: `${lastRepository.value?.name ?? '目标仓库'} 已登记并同步到平台。现在可以回到委托人工作台发布委托。`,
      steps: ['仓库已导入平台', 'Issue 已同步', '回到工作台发布委托'],
    }
  }

  if (mode.value === 'create') {
    return {
      tone: 'info',
      title: '新建受托仓库',
      body: '在平台本地 Gitea 直接创建一个空仓库，无需任何外部地址。创建后即可在工作台基于它发布委托。',
      steps: ['填写仓库名称', '创建空仓库', '返回工作台发布委托'],
    }
  }

  return {
    tone: 'info',
    title: '接入受托仓库',
    body: '在这里导入 GitHub 或 Gitea 仓库。平台会把外部仓库迁入本地 Gitea，并同步 Issue，供后续发布委托使用。',
    steps: ['填写仓库地址', '导入并同步仓库', '返回工作台发布委托'],
  }
})

const issueOptions = computed(() => repositoryIssues.value.filter((issue) => issue.status !== 'CLOSED'))
const canImport = computed(() => repositoryForm.value.sourceUrl.trim() && repositoryForm.value.name.trim() && !loading.value)
const canCreate = computed(() => createForm.value.name.trim() && !loading.value)

function inferRepositoryName(sourceUrl) {
  const clean = sourceUrl.trim().replace(/\.git$/i, '')
  const name = clean.split('/').filter(Boolean).pop()
  return name || repositoryForm.value.name
}

function syncNameFromUrl() {
  if (!repositoryForm.value.sourceUrl.trim()) return
  if (repositoryForm.value.name && repositoryForm.value.name !== 'gitguild-demo') return
  repositoryForm.value.name = inferRepositoryName(repositoryForm.value.sourceUrl)
}

async function importRepository() {
  errorMessage.value = ''
  loading.value = true
  repositoryIssues.value = []
  try {
    const response = await repositoryApi.importRepository({
      sourceUrl: repositoryForm.value.sourceUrl.trim(),
      name: repositoryForm.value.name.trim(),
      hostType: repositoryForm.value.hostType,
    })
    const repository = response?.data
    if (!repository?.repositoryId) throw new Error('仓库导入成功但未返回 repositoryId。')
    lastRepository.value = repository
    await repositoryApi.sync(repository.repositoryId)
    await loadIssues(repository.repositoryId)
    stage.value = 'imported'
  } catch (error) {
    stage.value = 'ready'
    errorMessage.value = readableError(error, '仓库迁移失败，请确认地址正确且仓库为公开仓库。')
  } finally {
    loading.value = false
  }
}

async function createRepository() {
  errorMessage.value = ''
  loading.value = true
  repositoryIssues.value = []
  try {
    const response = await repositoryApi.create({
      name: createForm.value.name.trim(),
      description: createForm.value.description.trim(),
    })
    const repository = response?.data
    if (!repository?.repositoryId) throw new Error('仓库创建成功但未返回 repositoryId。')
    lastRepository.value = repository
    stage.value = 'created'
  } catch (error) {
    stage.value = 'ready'
    errorMessage.value = readableError(error, '仓库创建失败，请确认仓库名未被占用且你有委托人权限。')
  } finally {
    loading.value = false
  }
}

async function loadIssues(repositoryId = lastRepository.value?.repositoryId) {
  if (!repositoryId) {
    repositoryIssues.value = []
    return
  }

  issueLoading.value = true
  try {
    const response = await repositoryApi.issues(repositoryId, { size: 50 })
    repositoryIssues.value = response?.data?.items ?? []
  } catch (error) {
    errorMessage.value = readableError(error, 'Issue 列表读取失败，可稍后在工作台重试同步。')
  } finally {
    issueLoading.value = false
  }
}

function readableError(error, fallback) {
  if (error?.details) return `${fallback} ${error.details}`
  if (error?.message) return `${fallback} ${error.message}`
  return fallback
}
</script>

<template>
  <div class="frontdesk-guide" aria-label="前台仓库导入向导">
    <section class="mascot-dialogue" :class="notice.tone" aria-live="polite">
      <span class="dialogue-tail" aria-hidden="true"></span>
      <p class="kicker">艾丽丝</p>
      <h2>{{ notice.title }}</h2>
      <p>{{ notice.body }}</p>
      <ol>
        <li v-for="step in notice.steps" :key="step">{{ step }}</li>
      </ol>
    </section>

    <form class="frontdesk-pocket import-panel" @submit.prevent="mode === 'create' ? createRepository() : importRepository()">
      <div class="pocket-head">
        <div>
          <p class="kicker">Repository Intake</p>
          <h2>{{ mode === 'create' ? '新建仓库' : '导入仓库' }}</h2>
        </div>
        <span v-if="lastRepository" class="repo-id">ID {{ lastRepository.repositoryId }}</span>
      </div>

      <div class="mode-switch" role="tablist" aria-label="仓库接入方式">
        <button
          type="button"
          role="tab"
          :aria-selected="mode === 'create'"
          :class="{ active: mode === 'create' }"
          @click="switchMode('create')"
        >新建空仓库</button>
        <button
          type="button"
          role="tab"
          :aria-selected="mode === 'import'"
          :class="{ active: mode === 'import' }"
          @click="switchMode('import')"
        >导入已有仓库</button>
      </div>

      <section v-if="mode === 'create'" class="form-section">
        <div class="section-head">
          <strong>新建仓库</strong>
          <span>在平台本地 Gitea 创建</span>
        </div>
        <label>
          仓库名称
          <input v-model.trim="createForm.name" placeholder="gitguild-demo" required />
          <small class="field-hint">仅字母、数字、连字符；将作为本地 Gitea 仓库名。</small>
        </label>
        <label>
          仓库描述
          <input v-model.trim="createForm.description" placeholder="可选，简述这个仓库的用途" />
        </label>
        <button class="primary-action section-action" type="submit" :disabled="!canCreate">
          {{ loading ? '正在创建仓库…' : '创建空仓库' }}
        </button>
      </section>

      <section v-else class="form-section">
        <div class="section-head">
          <strong>仓库来源</strong>
          <span>GitHub / Gitea 公网地址</span>
        </div>
        <label>
          仓库地址
          <input
            v-model="repositoryForm.sourceUrl"
            type="url"
            placeholder="https://gitea.com/owner/repo.git"
            required
            @blur="syncNameFromUrl"
          />
          <small class="field-hint">粘贴公开仓库地址，平台会自动迁入并接管。</small>
        </label>
        <div class="form-grid compact">
          <label>
            平台仓库名称
            <input v-model.trim="repositoryForm.name" placeholder="gitguild-demo" required />
          </label>
          <label>
            托管类型
            <select v-model="repositoryForm.hostType">
              <option value="GITEA">Gitea / GitHub</option>
            </select>
          </label>
        </div>
        <button class="primary-action section-action" type="submit" :disabled="!canImport">
          {{ loading ? '正在导入仓库…' : '导入仓库并同步' }}
        </button>
      </section>

      <section v-if="lastRepository" class="form-section repository-result">
        <div class="section-head">
          <strong>{{ stage === 'created' ? '创建结果' : '接入结果' }}</strong>
          <span>{{ stage === 'created' ? '已就绪' : lastRepository.syncStatus || 'SYNCED' }}</span>
        </div>
        <dl>
          <div>
            <dt>仓库</dt>
            <dd>{{ lastRepository.name }}</dd>
          </div>
          <div>
            <dt>仓库 ID</dt>
            <dd>{{ lastRepository.repositoryId }}</dd>
          </div>
          <div>
            <dt>默认分支</dt>
            <dd>{{ lastRepository.defaultBranch || 'main' }}</dd>
          </div>
          <div>
            <dt>地址</dt>
            <dd>{{ lastRepository.sourceUrl }}</dd>
          </div>
        </dl>
        <div class="result-actions">
          <button
            v-if="stage === 'imported'"
            class="quiet-action section-action"
            type="button"
            :disabled="issueLoading"
            @click="loadIssues()"
          >
            {{ issueLoading ? '同步 Issue 中…' : '重新读取 Issue' }}
          </button>
          <button class="primary-action section-action" type="button" @click="goToWorkbench">
            前往委托人工作台发布委托
          </button>
        </div>
      </section>

      <section v-if="lastRepository && stage === 'imported'" class="form-section">
        <div class="section-head">
          <strong>可用 Issue</strong>
          <span>{{ issueLoading ? '读取中' : `${issueOptions.length} 条可用于发布委托` }}</span>
        </div>
        <ul v-if="issueOptions.length" class="issue-list">
          <li v-for="issue in issueOptions.slice(0, 6)" :key="issue.issueId">
            <span>#{{ issue.externalIssueId || issue.issueId }}</span>
            <strong>{{ issue.title }}</strong>
          </li>
        </ul>
        <p v-else class="form-note">
          当前没有可用 OPEN Issue。导入完成后仍可回到工作台，通过“发布委托”新建 Gitea Issue。
        </p>
      </section>

    </form>
  </div>
</template>

<style scoped>
.frontdesk-guide {
  position: absolute;
  inset: 72px 28px 30px;
  z-index: 1;
  color: #ffe7b5;
  pointer-events: none;
}

.frontdesk-guide > * {
  pointer-events: auto;
}

.frontdesk-pocket,
.mascot-dialogue {
  border: 1px solid rgba(238, 184, 91, 0.56);
  border-radius: var(--radius);
  box-shadow: 0 20px 54px rgba(0, 0, 0, 0.38), inset 0 1px 0 rgba(255, 232, 176, 0.16);
  backdrop-filter: blur(7px);
  background:
    linear-gradient(180deg, rgba(31, 17, 9, 0.84), rgba(15, 8, 4, 0.78)),
    radial-gradient(circle at 10% 0%, rgba(216, 154, 50, 0.18), transparent 38%);
}

.mascot-dialogue p,
.frontdesk-pocket p,
.frontdesk-pocket label {
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.5;
}

.mascot-dialogue {
  position: absolute;
  left: 28px;
  top: 8%;
  width: min(500px, 38vw);
  padding: 22px 24px;
  border-radius: 26px 26px 8px 26px;
}

.dialogue-tail {
  position: absolute;
  left: 100%;
  bottom: 34px;
  width: 34px;
  height: 24px;
  clip-path: polygon(0 0, 100% 55%, 0 100%);
  background: rgba(31, 17, 9, 0.82);
}

.mascot-dialogue h2,
.frontdesk-pocket h2 {
  margin: 4px 0 8px;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.2rem;
}

.mascot-dialogue ol {
  display: grid;
  gap: 8px;
  margin: 14px 0 0;
  padding-left: 20px;
  color: rgba(255, 231, 183, 0.82);
}

.mascot-dialogue.success {
  border-color: rgba(129, 184, 98, 0.68);
}

.mascot-dialogue.warn {
  border-color: rgba(238, 120, 82, 0.72);
}

.frontdesk-pocket {
  position: absolute;
  display: grid;
  gap: 12px;
  padding: 16px;
}

.import-panel {
  right: 28px;
  bottom: 28px;
  width: min(540px, 43vw);
  max-height: min(74vh, 720px);
  overflow: auto;
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  border: 1px solid rgba(224, 163, 72, 0.3);
  border-radius: 8px;
  padding: 4px;
  background: rgba(8, 5, 3, 0.4);
}

.mode-switch button {
  min-height: 34px;
  border: 1px solid transparent;
  border-radius: 6px;
  color: rgba(255, 231, 183, 0.66);
  background: transparent;
  font-size: 0.82rem;
  cursor: pointer;
  transition: color 160ms ease, background 160ms ease, border-color 160ms ease;
}

.mode-switch button:hover {
  color: #ffe7b5;
}

.mode-switch button.active {
  border-color: rgba(245, 195, 99, 0.5);
  color: #1a0f06;
  font-weight: 700;
  background: linear-gradient(180deg, #ffd98a, #c07528);
}

.pocket-head,
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-head {
  align-items: center;
  color: #ffe1a0;
}

.section-head strong {
  font-family: var(--font-display);
  font-size: 1rem;
}

.section-head span,
.field-hint,
.form-note {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.74rem;
}

.repo-id {
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 4px 9px;
  color: #ffe4ad;
  font-size: 0.74rem;
  background: rgba(80, 43, 18, 0.44);
}

.field-hint {
  display: block;
  margin-top: 4px;
  line-height: 1.4;
}

.frontdesk-pocket label {
  display: grid;
  gap: 5px;
  font-size: 0.8rem;
}

.frontdesk-pocket input,
.frontdesk-pocket select {
  width: 100%;
  border: 1px solid rgba(224, 163, 72, 0.44);
  border-radius: 6px;
  padding: 9px 10px;
  color: #ffe9bb;
  background: rgba(8, 5, 3, 0.54);
}

.frontdesk-pocket input:focus,
.frontdesk-pocket select:focus {
  outline: 2px solid rgba(255, 203, 119, 0.34);
  border-color: var(--gold-bright);
}

.form-grid.compact {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.form-section {
  display: grid;
  gap: 10px;
  border: 1px solid rgba(224, 163, 72, 0.28);
  border-radius: 8px;
  padding: 12px;
  background: rgba(17, 9, 4, 0.34);
}

.section-action {
  justify-self: end;
  min-height: 34px;
  padding-inline: 16px;
}

.result-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.result-actions .section-action {
  justify-self: auto;
}

.repository-result dl {
  display: grid;
  gap: 8px;
  margin: 0;
}

.repository-result dl div {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 10px;
}

.repository-result dt {
  color: rgba(255, 231, 183, 0.55);
  font-size: 0.74rem;
}

.repository-result dd {
  min-width: 0;
  margin: 0;
  color: #ffe9bb;
  font-size: 0.82rem;
  overflow-wrap: anywhere;
}

.issue-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.issue-list li {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px;
  align-items: center;
  border: 1px solid rgba(238, 184, 91, 0.24);
  border-radius: 7px;
  padding: 8px 10px;
  background: rgba(8, 5, 3, 0.32);
}

.issue-list span {
  color: var(--gold-bright);
  font-size: 0.76rem;
  font-variant-numeric: tabular-nums;
}

.issue-list strong {
  min-width: 0;
  color: #ffe7b5;
  font-size: 0.82rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-note {
  margin: 0;
  line-height: 1.45;
}

.primary-action {
  min-height: 38px;
}

.primary-action:disabled {
  cursor: wait;
  opacity: 0.68;
}

@media (max-width: 980px) {
  .frontdesk-guide {
    position: relative;
    inset: auto;
    display: grid;
    gap: 14px;
    width: calc(100vw - 32px);
    margin: 72px auto 24px;
  }

  .mascot-dialogue,
  .frontdesk-pocket {
    position: relative;
    left: auto;
    right: auto;
    top: auto;
    bottom: auto;
    width: 100%;
    max-height: none;
  }

  .dialogue-tail {
    display: none;
  }
}

@media (max-width: 640px) {
  .form-grid.compact,
  .repository-result dl div {
    grid-template-columns: 1fr;
  }
}
</style>
