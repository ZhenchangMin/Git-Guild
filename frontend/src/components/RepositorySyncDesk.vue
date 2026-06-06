<script setup>
import { computed, onMounted, ref } from 'vue'

import { questApi } from '../api/questApi'
import { repositoryApi } from '../api/repositoryApi'

const repositoryForm = ref({
  sourceUrl: '',
  name: 'gitguild-demo',
  hostType: 'GITEA',
})

const publishForm = ref({
  repositoryId: '',
  issueId: '',
  title: 'P4-029 demo issue',
  description: 'Add a visible demo change for end-to-end integration.',
  completionCriteria: 'PR 已创建并通过维护者审核；说明中包含变更摘要和验证结果。',
  difficulty: 'B',
  techStack: 'Vue,Spring Boot,Gitea',
  estimatedHours: 2,
  rewardXp: 80,
  categoryId: '',
  tagIds: '',
})

const categories = ref([])
const tags = ref([])
const loading = ref(false)
const taxonomyLoading = ref(false)
const issueLoading = ref(false)
const stage = ref('ready')
const lastQuest = ref(null)
const lastRepository = ref(null)
const repositoryIssues = ref([])
const errorMessage = ref('')
const isPublishOpen = ref(false)

const notice = computed(() => {
  if (errorMessage.value) {
    return {
      tone: 'warn',
      title: '这份委托还不能送出',
      body: errorMessage.value,
      steps: ['确认已选择委托人身份', '检查仓库和 Issue 信息是否已准备好', '修正表单后再次提交'],
    }
  }

  if (stage.value === 'published') {
    return {
      tone: 'success',
      title: '委托已送达审核台',
      body: `委托 #${lastQuest.value?.questId ?? ''} 已提交给管理员审核。审核通过后，冒险家就能在任务板看到并接取这份委托。`,
      steps: ['等待管理员审核', '审核通过后任务会上架', '回到任务板确认委托可见'],
    }
  }

  return {
    tone: 'info',
    title: '需要发布新的委托吗？',
    body: '打开右下角发布单，先关联 Gitea 仓库，再选择 Issue，最后填写委托说明并送交管理员审核。',
    steps: ['关联仓库', '绑定 Issue', '填写委托说明与完成标准'],
  }
})

const categoryOptions = computed(() => categories.value.filter((category) => category.enabled !== false))
const tagOptions = computed(() => tags.value.filter((tag) => tag.enabled !== false))
const issueOptions = computed(() => repositoryIssues.value.filter((issue) => issue.status !== 'CLOSED'))

onMounted(() => {
  loadTaxonomy()
})

async function loadTaxonomy() {
  taxonomyLoading.value = true
  try {
    const [categoryResponse, tagResponse] = await Promise.all([
      questApi.categories(),
      questApi.tags({ size: 50 }),
    ])
    categories.value = categoryResponse?.data ?? []
    tags.value = tagResponse?.data?.items ?? []
    if (!publishForm.value.categoryId && categories.value[0]?.categoryId) {
      publishForm.value.categoryId = String(categories.value[0].categoryId)
    }
  } catch (error) {
    errorMessage.value = readableError(error, '分类和标签读取失败，可先手动填写 categoryId。')
  } finally {
    taxonomyLoading.value = false
  }
}

async function importRepository() {
  errorMessage.value = ''
  loading.value = true
  try {
    const response = await repositoryApi.importRepository({
      sourceUrl: repositoryForm.value.sourceUrl.trim(),
      name: repositoryForm.value.name.trim(),
      hostType: repositoryForm.value.hostType,
    })
    const repository = response?.data
    lastRepository.value = repository
    publishForm.value.repositoryId = String(repository.repositoryId)
    await repositoryApi.sync(repository.repositoryId)
    await loadIssues()
  } catch (error) {
    errorMessage.value = readableError(error, '仓库迁移失败，请确认地址正确且仓库为公开。')
  } finally {
    loading.value = false
  }
}

async function loadIssues() {
  if (!publishForm.value.repositoryId) {
    repositoryIssues.value = []
    return
  }

  issueLoading.value = true
  try {
    const response = await repositoryApi.issues(publishForm.value.repositoryId, { size: 50 })
    repositoryIssues.value = response?.data?.items ?? []
    if (!publishForm.value.issueId && repositoryIssues.value[0]?.issueId) {
      publishForm.value.issueId = String(repositoryIssues.value[0].issueId)
    }
  } catch (error) {
    errorMessage.value = readableError(error, 'Issue 列表读取失败，可先手动填写 issueId。')
  } finally {
    issueLoading.value = false
  }
}

async function publishQuest() {
  errorMessage.value = ''
  loading.value = true
  try {
    const payload = {
      repositoryId: toNumber(publishForm.value.repositoryId, 'repositoryId'),
      issueId: toNumber(publishForm.value.issueId, 'issueId'),
      title: publishForm.value.title.trim(),
      description: publishForm.value.description.trim(),
      completionCriteria: publishForm.value.completionCriteria.trim(),
      difficulty: publishForm.value.difficulty,
      techStack: splitCsv(publishForm.value.techStack),
      estimatedHours: toNumber(publishForm.value.estimatedHours, 'estimatedHours'),
      rewardXp: toNumber(publishForm.value.rewardXp, 'rewardXp'),
      categoryId: toNumber(publishForm.value.categoryId, 'categoryId'),
      tagIds: splitCsv(publishForm.value.tagIds).map((value) => toNumber(value, 'tagId')),
    }

    const createResponse = await questApi.create(payload)
    const quest = createResponse?.data
    lastQuest.value = quest
    await questApi.submitForReview(quest.questId)
    stage.value = 'published'
    isPublishOpen.value = false
  } catch (error) {
    errorMessage.value = readableError(error, '委托发布失败。')
  } finally {
    loading.value = false
  }
}

function splitCsv(value) {
  return String(value ?? '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function toNumber(value, field) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${field} 必须是大于 0 的数字。`)
  }
  return parsed
}

function readableError(error, fallback) {
  if (error?.details) return `${fallback} ${error.details}`
  if (error?.message) return `${fallback} ${error.message}`
  return fallback
}
</script>

<template>
  <div class="frontdesk-guide" aria-label="前台发布委托向导">
    <section class="mascot-dialogue" :class="notice.tone" aria-live="polite">
      <span class="dialogue-tail" aria-hidden="true"></span>
      <p class="kicker">看板娘提示</p>
      <h2>{{ notice.title }}</h2>
      <p>{{ notice.body }}</p>
      <ol>
        <li v-for="step in notice.steps" :key="step">{{ step }}</li>
      </ol>
    </section>

    <button
      class="publish-fab"
      type="button"
      :aria-expanded="isPublishOpen"
      aria-controls="publish-quest-panel"
      @click="isPublishOpen = !isPublishOpen"
    >
      {{ isPublishOpen ? '收起发布单' : '发布委托' }}
    </button>

    <form
      v-if="isPublishOpen"
      id="publish-quest-panel"
      class="frontdesk-pocket publish-bubble"
      @submit.prevent="publishQuest"
    >
      <div class="pocket-head">
        <div>
          <p class="kicker">发布单</p>
          <h2>发布委托</h2>
        </div>
        <button class="panel-close" type="button" aria-label="收起发布单" @click="isPublishOpen = false">
          关闭
        </button>
      </div>

      <section class="form-section">
        <div class="section-head">
          <strong>关联仓库</strong>
          <span>{{ lastRepository ? `已关联 ID ${lastRepository.repositoryId}` : '先登记仓库' }}</span>
        </div>
        <label>
          仓库地址（GitHub / Gitea 公网均可）
          <input
            v-model="repositoryForm.sourceUrl"
            type="url"
            placeholder="https://gitea.com/owner/repo.git"
          />
          <small class="field-hint">粘贴公网仓库地址，平台会自动把它迁入并接管，无需手动搬运。</small>
        </label>
        <div class="form-grid compact">
          <label>
            仓库名称
            <input v-model="repositoryForm.name" placeholder="gitguild-demo" />
          </label>
          <label>
            业务库仓库编号
            <input v-model="publishForm.repositoryId" inputmode="numeric" placeholder="导入后自动填写" required />
          </label>
        </div>
        <button class="quiet-action section-action" type="button" :disabled="loading" @click="importRepository">
          {{ loading ? '正在迁移仓库…' : '导入仓库并使用' }}
        </button>
      </section>

      <section class="form-section">
        <div class="section-head">
          <strong>绑定 Issue</strong>
          <span>{{ issueOptions.length ? `可选 ${issueOptions.length} 条` : '来自同一仓库' }}</span>
        </div>
        <label>
          业务库 Issue 编号
          <select v-if="issueOptions.length" v-model="publishForm.issueId" required>
            <option v-for="issue in issueOptions" :key="issue.issueId" :value="String(issue.issueId)">
              {{ issue.issueId }} · {{ issue.title || issue.externalIssueId }}
            </option>
          </select>
          <input v-else v-model="publishForm.issueId" inputmode="numeric" placeholder="issues 表中的 issue_id" required />
        </label>
        <button class="quiet-action section-action" type="button" :disabled="issueLoading || !publishForm.repositoryId" @click="loadIssues">
          {{ issueLoading ? '读取中' : '读取 Issue 列表' }}
        </button>
        <p class="section-note">这里使用业务库 Issue 编号；Gitea 页面上的 #1 不是同一个值。</p>
      </section>

      <label>
        委托标题
        <input v-model="publishForm.title" required maxlength="200" />
      </label>
      <label>
        背景说明
        <textarea v-model="publishForm.description" rows="3" required></textarea>
      </label>
      <label>
        完成标准
        <textarea v-model="publishForm.completionCriteria" rows="3" required></textarea>
      </label>

      <div class="form-grid">
        <label>
          难度
          <select v-model="publishForm.difficulty">
            <option value="A">A</option>
            <option value="B">B</option>
            <option value="C">C</option>
            <option value="D">D</option>
          </select>
        </label>
        <label>
          预计小时
          <input v-model="publishForm.estimatedHours" type="number" min="1" required />
        </label>
        <label>
          奖励 XP
          <input v-model="publishForm.rewardXp" type="number" min="1" required />
        </label>
      </div>

      <label>
        技术栈
        <input v-model="publishForm.techStack" placeholder="Vue,Spring Boot,Gitea" required />
      </label>

      <div class="form-grid compact">
        <label>
          分类
          <select v-if="categoryOptions.length" v-model="publishForm.categoryId">
            <option v-for="category in categoryOptions" :key="category.categoryId" :value="String(category.categoryId)">
              {{ category.name }}
            </option>
          </select>
          <input v-else v-model="publishForm.categoryId" inputmode="numeric" placeholder="categoryId" required />
        </label>
        <label>
          标签
          <input v-model="publishForm.tagIds" inputmode="numeric" placeholder="可留空；多个用逗号分隔" />
        </label>
      </div>

      <p class="form-note">
        发布后会先进入管理员审核；审核通过后才会出现在任务板。
      </p>
      <p v-if="taxonomyLoading" class="form-note">正在读取分类和标签...</p>
      <div v-if="tagOptions.length" class="tag-hints" aria-label="可用标签">
        <span v-for="tag in tagOptions.slice(0, 6)" :key="tag.tagId">{{ tag.tagId }} · {{ tag.name }}</span>
      </div>

      <button class="primary-action" type="submit" :disabled="loading">
        {{ loading ? '处理中' : '发布并提交审核' }}
      </button>
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

.publish-bubble {
  right: 28px;
  bottom: 82px;
  width: min(520px, 43vw);
  max-height: min(70vh, 690px);
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
.section-note {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.74rem;
}

.field-hint {
  display: block;
  margin-top: 4px;
  color: rgba(255, 231, 183, 0.55);
  font-size: 0.72rem;
  line-height: 1.4;
}

.panel-close {
  min-width: 52px;
  min-height: 32px;
  border: 1px solid rgba(238, 184, 91, 0.36);
  border-radius: 6px;
  color: #ffe8b9;
  background: rgba(8, 5, 3, 0.46);
  cursor: pointer;
}

.panel-close:hover,
.panel-close:focus-visible {
  border-color: var(--gold-bright);
  outline: none;
}

.frontdesk-pocket label {
  display: grid;
  gap: 5px;
  font-size: 0.8rem;
}

.frontdesk-pocket input,
.frontdesk-pocket select,
.frontdesk-pocket textarea {
  width: 100%;
  border: 1px solid rgba(224, 163, 72, 0.44);
  border-radius: 6px;
  padding: 9px 10px;
  color: #ffe9bb;
  background: rgba(8, 5, 3, 0.54);
}

.frontdesk-pocket textarea {
  resize: vertical;
}

.frontdesk-pocket input:focus,
.frontdesk-pocket select:focus,
.frontdesk-pocket textarea:focus {
  outline: 2px solid rgba(255, 203, 119, 0.34);
  border-color: var(--gold-bright);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.form-grid.compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

.section-note,
.form-note {
  margin: 0;
}

.form-note {
  color: rgba(255, 231, 183, 0.66);
  font-size: 0.78rem;
}

.tag-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-hints span {
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 3px 8px;
  color: #ffe4ad;
  font-size: 0.72rem;
  white-space: nowrap;
  background: rgba(80, 43, 18, 0.44);
}

.primary-action {
  min-height: 38px;
}

.primary-action:disabled {
  cursor: wait;
  opacity: 0.68;
}

.publish-fab {
  position: absolute;
  right: 28px;
  bottom: 28px;
  min-width: 220px;
  min-height: 66px;
  border: 1px solid rgba(255, 221, 151, 0.72);
  border-radius: 18px;
  color: #351b08;
  font-family: var(--font-display);
  font-size: 1.32rem;
  font-weight: 800;
  letter-spacing: 0;
  background:
    linear-gradient(180deg, #ffe7b4, #d99639),
    radial-gradient(circle at 18% 0%, rgba(255, 255, 255, 0.48), transparent 36%);
  box-shadow:
    0 18px 44px rgba(0, 0, 0, 0.44),
    0 0 0 5px rgba(67, 35, 11, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.58);
  cursor: pointer;
  transition: transform 160ms ease, filter 160ms ease;
}

.publish-fab:hover,
.publish-fab:focus-visible {
  transform: translateY(-1px);
  filter: brightness(1.05);
  outline: none;
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

  .publish-fab {
    position: relative;
    right: auto;
    bottom: auto;
    justify-self: end;
  }

  .dialogue-tail {
    display: none;
  }
}

@media (max-width: 640px) {
  .form-grid,
  .form-grid.compact {
    grid-template-columns: 1fr;
  }
}
</style>
