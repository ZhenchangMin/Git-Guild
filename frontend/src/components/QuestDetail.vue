<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { questApi } from '../api/questApi'
import QuestActionRail from './QuestActionRail.vue'
import QuestJourney from './QuestJourney.vue'
import { sessionStore } from '../stores/sessionStore'

const router = useRouter()
const route = useRoute()

const props = defineProps({
  quest: {
    type: Object,
    required: true,
  },
  intent: {
    type: String,
    default: 'view',
  },
})

const emit = defineEmits(['open-workbench', 'open-submission'])

const localWorkflowState = ref('available')
const inlineNotice = ref('')
const isAssigning = ref(false)
const showAcceptConfirm = ref(false)
const hasReadDetails = ref(false)
const isSealing = ref(false)

const defaultSubmissionRequirements = [
  '关联任务编号和任务标题。',
  '关联 PR 链接。',
  '成果说明，说明本次修改解决了什么问题。',
  '完成标准逐项自检。',
  '测试说明或运行截图；MVP 中附件可以为空。',
]

// Map each workflow status label to a seal colour, matching the quest board so
// the same state reads identically across the board and the detail scroll.
const STATUS_TONE = {
  可接取: 'open',
  进行中: 'active',
  待提交成果: 'ready',
  等待维护者审核: 'review',
  需要修改: 'returned',
  已完成: 'done',
}

const repository = computed(
  () =>
    props.quest.repository ?? {
      name: '未关联仓库',
      branch: '未提供',
      syncStatus: '未知',
    },
)
const issue = computed(
  () =>
    props.quest.issue ?? {
      number: '未关联',
      title: props.quest.title,
      status: '未知',
    },
)
const pullRequest = computed(
  () =>
    props.quest.pr ?? {
      number: 'Not created',
      status: 'Not started',
    },
)
const beginnerTags = computed(() => (props.quest.tags ?? []).filter((tag) => tag.includes('新手') || tag === '教程'))
const estimatedHours = computed(() => props.quest.estimatedHours ?? null)
const estimatedHoursLabel = computed(() => (estimatedHours.value === null ? '未提供' : `${estimatedHours.value}h`))
const description = computed(() => props.quest.description ?? props.quest.summary)
const submissionRequirements = computed(() => props.quest.submissionRequirements ?? defaultSubmissionRequirements)
const hasPullRequest = computed(() => pullRequest.value.number !== 'Not created')
const isAcceptIntent = computed(() => props.intent === 'accept' && localWorkflowState.value === 'available')

// Compact, icon-backed meta chips replace the old 6-column grid.
const metaChips = computed(() => [
  { key: 'difficulty', label: '难度', value: props.quest.difficulty },
  { key: 'reward', label: '奖励', value: props.quest.reward },
  { key: 'hours', label: '预计时长', value: estimatedHoursLabel.value },
  { key: 'stack', label: '技术栈', value: props.quest.stack },
  { key: 'beginner', label: '新手标签', value: beginnerTags.value.length ? beginnerTags.value.join(' / ') : '普通任务' },
])

// Single most-relevant exception hint surfaced on the action rail.
const exceptionHint = computed(() => {
  if (localWorkflowState.value === 'changes-requested') {
    return { title: '提交被退回', body: '先阅读逐项反馈，在工作台更新分支和 PR，再回提交柜台重新提交。' }
  }
  if (String(repository.value.syncStatus ?? '').toLowerCase().includes('warning')) {
    return { title: '同步状态滞后', body: '先查看仓库同步日志或手动同步，再判断 Issue、PR 和审核状态。' }
  }
  if (!hasPullRequest.value && ['in-progress', 'pr-ready'].includes(localWorkflowState.value)) {
    return { title: 'PR 未关联', body: '先在工作台发起 PR，再到提交柜台粘贴或选择对应 PR。' }
  }
  return null
})

const workflowConfig = computed(() => {
  const configs = {
    available: {
      status: '可接取',
      next: '确认完成标准后接取任务。',
      primary: isAssigning.value ? '正在接取...' : '接取委托',
      secondary: '查看仓库',
    },
    'in-progress': {
      status: '进行中',
      next: '进入工作台创建分支、上传提交、发起 PR。',
      primary: '进入工作台',
      secondary: '提交成果',
    },
    'pr-ready': {
      status: '待提交成果',
      next: '到提交柜台填写成果说明并提交审核。',
      primary: '提交成果',
      secondary: '查看 PR',
    },
    'in-review': {
      status: '等待维护者审核',
      next: '等待维护者审核。',
      primary: '查看 PR',
      secondary: '查看提交记录',
    },
    'changes-requested': {
      status: '需要修改',
      next: '根据反馈修改后重新提交。',
      primary: '查看反馈',
      secondary: '进入工作台更新 PR',
    },
    completed: {
      status: '已完成',
      next: '查看 XP 和贡献记录。',
      primary: '查看成长记录',
      secondary: '查看贡献记录',
    },
  }

  return configs[localWorkflowState.value] ?? configs.available
})

const heroTone = computed(() => STATUS_TONE[workflowConfig.value.status] ?? 'open')

watch(
  () => [props.quest.id, props.intent],
  () => {
    localWorkflowState.value = props.quest.workflowState ?? (props.quest.status === '待审核' ? 'in-review' : 'available')
    inlineNotice.value =
      props.intent === 'accept' && localWorkflowState.value === 'available'
        ? '请确认任务背景和完成标准后接取该任务。点击任务板的“接取”不会直接完成接取。'
        : ''
  },
  { immediate: true },
)

async function handlePrimaryAction() {
  if (localWorkflowState.value === 'available') {
    // Guests can browse the commission scroll but must register before
    // claiming a quest — bounce them to the gate with a redirect back here.
    if (sessionStore.role === 'VISITOR') {
      router.push({ name: 'login', query: { redirect: route.fullPath } })
      return
    }

    if (isAssigning.value) return
    hasReadDetails.value = false
    showAcceptConfirm.value = true
    return
  }

  if (localWorkflowState.value === 'pr-ready') {
    inlineNotice.value = '将进入提交柜台登记该任务成果。'
    emit('open-submission', props.quest.id)
    return
  }

  if (localWorkflowState.value === 'changes-requested') {
    inlineNotice.value = '请到工作台查看维护者反馈，并根据反馈更新分支、PR 和提交材料。'
    return
  }

  if (localWorkflowState.value === 'completed') {
    inlineNotice.value = '该任务已完成，可到个人档案查看 XP 和贡献记录。'
    return
  }

  emit('open-workbench')
}

function cancelAcceptConfirm() {
  if (isAssigning.value) return
  showAcceptConfirm.value = false
}

async function confirmAccept() {
  if (!hasReadDetails.value || isAssigning.value) return
  isAssigning.value = true
  isSealing.value = true
  inlineNotice.value = ''

  try {
    await questApi.assign(props.quest.questId ?? props.quest.id)
    localWorkflowState.value = 'in-progress'
    inlineNotice.value = '已接取该任务，下一步请进入工作台创建任务分支。任务已加入你的工作台待办。'
    showAcceptConfirm.value = false
  } catch (error) {
    inlineNotice.value = error?.message || '接取失败，请刷新任务详情后重试。'
    showAcceptConfirm.value = false
  } finally {
    isAssigning.value = false
    isSealing.value = false
  }
}

// “查看仓库”跳转到平台 Gitea 仓库页（新标签）；缺地址时回退到工作台。
function viewRepository() {
  const url = repository.value.webUrl
  if (url) {
    window.open(url, '_blank', 'noopener')
    return
  }
  emit('open-workbench')
}

function handleSecondaryAction() {
  if (localWorkflowState.value === 'available') {
    viewRepository()
    return
  }

  if (localWorkflowState.value === 'in-progress') {
    inlineNotice.value = '将进入提交柜台登记该任务成果。'
    emit('open-submission', props.quest.id)
    return
  }

  if (localWorkflowState.value === 'completed') {
    inlineNotice.value = '可到个人档案查看任务标题、仓库、合并 PR 和完成时间。'
    return
  }

  emit('open-workbench')
}

function showIssueHint() {
  inlineNotice.value = `已定位 Issue ${issue.value.number}：${issue.value.title}。可进入工作台查看仓库与 Issue 上下文。`
}
</script>

<template>
  <div class="quest-detail-workspace" aria-label="任务详情">
    <header class="quest-detail-hero">
      <p class="kicker">悬赏任务详情</p>
      <div class="hero-title-row">
        <h1>{{ quest.code ?? quest.id }} · {{ quest.title }}</h1>
        <span class="hero-state" :class="`tone-${heroTone}`">
          {{ workflowConfig.status }}
        </span>
      </div>
      <p class="hero-desc">{{ description }}</p>

      <ul class="meta-chips">
        <li v-for="chip in metaChips" :key="chip.key" :class="`chip-${chip.key}`">
          <svg viewBox="0 0 24 24" aria-hidden="true" class="chip-icon">
            <template v-if="chip.key === 'difficulty'">
              <path d="M12 3 4 7v6c0 4 3.5 7 8 8 4.5-1 8-4 8-8V7Z" />
            </template>
            <template v-else-if="chip.key === 'reward'">
              <circle cx="12" cy="12" r="8" />
              <path d="M12 8v8M9 10.5h6M9 13.5h6" />
            </template>
            <template v-else-if="chip.key === 'hours'">
              <circle cx="12" cy="12" r="8" />
              <path d="M12 8v4l3 2" />
            </template>
            <template v-else-if="chip.key === 'stack'">
              <path d="M12 4 3 9l9 5 9-5Z" />
              <path d="M3 14l9 5 9-5" />
            </template>
            <template v-else>
              <path d="M12 4l2.2 4.6 5 .7-3.6 3.5.9 5-4.5-2.4L7.5 17.8l.9-5L4.8 9.3l5-.7Z" />
            </template>
          </svg>
          <span class="chip-label">{{ chip.label }}</span>
          <span class="chip-value">{{ chip.value }}</span>
        </li>
      </ul>

      <p class="workflow-boundary">
        项目操作在工作台完成：创建分支、上传文件生成 commit、发起 PR。任务成果提交在提交柜台完成：关联 PR、填写成果说明、完成标准自检、提交审核。
      </p>
    </header>

    <div class="quest-detail-grid">
      <main class="quest-main-column">
        <section class="quest-detail-card">
          <p class="kicker">任务背景</p>
          <h2>任务背景</h2>
          <p>{{ description }}</p>
          <dl class="source-summary">
            <div>
              <dt>来源仓库</dt>
              <dd>{{ repository.name }}</dd>
            </div>
            <div>
              <dt>来源 Issue</dt>
              <dd>{{ issue.number }} · {{ issue.title }}</dd>
            </div>
          </dl>
        </section>

        <section class="quest-detail-card">
          <p class="kicker">验收清单</p>
          <h2>完成标准</h2>
          <p class="section-note">提交前请逐项确认。这里检查任务完成结果，不替代工作台中的 commit 和 PR 操作。</p>
          <div class="criteria-list">
            <label v-for="line in quest.criteria" :key="line">
              <input type="checkbox" disabled />
              <span>{{ line }}</span>
            </label>
          </div>
        </section>

        <QuestJourney
          :quest="quest"
          :workflow-state="localWorkflowState"
          :repository="repository"
          :pull-request="pullRequest"
        />
      </main>

      <aside class="quest-side-column">
        <QuestActionRail
          :status="localWorkflowState"
          :config="workflowConfig"
          :notice="inlineNotice"
          :accept-intent="isAcceptIntent"
          :exception-hint="exceptionHint"
          @primary="handlePrimaryAction"
          @secondary="handleSecondaryAction"
        />

        <section class="quest-detail-card side-card">
          <p class="kicker">仓库与 Issue</p>
          <h2>关联仓库与 Issue</h2>
          <dl>
            <div>
              <dt>仓库</dt>
              <dd>{{ repository.name }}</dd>
            </div>
            <div>
              <dt>默认分支</dt>
              <dd>{{ repository.branch }}</dd>
            </div>
            <div>
              <dt>Issue</dt>
              <dd>{{ issue.number }} · {{ issue.title }}</dd>
            </div>
            <div>
              <dt>Issue 状态</dt>
              <dd>{{ issue.status }}</dd>
            </div>
            <div>
              <dt>仓库同步</dt>
              <dd>{{ repository.syncStatus }}</dd>
            </div>
            <div>
              <dt>PR</dt>
              <dd>{{ hasPullRequest ? `${pullRequest.number} · ${pullRequest.status}` : '尚未创建 PR' }}</dd>
            </div>
          </dl>
          <div class="side-actions">
            <button class="primary-action" type="button" @click="viewRepository">查看仓库</button>
            <button class="quiet-action" type="button" @click="showIssueHint">查看 Issue</button>
          </div>
        </section>

        <section class="quest-detail-card side-card">
          <p class="kicker">提交柜台</p>
          <h2>提交要求</h2>
          <ul class="submission-requirements">
            <li v-for="item in submissionRequirements" :key="item">{{ item }}</li>
          </ul>
          <p class="counter-boundary">提交柜台只登记任务成果和审核材料，不负责创建 commit 或发起 PR。</p>
        </section>
      </aside>
    </div>

    <Teleport to="body">
      <Transition name="contract-fade">
        <div v-if="showAcceptConfirm" class="accept-overlay" @click.self="cancelAcceptConfirm">
          <Transition name="contract-rise" appear>
            <div
              class="accept-dialog"
              role="alertdialog"
              aria-modal="true"
              aria-labelledby="accept-dialog-title"
              :class="{ 'is-sealing': isSealing }"
            >
              <span class="accept-dialog-seal" aria-hidden="true"></span>
              <p class="kicker">接取契约 · 二次确认</p>
              <h2 id="accept-dialog-title">{{ quest.code ?? quest.id }} · {{ quest.title }}</h2>
              <p class="accept-dialog-body">
                接取后任务将记入你的工作台待办，请确认已仔细阅读任务背景、完成标准与提交要求。
              </p>

              <label class="accept-dialog-check">
                <input type="checkbox" v-model="hasReadDetails" :disabled="isAssigning" />
                <span>我已阅读任务背景与完成标准，确认接取该任务。</span>
              </label>

              <div class="accept-dialog-actions">
                <button type="button" class="quiet-action" :disabled="isAssigning" @click="cancelAcceptConfirm">
                  取消
                </button>
                <button
                  type="button"
                  class="primary-action accept-confirm-btn"
                  :disabled="!hasReadDetails || isAssigning"
                  @click="confirmAccept"
                >
                  {{ isAssigning ? '正在盖印…' : '确认接取' }}
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.quest-detail-workspace {
  width: min(1260px, calc(100vw - 64px));
  margin: clamp(78px, 8svh, 104px) auto 34px;
  color: #ffe7b5;
  text-wrap: pretty;
}

.quest-detail-hero,
.quest-detail-card {
  border: 1px solid rgba(238, 184, 91, 0.58);
  border-radius: var(--radius);
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.82), rgba(15, 8, 4, 0.76)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.16), transparent 58%);
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.36), inset 0 1px 0 rgba(255, 229, 163, 0.14);
  backdrop-filter: blur(6px);
}

/* Hero takes on a commission-scroll feel with a wax-seal accent. */
.quest-detail-hero {
  position: relative;
  padding: 22px 20px 18px;
  overflow: hidden;
}

.quest-detail-hero::before {
  position: absolute;
  left: 18px;
  top: 14px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  content: '';
  background: radial-gradient(circle at 35% 30%, #ffe6a6, #b56c22 60%, #6e3c12);
  box-shadow: 0 3px 6px rgba(40, 18, 4, 0.5), inset 0 1px 1px rgba(255, 255, 255, 0.55);
}

.quest-detail-hero .kicker {
  margin-left: 28px;
}

.hero-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.hero-title-row h1 {
  font-size: clamp(1.7rem, 3vw, 2.6rem);
}

.hero-state {
  flex: 0 0 auto;
  margin-top: 6px;
  border: 1px solid currentColor;
  border-radius: 999px;
  padding: 5px 14px;
  font-size: 0.82rem;
  font-weight: 700;
  font-family: var(--font-display);
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.hero-state.tone-open {
  color: #a6e08e;
  background: rgba(108, 167, 96, 0.22);
}

.hero-state.tone-active {
  color: #f2c06f;
  background: rgba(232, 170, 60, 0.2);
}

.hero-state.tone-ready {
  color: #8fbce8;
  background: rgba(70, 116, 178, 0.22);
}

.hero-state.tone-review {
  color: #c8a8e0;
  background: rgba(140, 104, 168, 0.24);
}

.hero-state.tone-returned {
  color: #f0a18f;
  background: rgba(178, 86, 70, 0.24);
}

.hero-state.tone-done {
  color: #f1ffd6;
  background: rgba(67, 97, 58, 0.6);
}

.hero-desc {
  max-width: 72ch;
  margin: 10px 0 0;
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.48;
}

.meta-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.meta-chips li {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid rgba(240, 198, 118, 0.28);
  border-radius: 999px;
  padding: 6px 12px 6px 9px;
  background: rgba(11, 6, 3, 0.36);
}

.chip-icon {
  width: 17px;
  height: 17px;
  flex: 0 0 auto;
  fill: none;
  stroke: var(--gold-bright);
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chip-label {
  color: rgba(255, 231, 183, 0.6);
  font-size: 0.74rem;
}

.chip-value {
  color: #ffe8b9;
  font-size: 0.84rem;
  font-weight: 700;
}

/* The reward chip reads as treasure: warm border + gold coin marker. */
.chip-reward {
  border-color: rgba(242, 192, 111, 0.5) !important;
  background: rgba(122, 61, 12, 0.34) !important;
}

.chip-reward .chip-value {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #ffd98a;
}

.chip-reward .chip-value::before {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  content: '';
  background: radial-gradient(circle at 35% 30%, #ffe39a, #d89a32 65%, #9e5a19);
  box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.6);
}

.workflow-boundary {
  margin: 16px 0 0;
  border-left: 3px solid rgba(242, 192, 111, 0.8);
  padding: 10px 12px;
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.48;
  background: rgba(9, 5, 2, 0.28);
}

.quest-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(310px, 0.42fr);
  gap: 16px;
  margin-top: 16px;
}

.quest-main-column,
.quest-side-column {
  display: grid;
  align-content: start;
  gap: 16px;
}

.quest-detail-card {
  padding: 16px;
}

.quest-detail-card h2 {
  font-size: 1.18rem;
}

.quest-detail-card p,
.section-note,
.submission-requirements {
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.48;
}

.quest-detail-card p {
  margin: 10px 0 0;
}

.source-summary,
.quest-detail-card dl {
  display: grid;
  margin: 0;
}

.source-summary {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.source-summary div,
.quest-detail-card dl div {
  min-width: 0;
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 5px;
  padding: 9px 10px;
  background: rgba(11, 6, 3, 0.28);
}

.source-summary dd,
.quest-detail-card dd {
  overflow-wrap: anywhere;
}

.criteria-list {
  display: grid;
  gap: 9px;
  margin-top: 14px;
}

.criteria-list label {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr);
  gap: 9px;
  align-items: start;
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 5px;
  padding: 10px;
  color: #ffe8b9;
  background: rgba(11, 6, 3, 0.3);
}

.criteria-list input {
  width: 16px;
  height: 16px;
  margin: 2px 0 0;
  accent-color: var(--green);
}

.side-card dl {
  gap: 9px;
}

.side-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.side-actions .primary-action,
.side-actions .quiet-action {
  min-height: 36px;
  padding: 0 12px;
}

.submission-requirements {
  display: grid;
  gap: 8px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.submission-requirements li {
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 7px;
}

.counter-boundary {
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 5px;
  padding: 10px;
  background: rgba(11, 6, 3, 0.28);
}

/* Orchestrated page-load: the scroll unfurls top-to-bottom, cards rise in.
   Targets the columns' direct children (not the wrappers) so the sticky
   action rail keeps an untransformed ancestor and stays pinned. */
@media (prefers-reduced-motion: no-preference) {
  .quest-detail-hero,
  .quest-main-column > *,
  .quest-side-column > * {
    animation: quest-rise 560ms cubic-bezier(0.22, 1, 0.36, 1) both;
  }

  .quest-detail-hero {
    animation-delay: 40ms;
  }

  .quest-main-column > *:nth-child(1) {
    animation-delay: 150ms;
  }

  .quest-main-column > *:nth-child(2) {
    animation-delay: 230ms;
  }

  .quest-main-column > *:nth-child(3) {
    animation-delay: 310ms;
  }

  .quest-side-column > *:nth-child(1) {
    animation-delay: 200ms;
  }

  .quest-side-column > *:nth-child(2) {
    animation-delay: 280ms;
  }

  .quest-side-column > *:nth-child(3) {
    animation-delay: 360ms;
  }
}

@keyframes quest-rise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 980px) {
  .quest-detail-workspace {
    width: min(100% - 28px, 760px);
    margin-top: 72px;
  }

  .quest-detail-grid,
  .source-summary {
    grid-template-columns: 1fr;
  }
}

/* ── 接取契约二次确认弹窗：与任务详情同一蜡封羊皮纸语言，但作为独立浮层呈现 ── */
.accept-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 24px;
  background: radial-gradient(circle at 50% 30%, rgba(58, 30, 10, 0.55), rgba(5, 3, 1, 0.82));
  backdrop-filter: blur(3px);
}

.accept-dialog {
  position: relative;
  width: min(480px, 100%);
  border: 1px solid rgba(238, 184, 91, 0.65);
  border-radius: var(--radius);
  padding: 26px 24px 22px;
  color: #ffe7b5;
  background:
    linear-gradient(180deg, rgba(36, 19, 9, 0.96), rgba(16, 9, 4, 0.96)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.18), transparent 60%);
  box-shadow: 0 28px 64px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 229, 163, 0.16);
}

.accept-dialog-seal {
  position: absolute;
  top: -16px;
  right: 22px;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #ffe6a6, #b56c22 60%, #6e3c12);
  box-shadow: 0 6px 12px rgba(40, 18, 4, 0.55), inset 0 1px 1px rgba(255, 255, 255, 0.55);
  transition: transform 320ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

.accept-dialog.is-sealing .accept-dialog-seal {
  transform: translateY(6px) scale(0.88) rotate(-8deg);
}

.accept-dialog h2 {
  margin: 6px 0 0;
  font-size: 1.28rem;
  line-height: 1.3;
}

.accept-dialog-body {
  margin: 12px 0 0;
  color: rgba(255, 231, 183, 0.82);
  line-height: 1.55;
}

.accept-dialog-check {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 18px;
  border: 1px solid rgba(240, 198, 118, 0.28);
  border-radius: 7px;
  padding: 11px 12px;
  background: rgba(11, 6, 3, 0.4);
  cursor: pointer;
}

.accept-dialog-check input {
  width: 17px;
  height: 17px;
  flex: 0 0 auto;
  margin: 1px 0 0;
  accent-color: var(--green);
  cursor: pointer;
}

.accept-dialog-check span {
  font-size: 0.92rem;
  line-height: 1.45;
}

.accept-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.accept-dialog-actions .quiet-action,
.accept-dialog-actions .primary-action {
  min-height: 38px;
  padding: 0 16px;
}

.accept-confirm-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.contract-fade-enter-active,
.contract-fade-leave-active {
  transition: opacity 220ms ease;
}

.contract-fade-enter-from,
.contract-fade-leave-to {
  opacity: 0;
}

.contract-rise-enter-active {
  transition: transform 320ms cubic-bezier(0.22, 1, 0.36, 1), opacity 320ms ease;
}

.contract-rise-leave-active {
  transition: transform 180ms ease, opacity 180ms ease;
}

.contract-rise-enter-from {
  opacity: 0;
  transform: translateY(18px) scale(0.96);
}

.contract-rise-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.97);
}
</style>
