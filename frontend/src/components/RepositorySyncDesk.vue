<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'

import { repositoryApi } from '../api/repositoryApi'
import { toBrowsableGiteaUrl } from '../utils/giteaUrl'

const router = useRouter()

const repositoryForm = ref({
  sourceUrl: '',
  name: '',
  hostType: 'GITEA',
})

const loading = ref(false)
const issueLoading = ref(false)
const stage = ref('ready')
// 用户主动停止导入的标记 + 中断进行中请求的控制器
const cancelled = ref(false)
let abortController = null
const lastRepository = ref(null)
const repositoryIssues = ref([])
const errorMessage = ref('')
// 导入失败弹窗：null = 不显示；否则为 { title, reason, hints, detail, sourceUrl }
const failure = ref(null)

// 迁移进度：后端是单次异步调用、不流式回传百分比，所以进度绑定真实阶段边界
// （迁移 → 同步 Issue → 读取 Issue → 完成）；长耗时的迁移阶段用渐近爬升避免“卡死感”。
const progress = ref(0)
const progressStage = ref('')
let creepTimer = null

function stopCreep() {
  if (creepTimer) {
    clearInterval(creepTimer)
    creepTimer = null
  }
}

// 在迁移阶段平滑渐近爬升到 cap（永不到顶），制造“持续推进”的真实反馈。
function startCreep(cap) {
  stopCreep()
  creepTimer = setInterval(() => {
    if (progress.value < cap) {
      progress.value += Math.max(0.4, (cap - progress.value) * 0.06)
      if (progress.value > cap) progress.value = cap
    }
  }, 220)
}

const notice = computed(() => {
  if (errorMessage.value) {
    return {
      tone: 'warn',
      title: '仓库暂时无法接入',
      body: errorMessage.value,
      steps: ['确认仓库地址可访问', '确认仓库为公开仓库或平台有权限', '修正后重新导入'],
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

  return {
    tone: 'info',
    title: '接入受托仓库',
    body: '在这里导入 GitHub 或 Gitea 仓库。平台会把外部仓库迁入本地 Gitea，并同步 Issue，供后续发布委托使用。',
    steps: ['填写仓库地址', '导入并同步仓库', '返回工作台发布委托'],
  }
})

const issueOptions = computed(() => repositoryIssues.value.filter((issue) => issue.status !== 'CLOSED'))
const canImport = computed(() => repositoryForm.value.sourceUrl.trim() && repositoryForm.value.name.trim() && !loading.value)

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
  failure.value = null
  cancelled.value = false
  loading.value = true
  repositoryIssues.value = []
  lastRepository.value = null
  progress.value = 6
  progressStage.value = '正在迁移仓库到平台…'
  startCreep(68)
  abortController = new AbortController()
  const signal = abortController.signal
  try {
    const response = await repositoryApi.importRepository({
      sourceUrl: repositoryForm.value.sourceUrl.trim(),
      name: repositoryForm.value.name.trim(),
      hostType: repositoryForm.value.hostType,
    }, { signal })
    if (cancelled.value) return
    const repository = response?.data
    if (!repository?.repositoryId) throw new Error('仓库导入成功但未返回 repositoryId。')
    lastRepository.value = repository

    // 真实阶段锚点：迁移完成 → 同步 Issue → 读取 Issue → 完成
    stopCreep()
    progress.value = 78
    progressStage.value = '同步 Issue…'
    await repositoryApi.sync(repository.repositoryId, undefined, { signal })
    if (cancelled.value) return

    progress.value = 90
    progressStage.value = '读取 Issue 列表…'
    await loadIssues(repository.repositoryId, { signal })
    if (cancelled.value) return

    progress.value = 100
    progressStage.value = '接入完成'
    stage.value = 'imported'
    // 让满格进度条短暂亮相再隐去，给一个“完成”的收尾反馈
    await new Promise((resolve) => setTimeout(resolve, 350))
  } catch (error) {
    // 用户主动停止：静默重置，不报错、不弹窗
    if (cancelled.value || error?.name === 'AbortError') return
    stage.value = 'ready'
    progress.value = 0
    progressStage.value = ''
    // 左侧前台提示（保留）
    errorMessage.value = readableError(error, '仓库迁移失败，请确认地址正确且仓库为公开仓库。')
    // 弹窗：尽力把失败原因诊断清楚后弹给用户
    failure.value = diagnoseImportError(error, repositoryForm.value.sourceUrl.trim())
  } finally {
    stopCreep()
    loading.value = false
    abortController = null
  }
}

// 停止导入：中断进行中的请求，丢弃本次进度与已产生的服务器副本，回到初始态。
async function stopImport() {
  if (!loading.value) return
  cancelled.value = true
  if (abortController) abortController.abort()

  const created = lastRepository.value
  // 立即重置界面，丢掉本次所有导入痕迹
  lastRepository.value = null
  repositoryIssues.value = []
  progress.value = 0
  progressStage.value = ''
  stage.value = 'ready'
  errorMessage.value = ''
  failure.value = null
  stopCreep()
  loading.value = false

  // 若服务器已创建仓库副本，一并删除（级联 + Gitea 副本），best-effort
  if (created?.repositoryId) {
    try {
      await repositoryApi.remove(created.repositoryId)
    } catch {
      // 删除失败不阻断停止；残留副本可稍后在受托仓库列表手动删除
    }
  }
}

/**
 * 尽力从后端错误中诊断导入失败的真实原因。
 * 综合 业务码(code) + HTTP 状态 + message/details 关键词，给出人话原因与可操作建议。
 */
function diagnoseImportError(error, sourceUrl) {
  const code = error?.code ?? ''
  const status = error?.status
  const raw = `${error?.message ?? ''} ${error?.details ?? ''}`.toLowerCase()

  let reason = '仓库导入失败，原因暂未能精确定位。'
  let hints = ['确认仓库地址可访问', '确认仓库为公开仓库', '修正后重新导入']

  if (code === 'VALIDATION_FAILED' || status === 400) {
    reason = '仓库地址不合法或为空。'
    hints = ['检查地址是否完整（含 https:// 与 owner/repo）', '去掉多余空格后重试']
  } else if (code === 'REPOSITORY_MIGRATION_INCOMPLETE') {
    reason = '仓库迁移未真正完成：源仓库可能过大、不存在或为私有，平台尚未接入成功。'
    hints = ['确认地址有效且仓库为公开仓库', '超大仓库建议精简历史（剔除大文件）后再导入', '稍后重试']
  } else if (/rate limit|rate limitation|429|too many request/.test(raw)) {
    reason = 'GitHub 访问被限流（匿名访问约每小时 60 次）。'
    hints = ['等待几分钟后重试', '若需频繁导入，请联系管理员为迁移配置 GitHub Token']
  } else if (/not found|404/.test(raw)) {
    reason = '找不到该仓库：地址可能拼写错误，或仓库是私有的。'
    hints = ['核对地址里的 owner/repo 是否正确', '确认仓库为公开仓库（私有仓库暂不支持）']
  } else if (/unauthorized|authentication|401|forbidden|403|private|permission/.test(raw) || code === 'FORBIDDEN') {
    reason = '没有权限访问该仓库，通常是私有仓库。'
    hints = ['确认仓库为公开仓库', '私有仓库目前暂不支持导入']
  } else if (/already exists|conflict/.test(raw) || code === 'CODE_HOST_RESOURCE_CONFLICT' || status === 409) {
    reason = '平台上已存在同名的仓库副本。'
    hints = ['换一个「平台仓库名称」后重试', '或在受托仓库列表删除旧副本后再导入']
  } else if (/timeout|timed out|deadline/.test(raw)) {
    reason = '迁移超时：仓库可能过大，或网络较慢。'
    hints = ['稍后重试', '大仓库迁移耗时较长，请耐心等待页面完成']
  } else if (code === 'CODE_HOST_UNAVAILABLE' || status === 502 || status === 503 || status === 504) {
    reason = '平台代码托管服务（Gitea）暂时不可用。'
    hints = ['稍后重试', '若持续失败，请联系管理员检查 Gitea 服务状态']
  } else if (!status || error?.name === 'TypeError' || /failed to fetch|networkerror/.test(raw)) {
    reason = '网络请求失败，无法连接到后端服务。'
    hints = ['检查本机网络连接', '确认后端服务正在运行后重试']
  } else if (code === 'REPOSITORY_MIGRATION_FAILED') {
    reason = '仓库迁移失败：通常是地址错误或仓库非公开。'
    hints = ['确认仓库地址正确且可访问', '确认仓库为公开仓库（私有仓库暂不支持）']
  }

  return {
    title: '仓库导入失败',
    reason,
    hints,
    detail: error?.details || error?.message || '',
    sourceUrl,
  }
}

function closeFailure() {
  failure.value = null
}

// 弹窗内「重新导入」：关掉弹窗并用当前表单重试
function retryFromFailure() {
  if (loading.value) return
  failure.value = null
  importRepository()
}

async function loadIssues(repositoryId = lastRepository.value?.repositoryId, options = {}) {
  if (!repositoryId) {
    repositoryIssues.value = []
    return
  }

  issueLoading.value = true
  try {
    const response = await repositoryApi.issues(repositoryId, { size: 50 }, { signal: options.signal })
    repositoryIssues.value = response?.data?.items ?? []
  } catch (error) {
    // 用户停止导入而中断的请求：静默忽略
    if (cancelled.value || error?.name === 'AbortError') return
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

// 接入成功后引导维护者直接去发布委托
function goPublish() {
  router.push({ name: 'maintainer-publish' })
}

onBeforeUnmount(stopCreep)
</script>

<template>
  <div class="frontdesk-guide" aria-label="前台仓库导入向导">
    <section class="mascot-dialogue" :class="notice.tone" aria-live="polite">
      <span class="dialogue-tail" aria-hidden="true"></span>
      <p class="kicker">前台向导</p>
      <h2>{{ notice.title }}</h2>
      <p>{{ notice.body }}</p>
      <ol>
        <li v-for="step in notice.steps" :key="step">{{ step }}</li>
      </ol>
    </section>

    <form class="frontdesk-pocket import-panel" @submit.prevent="importRepository">
      <div class="pocket-head">
        <div>
          <p class="kicker">Repository Intake</p>
          <h2>导入仓库</h2>
        </div>
        <span v-if="lastRepository" class="repo-id">ID {{ lastRepository.repositoryId }}</span>
      </div>

      <section class="form-section">
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
            <input v-model.trim="repositoryForm.name" placeholder="例: GitGuild" required />
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

        <button
          v-if="loading"
          class="section-action stop-import"
          type="button"
          @click="stopImport"
        >
          停止导入
        </button>

        <div
          v-if="loading"
          class="migrate-progress"
          role="progressbar"
          :aria-valuenow="Math.round(progress)"
          aria-valuemin="0"
          aria-valuemax="100"
        >
          <div class="migrate-progress-head">
            <span class="migrate-stage">{{ progressStage }}</span>
            <span class="migrate-pct">{{ Math.round(progress) }}%</span>
          </div>
          <div class="migrate-track">
            <div class="migrate-fill" :style="{ width: `${progress}%` }">
              <span class="migrate-spark" aria-hidden="true"></span>
            </div>
          </div>
          <p class="migrate-hint">大型仓库迁移可能需要一会儿，请保持页面打开…</p>
        </div>
      </section>

      <section v-if="lastRepository" class="form-section">
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
        <button class="quiet-action section-action" type="button" :disabled="issueLoading" @click="loadIssues()">
          {{ issueLoading ? '同步 Issue 中…' : '重新读取 Issue' }}
        </button>
      </section>

      <section v-if="lastRepository" class="form-section repository-result">
        <div class="section-head">
          <strong>接入结果</strong>
          <span>{{ lastRepository.syncStatus || 'SYNCED' }}</span>
        </div>
        <dl>
          <div>
            <dt>仓库</dt>
            <dd>{{ lastRepository.name }}</dd>
          </div>
          <div>
            <dt>默认分支</dt>
            <dd>{{ lastRepository.defaultBranch || 'main' }}</dd>
          </div>
          <div>
            <dt>地址</dt>
            <dd>{{ toBrowsableGiteaUrl(lastRepository.sourceUrl) }}</dd>
          </div>
        </dl>
      </section>

      <div v-if="stage === 'imported'" class="next-step">
        <button class="cta-publish" type="button" @click="goPublish">
          <span>去发布委托</span>
          <span class="cta-publish-arrow" aria-hidden="true">→</span>
        </button>
        <span class="next-step-hint">仓库已接入 · 下一步起草并发布委托</span>
      </div>

    </form>

    <transition name="failure-pop">
      <div
        v-if="failure"
        class="import-failure-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="failure-title"
        @click.self="closeFailure"
      >
        <div class="failure-card">
          <button class="failure-close" type="button" aria-label="关闭" @click="closeFailure">×</button>
          <span class="failure-icon" aria-hidden="true">!</span>
          <p class="kicker">Import Failed</p>
          <h2 id="failure-title">{{ failure.title }}</h2>
          <p class="failure-reason">{{ failure.reason }}</p>
          <p v-if="failure.sourceUrl" class="failure-source">{{ failure.sourceUrl }}</p>

          <ul class="failure-hints">
            <li v-for="hint in failure.hints" :key="hint">{{ hint }}</li>
          </ul>

          <details v-if="failure.detail" class="failure-detail">
            <summary>技术细节</summary>
            <code>{{ failure.detail }}</code>
          </details>

          <div class="failure-actions">
            <button class="quiet-action" type="button" @click="closeFailure">关闭</button>
            <button class="primary-action" type="button" :disabled="loading" @click="retryFromFailure">
              重新导入
            </button>
          </div>
        </div>
      </div>
    </transition>
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

/* 输入框占位示例文字：淡色，仅作示例引导，用户输入即覆盖。 */
.frontdesk-pocket input::placeholder {
  color: rgba(255, 231, 183, 0.4);
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

/* 停止导入：红褐警示，立即中断本次导入并丢弃已产生的副本 */
.stop-import {
  min-height: 34px;
  padding-inline: 16px;
  border: 1px solid rgba(238, 120, 82, 0.6);
  border-radius: 8px;
  color: #ffd2bf;
  cursor: pointer;
  background: linear-gradient(180deg, rgba(120, 44, 28, 0.66), rgba(70, 24, 14, 0.72));
  transition: filter 150ms ease, transform 120ms ease;
}
.stop-import:hover {
  filter: brightness(1.12);
}
.stop-import:active {
  transform: translateY(1px);
}

/* ── 迁移进度条（暗金鎏光 + 前缘 spark） ───────────────────── */
.migrate-progress {
  display: grid;
  gap: 7px;
  margin-top: 2px;
}
.migrate-progress-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.migrate-stage {
  color: #ffe1a0;
  font-family: var(--font-display);
  font-size: 0.82rem;
}
.migrate-pct {
  color: var(--gold-bright);
  font-size: 0.82rem;
  font-variant-numeric: tabular-nums;
}
.migrate-track {
  position: relative;
  height: 10px;
  border-radius: 999px;
  border: 1px solid rgba(224, 163, 72, 0.3);
  background: rgba(8, 5, 3, 0.6);
  box-shadow: inset 0 1px 4px rgba(0, 0, 0, 0.55);
  overflow: hidden;
}
.migrate-fill {
  position: relative;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #b9772a, #ffd277 55%, #ffe6a8);
  background-size: 200% 100%;
  box-shadow: 0 0 12px rgba(255, 194, 92, 0.5);
  transition: width 420ms cubic-bezier(0.4, 0, 0.2, 1);
  animation: migrate-shimmer 1.6s linear infinite;
}
@keyframes migrate-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
.migrate-spark {
  position: absolute;
  right: -1px;
  top: 50%;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  transform: translateY(-50%);
  background: radial-gradient(circle, #fff6dd, rgba(255, 210, 119, 0.2) 70%, transparent);
  box-shadow: 0 0 10px 2px rgba(255, 214, 130, 0.7);
  animation: migrate-pulse 1.1s ease-in-out infinite;
}
@keyframes migrate-pulse {
  0%,
  100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}
.migrate-hint {
  margin: 1px 0 0;
  color: rgba(255, 231, 183, 0.5);
  font-size: 0.72rem;
}

/* ── 接入成功后的下一步 CTA：去发布委托 ───────────────────── */
.next-step {
  display: grid;
  gap: 6px;
  margin: 2px 0 2px;
}
.cta-publish {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  min-height: 42px;
  border: none;
  border-radius: 9px;
  color: #3a1f0c;
  font-family: var(--font-display);
  font-size: 1rem;
  letter-spacing: 0.02em;
  cursor: pointer;
  background: linear-gradient(180deg, #ffe6a6, #e9b860 60%, #d89a32);
  box-shadow: 0 8px 22px rgba(120, 70, 18, 0.45), inset 0 1px 0 rgba(255, 247, 220, 0.6);
  transition: transform 160ms ease, box-shadow 160ms ease, filter 160ms ease;
}
.cta-publish:hover,
.cta-publish:focus-visible {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 0 0 5px rgba(255, 204, 105, 0.14), 0 12px 26px rgba(120, 70, 18, 0.5);
  outline: none;
}
.cta-publish:active {
  transform: translateY(0) scale(0.99);
}
.cta-publish-arrow {
  font-size: 1.15rem;
  transition: transform 160ms ease;
}
.cta-publish:hover .cta-publish-arrow,
.cta-publish:focus-visible .cta-publish-arrow {
  transform: translateX(4px);
}
.next-step-hint {
  text-align: center;
  color: rgba(255, 231, 183, 0.6);
  font-size: 0.74rem;
}

/* ── 导入失败弹窗 ─────────────────────────────────────────── */
.import-failure-modal {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 24px;
  background: radial-gradient(circle at 50% 35%, rgba(40, 14, 6, 0.66), rgba(6, 3, 2, 0.82));
  backdrop-filter: blur(4px);
}

.failure-card {
  position: relative;
  width: min(440px, 92vw);
  max-height: 86vh;
  overflow: auto;
  padding: 26px 26px 22px;
  border: 1px solid rgba(238, 120, 82, 0.6);
  border-radius: 16px;
  color: #ffe7b5;
  text-align: center;
  background:
    linear-gradient(180deg, rgba(38, 18, 10, 0.96), rgba(20, 9, 5, 0.96)),
    radial-gradient(circle at 50% 0%, rgba(238, 120, 82, 0.22), transparent 52%);
  box-shadow: 0 26px 70px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 200, 160, 0.16);
}

.failure-close {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  color: rgba(255, 220, 196, 0.72);
  font-size: 1.25rem;
  line-height: 1;
  cursor: pointer;
  background: transparent;
  transition: background 150ms ease, color 150ms ease;
}
.failure-close:hover {
  color: #ffe7b5;
  background: rgba(238, 120, 82, 0.2);
}

.failure-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  margin: 2px auto 10px;
  border-radius: 50%;
  color: #fff1e6;
  font-family: var(--font-display);
  font-size: 1.7rem;
  font-weight: 700;
  background: radial-gradient(circle at 50% 30%, #ff8a5c, #d8431f);
  box-shadow: 0 0 0 6px rgba(238, 120, 82, 0.16), 0 8px 20px rgba(160, 50, 20, 0.5);
}

.failure-card .kicker {
  color: rgba(255, 176, 142, 0.82);
}

.failure-card h2 {
  margin: 2px 0 10px;
  color: #ffd9c4;
  font-family: var(--font-display);
  font-size: 1.32rem;
}

.failure-reason {
  margin: 0 0 12px;
  color: #ffe7d2;
  font-size: 0.98rem;
  line-height: 1.55;
}

.failure-source {
  margin: 0 auto 14px;
  max-width: 100%;
  padding: 6px 10px;
  border-radius: 7px;
  color: rgba(255, 224, 196, 0.78);
  font-size: 0.78rem;
  overflow-wrap: anywhere;
  background: rgba(8, 5, 3, 0.5);
  border: 1px solid rgba(238, 120, 82, 0.26);
}

.failure-hints {
  display: grid;
  gap: 7px;
  margin: 0 0 14px;
  padding: 0;
  text-align: left;
  list-style: none;
}
.failure-hints li {
  position: relative;
  padding-left: 22px;
  color: rgba(255, 231, 183, 0.86);
  font-size: 0.86rem;
  line-height: 1.45;
}
.failure-hints li::before {
  content: '→';
  position: absolute;
  left: 4px;
  color: #ff9c6f;
}

.failure-detail {
  margin: 0 0 16px;
  text-align: left;
}
.failure-detail summary {
  color: rgba(255, 200, 160, 0.7);
  font-size: 0.78rem;
  cursor: pointer;
  user-select: none;
}
.failure-detail code {
  display: block;
  margin-top: 8px;
  padding: 9px 11px;
  border-radius: 7px;
  color: rgba(255, 226, 200, 0.82);
  font-size: 0.74rem;
  line-height: 1.5;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  background: rgba(0, 0, 0, 0.42);
  border: 1px solid rgba(238, 120, 82, 0.2);
}

.failure-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
.failure-actions .quiet-action,
.failure-actions .primary-action {
  min-height: 38px;
  padding-inline: 20px;
}

.failure-pop-enter-active {
  transition: opacity 200ms ease;
}
.failure-pop-leave-active {
  transition: opacity 160ms ease;
}
.failure-pop-enter-from,
.failure-pop-leave-to {
  opacity: 0;
}
.failure-pop-enter-active .failure-card {
  animation: failure-rise 260ms cubic-bezier(0.16, 0.84, 0.3, 1);
}
@keyframes failure-rise {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
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
