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
  submitted: 'review',
  'in-review': 'review',
  review: 'review',
  approved: 'approved',
  completed: 'approved',
  returned: 'returned',
  'changes-requested': 'returned',
  rejected: 'returned',
}

// 顺序即叙事顺序：退回修改是 3 的分支，修改完回到 3 重新提交；
// 只有真正通过才走到终点 5，所以「退回」排在「通过」前面。
const flowNodes = [
  {
    key: 'available',
    label: '待接取',
    place: '悬赏任务板',
    tone: 'open',
    icon: 'scroll',
    next: '看清完成标准再接取。',
  },
  {
    key: 'active',
    label: '进行中',
    place: '工作台 · 仓库',
    tone: 'active',
    icon: 'hammer',
    next: '仓库已经是你的了，把代码改完，按下面三步推送上去。',
  },
  {
    key: 'review',
    label: '已提交 / 审核中',
    place: '提交柜台 → 审核台',
    tone: 'review',
    icon: 'paper-plane',
    next: '维护者正在检查 PR。',
  },
  {
    key: 'returned',
    label: '退回修改',
    place: '审核反馈',
    tone: 'returned',
    icon: 'return-arrow',
    next: '改完点这里回到「已提交」。',
  },
  {
    key: 'approved',
    label: '通过',
    place: '成长记录',
    tone: 'approved',
    icon: 'trophy',
    next: '任务结清，XP 已入账。',
  },
]

const currentKey = computed(() => statusMap[props.status] ?? 'available')
const selectedKey = ref(currentKey.value)

const selectedNode = computed(() => flowNodes.find((node) => node.key === selectedKey.value) ?? flowNodes[0])
const currentIndex = computed(() => flowNodes.findIndex((node) => node.key === currentKey.value))
const contextRows = computed(() =>
  [
    ['任务', props.context.quest],
    ['仓库', props.context.repository],
    ['分支', props.context.branch],
    ['PR', props.context.pullRequest ?? props.context.prStatus],
    ['反馈', props.context.feedback],
  ].filter(([, value]) => Boolean(value)),
)
const exceptionHints = computed(() => {
  const hints = [
    {
      title: '任务被抢',
      active: props.context.assignee && props.context.assignee !== props.context.viewer,
      body: '刷新任务板，换一个未锁定的任务。',
    },
    {
      title: 'PR 未关联',
      active:
        String(props.context.pullRequest ?? props.context.prStatus ?? '')
          .toLowerCase()
          .includes('未') || String(props.context.pullRequest ?? '').toLowerCase().includes('not created'),
      body: '先在工作台发 PR，再到提交柜台关联。',
    },
    {
      title: '同步滞后',
      active: String(props.context.syncStatus ?? '').toLowerCase().includes('warning'),
      body: '检查仓库同步日志或手动同步。',
    },
  ]

  return hints.filter((hint) => hint.active)
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
    !['returned', 'approved'].includes(currentKey.value)

  return {
    current: isCurrent,
    selected: isSelected,
    done: isDone,
    locked: !isReachable(index),
    [`tone-${node.tone}`]: isCurrent || isSelected,
  }
}

function jumpToReview() {
  selectedKey.value = 'review'
}

// 「退回修改」不是线性轴上的必经点，而是「已提交/审核中」的分支：
// 只有真的被退回过（存在反馈历史）才允许查看，不按线性序号解锁。
const feedbackHistory = computed(() => props.context.feedbackHistory ?? [])
const hasFeedbackHistory = computed(() => feedbackHistory.value.length > 0)
const feedbackHistoryDesc = computed(() => [...feedbackHistory.value].reverse())

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hh}:${mm}`
}

function isReachable(index) {
  if (flowNodes[index]?.key === 'returned') return hasFeedbackHistory.value
  return currentIndex.value > -1 && index <= currentIndex.value
}

function selectNode(node, index) {
  if (!isReachable(index)) return
  selectedKey.value = node.key
}
</script>

<template>
  <section class="quest-status-flow" :class="{ compact }" aria-label="任务状态流转">
    <header class="flow-head">
      <p class="kicker">任务状态流转</p>
      <span class="flow-current" :class="`tone-${selectedNode.tone}`">{{ selectedNode.label }}</span>
    </header>

    <div class="flow-node-row" role="list">
      <button
        v-for="(node, index) in flowNodes"
        :key="node.key"
        class="flow-node"
        :class="getNodeClass(node, index)"
        type="button"
        role="listitem"
        :disabled="!isReachable(index)"
        :aria-disabled="!isReachable(index)"
        @click="selectNode(node, index)"
      >
        <span class="flow-dot" aria-hidden="true">
          <svg v-if="index < currentIndex && !['returned', 'approved'].includes(currentKey)" viewBox="0 0 24 24" class="dot-check">
            <path d="m5 13 4 4L19 7" />
          </svg>
          <svg v-else-if="node.icon === 'scroll'" viewBox="0 0 24 24"><path d="M6 4h11a2 2 0 0 1 2 2v13a1 1 0 0 1-1.6.8L15 18l-2.4 1.8a1 1 0 0 1-1.2 0L9 18l-2.4 1.8A1 1 0 0 1 5 19V6a2 2 0 0 1 1-2Z" /><path d="M8.5 8h7M8.5 11.5h7" /></svg>
          <svg v-else-if="node.icon === 'hammer'" viewBox="0 0 24 24"><path d="m14.5 6.5 3 3L8 19l-3.5.5L5 16Z" /><path d="m13 5 5 5M16 3l5 5-2 2-5-5Z" /></svg>
          <svg v-else-if="node.icon === 'paper-plane'" viewBox="0 0 24 24"><path d="M21 3 11 13M21 3l-7 18-4-8-8-4Z" /></svg>
          <svg v-else-if="node.icon === 'return-arrow'" viewBox="0 0 24 24"><path d="M9 14 4 9l5-5" /><path d="M4 9h10a6 6 0 0 1 6 6v3" /></svg>
          <svg v-else viewBox="0 0 24 24"><path d="M7 4h10v4a5 5 0 0 1-5 5 5 5 0 0 1-5-5Z" /><path d="M7 6H4v1a4 4 0 0 0 4 4M17 6h3v1a4 4 0 0 1-4 4M12 13v3m-3 4h6m-3-4v4" /></svg>
        </span>
        <strong>{{ node.label }}</strong>
        <small>{{ node.place }}</small>
      </button>
    </div>

    <div class="flow-stage">
      <!-- 2：进行中 -->
      <article v-if="selectedNode.key === 'active'" class="stage-card stage-clone">
        <p class="stage-foot">{{ selectedNode.next }}</p>
      </article>

      <!-- 3：已提交/审核中 — 祝贺 + 安抚等待 -->
      <article v-else-if="selectedNode.key === 'review'" class="stage-card stage-celebrate tone-review">
        <span class="stage-emoji" aria-hidden="true">🎉</span>
        <h3>提交成功！</h3>
        <p>已收到你的成果，维护者正在审核，耐心等一等。</p>
      </article>

      <!-- 4：退回修改 — 指回 3，按时间倒序展示历次退回意见 -->
      <article v-else-if="selectedNode.key === 'returned'" class="stage-card stage-feedback tone-returned">
        <span class="stage-emoji" aria-hidden="true">📝</span>
        <h3>收到退回意见</h3>
        <ol v-if="feedbackHistoryDesc.length" class="feedback-history">
          <li v-for="(entry, index) in feedbackHistoryDesc" :key="entry.reviewedAt ?? index">
            <p>{{ entry.summary }}</p>
            <time v-if="entry.reviewedAt">{{ formatDateTime(entry.reviewedAt) }}</time>
          </li>
        </ol>
        <p v-else>改完代码、更新 PR 后，回到「已提交 / 审核中」重新等待审核。</p>
        <button type="button" class="stage-link-btn" @click="jumpToReview">→ 回到已提交</button>
      </article>

      <!-- 5：通过 — 终点祝贺 -->
      <article v-else-if="selectedNode.key === 'approved'" class="stage-card stage-celebrate tone-approved">
        <span class="stage-emoji" aria-hidden="true">🏆</span>
        <h3>任务完成！</h3>
        <p>XP 已经入账，去成长记录看看战绩，准备接下一个任务吧。</p>
      </article>

      <!-- 1：待接取 — 默认简短提示 -->
      <article v-else class="stage-card">
        <p class="stage-foot">{{ selectedNode.next }}</p>
      </article>
    </div>

    <div v-if="contextRows.length || exceptionHints.length" class="flow-foot-grid">
      <dl v-if="contextRows.length" class="flow-context">
        <div v-for="[label, value] in contextRows" :key="label">
          <dt>{{ label }}</dt>
          <dd>{{ value }}</dd>
        </div>
      </dl>

      <ul v-if="exceptionHints.length" class="flow-exceptions">
        <li v-for="hint in exceptionHints" :key="hint.title">
          <strong>{{ hint.title }}</strong>
          <span>{{ hint.body }}</span>
        </li>
      </ul>
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
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.flow-head .kicker {
  margin: 0;
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

.flow-current.tone-returned {
  border-color: rgba(214, 142, 56, 0.65);
  color: #ffd9a8;
  background: rgba(150, 79, 24, 0.42);
}

.flow-current.tone-approved {
  border-color: rgba(129, 184, 98, 0.7);
  color: #e8ffd2;
  background: rgba(67, 97, 58, 0.6);
}

.flow-node-row {
  position: relative;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
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

.flow-node.locked {
  color: rgba(255, 231, 183, 0.32);
  cursor: not-allowed;
  opacity: 0.55;
}

.flow-node.locked:hover,
.flow-node.locked:focus-visible {
  border-color: rgba(240, 198, 118, 0.18);
  background: rgba(8, 4, 2, 0.5);
  transform: none;
}

.flow-node.locked .flow-dot {
  color: rgba(255, 231, 183, 0.32);
  background: rgba(21, 12, 7, 0.6);
}

.flow-node.current {
  border-color: rgba(242, 192, 111, 0.86);
  color: #fff1c9;
  background:
    linear-gradient(180deg, rgba(103, 58, 20, 0.74), rgba(37, 20, 8, 0.72)),
    radial-gradient(circle at 50% 0%, rgba(255, 231, 163, 0.22), transparent 0 52%);
  box-shadow: inset 0 0 0 1px rgba(255, 217, 138, 0.18), 0 12px 28px rgba(0, 0, 0, 0.24);
}

.flow-node.current.tone-returned {
  border-color: rgba(214, 142, 56, 0.82);
}

.flow-node.current.tone-approved {
  border-color: rgba(129, 184, 98, 0.82);
}

.flow-node.done .flow-dot {
  border-color: rgba(129, 184, 98, 0.68);
  color: #f1ffd6;
  background: rgba(67, 97, 58, 0.78);
}

.flow-dot {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 1px solid rgba(238, 184, 91, 0.48);
  border-radius: 50%;
  color: #ffe4ad;
  background: rgba(21, 12, 7, 0.86);
}

.flow-dot svg {
  width: 15px;
  height: 15px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.flow-node.current .flow-dot {
  background: var(--gold-bright);
  color: #2a180d;
  box-shadow: 0 0 0 5px rgba(242, 192, 111, 0.16), 0 0 22px rgba(242, 192, 111, 0.28);
}

.flow-node.current.tone-returned .flow-dot {
  background: #d68e38;
  box-shadow: 0 0 0 5px rgba(214, 142, 56, 0.2), 0 0 22px rgba(214, 142, 56, 0.3);
}

.flow-node.current.tone-approved .flow-dot {
  background: #81b862;
  box-shadow: 0 0 0 5px rgba(129, 184, 98, 0.2), 0 0 22px rgba(129, 184, 98, 0.3);
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

.flow-stage {
  min-height: 88px;
}

.stage-card {
  display: grid;
  gap: 6px;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 6px;
  padding: 13px 14px;
  background: rgba(7, 4, 2, 0.32);
}

.stage-foot {
  margin: 0;
  color: rgba(255, 231, 183, 0.82);
  font-size: 0.92rem;
}

.stage-kicker {
  margin: 0;
  color: rgba(255, 231, 183, 0.56);
  font-size: 0.72rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.stage-celebrate {
  align-content: start;
  text-align: left;
  padding: 16px 16px 18px;
}

.stage-emoji {
  font-size: 1.7rem;
  line-height: 1;
}

.stage-card h3 {
  margin: 2px 0 0;
  font-family: var(--font-display);
  font-size: 1.08rem;
  color: #ffe8b9;
}

.stage-celebrate p,
.stage-feedback p {
  margin: 0;
  color: rgba(255, 231, 183, 0.82);
  line-height: 1.45;
}

.stage-celebrate.tone-review {
  border-color: rgba(242, 192, 111, 0.42);
  background: rgba(103, 58, 20, 0.32);
}

.stage-celebrate.tone-approved {
  border-color: rgba(129, 184, 98, 0.45);
  background: rgba(56, 80, 46, 0.34);
}

.stage-feedback.tone-returned {
  border-color: rgba(214, 142, 56, 0.4);
  background: rgba(110, 67, 24, 0.3);
}

.feedback-history {
  display: grid;
  gap: 10px;
  margin: 2px 0 0;
  padding: 0;
  list-style: none;
}

.feedback-history li {
  display: grid;
  gap: 4px;
  border-left: 3px solid #d68e38;
  padding: 2px 0 2px 10px;
}

.feedback-history li:first-child p {
  color: #ffe8b9;
}

.feedback-history p {
  margin: 0;
  color: rgba(255, 231, 183, 0.82);
  line-height: 1.45;
}

.feedback-history time {
  color: rgba(255, 231, 183, 0.5);
  font-size: 0.74rem;
}

.stage-link-btn {
  justify-self: start;
  margin-top: 4px;
  border: 1px solid rgba(214, 142, 56, 0.6);
  border-radius: 999px;
  padding: 6px 14px;
  color: #ffe0ad;
  font-size: 0.82rem;
  background: rgba(150, 79, 24, 0.32);
  cursor: pointer;
  transition: background 150ms ease, transform 150ms ease;
}

.stage-link-btn:hover {
  background: rgba(150, 79, 24, 0.5);
  transform: translateY(-1px);
}

.flow-foot-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.flow-context {
  display: grid;
  gap: 6px;
  margin: 0;
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 5px;
  padding: 10px 12px;
  background: rgba(7, 4, 2, 0.26);
}

.flow-context div {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 8px;
  font-size: 0.8rem;
}

.flow-context dt {
  color: rgba(255, 231, 183, 0.5);
}

.flow-context dd {
  margin: 0;
  color: #ffe8b9;
  overflow-wrap: anywhere;
}

.flow-exceptions {
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.flow-exceptions li {
  display: grid;
  gap: 2px;
  border-left: 3px solid #d66a48;
  padding-left: 9px;
  font-size: 0.8rem;
}

.flow-exceptions strong {
  color: #ffe8b9;
}

.flow-exceptions span {
  color: rgba(255, 231, 183, 0.74);
}

.quest-status-flow.compact {
  padding: 13px;
}

.quest-status-flow.compact .flow-foot-grid {
  grid-template-columns: 1fr;
}

@media (max-width: 980px) {
  .flow-node-row {
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

  .flow-foot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
