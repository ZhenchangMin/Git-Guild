<script setup>
const emit = defineEmits(['run-action'])

defineProps({
  task: {
    type: Object,
    default: null,
  },
  actions: {
    type: Array,
    default: () => [],
  },
  counterReady: {
    type: Boolean,
    default: false,
  },
  helper: {
    type: String,
    required: true,
  },
  tutorialSteps: {
    type: Array,
    default: () => [],
  },
  source: {
    type: String,
    required: true,
  },
  isActionReady: {
    type: Function,
    required: true,
  },
  getActionTitle: {
    type: Function,
    required: true,
  },
})

function getActionLabel(action) {
  if (action.type !== 'submit') return action.label
  if (action.label.includes('重新')) return '重新提交成果'
  return '去提交柜台'
}
</script>

<template>
  <article class="detail-card wide git-operation-card">
    <div class="link-ledger-head">
      <div>
        <p class="kicker">Git Actions</p>
        <h3>Git 操作区</h3>
      </div>
      <span v-if="task" class="status-pill" :class="{ warning: !counterReady }">
        {{ counterReady ? '可提交成果' : '待完成 Git 步骤' }}
      </span>
    </div>
    <p class="section-helper">{{ helper }}</p>
    <p v-if="task" class="next-action-note">当前建议：{{ task.nextStep }}</p>
    <ol v-if="tutorialSteps.length > 0" class="git-tutorial-list" aria-label="Git 操作教程">
      <li v-for="step in tutorialSteps" :key="step.title">
        <strong>{{ step.title }}</strong>
        <span>{{ step.body }}</span>
        <code v-if="step.command">{{ step.command }}</code>
      </li>
    </ol>
    <div class="card-actions detail-actions">
      <button
        v-for="action in actions"
        :key="action.label"
        :class="action.primary ? 'primary-action' : 'quiet-action'"
        type="button"
        :disabled="!isActionReady(action.type, task)"
        :title="getActionTitle(action.type, task)"
        @click="emit('run-action', action, source)"
      >
        {{ getActionLabel(action) }}
      </button>
    </div>
  </article>
</template>

<style scoped>
.detail-card {
  display: grid;
  align-content: start;
  gap: 12px;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 13px;
  background: rgba(11, 6, 3, 0.34);
}

.detail-card.wide {
  grid-column: 1 / -1;
}

.status-pill {
  display: inline-flex;
  width: fit-content;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 2px 8px;
  color: #ffe4ad;
  font-size: 0.72rem;
  background: rgba(80, 43, 18, 0.44);
}

.status-pill.warning {
  border-color: rgba(204, 95, 65, 0.72);
  color: #ffd7c9;
  background: rgba(110, 42, 36, 0.44);
}

.git-operation-card {
  border-color: rgba(238, 184, 91, 0.42);
  background:
    linear-gradient(135deg, rgba(93, 49, 16, 0.6), rgba(11, 6, 3, 0.44)),
    radial-gradient(circle at 8% 0%, rgba(255, 217, 138, 0.14), transparent 0 28%, transparent 58%);
}

.link-ledger-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.section-helper {
  margin: 0;
  color: rgba(255, 231, 183, 0.75);
  line-height: 1.45;
}

.next-action-note {
  border-left: 3px solid rgba(238, 184, 91, 0.76);
  margin: 0;
  padding: 8px 10px;
  color: #ffe4ad;
  background: rgba(80, 43, 18, 0.36);
  line-height: 1.4;
}

.git-tutorial-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.git-tutorial-list li {
  display: grid;
  gap: 5px;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 10px;
  background: rgba(7, 4, 2, 0.28);
}

.git-tutorial-list strong {
  color: #ffe8b9;
  line-height: 1.25;
}

.git-tutorial-list span {
  color: rgba(255, 231, 183, 0.74);
  line-height: 1.38;
}

.git-tutorial-list code {
  overflow-x: auto;
  border: 1px solid rgba(238, 184, 91, 0.22);
  border-radius: 4px;
  padding: 7px 8px;
  color: #ffe4ad;
  background: rgba(12, 7, 4, 0.55);
  font-size: 0.76rem;
  white-space: nowrap;
}
</style>
