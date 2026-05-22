<script setup>
import { computed, onMounted, ref, watch } from 'vue'

const props = defineProps({
  quest: {
    type: Object,
    required: true,
  },
  workflowState: {
    type: String,
    default: 'available',
  },
  repository: {
    type: Object,
    default: () => ({}),
  },
  pullRequest: {
    type: Object,
    default: () => ({}),
  },
})

const checkedSteps = ref({})
const storageReady = ref(false)

const hasPullRequest = computed(() => {
  const number = String(props.pullRequest.number ?? '')
  return number && number !== 'Not created'
})

const storageKey = computed(() => `git-guild:beginner-checklist:${props.quest.id}`)
const criteriaSummary = computed(() => props.quest.criteria?.join(' / ') || '任务详情中的完成标准')
const branchName = computed(() => `quest/${props.quest.id.toLowerCase()}`)

const checklistSteps = computed(() => [
  {
    id: 'standards',
    title: '确认完成标准',
    place: '任务详情 / Issue',
    action: `先读任务背景、关联 Issue 和完成标准：${criteriaSummary.value}。不确定就先问维护者，不要直接改代码。`,
    caution: '这里确认“做到什么算完成”，不是提交成果的位置。',
    recommendedWhen: ['available'],
  },
  {
    id: 'branch',
    title: '创建任务分支',
    place: '工作台 / 仓库',
    action: `接取后进入工作台，从 ${props.repository.branch ?? 'main'} 创建任务分支，例如 ${branchName.value}。`,
    caution: '工作台负责项目和 Git 操作；不要在提交柜台创建分支。',
    recommendedWhen: ['in-progress'],
  },
  {
    id: 'commit',
    title: '上传 / 提交变更生成 commit',
    place: '工作台 / 文件变更',
    action: '在本地完成代码修改后，通过工作台上传或提交变更生成 commit，并写清楚提交说明。',
    caution: '网页不做在线编辑器，代码变更通过上传或提交完成。',
    recommendedWhen: ['in-progress'],
  },
  {
    id: 'pr',
    title: '发起 PR',
    place: '工作台 / PR',
    action: '用任务分支发起 PR，标题或描述里关联任务和 Issue，确认基础检查结果可见。',
    caution: 'PR 是代码审核入口；提交柜台只登记成果，不替代 PR。',
    recommendedWhen: ['in-progress', 'pr-ready'],
  },
  {
    id: 'counter',
    title: '在提交柜台提交成果',
    place: '提交柜台',
    action: '选择任务，关联 PR，填写成果说明、完成标准自检和必要截图，然后提交审核。',
    caution: '提交柜台负责平台内任务成果提交，不负责创建 commit 或发起 PR。',
    recommendedWhen: ['pr-ready'],
  },
  {
    id: 'review',
    title: '等待审核 / 查看反馈',
    place: '提交柜台 / 工作台',
    action: '提交后等待维护者审核；如果退回，先读反馈，再回工作台更新分支和 PR，最后重新提交成果。',
    caution: '退回修改不是失败，必须让 PR 和提交柜台材料都同步更新。',
    recommendedWhen: ['in-review', 'changes-requested', 'completed'],
  },
])

const firstUncheckedIndex = computed(() => checklistSteps.value.findIndex((step) => !checkedSteps.value[step.id]))
const recommendedIndex = computed(() => {
  const stateMatch = checklistSteps.value.findIndex(
    (step) => !checkedSteps.value[step.id] && step.recommendedWhen.includes(props.workflowState),
  )

  if (stateMatch > -1) return stateMatch
  return firstUncheckedIndex.value > -1 ? firstUncheckedIndex.value : checklistSteps.value.length - 1
})
const completedCount = computed(() => checklistSteps.value.filter((step) => checkedSteps.value[step.id]).length)
const progressPercent = computed(() => Math.round((completedCount.value / checklistSteps.value.length) * 100))
const nextStep = computed(() => checklistSteps.value[recommendedIndex.value])
const panelStatus = computed(() => {
  if (completedCount.value === checklistSteps.value.length) return '清单已完成'
  if (blockingReason.value) return '需要先解除阻塞'
  return `推荐下一步：${nextStep.value?.title ?? '继续执行'}`
})
const blockingReason = computed(() => {
  const step = nextStep.value
  if (!step) return ''
  if (step.id === 'branch' && props.workflowState === 'available') {
    return '还未接取任务。请先在任务详情确认标准并点击“接取委托”。'
  }
  if (step.id === 'counter' && !hasPullRequest.value && props.workflowState !== 'pr-ready') {
    return '提交柜台需要关联 PR。请先在工作台完成 commit 并发起 PR。'
  }
  if (props.workflowState === 'changes-requested' && step.id !== 'review') {
    return '当前任务有退回反馈。请先查看反馈，再决定要更新哪些代码和提交材料。'
  }
  return ''
})

watch(
  storageKey,
  () => {
    loadChecklist()
  },
  { immediate: false },
)

watch(
  checkedSteps,
  () => {
    if (!storageReady.value) return
    window.localStorage.setItem(storageKey.value, JSON.stringify(checkedSteps.value))
  },
  { deep: true },
)

onMounted(() => {
  storageReady.value = true
  loadChecklist()
})

function loadChecklist() {
  if (!storageReady.value) return

  try {
    checkedSteps.value = JSON.parse(window.localStorage.getItem(storageKey.value) ?? '{}')
  } catch {
    checkedSteps.value = {}
  }
}

function toggleStep(stepId) {
  checkedSteps.value = {
    ...checkedSteps.value,
    [stepId]: !checkedSteps.value[stepId],
  }
}

function getStepState(step, index) {
  return {
    done: Boolean(checkedSteps.value[step.id]),
    recommended: index === recommendedIndex.value && completedCount.value < checklistSteps.value.length,
    blocked: index === recommendedIndex.value && Boolean(blockingReason.value),
  }
}
</script>

<template>
  <section class="beginner-checklist-card" aria-label="新手任务执行清单">
    <header class="checklist-head">
      <div>
        <p class="kicker">Beginner Execution List</p>
        <h2>接取任务后怎么做</h2>
        <p>按课堂演示顺序勾选。工作台处理项目与 Git 操作，提交柜台只处理平台内成果提交。</p>
      </div>
      <div class="checklist-meter" :aria-label="`已完成 ${completedCount} / ${checklistSteps.length}`">
        <strong>{{ completedCount }} / {{ checklistSteps.length }}</strong>
        <span>{{ progressPercent }}%</span>
      </div>
    </header>

    <div class="checklist-progress" aria-hidden="true">
      <span :style="{ width: `${progressPercent}%` }"></span>
    </div>

    <div class="checklist-next" :class="{ blocked: blockingReason }" aria-live="polite">
      <strong>{{ panelStatus }}</strong>
      <p v-if="blockingReason">{{ blockingReason }}</p>
      <p v-else-if="nextStep">请在{{ nextStep.place }}完成：{{ nextStep.title }}。</p>
    </div>

    <ol class="beginner-steps">
      <li
        v-for="(step, index) in checklistSteps"
        :key="step.id"
        class="beginner-step"
        :class="getStepState(step, index)"
      >
        <label class="step-check">
          <input
            type="checkbox"
            :checked="Boolean(checkedSteps[step.id])"
            :aria-describedby="`${step.id}-details`"
            @change="toggleStep(step.id)"
          />
          <span>{{ index + 1 }}</span>
        </label>
        <div :id="`${step.id}-details`" class="step-body">
          <div class="step-title-row">
            <h3>{{ step.title }}</h3>
            <em>{{ step.place }}</em>
          </div>
          <p>{{ step.action }}</p>
          <small>{{ step.caution }}</small>
          <strong v-if="index === recommendedIndex && !checkedSteps[step.id]" class="step-recommend">
            {{ blockingReason ? '当前阻塞' : '当前推荐' }}
          </strong>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.beginner-checklist-card {
  border: 1px solid rgba(238, 184, 91, 0.58);
  border-radius: var(--radius);
  padding: 16px;
  color: #ffe7b5;
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.82), rgba(15, 8, 4, 0.76)),
    radial-gradient(circle at 10% 0%, rgba(255, 231, 163, 0.14), transparent 0 32%),
    linear-gradient(135deg, rgba(216, 154, 50, 0.13), transparent 58%);
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.34), inset 0 1px 0 rgba(255, 229, 163, 0.14);
  backdrop-filter: blur(6px);
}

.checklist-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.checklist-head p {
  max-width: 70ch;
  margin: 8px 0 0;
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.48;
}

.checklist-meter {
  display: grid;
  justify-items: end;
  gap: 3px;
  min-width: 86px;
  border: 1px solid rgba(238, 184, 91, 0.32);
  border-radius: 5px;
  padding: 9px 10px;
  background: rgba(11, 6, 3, 0.32);
}

.checklist-meter strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.18rem;
}

.checklist-meter span {
  color: rgba(255, 231, 183, 0.66);
  font-size: 0.78rem;
}

.checklist-progress {
  height: 8px;
  overflow: hidden;
  border: 1px solid rgba(238, 184, 91, 0.24);
  border-radius: 999px;
  margin-top: 14px;
  background: rgba(9, 5, 2, 0.46);
}

.checklist-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(67, 97, 58, 0.9), rgba(242, 192, 111, 0.95));
  box-shadow: 0 0 18px rgba(242, 192, 111, 0.24);
  transition: width 180ms ease;
}

.checklist-next {
  border: 1px solid rgba(67, 97, 58, 0.5);
  border-radius: 5px;
  margin-top: 14px;
  padding: 10px 12px;
  background: rgba(67, 97, 58, 0.18);
}

.checklist-next.blocked {
  border-color: rgba(202, 104, 88, 0.72);
  background: rgba(110, 42, 36, 0.24);
}

.checklist-next strong {
  color: #f6ffd9;
}

.checklist-next p {
  margin: 5px 0 0;
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.42;
}

.beginner-steps {
  display: grid;
  gap: 10px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.beginner-step {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 10px;
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 6px;
  padding: 10px;
  background:
    linear-gradient(180deg, rgba(11, 6, 3, 0.34), rgba(11, 6, 3, 0.24)),
    radial-gradient(circle at 0% 0%, rgba(255, 217, 138, 0.08), transparent 0 38%);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.beginner-step.recommended {
  border-color: rgba(242, 192, 111, 0.86);
  background:
    linear-gradient(180deg, rgba(73, 42, 17, 0.72), rgba(18, 10, 5, 0.66)),
    radial-gradient(circle at 0% 0%, rgba(255, 217, 138, 0.18), transparent 0 46%);
  box-shadow: inset 0 0 0 1px rgba(255, 217, 138, 0.16), 0 12px 28px rgba(0, 0, 0, 0.24);
  transform: translateY(-1px);
}

.beginner-step.blocked {
  border-color: rgba(202, 104, 88, 0.72);
}

.beginner-step.done {
  border-color: rgba(129, 184, 98, 0.45);
  background: rgba(32, 52, 28, 0.32);
}

.step-check {
  display: grid;
  justify-items: center;
  gap: 7px;
  color: #ffe4ad;
}

.step-check input {
  width: 18px;
  height: 18px;
  margin: 0;
  accent-color: var(--green);
}

.step-check span {
  display: grid;
  place-items: center;
  width: 25px;
  height: 25px;
  border: 1px solid rgba(238, 184, 91, 0.48);
  border-radius: 50%;
  color: #ffe4ad;
  background: rgba(21, 12, 7, 0.78);
  font-size: 0.78rem;
  font-weight: 700;
}

.beginner-step.done .step-check span {
  border-color: rgba(129, 184, 98, 0.68);
  color: #f1ffd6;
  background: rgba(67, 97, 58, 0.78);
}

.step-body {
  min-width: 0;
}

.step-title-row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 10px;
}

.step-title-row h3 {
  margin: 0;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1rem;
  line-height: 1.22;
}

.step-title-row em {
  flex: 0 0 auto;
  border: 1px solid rgba(238, 184, 91, 0.34);
  border-radius: 999px;
  padding: 3px 8px;
  color: rgba(255, 231, 183, 0.74);
  background: rgba(80, 43, 18, 0.32);
  font-size: 0.72rem;
  font-style: normal;
}

.step-body p {
  margin: 8px 0 0;
  color: rgba(255, 231, 183, 0.82);
  line-height: 1.45;
}

.step-body small {
  display: block;
  margin-top: 7px;
  color: rgba(255, 231, 183, 0.62);
  line-height: 1.35;
}

.step-recommend {
  display: inline-flex;
  width: fit-content;
  margin-top: 9px;
  border: 1px solid rgba(242, 192, 111, 0.54);
  border-radius: 999px;
  padding: 3px 8px;
  color: #fff1c9;
  background: rgba(133, 76, 20, 0.48);
  font-size: 0.74rem;
}

.beginner-step.blocked .step-recommend {
  border-color: rgba(202, 104, 88, 0.72);
  color: #ffe0da;
  background: rgba(110, 42, 36, 0.56);
}

@media (max-width: 720px) {
  .checklist-head,
  .step-title-row {
    display: grid;
  }

  .checklist-meter {
    justify-items: start;
    width: 100%;
  }

  .beginner-step {
    grid-template-columns: 34px minmax(0, 1fr);
  }

  .step-title-row em {
    width: fit-content;
  }
}
</style>
