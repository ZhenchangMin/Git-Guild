<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { questApi } from '../../api/questApi'
import { repositoryApi } from '../../api/repositoryApi'
import parchmentFormImg from '../../assets/submission-form-parchment-v0-clean.webp'

const router = useRouter()

// ── 选项数据 ────────────────────────────────────────────────────────────────
const repositories = ref([])
const categories = ref([])
const tags = ref([])
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
  title: '',
  description: '',
  completionCriteria: '',
  categoryId: '',
  tagIds: [],
  difficulty: 'A',
  techStack: '',
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

const tagOptions = computed(() => tags.value.filter((tag) => tag.enabled !== false))

// ── 加载仓库 + 分类 ─────────────────────────────────────────────────────────
// 首屏只等"仓库 + 分类"这两个必填项；标签非必填，拆成非阻塞异步，避免拖慢表单首屏。
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
      // 赋值即触发下方 watch(repositoryId) 去拉 Issue，无需再手动调用（否则会重复请求）。
      form.value.repositoryId = String(repositories.value[0].repositoryId)
    }
    const mvp = categories.value.find((c) => c.name === 'MVP') ?? categories.value[0]
    if (mvp) form.value.categoryId = String(mvp.categoryId)
  } catch (error) {
    metaError.value = error?.message ?? '加载仓库/分类失败，请确认后端已启动。'
  } finally {
    loadingMeta.value = false
  }
  loadTags()
}

// 标签：非阻塞加载，失败不影响发布（标签可选）。
async function loadTags() {
  try {
    tags.value = unwrapItems(await questApi.tags({ size: 100 }))
  } catch {
    tags.value = []
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

// Issue 由上方 watch(repositoryId) 在 loadMeta 设置首个仓库时自动加载，无需在此重复调用。
onMounted(loadMeta)

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
      tagIds: form.value.tagIds.map(Number),
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

// 返回上一页：有站内历史就弹出（通常是事务所），避免用 push 反复压入新条目造成
// publish ↔ maintainer 历史栈来回 ping-pong 死循环；无历史（深链进入）时兜底回事务所。
function backToWorkbench() {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push({ name: 'maintainer-workbench' })
  }
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
  <main class="app-shell">
    <section class="scene writ-scene">
      <button class="back-orb" type="button" aria-label="返回委托人事务所" @click="backToWorkbench">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回事务所</span>
      </button>

      <article class="writ-panel" :style="{ '--writ-sheet-image': `url(${parchmentFormImg})` }">
        <header class="writ-head">
          <p class="writ-eyebrow">委托书 · Commission Writ</p>
          <h1>发布委托</h1>
          <p class="writ-sub">委托人起草任务并提交管理员审核（DRAFT → 待审核），通过后上架悬赏板。</p>
        </header>

        <div v-if="loadingMeta" class="writ-skeleton" aria-hidden="true">
          <span v-for="n in 5" :key="n" class="sk sk-row"></span>
        </div>

        <p v-else-if="metaError" class="writ-banner error">{{ metaError }}</p>

        <div v-else-if="submitOk" class="writ-receipt">
          <div class="wax-seal" aria-hidden="true">
            <span class="wax-glow"></span>
            <span class="wax-rune">委</span>
          </div>
          <p class="writ-eyebrow">受理回执 · Filed</p>
          <h2>委托 #{{ submitOk.questId }} 已呈交审核</h2>
          <p class="writ-receipt-hint">
            任务已创建并提交管理员审核。请用 admin 账号到审核台通过后，委托即上架悬赏板。
          </p>
          <div class="writ-receipt-actions">
            <button class="quiet-action" type="button" @click="submitOk = null">继续发布</button>
            <button class="primary-action" type="button" @click="backToWorkbench">返回事务所</button>
          </div>
        </div>

        <form v-else class="writ-form" @submit.prevent="publish">
          <label class="writ-field">
            <span>目标仓库</span>
            <select v-model="form.repositoryId">
              <option v-for="r in repositories" :key="r.repositoryId" :value="String(r.repositoryId)">
                {{ r.name }}（{{ r.defaultBranch || 'main' }}）
              </option>
            </select>
            <small v-if="errors.repositoryId" class="writ-error">{{ errors.repositoryId }}</small>
            <small v-else-if="selectedRepo" class="writ-hint">{{ selectedRepo.sourceUrl }}</small>
          </label>

          <label class="writ-field">
            <span>分类</span>
            <select v-model="form.categoryId">
              <option v-for="c in categories" :key="c.categoryId" :value="String(c.categoryId)">{{ c.name }}</option>
            </select>
            <small v-if="errors.categoryId" class="writ-error">{{ errors.categoryId }}</small>
          </label>

          <fieldset class="writ-field writ-wide">
            <span>标签</span>
            <div v-if="tagOptions.length" class="writ-tag-options" aria-label="任务标签">
              <label v-for="tag in tagOptions" :key="tag.tagId" class="writ-tag-option">
                <input v-model="form.tagIds" type="checkbox" :value="String(tag.tagId)" />
                <span class="writ-tag-dot" :style="{ background: tag.color }" aria-hidden="true"></span>
                <span>{{ tag.name }}</span>
              </label>
            </div>
            <small v-else class="writ-hint">暂无可用标签；可先在管理员平台配置中新增。</small>
          </fieldset>

          <fieldset class="writ-field writ-wide writ-issue">
            <span>关联 Issue</span>
            <div class="writ-segmented" role="tablist" aria-label="Issue 来源">
              <button
                type="button"
                role="tab"
                :aria-selected="form.issueMode === 'existing'"
                :class="{ active: form.issueMode === 'existing' }"
                @click="form.issueMode = 'existing'"
              >选择已有</button>
              <button
                type="button"
                role="tab"
                :aria-selected="form.issueMode === 'new'"
                :class="{ active: form.issueMode === 'new' }"
                @click="form.issueMode = 'new'"
              >新建 Gitea Issue</button>
            </div>
            <template v-if="form.issueMode === 'existing'">
              <select v-model="form.issueId" :disabled="loadingIssues">
                <option v-if="loadingIssues" value="">加载中…</option>
                <option v-for="i in issues" :key="i.issueId" :value="String(i.issueId)">
                  #{{ i.externalIssueId }} · {{ i.title }}
                </option>
              </select>
              <small v-if="!loadingIssues && !issues.length" class="writ-hint">该仓库暂无 OPEN Issue，请切换为新建。</small>
            </template>
            <template v-else>
              <input v-model.trim="form.giteaIssueTitle" placeholder="Issue 标题，如：Add hello world file" />
              <textarea v-model="form.giteaIssueBody" rows="2" placeholder="Issue 描述（可选）"></textarea>
            </template>
            <small v-if="errors.issue" class="writ-error">{{ errors.issue }}</small>
          </fieldset>

          <label class="writ-field writ-wide">
            <span>任务标题</span>
            <input v-model.trim="form.title" placeholder="例：Hello World" />
            <small v-if="errors.title" class="writ-error">{{ errors.title }}</small>
          </label>

          <label class="writ-field writ-wide">
            <span>任务描述</span>
            <textarea
              v-model="form.description"
              rows="3"
              placeholder="例：在仓库中新增一个 hello world 文件，作为最短 MVP 演示任务。"
            ></textarea>
            <small v-if="errors.description" class="writ-error">{{ errors.description }}</small>
          </label>

          <label class="writ-field writ-wide">
            <span>完成标准</span>
            <textarea
              v-model="form.completionCriteria"
              rows="2"
              placeholder='例：新增 hello.md（或 hello world 文件），内容包含 "Hello, Git-Guild!"，并通过 PR 合并。'
            ></textarea>
            <small v-if="errors.completionCriteria" class="writ-error">{{ errors.completionCriteria }}</small>
          </label>

          <label class="writ-field">
            <span>难度</span>
            <select v-model="form.difficulty">
              <option v-for="d in DIFFICULTIES" :key="d" :value="d">{{ d }}</option>
            </select>
          </label>

          <label class="writ-field">
            <span>技术栈（逗号分隔）</span>
            <input v-model.trim="form.techStack" placeholder="Markdown, Git" />
            <small v-if="errors.techStack" class="writ-error">{{ errors.techStack }}</small>
          </label>

          <label class="writ-field">
            <span>预估工时（小时）</span>
            <input v-model="form.estimatedHours" type="number" min="1" />
            <small v-if="errors.estimatedHours" class="writ-error">{{ errors.estimatedHours }}</small>
          </label>

          <label class="writ-field">
            <span>奖励 XP</span>
            <input v-model="form.rewardXp" type="number" min="1" />
            <small v-if="errors.rewardXp" class="writ-error">{{ errors.rewardXp }}</small>
          </label>

          <div class="writ-actions">
            <button class="primary-action" type="submit" :disabled="!canSubmit">
              {{ submitting ? '发布中…' : '发布并提交审核' }}
            </button>
            <p v-if="submitError" class="writ-banner error">{{ submitError }}</p>
          </div>
        </form>
      </article>
    </section>
  </main>
</template>

<style scoped>
.writ-scene {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: clamp(24px, 6vh, 64px) 18px;
  background-image: linear-gradient(rgba(8, 4, 2, 0.52), rgba(8, 4, 2, 0.52)),
    url('../../assets/desk.webp');
}

.writ-panel {
  --writ-pad-x: clamp(76px, 14%, 152px);
  --writ-pad-y: clamp(88px, 12%, 140px);

  position: relative;
  box-sizing: border-box;
  width: min(1086px, calc(100vw - 36px));
  aspect-ratio: 1086 / 1448;
  padding: var(--writ-pad-y) var(--writ-pad-x);
  color: var(--ink);
  isolation: isolate;
}

.writ-panel::before {
  position: absolute;
  inset: 0;
  z-index: -2;
  content: '';
  pointer-events: none;
  background-image: var(--writ-sheet-image);
  background-repeat: no-repeat;
  background-position: center;
  background-size: 100% 100%;
  filter:
    drop-shadow(0 0 1px rgba(0, 0, 0, 0.86))
    drop-shadow(0 0 4px rgba(0, 0, 0, 0.64))
    drop-shadow(0 12px 28px rgba(0, 0, 0, 0.5))
    drop-shadow(0 30px 58px rgba(0, 0, 0, 0.58));
}

.writ-panel::after {
  position: absolute;
  inset: 6.5% 8%;
  z-index: -1;
  content: '';
  pointer-events: none;
  background: radial-gradient(
    ellipse 68% 58% at 50% 50%,
    rgba(243, 231, 202, 0.86) 0%,
    rgba(243, 231, 202, 0.58) 48%,
    rgba(243, 231, 202, 0) 82%
  );
}

.writ-eyebrow {
  margin: 0 0 8px;
  color: #9a5a1c;
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}
.writ-head {
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(122, 74, 24, 0.24);
}
.writ-head h1 {
  color: var(--ink);
  font-size: clamp(1.7rem, 3.4vw, 2.2rem);
}
.writ-sub {
  margin: 8px 0 0;
  max-width: 58ch;
  color: var(--ink-soft);
  font-size: 0.92rem;
  line-height: 1.55;
}

.writ-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  /* 顶端对齐：避免"目标仓库"格因多一行地址提示而拉高同行的"分类"选择框，导致两框不齐。 */
  align-items: start;
}
.writ-field {
  display: grid;
  gap: 6px;
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}
.writ-field.writ-wide,
.writ-issue {
  grid-column: 1 / -1;
}
.writ-field > span {
  color: var(--ink-soft);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.writ-field input,
.writ-field select,
.writ-field textarea {
  width: 100%;
  border: 1px solid rgba(98, 55, 20, 0.26);
  border-radius: 4px;
  padding: 9px 10px;
  color: #3b210f;
  font-family: var(--font-body);
  font-size: 0.92rem;
  background: rgba(255, 244, 210, 0.56);
  box-shadow: inset 0 1px 6px rgba(70, 34, 10, 0.12);
  transition: border-color 150ms ease, box-shadow 150ms ease, background 150ms ease;
}
.writ-field input::placeholder,
.writ-field textarea::placeholder {
  color: rgba(91, 53, 29, 0.5);
}
.writ-field input:focus,
.writ-field select:focus,
.writ-field textarea:focus {
  outline: none;
  border-color: var(--gold);
  background: rgba(255, 244, 210, 0.72);
  box-shadow:
    inset 0 1px 6px rgba(70, 34, 10, 0.1),
    0 0 0 2px rgba(216, 154, 50, 0.18);
}
.writ-field textarea {
  resize: vertical;
}

.writ-tag-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.writ-tag-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  border: 1px solid rgba(98, 55, 20, 0.24);
  border-radius: 999px;
  padding: 6px 10px;
  color: #4c2a12;
  font-size: 0.84rem;
  background: rgba(255, 244, 210, 0.46);
  box-shadow: inset 0 1px 5px rgba(70, 34, 10, 0.1);
  cursor: pointer;
  transition: border-color 150ms ease, background 150ms ease, box-shadow 150ms ease;
}

.writ-tag-option:has(input:checked) {
  border-color: rgba(169, 106, 28, 0.58);
  background: rgba(235, 186, 94, 0.32);
  box-shadow:
    inset 0 1px 5px rgba(70, 34, 10, 0.1),
    0 0 0 2px rgba(216, 154, 50, 0.14);
}

.writ-tag-option input {
  width: 14px;
  height: 14px;
  margin: 0;
  accent-color: #b67a24;
}

.writ-tag-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgba(70, 34, 10, 0.18);
}

.writ-segmented {
  display: inline-flex;
  width: fit-content;
  gap: 3px;
  padding: 3px;
  border: 1px solid rgba(122, 74, 24, 0.3);
  border-radius: 7px;
  background: rgba(122, 74, 24, 0.08);
}
.writ-segmented button {
  border: none;
  border-radius: 5px;
  padding: 6px 14px;
  color: var(--ink-soft);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.84rem;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease, box-shadow 150ms ease;
}
.writ-segmented button.active {
  color: #3a1f0c;
  background: linear-gradient(180deg, #ffe6a6, #e3b362);
  box-shadow: 0 2px 6px rgba(80, 40, 8, 0.26);
}

.writ-error {
  color: var(--wine);
  font-size: 0.8rem;
}
.writ-hint {
  color: rgba(91, 53, 29, 0.6);
  font-size: 0.8rem;
  word-break: break-all;
}

.writ-actions {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-top: 4px;
}
.writ-banner {
  margin: 0;
  padding: 9px 12px;
  border-radius: 6px;
  font-size: 0.86rem;
}
.writ-banner.error {
  color: var(--wine);
  border: 1px solid rgba(110, 42, 36, 0.3);
  background: rgba(110, 42, 36, 0.12);
}

.writ-receipt {
  position: relative;
  margin-top: 30px;
  padding: 46px 28px 28px;
  text-align: center;
  border: 1px solid rgba(122, 74, 24, 0.3);
  border-radius: 10px;
  background: rgba(255, 248, 228, 0.6);
}
.writ-receipt h2 {
  margin: 10px 0 6px;
  color: var(--ink);
}
.writ-receipt-hint {
  margin: 0 auto;
  max-width: 46ch;
  color: var(--ink-soft);
  font-size: 0.9rem;
  line-height: 1.6;
}
.writ-receipt-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}

.writ-skeleton {
  display: grid;
  gap: 14px;
  margin-top: 8px;
}
.sk {
  border-radius: 5px;
  background: linear-gradient(
    90deg,
    rgba(122, 74, 24, 0.1),
    rgba(122, 74, 24, 0.22),
    rgba(122, 74, 24, 0.1)
  );
  background-size: 200% 100%;
  animation: writ-shimmer 1.3s ease-in-out infinite;
}
.sk-row {
  height: 40px;
}
@keyframes writ-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@media (max-width: 640px) {
  .writ-panel {
    --writ-pad-x: clamp(40px, 12vw, 76px);
    --writ-pad-y: clamp(54px, 14vw, 88px);
  }

  .writ-form {
    grid-template-columns: 1fr;
  }
}
</style>
