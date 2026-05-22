<script setup>
import { computed, ref, watch } from 'vue'

import BeginnerQuestChecklist from './BeginnerQuestChecklist.vue'
import QuestStatusFlow from './QuestStatusFlow.vue'
import { defaultContributionSteps, defaultSubmissionRequirements, questDetails } from '../data/quests'

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

const detail = computed(() => questDetails[props.quest.id] ?? {})
const repository = computed(
  () =>
    detail.value.repository ?? {
      name: 'git-guild / frontend',
      branch: 'main',
      syncStatus: 'Synced',
    },
)
const issue = computed(
  () =>
    detail.value.issue ?? {
      number: '#42',
      title: props.quest.title,
      status: '可接取',
    },
)
const pullRequest = computed(
  () =>
    detail.value.pr ?? {
      number: 'Not created',
      status: 'Not started',
    },
)
const beginnerTags = computed(() => props.quest.tags.filter((tag) => tag.includes('新手') || tag === '教程'))
const estimatedHours = computed(() => detail.value.estimatedHours ?? 6)
const description = computed(() => detail.value.description ?? props.quest.summary)
const contributionSteps = computed(() => detail.value.contributionSteps ?? defaultContributionSteps)
const submissionRequirements = computed(() => detail.value.submissionRequirements ?? defaultSubmissionRequirements)
const hasPullRequest = computed(() => pullRequest.value.number !== 'Not created')
const isAcceptIntent = computed(() => props.intent === 'accept' && localWorkflowState.value === 'available')
const statusFlowContext = computed(() => ({
  quest: `${props.quest.id} · ${props.quest.title}`,
  repository: repository.value.name,
  branch: hasPullRequest.value ? `任务分支 → ${repository.value.branch}` : `待创建任务分支，默认从 ${repository.value.branch} 切出`,
  pullRequest: hasPullRequest.value
    ? `${pullRequest.value.number} · ${pullRequest.value.status}`
    : `${pullRequest.value.number} · ${pullRequest.value.status}`,
  counter:
    localWorkflowState.value === 'available' || localWorkflowState.value === 'in-progress'
      ? 'PR 准备好后到提交柜台登记'
      : '已进入提交柜台材料链路',
  feedback:
    localWorkflowState.value === 'changes-requested'
      ? '维护者已退回，请查看逐项反馈'
      : localWorkflowState.value === 'completed'
        ? '审核通过，已写入成长记录'
        : '等待维护者审核后生成',
  syncStatus: repository.value.syncStatus,
}))

const workflowConfig = computed(() => {
  const configs = {
    available: {
      status: '可接取',
      next: '确认完成标准后接取任务。',
      primary: '接取委托',
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

watch(
  () => [props.quest.id, props.intent],
  () => {
    localWorkflowState.value = detail.value.workflowState ?? (props.quest.status === '待审核' ? 'in-review' : 'available')
    inlineNotice.value =
      props.intent === 'accept' && localWorkflowState.value === 'available'
        ? '请确认任务背景和完成标准后接取该任务。点击任务板的“接取”不会直接完成接取。'
        : ''
  },
  { immediate: true },
)

function handlePrimaryAction() {
  if (localWorkflowState.value === 'available') {
    localWorkflowState.value = 'in-progress'
    inlineNotice.value = '已接取该任务，下一步请进入工作台创建任务分支。任务已加入你的工作台待办。'
    return
  }

  if (localWorkflowState.value === 'pr-ready') {
    inlineNotice.value = '将进入提交柜台登记该任务成果。'
    emit('open-submission', props.quest.id)
    return
  }

  if (localWorkflowState.value === 'changes-requested') {
    inlineNotice.value = '模拟反馈：请补充 PR 链接校验的异常提示，并在提交说明中写明测试结果。'
    return
  }

  if (localWorkflowState.value === 'completed') {
    inlineNotice.value = '模拟成长记录：该任务已写入贡献记录，XP 已计入个人成长档案。'
    return
  }

  emit('open-workbench')
}

function handleSecondaryAction() {
  if (localWorkflowState.value === 'in-progress') {
    inlineNotice.value = '将进入提交柜台登记该任务成果。'
    emit('open-submission', props.quest.id)
    return
  }

  if (localWorkflowState.value === 'completed') {
    inlineNotice.value = '模拟贡献记录：展示任务标题、仓库、合并 PR 和完成时间。'
    return
  }

  emit('open-workbench')
}

function showIssueHint() {
  inlineNotice.value = `已定位模拟 Issue ${issue.value.number}：${issue.value.title}。真实实现会在工作台中打开 Issue 详情。`
}
</script>

<template>
  <div class="quest-detail-workspace" aria-label="任务详情">
    <header class="quest-detail-hero">
      <div class="quest-title-block">
        <p class="kicker">悬赏任务详情</p>
        <h1>{{ quest.id }} · {{ quest.title }}</h1>
        <p>{{ description }}</p>
      </div>

      <div class="quest-action-panel" :class="{ 'accept-intent': isAcceptIntent }">
        <span class="quest-state">{{ workflowConfig.status }}</span>
        <button class="primary-action" type="button" @click="handlePrimaryAction">{{ workflowConfig.primary }}</button>
        <button class="quiet-action" type="button" @click="handleSecondaryAction">{{ workflowConfig.secondary }}</button>
        <p>{{ workflowConfig.next }}</p>
        <p v-if="isAcceptIntent" class="accept-intent-note">确认任务背景和完成标准后接取该任务。</p>
      </div>

      <dl class="quest-meta-grid">
        <div>
          <dt>状态</dt>
          <dd>{{ workflowConfig.status }}</dd>
        </div>
        <div>
          <dt>难度</dt>
          <dd>{{ quest.difficulty }}</dd>
        </div>
        <div>
          <dt>奖励</dt>
          <dd>{{ quest.reward }}</dd>
        </div>
        <div>
          <dt>预计时长</dt>
          <dd>{{ estimatedHours }}h</dd>
        </div>
        <div>
          <dt>技术栈</dt>
          <dd>{{ quest.stack }}</dd>
        </div>
        <div>
          <dt>新手标签</dt>
          <dd>{{ beginnerTags.length ? beginnerTags.join(' / ') : '普通任务' }}</dd>
        </div>
      </dl>

      <p class="workflow-boundary">
        项目操作在工作台完成：创建分支、上传文件生成 commit、发起 PR。任务成果提交在提交柜台完成：关联 PR、填写成果说明、完成标准自检、提交审核。
      </p>
    </header>

    <QuestStatusFlow :status="localWorkflowState" :context="statusFlowContext" />

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

        <BeginnerQuestChecklist
          :quest="quest"
          :workflow-state="localWorkflowState"
          :repository="repository"
          :pull-request="pullRequest"
        />

        <section class="quest-detail-card">
          <p class="kicker">Beginner Path</p>
          <h2>简版路径</h2>
          <p class="section-note">上方清单用于课堂演示和本地勾选；这里保留快速回看路径。</p>
          <ol class="contribution-steps">
            <li v-for="step in contributionSteps" :key="step">{{ step }}</li>
          </ol>
        </section>
      </main>

      <aside class="quest-side-column">
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
            <button class="primary-action" type="button" @click="emit('open-workbench')">查看仓库</button>
            <button class="quiet-action" type="button" @click="showIssueHint">查看 Issue</button>
          </div>
        </section>

        <section class="quest-detail-card side-card progress-card">
          <p class="kicker">下一步</p>
          <h2>当前进度 / 下一步</h2>
          <strong>{{ workflowConfig.status }}</strong>
          <p>{{ workflowConfig.next }}</p>
          <div class="progress-actions">
            <button class="primary-action" type="button" @click="handlePrimaryAction">{{ workflowConfig.primary }}</button>
            <button class="quiet-action" type="button" @click="handleSecondaryAction">{{ workflowConfig.secondary }}</button>
          </div>
          <p v-if="inlineNotice" class="inline-notice">{{ inlineNotice }}</p>
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

.quest-detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 330px);
  gap: 18px;
  padding: 18px;
}

.quest-title-block h1 {
  font-size: clamp(1.7rem, 3vw, 2.6rem);
}

.quest-title-block p,
.workflow-boundary,
.quest-detail-card p,
.section-note,
.contribution-steps,
.submission-requirements {
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.48;
}

.quest-title-block p {
  max-width: 72ch;
  margin: 10px 0 0;
}

.quest-action-panel {
  display: grid;
  align-content: start;
  gap: 10px;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 6px;
  padding: 14px;
  background: rgba(11, 6, 3, 0.34);
}

.quest-action-panel.accept-intent {
  border-color: rgba(242, 192, 111, 0.84);
  background:
    linear-gradient(180deg, rgba(72, 41, 15, 0.72), rgba(21, 12, 7, 0.72)),
    radial-gradient(circle at 88% 0%, rgba(255, 217, 138, 0.18), transparent 0 42%);
  box-shadow: inset 0 0 0 1px rgba(255, 217, 138, 0.18), 0 18px 42px rgba(0, 0, 0, 0.34);
}

.quest-action-panel .primary-action,
.quest-action-panel .quiet-action {
  width: 100%;
}

.quest-action-panel p {
  margin: 0;
  color: rgba(255, 231, 183, 0.72);
  line-height: 1.4;
}

.quest-action-panel .accept-intent-note {
  border-left: 3px solid rgba(242, 192, 111, 0.9);
  padding-left: 10px;
  color: #ffe8b9;
}

.quest-state {
  display: inline-flex;
  width: fit-content;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 3px 9px;
  color: #ffe4ad;
  font-size: 0.78rem;
  background: rgba(80, 43, 18, 0.44);
}

.quest-meta-grid,
.source-summary,
.quest-detail-card dl {
  display: grid;
  margin: 0;
}

.quest-meta-grid {
  grid-column: 1 / -1;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 9px;
}

.quest-meta-grid div,
.source-summary div,
.quest-detail-card dl div {
  min-width: 0;
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 5px;
  padding: 9px 10px;
  background: rgba(11, 6, 3, 0.28);
}

.quest-meta-grid dd,
.source-summary dd,
.quest-detail-card dd {
  overflow-wrap: anywhere;
}

.workflow-boundary {
  grid-column: 1 / -1;
  margin: 0;
  border-left: 3px solid rgba(242, 192, 111, 0.8);
  padding: 10px 12px;
  background: rgba(9, 5, 2, 0.28);
}

.quest-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(310px, 0.42fr);
  gap: 16px;
  margin-top: 16px;
}

.quest-status-flow {
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

.quest-detail-card p {
  margin: 10px 0 0;
}

.source-summary {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
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

.contribution-steps {
  display: grid;
  gap: 9px;
  margin: 14px 0 0;
  padding-left: 1.25rem;
}

.contribution-steps li {
  padding-left: 4px;
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

.progress-card strong {
  display: block;
  margin-top: 12px;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.3rem;
}

.progress-actions {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.progress-actions .primary-action,
.progress-actions .quiet-action {
  width: 100%;
  min-height: 38px;
}

.inline-notice {
  border: 1px solid rgba(67, 97, 58, 0.56);
  border-radius: 5px;
  padding: 10px;
  color: #f2ffd9;
  background: rgba(67, 97, 58, 0.24);
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

@media (max-width: 980px) {
  .quest-detail-workspace {
    width: min(100% - 28px, 760px);
    margin-top: 72px;
  }

  .quest-detail-hero,
  .quest-detail-grid,
  .source-summary {
    grid-template-columns: 1fr;
  }

  .quest-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
