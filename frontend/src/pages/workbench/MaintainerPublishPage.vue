<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { questApi } from '../../api/questApi'
import { repositoryApi } from '../../api/repositoryApi'
import { questDifficulties } from '../../data/adminTaxonomy'
import HomeOrb from '../../components/HomeOrb.vue'
import parchmentFormImg from '../../assets/submission-form-parchment-v0-clean.webp'
import { toBrowsableGiteaUrl } from '../../utils/giteaUrl'

const router = useRouter()
const route = useRoute()

// ── 选项数据 ────────────────────────────────────────────────────────────────
const repositories = ref([])
const categories = ref([])
const tags = ref([])
const issues = ref([])
const loadingMeta = ref(true)
const loadingIssues = ref(false)
const metaError = ref('')

// ── 表单状态 ────────────────────────────────────────────────────────────────
function blankForm() {
  return {
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
  }
}

const form = ref(blankForm())

const submitting = ref(false)
const submitError = ref('')
const submitOk = ref(null) // { questId }
// 该仓库+Issue 已发布过委托时的提示弹窗（{ issueLabel }）。
const duplicateIssue = ref(null)

// 当前选中 Issue 的可读标签，用于重复发布弹窗里指明是哪一条。
const selectedIssueLabel = computed(() => {
  if (form.value.issueMode === 'new') return form.value.giteaIssueTitle.trim()
  const found = issues.value.find((i) => String(i.issueId) === String(form.value.issueId))
  if (!found) return ''
  const num = found.externalIssueId ?? found.issueId
  return [num ? `#${num}` : '', found.title ?? ''].filter(Boolean).join(' · ')
})

// 难度选项复用 admin 平台配置的同一份枚举，附带分级说明，
// 让委托人一眼看懂 A=最难、D=最易，而非只见字母。
const DIFFICULTIES = questDifficulties

const selectedDifficulty = computed(
  () => DIFFICULTIES.find((d) => d.code === form.value.difficulty) ?? null,
)

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
      // 若从同步台「去发布委托」跳入，query.repoId 指定了刚导入的仓库，优先预选它；
      // 赋值即触发下方 watch(repositoryId) 去拉 Issue，无需再手动调用。
      const preselect = route.query.repoId
        ? repositories.value.find((r) => String(r.repositoryId) === String(route.query.repoId))
        : null
      form.value.repositoryId = String((preselect ?? repositories.value[0]).repositoryId)
    }
    preselectCategory()
  } catch (error) {
    metaError.value = error?.message ?? '加载仓库/分类失败，请确认后端已启动。'
  } finally {
    loadingMeta.value = false
  }
  loadTags()
}

function preselectCategory() {
  const mvp = categories.value.find((c) => c.name === 'MVP') ?? categories.value[0]
  if (mvp) form.value.categoryId = String(mvp.categoryId)
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
  duplicateIssue.value = null
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
    // 该 Issue 已关联进行中的委托（或已关闭）→ 后端返回 ISSUE_NOT_AVAILABLE，
    // 改为弹窗引导维护者去工作台查看自己已发布的委托，而非裸报错。
    if (error?.code === 'ISSUE_NOT_AVAILABLE') {
      duplicateIssue.value = { issueLabel: selectedIssueLabel.value }
    } else {
      // 后端把根因放进 details（如异常类型+消息、字段校验明细），
      // 拼到提示里而非只给一句通用文案，方便委托人定位是哪一步出了问题。
      const base = error?.message ?? '发布失败，请稍后再试。'
      submitError.value = error?.details ? `${base}（${error.details}）` : base
    }
  } finally {
    submitting.value = false
  }
}

function goWorkbench() {
  router.push({ name: 'maintainer-workbench' })
}

// 「继续发布」：回到一个干净的初始表单（保留仓库/分类的默认预选），而非沿用刚提交过的内容。
function startNewPublish() {
  submitOk.value = null
  submitError.value = ''
  form.value = blankForm()
  if (repositories.value.length > 0) {
    form.value.repositoryId = String(repositories.value[0].repositoryId)
  }
  preselectCategory()
  loadIssues(form.value.repositoryId)
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
      <HomeOrb />
      <button class="back-orb" type="button" aria-label="返回" @click="backToWorkbench">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回</span>
      </button>

      <article class="writ-panel" :style="{ '--writ-sheet-image': `url(${parchmentFormImg})` }">
        <header class="writ-head">
          <p class="writ-eyebrow">委托书 · Commission Writ</p>
          <h1>发布委托</h1>
          <p class="writ-sub">委托人起草任务并提交管理员审核（DRAFT → 待审核），通过后上架悬赏板。</p>
        </header>

        <div v-if="submitOk" class="writ-receipt">
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
            <button class="quiet-action" type="button" @click="startNewPublish">继续发布</button>
            <button class="primary-action" type="button" @click="goWorkbench">返回事务所</button>
          </div>
        </div>

        <form v-else class="writ-form" @submit.prevent="publish">
          <p v-if="metaError" class="writ-banner error writ-wide">{{ metaError }}</p>

          <label class="writ-field" data-tutorial="publish-repository">
            <span>目标仓库</span>
            <select v-model="form.repositoryId" :disabled="loadingMeta">
              <option v-if="loadingMeta" value="" disabled>载入仓库中…</option>
              <option v-else-if="!repositories.length" value="" disabled>暂无可用仓库，请先导入</option>
              <option v-for="r in repositories" :key="r.repositoryId" :value="String(r.repositoryId)">
                {{ r.name }}（{{ r.defaultBranch || 'main' }}）
              </option>
            </select>
            <small v-if="errors.repositoryId" class="writ-error">{{ errors.repositoryId }}</small>
            <small v-else-if="selectedRepo" class="writ-hint">{{ toBrowsableGiteaUrl(selectedRepo.sourceUrl) }}</small>
          </label>

          <label class="writ-field">
            <span>分类</span>
            <select v-model="form.categoryId" :disabled="loadingMeta">
              <option v-if="loadingMeta" value="" disabled>载入分类中…</option>
              <option v-else-if="!categories.length" value="" disabled>暂无分类</option>
              <option v-for="c in categories" :key="c.categoryId" :value="String(c.categoryId)">{{ c.name }}</option>
            </select>
            <small v-if="errors.categoryId" class="writ-error">{{ errors.categoryId }}</small>
          </label>

          <fieldset class="writ-field writ-wide" data-tutorial="publish-taxonomy">
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

          <fieldset class="writ-field writ-wide writ-issue" data-tutorial="publish-issue">
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

          <div class="writ-task-basics" data-tutorial="publish-task-basics">
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
              <option v-for="d in DIFFICULTIES" :key="d.code" :value="d.code">
                {{ d.code }} · {{ d.label }} — {{ d.hint }}
              </option>
            </select>
            <small v-if="selectedDifficulty" class="writ-hint">
              {{ selectedDifficulty.label }}：{{ selectedDifficulty.hint }}（A 最难 · D 最易）
            </small>
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

          </div>

          <div class="writ-actions">
            <button class="primary-action" type="submit" data-tutorial="publish-submit" :disabled="!canSubmit">
              {{ submitting ? '发布中…' : '发布并提交审核' }}
            </button>
            <p v-if="submitError" class="writ-banner error">{{ submitError }}</p>
          </div>
        </form>
      </article>

      <!-- 该仓库 + Issue 已发布过委托：弹窗指引维护者去工作台查看已发布委托。 -->
      <transition name="dup-pop">
        <div
          v-if="duplicateIssue"
          class="dup-modal"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="dup-title"
          @click.self="duplicateIssue = null"
        >
          <div class="dup-card">
            <button class="dup-close" type="button" aria-label="关闭" @click="duplicateIssue = null">×</button>
            <span class="dup-icon" aria-hidden="true">!</span>
            <p class="writ-eyebrow">Already Published</p>
            <h2 id="dup-title">这个 Issue 已发布过委托</h2>
            <p class="dup-reason">
              <strong>{{ duplicateIssue.issueLabel || '该 Issue' }}</strong>
              已关联一个进行中的委托（或该 Issue 已关闭），无法重复发布。可前往工作台查看你已发布的委托。
            </p>
            <div class="dup-actions">
              <button class="quiet-action" type="button" @click="duplicateIssue = null">换一个 Issue</button>
              <button class="primary-action" type="button" @click="goWorkbench">去工作台查看 →</button>
            </div>
          </div>
        </div>
      </transition>
    </section>
  </main>
</template>

<style scoped>
.writ-scene {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: clamp(24px, 6vh, 64px) 18px;
  /* desk.webp 主色兜底：图片加载完成前先铺同色调底，避免背景突兀切换。 */
  background-color: #241710;
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
  color: var(--ink-soft);
  font-size: 0.92rem;
  line-height: 1.55;
  /* 流程说明单行展示，不再按 58ch 折行。 */
  white-space: nowrap;
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

.writ-task-basics {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

.writ-task-basics .writ-wide {
  grid-column: 1 / -1;
}

.writ-field > span {
  color: var(--ink-soft);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
/* 文本类输入框样式不应套到勾选框 / 单选框上：否则复选框会被加上
   padding/border/box-shadow 与聚焦描边，点击聚焦时触发回流，表现为按钮抖动。 */
.writ-field input:not([type='checkbox']):not([type='radio']),
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
.writ-field input:not([type='checkbox']):not([type='radio']):focus,
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
/* 仓库/分类下拉在元数据到达前可见但禁用：轻微脉动，提示"正在载入"而非故障。 */
.writ-field select:disabled {
  color: rgba(91, 53, 29, 0.55);
  cursor: progress;
  animation: writ-loading-pulse 1.3s ease-in-out infinite;
}
@keyframes writ-loading-pulse {
  0%, 100% { background: rgba(255, 244, 210, 0.4); }
  50% { background: rgba(255, 244, 210, 0.66); }
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
  justify-content: flex-end;
  gap: 16px;
  margin-top: 4px;
}
/* 发布按钮固定在右下角；错误提示挤到左侧同一行，窄屏自动换行。 */
.writ-actions .writ-banner {
  order: -1;
  margin-right: auto;
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

/* ── 重复发布弹窗 ───────────────────────────────────────────────────────────── */
.dup-modal {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(20, 11, 4, 0.56);
  backdrop-filter: blur(2px);
}
.dup-card {
  position: relative;
  width: min(440px, 100%);
  padding: 34px 30px 26px;
  text-align: center;
  border: 1px solid rgba(169, 106, 28, 0.5);
  border-radius: 14px;
  background: linear-gradient(180deg, #fffaf0, #f4e7c9);
  box-shadow: 0 24px 60px rgba(20, 11, 4, 0.4);
}
.dup-close {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: rgba(91, 53, 29, 0.7);
  font-size: 1.4rem;
  line-height: 1;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease;
}
.dup-close:hover {
  background: rgba(216, 154, 50, 0.16);
  color: #6a3a14;
}
.dup-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  margin-bottom: 8px;
  border-radius: 50%;
  background: rgba(216, 154, 50, 0.22);
  color: #a9661c;
  font-size: 1.5rem;
  font-weight: 700;
}
.dup-card h2 {
  margin: 8px 0 6px;
  color: var(--ink);
}
.dup-reason {
  margin: 0 auto;
  max-width: 40ch;
  color: var(--ink-soft);
  font-size: 0.92rem;
  line-height: 1.65;
}
.dup-reason strong {
  color: #6a3a14;
}
.dup-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
}
.dup-pop-enter-active,
.dup-pop-leave-active {
  transition: opacity 180ms ease;
}
.dup-pop-enter-active .dup-card,
.dup-pop-leave-active .dup-card {
  transition: transform 180ms cubic-bezier(0.2, 0.9, 0.3, 1.2), opacity 180ms ease;
}
.dup-pop-enter-from,
.dup-pop-leave-to {
  opacity: 0;
}
.dup-pop-enter-from .dup-card,
.dup-pop-leave-to .dup-card {
  transform: translateY(12px) scale(0.96);
  opacity: 0;
}

@media (max-width: 640px) {
  .writ-panel {
    --writ-pad-x: clamp(40px, 12vw, 76px);
    --writ-pad-y: clamp(54px, 14vw, 88px);
  }

  .writ-form {
    grid-template-columns: 1fr;
  }

  .writ-task-basics {
    grid-template-columns: 1fr;
  }

  /* 窄屏放开单行限制，避免横向溢出。 */
  .writ-sub {
    white-space: normal;
  }
}
</style>
