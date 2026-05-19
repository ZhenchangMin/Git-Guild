<script setup>
import { computed, ref } from 'vue'

import { repositorySyncDefaults, repositorySyncExceptions, repositorySyncProjects } from '../data/repositorySync'

const clone = (value) => JSON.parse(JSON.stringify(value))

const repositories = ref(clone(repositorySyncProjects))
const syncExceptions = ref(clone(repositorySyncExceptions))
const selectedRepositoryId = ref(repositories.value[0]?.id ?? null)
const selectedTopicId = ref('start')
const importForm = ref({ ...repositorySyncDefaults })
const importStage = ref('idle')
const showFailureDetail = ref(false)
const notice = ref({
  title: '前台向导待命',
  body: '你可以先问我怎么开始，也可以直接选择仓库接入、任务流程、PR 联动或异常求助。',
  tone: 'info',
})

const guideTopics = [
  {
    id: 'start',
    label: '我该从哪开始',
    title: '先找一个适合自己的任务',
    body: '建议从任务板筛选“新手友好”和熟悉技术栈，再进入任务详情查看完成标准。接取后回到工作台做分支、commit 和 PR。',
    steps: ['打开悬赏任务板浏览任务', '在任务详情确认完成标准', '接取后进入工作台', '完成代码协作后去提交柜台登记成果'],
  },
  {
    id: 'repo',
    label: '仓库接入怎么做',
    title: '仓库接入只是前台服务之一',
    body: 'MVP 采用 GitHub 到 Gitea 的单向同步。这里提供导入和同步状态指引，但不会把前台变成单一仓库控制台。',
    steps: ['填写源仓库与目标 Gitea 组织', '创建镜像仓库', '同步代码、Issue 和 PR 状态', '失败时查看原因并重试'],
  },
  {
    id: 'workflow',
    label: '任务和 PR 怎么关联',
    title: '任务成果提交和项目提交是两件事',
    body: '工作台负责创建分支、上传提交、发起 PR；提交柜台负责把任务成果提交给维护者审核。',
    steps: ['任务绑定 Issue', '工作台创建任务分支', '上传变更生成 commit', '发起 PR 后到提交柜台登记成果'],
  },
  {
    id: 'exception',
    label: '遇到异常怎么办',
    title: '先看原因、影响和下一步',
    body: '导入失败、同步失败、PR 未关联、权限不足等异常，都应该告诉用户为什么失败、影响哪里、下一步做什么。',
    steps: ['确认异常类型', '查看影响范围', '按建议操作重试或提交管理员处理', '处理后观察状态是否恢复'],
  },
]

const selectedGuide = computed(
  () => guideTopics.find((topic) => topic.id === selectedTopicId.value) ?? guideTopics[0],
)
const selectedRepository = computed(
  () => repositories.value.find((repository) => repository.id === selectedRepositoryId.value) ?? repositories.value[0],
)
const syncSummary = computed(() => {
  const repository = selectedRepository.value
  if (!repository) return []
  return repository.pipelines.map((pipeline) => ({
    id: pipeline.id,
    label: pipeline.label,
    status: pipeline.status,
    tone: pipeline.tone,
  }))
})
const pendingExceptionCount = computed(
  () => syncExceptions.value.filter((exception) => exception.status === '待处理').length,
)

function selectTopic(topicId) {
  selectedTopicId.value = topicId
  showFailureDetail.value = false
  const topic = guideTopics.find((item) => item.id === topicId)
  notice.value = {
    title: topic?.title ?? '前台向导已切换',
    body: topic?.body ?? '选择一个主题后，我会用对话气泡说明下一步。',
    tone: topicId === 'exception' ? 'warn' : 'info',
  }
}

function selectRepository(repository) {
  selectedRepositoryId.value = repository.id
  selectedTopicId.value = 'repo'
  showFailureDetail.value = false
  notice.value = {
    title: `${repository.title} 已选中`,
    body: repository.failure
      ? '这个仓库存在同步异常。看板娘会先解释影响，再给出重试入口。'
      : '该仓库同步状态正常，可作为课堂演示的成功路径。',
    tone: repository.failure ? 'warn' : 'success',
  }
}

function pushLog(repository, type, text) {
  repository.logs.unshift({ time: '刚刚', type, text })
  repository.logs = repository.logs.slice(0, 4)
}

function beginImport() {
  importStage.value = 'done'
  selectedTopicId.value = 'repo'

  const importedRepository = {
    id: 'new-import',
    title: importForm.value.targetName || 'new-import',
    source: importForm.value.sourceUrl.replace(/^https?:\/\//, ''),
    target: `gitea.local/${importForm.value.targetOwner}/${importForm.value.targetName}`,
    branch: importForm.value.branch,
    owner: '委托人 · 前台向导',
    importedAt: '刚刚',
    lastSyncedAt: '刚刚',
    nextSyncAt: importForm.value.schedule,
    mode: '单向同步',
    summary: '前台已模拟完成导入，真实后端接入前仅作为课堂演示状态。',
    stats: [
      { label: '代码分支', value: '1' },
      { label: '开放 Issue', value: '6' },
      { label: '开放 PR', value: '2' },
      { label: '最近提交', value: 'demo42f' },
    ],
    pipelines: [
      { id: 'code', label: '代码同步', status: '已同步', tone: 'ok', detail: '代码已模拟镜像到 Gitea。', updatedAt: '刚刚' },
      { id: 'issue', label: 'Issue 同步', status: '已同步', tone: 'ok', detail: 'Issue 已模拟刷新。', updatedAt: '刚刚' },
      { id: 'pr', label: 'PR 状态同步', status: '已同步', tone: 'ok', detail: 'PR 状态已模拟刷新。', updatedAt: '刚刚' },
    ],
    logs: [
      { time: '刚刚', type: 'success', text: '前台已模拟创建 Gitea 镜像仓库。' },
      { time: '刚刚', type: 'success', text: '首次代码、Issue、PR 状态同步完成。' },
    ],
    failure: null,
  }

  const existingIndex = repositories.value.findIndex((repository) => repository.id === importedRepository.id)
  if (existingIndex >= 0) {
    repositories.value.splice(existingIndex, 1, importedRepository)
  } else {
    repositories.value.unshift(importedRepository)
  }
  selectedRepositoryId.value = importedRepository.id
  notice.value = {
    title: '导入演示完成',
    body: '仓库已进入可同步状态。前台仍以引导为主，详细开发操作请进入工作台。',
    tone: 'success',
  }
}

function resyncRepository() {
  const repository = selectedRepository.value
  if (!repository) return

  repository.pipelines = repository.pipelines.map((pipeline) => ({
    ...pipeline,
    status: '已同步',
    tone: 'ok',
    detail: `${pipeline.label} 已通过手动同步恢复。`,
    updatedAt: '刚刚',
  }))
  repository.lastSyncedAt = '刚刚'
  repository.nextSyncAt = '明天 09:00'
  repository.failure = null
  pushLog(repository, 'success', '手动重新同步完成，异常状态已恢复。')
  showFailureDetail.value = false
  notice.value = {
    title: '同步已恢复',
    body: '代码、Issue 和 PR 状态已刷新。若是实际系统，还需要后端记录同步日志。',
    tone: 'success',
  }
}

function showFailure() {
  showFailureDetail.value = true
  selectedTopicId.value = 'exception'
  notice.value = {
    title: '失败详情已展开',
    body: '请先确认原因和影响，再决定重试、补权限或交给管理员处理。',
    tone: 'warn',
  }
}

function resolveSyncException(exception) {
  exception.status = exception.resultStatus
  exception.statusTone = exception.resultTone

  if (exception.type === '仓库导入失败') {
    beginImport()
  } else {
    const failedRepository = repositories.value.find((repository) => repository.id === 'backend-warning')
    if (failedRepository) selectedRepositoryId.value = failedRepository.id
    resyncRepository()
  }

  notice.value = {
    title: `${exception.type}：${exception.status}`,
    body: exception.resultMessage,
    tone: exception.resultTone === 'approved' ? 'success' : 'warn',
  }
}
</script>

<template>
  <div class="frontdesk-guide" aria-label="前台看板娘引导">
    <aside class="frontdesk-topic-rail">
      <p class="kicker">前台向导</p>
      <h1>前台向导</h1>
      <p>这里主要负责指引和答疑，具体开发操作请进入工作台。</p>

      <div class="frontdesk-topic-list">
        <button
          v-for="topic in guideTopics"
          :key="topic.id"
          type="button"
          :class="{ active: selectedTopicId === topic.id }"
          @click="selectTopic(topic.id)"
        >
          {{ topic.label }}
        </button>
      </div>
    </aside>

    <section class="mascot-dialogue" :class="notice.tone" aria-live="polite">
      <span class="dialogue-tail" aria-hidden="true"></span>
      <p class="kicker">看板娘提示</p>
      <h2>{{ notice.title }}</h2>
      <p>{{ notice.body }}</p>
      <ol>
        <li v-for="step in selectedGuide.steps" :key="step">{{ step }}</li>
      </ol>
    </section>

    <aside class="frontdesk-pocket frontdesk-repo-pocket">
      <div class="pocket-head">
        <div>
        <p class="kicker">仓库帮助</p>
          <h2>仓库与同步指引</h2>
        </div>
        <span>{{ selectedRepository?.mode }}</span>
      </div>

      <div class="repository-mini-list">
        <button
          v-for="repository in repositories"
          :key="repository.id"
          type="button"
          :class="{ active: selectedRepository?.id === repository.id, warning: repository.failure }"
          @click="selectRepository(repository)"
        >
          <strong>{{ repository.title }}</strong>
          <small>{{ repository.source }}</small>
        </button>
      </div>

      <div v-if="selectedRepository" class="sync-chip-row">
        <span v-for="item in syncSummary" :key="item.id" :class="item.tone">
          {{ item.label }} · {{ item.status }}
        </span>
      </div>

      <div class="pocket-actions">
        <button class="quiet-action" type="button" @click="resyncRepository">重新同步</button>
        <button class="quiet-action danger" type="button" :disabled="!selectedRepository?.failure" @click="showFailure">
          查看失败原因
        </button>
      </div>
    </aside>

    <aside class="frontdesk-pocket import-bubble" :class="{ open: selectedTopicId === 'repo' }">
      <div class="pocket-head">
        <div>
          <p class="kicker">Import Bubble</p>
          <h2>需要接入仓库？</h2>
        </div>
        <span>{{ importStage === 'done' ? '已演示' : '可选' }}</span>
      </div>

      <form class="mini-import-form" @submit.prevent="beginImport">
        <label>
          源仓库
          <input v-model="importForm.sourceUrl" type="url" />
        </label>
        <label>
          Gitea 组织
          <input v-model="importForm.targetOwner" />
        </label>
        <label>
          仓库名
          <input v-model="importForm.targetName" />
        </label>
        <button class="primary-action" type="submit">模拟导入</button>
      </form>
    </aside>

    <aside class="frontdesk-pocket exception-bubble" :class="{ open: selectedTopicId === 'exception' }">
      <div class="pocket-head">
        <div>
          <p class="kicker">Exception Help</p>
          <h2>异常求助</h2>
        </div>
        <span>{{ pendingExceptionCount }} 待处理</span>
      </div>

      <div class="exception-mini-list">
        <article v-for="exception in syncExceptions" :key="exception.id" :class="exception.statusTone">
          <div>
            <strong>{{ exception.type }}</strong>
            <em>{{ exception.status }}</em>
          </div>
          <p>{{ exception.reason }}</p>
          <button
            class="quiet-action"
            type="button"
            :disabled="exception.status !== '待处理'"
            @click="resolveSyncException(exception)"
          >
            {{ exception.status === '待处理' ? exception.actionLabel : '处理完成' }}
          </button>
        </article>
      </div>
    </aside>

    <section v-if="showFailureDetail && selectedRepository?.failure" class="frontdesk-failure-popover">
      <button type="button" aria-label="关闭失败详情" @click="showFailureDetail = false">×</button>
      <p class="kicker">Failure Detail</p>
      <h2>{{ selectedRepository.failure.title }}</h2>
      <dl>
        <div>
          <dt>原因</dt>
          <dd>{{ selectedRepository.failure.reason }}</dd>
        </div>
        <div>
          <dt>影响</dt>
          <dd>{{ selectedRepository.failure.impact }}</dd>
        </div>
      </dl>
      <ul>
        <li v-for="item in selectedRepository.failure.recovery" :key="item">{{ item }}</li>
      </ul>
    </section>
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

.frontdesk-topic-rail,
.frontdesk-pocket,
.mascot-dialogue,
.frontdesk-failure-popover {
  border: 1px solid rgba(238, 184, 91, 0.56);
  border-radius: var(--radius);
  box-shadow: 0 20px 54px rgba(0, 0, 0, 0.38), inset 0 1px 0 rgba(255, 232, 176, 0.16);
  backdrop-filter: blur(7px);
  background:
    linear-gradient(180deg, rgba(31, 17, 9, 0.82), rgba(15, 8, 4, 0.74)),
    radial-gradient(circle at 10% 0%, rgba(216, 154, 50, 0.18), transparent 38%);
}

.frontdesk-topic-rail {
  position: absolute;
  left: 0;
  top: 0;
  width: min(300px, 28vw);
  padding: 18px;
}

.frontdesk-topic-rail h1 {
  margin: 4px 0 8px;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: clamp(1.8rem, 3vw, 2.8rem);
}

.frontdesk-topic-rail p,
.mascot-dialogue p,
.frontdesk-pocket p,
.frontdesk-failure-popover p,
.frontdesk-failure-popover li {
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.5;
}

.frontdesk-topic-list {
  display: grid;
  gap: 9px;
  margin-top: 16px;
}

.frontdesk-topic-list button,
.repository-mini-list button {
  border: 1px solid rgba(238, 184, 91, 0.28);
  border-radius: 999px;
  padding: 9px 12px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(13, 8, 4, 0.46);
  cursor: pointer;
  transition: border-color 160ms ease, background 160ms ease, transform 160ms ease;
}

.frontdesk-topic-list button:hover,
.frontdesk-topic-list button:focus-visible,
.frontdesk-topic-list button.active,
.repository-mini-list button:hover,
.repository-mini-list button:focus-visible,
.repository-mini-list button.active {
  border-color: var(--gold-bright);
  background: rgba(63, 34, 13, 0.68);
  transform: translateY(-1px);
}

.mascot-dialogue {
  position: absolute;
  right: 6%;
  top: 4%;
  width: min(540px, 42vw);
  padding: 20px 22px;
  border-radius: 26px 26px 26px 8px;
}

.dialogue-tail {
  position: absolute;
  right: 100%;
  bottom: 34px;
  width: 34px;
  height: 24px;
  clip-path: polygon(100% 0, 0 55%, 100% 100%);
  background: rgba(31, 17, 9, 0.82);
}

.mascot-dialogue h2,
.frontdesk-pocket h2,
.frontdesk-failure-popover h2 {
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
  border-color: rgba(238, 184, 91, 0.78);
}

.frontdesk-pocket {
  position: absolute;
  display: grid;
  gap: 12px;
  padding: 16px;
}

.frontdesk-repo-pocket {
  left: 0;
  bottom: 0;
  width: min(360px, 32vw);
}

.import-bubble {
  right: 0;
  bottom: 0;
  width: min(360px, 31vw);
}

.exception-bubble {
  right: 0;
  top: 42%;
  width: min(380px, 32vw);
  max-height: 38vh;
  overflow: auto;
}

.pocket-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.pocket-head > span,
.sync-chip-row span,
.exception-mini-list em {
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 3px 8px;
  color: #ffe4ad;
  font-size: 0.72rem;
  white-space: nowrap;
  background: rgba(80, 43, 18, 0.44);
}

.repository-mini-list,
.sync-chip-row,
.exception-mini-list,
.mini-import-form {
  display: grid;
  gap: 9px;
}

.repository-mini-list button {
  display: grid;
  border-radius: 8px;
}

.repository-mini-list small {
  margin-top: 3px;
  color: rgba(255, 231, 183, 0.62);
}

.repository-mini-list button.warning {
  border-color: rgba(204, 95, 65, 0.58);
}

.sync-chip-row {
  grid-template-columns: 1fr;
}

.sync-chip-row span.ok {
  border-color: rgba(129, 184, 98, 0.68);
  color: #dcf4c2;
}

.sync-chip-row span.warn,
.sync-chip-row span.working {
  border-color: rgba(238, 184, 91, 0.72);
  color: #fff0c5;
}

.sync-chip-row span.danger {
  border-color: rgba(238, 120, 82, 0.72);
  color: #ffd7c9;
}

.pocket-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mini-import-form label {
  display: grid;
  gap: 5px;
  color: rgba(255, 231, 183, 0.72);
  font-size: 0.8rem;
}

.mini-import-form input {
  width: 100%;
  min-height: 34px;
  border: 1px solid rgba(224, 163, 72, 0.44);
  border-radius: 6px;
  padding: 0 10px;
  color: #ffe9bb;
  background: rgba(8, 5, 3, 0.48);
}

.exception-mini-list article {
  display: grid;
  gap: 8px;
  border: 1px solid rgba(238, 184, 91, 0.28);
  border-radius: 8px;
  padding: 11px;
  background: rgba(7, 4, 2, 0.28);
}

.exception-mini-list article.danger {
  border-color: rgba(238, 120, 82, 0.72);
}

.exception-mini-list article.return {
  border-color: rgba(238, 184, 91, 0.64);
}

.exception-mini-list article.approved {
  border-color: rgba(129, 184, 98, 0.68);
}

.exception-mini-list article > div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.exception-mini-list strong {
  color: #ffe8b9;
}

.exception-mini-list p {
  margin: 0;
  font-size: 0.82rem;
}

.frontdesk-failure-popover {
  position: absolute;
  right: min(400px, 35vw);
  bottom: 0;
  width: min(420px, 34vw);
  padding: 16px;
}

.frontdesk-failure-popover > button {
  position: absolute;
  top: 10px;
  right: 12px;
  border: 0;
  color: #ffe8b9;
  font-size: 1.2rem;
  background: transparent;
  cursor: pointer;
}

.frontdesk-failure-popover dl {
  display: grid;
  gap: 8px;
  margin: 12px 0;
}

.frontdesk-failure-popover dt {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.76rem;
}

.frontdesk-failure-popover dd {
  margin: 2px 0 0;
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.42;
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

  .frontdesk-topic-rail,
  .mascot-dialogue,
  .frontdesk-pocket,
  .frontdesk-failure-popover {
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
</style>
