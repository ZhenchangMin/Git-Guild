<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    default: 'available',
  },
})

// Maps the granular workflow state to one of the canonical journey keys.
// Kept in sync with QuestStatusFlow.vue's statusMap so both views agree.
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

// The four linear nodes. "退回修改" is a branch off "已提交 / 审核中" rather than a node,
// so a returned quest highlights the review node in a warning tone instead.
const nodes = [
  { key: 'available', label: '待接取', place: '任务详情' },
  { key: 'active', label: '进行中', place: '工作台 / 仓库' },
  { key: 'review', label: '已提交 / 审核中', place: '提交柜台 → 维护者审核台' },
  { key: 'approved', label: '通过', place: '成长记录' },
]

const currentKey = computed(() => statusMap[props.status] ?? 'available')
const isReturned = computed(() => currentKey.value === 'returned')
// A returned quest sits visually at the review node.
const activeKey = computed(() => (isReturned.value ? 'review' : currentKey.value))
const currentIndex = computed(() => nodes.findIndex((node) => node.key === activeKey.value))

function nodeClass(node, index) {
  return {
    done: index < currentIndex.value,
    current: index === currentIndex.value,
    returned: isReturned.value && index === currentIndex.value,
  }
}
</script>

<template>
  <ol class="quest-stepper" :class="{ 'is-returned': isReturned }" aria-label="任务进度">
    <li
      v-for="(node, index) in nodes"
      :key="node.key"
      class="stepper-node"
      :class="nodeClass(node, index)"
    >
      <span class="stepper-dot" aria-hidden="true">
        <svg v-if="index < currentIndex" viewBox="0 0 24 24" class="dot-check">
          <path d="m5 13 4 4L19 7" />
        </svg>
        <template v-else>{{ index + 1 }}</template>
      </span>
      <span class="stepper-label">{{ node.label }}</span>
      <span class="stepper-place">{{ node.place }}</span>
      <span v-if="isReturned && node.key === 'review'" class="stepper-branch">退回修改</span>
    </li>
  </ol>
</template>

<style scoped>
.quest-stepper {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.quest-stepper::before {
  position: absolute;
  left: 10%;
  right: 10%;
  top: 17px;
  height: 2px;
  content: '';
  background: linear-gradient(90deg, rgba(238, 184, 91, 0.18), rgba(238, 184, 91, 0.5), rgba(238, 184, 91, 0.18));
}

.stepper-node {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 5px;
  min-width: 0;
  text-align: center;
}

.stepper-dot {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(238, 184, 91, 0.46);
  border-radius: 50%;
  color: #ffe4ad;
  background: rgba(21, 12, 7, 0.92);
  font-size: 0.82rem;
  font-weight: 700;
  font-family: var(--font-display);
  transition: border-color 180ms ease, background 180ms ease, color 180ms ease, box-shadow 180ms ease;
}

.dot-check {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.stepper-node.done .stepper-dot {
  border-color: rgba(129, 184, 98, 0.7);
  color: #f1ffd6;
  background: rgba(67, 97, 58, 0.82);
}

.stepper-node.current .stepper-dot {
  border-color: var(--gold-bright);
  color: #2a180d;
  background: var(--gold-bright);
  animation: stepper-pulse 1900ms ease-in-out infinite;
}

.stepper-node.current.returned .stepper-dot {
  color: #ffe0da;
  background: #b5563a;
  border-color: rgba(204, 95, 65, 0.82);
}

.stepper-label {
  color: rgba(255, 231, 183, 0.7);
  font-family: var(--font-display);
  font-size: 0.9rem;
  line-height: 1.1;
  transition: color 180ms ease;
}

.stepper-node.current .stepper-label,
.stepper-node.done .stepper-label {
  color: #ffe8b9;
}

.stepper-place {
  color: rgba(255, 231, 183, 0.46);
  font-size: 0.7rem;
  line-height: 1.2;
}

.stepper-branch {
  margin-top: 2px;
  border: 1px solid rgba(204, 95, 65, 0.6);
  border-radius: 999px;
  padding: 1px 7px;
  color: #ffe0da;
  font-size: 0.68rem;
  background: rgba(110, 42, 36, 0.5);
}

@keyframes stepper-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(242, 192, 111, 0);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(242, 192, 111, 0.18), 0 0 20px rgba(242, 192, 111, 0.3);
  }
}

@media (max-width: 720px) {
  .quest-stepper {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .quest-stepper::before {
    display: none;
  }

  .stepper-node {
    grid-template-columns: 34px minmax(0, 1fr);
    justify-items: start;
    align-items: center;
    gap: 4px 10px;
    text-align: left;
  }

  .stepper-label {
    grid-column: 2;
  }

  .stepper-place {
    grid-column: 2;
  }

  .stepper-branch {
    grid-column: 2;
  }
}
</style>
