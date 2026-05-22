<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  status: {
    type: String,
    default: 'available',
  },
  context: {
    type: Object,
    default: () => ({}),
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const statusMap = {
  available: 'available',
  accepted: 'active',
  'in-progress': 'active',
  'pr-ready': 'active',
  submitted: 'submitted',
  'in-review': 'review',
  review: 'review',
  approved: 'approved',
  completed: 'approved',
  returned: 'returned',
  'changes-requested': 'returned',
  rejected: 'returned',
}

const flowNodes = [
  {
    key: 'available',
    label: '待接取',
    place: '悬赏任务板 / 任务详情',
    next: '确认背景、完成标准、奖励和仓库后，再接取任务。',
    detail: '这里解决“我要不要做”的问题。接取前先看任务详情里的 Issue、完成标准和新手步骤，避免接下不适合当前能力的任务。',
    links: ['任务', 'Issue', '完成标准'],
  },
  {
    key: 'active',
    label: '已接取 / 进行中',
    place: '工作台 / 仓库',
    next: '在工作台打开仓库，创建任务分支，上传文件生成 commit，再发起 PR。',
    detail: '这里解决“代码在哪里做”的问题。分支、commit、PR 都属于仓库工作台，不在提交柜台里直接编辑代码。',
    links: ['任务', '分支', 'PR'],
  },
  {
    key: 'submitted',
    label: '已提交',
    place: '提交柜台',
    next: '把 PR、成果说明、完成标准自检和截图材料提交给维护者。',
    detail: '这里解决“成果如何登记”的问题。提交柜台只登记审核材料，PR 仍然来自工作台。',
    links: ['提交柜台', 'PR', '完成标准'],
  },
  {
    key: 'review',
    label: '审核中',
    place: '维护者审核台',
    next: '等待维护者检查 PR、提交说明和完成标准，期间可查看 PR 或提交记录。',
    detail: '这里解决“谁来判断是否达标”的问题。维护者会根据 PR、commit、完成标准和提交说明给出通过或退回。',
    links: ['审核反馈', 'PR', '提交记录'],
  },
  {
    key: 'approved',
    label: '通过',
    place: '成长记录',
    next: '任务写入贡献记录，XP 和完成任务数更新。',
    detail: '通过后任务链路结束，可以在个人成长记录里回看任务、合并 PR、获得 XP 和贡献摘要。',
    links: ['任务', 'PR', '成长记录'],
  },
  {
    key: 'returned',
    label: '退回修改',
    place: '审核反馈 / 工作台',
    next: '先读逐项反馈，在工作台更新 PR，再回提交柜台重新提交。',
    detail: '退回不是结束。必须先按反馈更新代码和 PR，再通过提交柜台重新登记本次修改，否则维护者看不到新的自检材料。',
    links: ['审核反馈', 'PR', '提交柜台'],
  },
]

const currentKey = computed(() => statusMap[props.status] ?? 'available')
const selectedKey = ref(currentKey.value)

const selectedNode = computed(() => flowNodes.find((node) => node.key === selectedKey.value) ?? flowNodes[0])
const currentIndex = computed(() => flowNodes.findIndex((node) => node.key === currentKey.value))
const contextRows = computed(() => [
  ['任务', props.context.quest ?? '当前任务'],
  ['仓库', props.context.repository ?? '未选择仓库'],
  ['分支', props.context.branch ?? '接取后在工作台创建'],
  ['PR', props.context.pullRequest ?? props.context.prStatus ?? '尚未关联 PR'],
  ['提交柜台', props.context.counter ?? '提交成果时登记 PR 与自检材料'],
  ['审核反馈', props.context.feedback ?? '通过或退回后在工作台查看'],
])
const exceptionHints = computed(() => {
  const hints = [
    {
      title: '任务被他人接取',
      active: props.context.assignee && props.context.assignee !== props.context.viewer,
      body: '回到悬赏任务板刷新状态，选择未被锁定的任务；不要在他人任务分支上继续提交。',
    },
    {
      title: 'PR 未关联',
      active:
        String(props.context.pullRequest ?? props.context.prStatus ?? '')
          .toLowerCase()
          .includes('未') || String(props.context.pullRequest ?? '').toLowerCase().includes('not created'),
      body: '先在工作台发起 PR，再到提交柜台粘贴或选择对应 PR。',
    },
    {
      title: '提交被退回',
      active: currentKey.value === 'returned' || String(props.context.feedback ?? '').includes('修改'),
      body: '阅读逐项反馈，更新分支和 PR，然后重新提交成果记录。',
    },
    {
      title: '同步状态滞后',
      active: String(props.context.syncStatus ?? '').toLowerCase().includes('warning'),
      body: '先查看仓库同步日志或手动同步，再判断 Issue、PR 和审核状态。',
    },
  ]

  return hints.sort((a, b) => Number(b.active) - Number(a.active))
})

watch(currentKey, (key) => {
  selectedKey.value = key
})

function getNodeClass(node, index) {
  const isCurrent = node.key === currentKey.value
  const isSelected = node.key === selectedKey.value
  const isDone =
    currentIndex.value > -1 &&
    index < currentIndex.value &&
    !['approved', 'returned'].includes(currentKey.value)

  return {
    current: isCurrent,
    selected: isSelected,
    done: isDone,
    branch: node.key === 'approved' || node.key === 'returned',
  }
}
</script>

<template>
  <section class="quest-status-flow" :class="{ compact }" aria-label="任务状态流转">
    <header class="flow-head">
      <div>
        <p class="kicker">任务状态流转</p>
        <h2>任务状态流转</h2>
      </div>
      <span class="flow-current">{{ selectedNode.label }}</span>
    </header>

    <div class="flow-node-row" role="list">
      <button
        v-for="(node, index) in flowNodes"
        :key="node.key"
        class="flow-node"
        :class="getNodeClass(node, index)"
        type="button"
        role="listitem"
        @click="selectedKey = node.key"
      >
        <span class="flow-dot" aria-hidden="true">{{ index + 1 }}</span>
        <strong>{{ node.label }}</strong>
        <small>{{ node.place }}</small>
      </button>
    </div>

    <div class="flow-detail-grid">
      <article class="flow-explain">
        <span>当前查看节点</span>
        <h3>{{ selectedNode.label }}</h3>
        <p>{{ selectedNode.detail }}</p>
        <p class="flow-next">下一步：{{ selectedNode.next }}</p>
        <div class="flow-link-tags" aria-label="关联平台位置">
          <span v-for="link in selectedNode.links" :key="link">{{ link }}</span>
        </div>
      </article>

      <article class="flow-context">
        <h3>本任务关联对象</h3>
        <dl>
          <div v-for="[label, value] in contextRows" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </div>
        </dl>
      </article>

      <article class="flow-exceptions">
        <h3>常见异常提示</h3>
        <ul>
          <li v-for="hint in exceptionHints" :key="hint.title" :class="{ active: hint.active }">
            <strong>{{ hint.title }}</strong>
            <span>{{ hint.body }}</span>
          </li>
        </ul>
      </article>
    </div>
  </section>
</template>

<style scoped>
.quest-status-flow {
  display: grid;
  gap: 14px;
  border: 1px solid rgba(238, 184, 91, 0.5);
  border-radius: var(--radius);
  padding: 16px;
  color: #ffe7b5;
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.76), rgba(15, 8, 4, 0.72)),
    radial-gradient(circle at 8% 0%, rgba(255, 217, 138, 0.13), transparent 0 30%, transparent 58%);
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255, 229, 163, 0.12);
}

.flow-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 14px;
}

.flow-current {
  border: 1px solid rgba(238, 184, 91, 0.48);
  border-radius: 999px;
  padding: 5px 10px;
  color: #ffe4ad;
  background: rgba(80, 43, 18, 0.44);
  font-size: 0.78rem;
  white-space: nowrap;
}

.flow-node-row {
  position: relative;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.flow-node-row::before {
  position: absolute;
  left: 7%;
  right: 7%;
  top: 19px;
  height: 1px;
  content: '';
  background: linear-gradient(90deg, rgba(238, 184, 91, 0.2), rgba(238, 184, 91, 0.58), rgba(238, 184, 91, 0.2));
}

.flow-node {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 6px;
  min-width: 0;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 8px 7px 10px;
  color: rgba(255, 231, 183, 0.72);
  text-align: center;
  background: rgba(8, 4, 2, 0.5);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.flow-node:hover,
.flow-node:focus-visible,
.flow-node.selected {
  border-color: var(--gold-bright);
  color: #ffe8b9;
  background: rgba(82, 45, 16, 0.58);
  transform: translateY(-1px);
}

.flow-node.current {
  border-color: rgba(242, 192, 111, 0.86);
  color: #fff1c9;
  background:
    linear-gradient(180deg, rgba(103, 58, 20, 0.74), rgba(37, 20, 8, 0.72)),
    radial-gradient(circle at 50% 0%, rgba(255, 231, 163, 0.22), transparent 0 52%);
  box-shadow: inset 0 0 0 1px rgba(255, 217, 138, 0.18), 0 12px 28px rgba(0, 0, 0, 0.24);
}

.flow-node.done .flow-dot {
  border-color: rgba(129, 184, 98, 0.68);
  color: #f1ffd6;
  background: rgba(67, 97, 58, 0.78);
}

.flow-node.branch.current:last-child,
.flow-node.branch.current:nth-last-child(2) {
  border-color: rgba(204, 95, 65, 0.68);
}

.flow-dot {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 1px solid rgba(238, 184, 91, 0.48);
  border-radius: 50%;
  color: #ffe4ad;
  background: rgba(21, 12, 7, 0.86);
  font-size: 0.76rem;
  font-weight: 700;
}

.flow-node.current .flow-dot {
  background: var(--gold-bright);
  color: #2a180d;
  box-shadow: 0 0 0 5px rgba(242, 192, 111, 0.16), 0 0 22px rgba(242, 192, 111, 0.28);
}

.flow-node strong {
  color: inherit;
  font-family: var(--font-display);
  font-size: 0.92rem;
  line-height: 1.16;
}

.flow-node small {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.72rem;
  line-height: 1.25;
}

.flow-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 0.72fr) minmax(240px, 0.84fr);
  gap: 10px;
}

.flow-explain,
.flow-context,
.flow-exceptions {
  min-width: 0;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 12px;
  background: rgba(7, 4, 2, 0.3);
}

.flow-explain span,
.flow-context dt {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.74rem;
}

.flow-explain h3,
.flow-context h3,
.flow-exceptions h3 {
  margin: 0 0 8px;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1rem;
}

.flow-explain h3 {
  margin-top: 4px;
}

.flow-explain p,
.flow-exceptions span {
  margin: 0;
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.45;
}

.flow-next {
  margin-top: 9px !important;
  border-left: 3px solid rgba(242, 192, 111, 0.78);
  padding-left: 10px;
  color: #ffe8b9 !important;
}

.flow-link-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.flow-link-tags span {
  border: 1px solid rgba(238, 184, 91, 0.34);
  border-radius: 999px;
  padding: 3px 8px;
  color: #ffe4ad;
  background: rgba(80, 43, 18, 0.34);
}

.flow-context dl {
  display: grid;
  gap: 7px;
  margin: 0;
}

.flow-context div {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 9px;
  border-bottom: 1px solid rgba(240, 198, 118, 0.14);
  padding-bottom: 6px;
}

.flow-context dd {
  margin: 0;
  color: #ffe8b9;
  overflow-wrap: anywhere;
}

.flow-exceptions ul {
  display: grid;
  gap: 7px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.flow-exceptions li {
  display: grid;
  gap: 3px;
  border-left: 3px solid rgba(238, 184, 91, 0.34);
  padding-left: 9px;
}

.flow-exceptions li.active {
  border-color: #d66a48;
}

.flow-exceptions strong {
  color: #ffe8b9;
  font-size: 0.86rem;
}

.quest-status-flow.compact {
  padding: 13px;
}

.quest-status-flow.compact .flow-detail-grid {
  grid-template-columns: 1fr;
}

@media (max-width: 980px) {
  .flow-node-row,
  .flow-detail-grid {
    grid-template-columns: 1fr;
  }

  .flow-node-row::before {
    display: none;
  }

  .flow-node {
    justify-items: start;
    grid-template-columns: 28px minmax(0, 1fr);
    text-align: left;
  }

  .flow-node small {
    grid-column: 2;
  }
}
</style>
