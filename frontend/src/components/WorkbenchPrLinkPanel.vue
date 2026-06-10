<script setup>
const emit = defineEmits(['select-task', 'open-pr'])

defineProps({
  task: {
    type: Object,
    default: null,
  },
  repository: {
    type: Object,
    default: null,
  },
  checks: {
    type: Array,
    default: () => [],
  },
  pullRequests: {
    type: Array,
    default: () => [],
  },
  repositoryTasks: {
    type: Array,
    default: () => [],
  },
  counterReady: {
    type: Boolean,
    default: false,
  },
  showRepository: {
    type: Boolean,
    default: false,
  },
  showTaskStrip: {
    type: Boolean,
    default: false,
  },
  emptyMode: {
    type: String,
    default: 'task',
  },
  resolvePullRequestTask: {
    type: Function,
    default: () => null,
  },
})
</script>

<template>
  <article v-if="task" class="detail-card link-ledger-card" :class="{ wide: emptyMode === 'task', 'repository-link-card': emptyMode === 'repository' }">
    <div class="link-ledger-head">
      <div>
        <p class="kicker">PR × Quest Ledger</p>
        <h3>PR 与任务联动</h3>
      </div>
      <span class="status-pill" :class="{ warning: !counterReady }">
        {{ task.counterLink }}
      </span>
    </div>

    <div v-if="showTaskStrip && repositoryTasks.length > 1" class="repository-task-strip">
      <button
        v-for="repositoryTask in repositoryTasks"
        :key="repositoryTask.id"
        class="quiet-action"
        type="button"
        @click="emit('select-task', repositoryTask)"
      >
        {{ repositoryTask.id }} · {{ repositoryTask.statusLabel }}
      </button>
    </div>

    <dl class="link-ledger-grid">
      <div>
        <dt>任务 ID</dt>
        <dd>{{ task.id }}</dd>
      </div>
      <div>
        <dt>关联 Issue</dt>
        <dd>{{ task.issue }}</dd>
      </div>
      <div>
        <dt>当前分支</dt>
        <dd>{{ task.branch || '未创建' }}</dd>
      </div>
      <div>
        <dt>最近 commit</dt>
        <dd>{{ task.recentCommit }}</dd>
      </div>
      <div>
        <dt>PR 编号 / 状态</dt>
        <dd>{{ task.prNumber || '未创建' }} · {{ task.prState }}</dd>
      </div>
      <div>
        <dt>检查结果</dt>
        <dd>{{ task.checkResult }}</dd>
      </div>
      <div v-if="showRepository">
        <dt>关联仓库</dt>
        <dd>{{ repository?.name ?? task.repository }}</dd>
      </div>
      <div v-if="showRepository">
        <dt>提交柜台</dt>
        <dd>{{ task.counterLink }}</dd>
      </div>
    </dl>

    <div class="link-check-list">
      <section
        v-for="check in checks"
        :key="check.label"
        class="link-check-row"
        :class="{ passed: check.passed, blocked: !check.passed }"
      >
        <strong>{{ check.label }}</strong>
        <span>{{ check.passed ? '已就绪' : '待完成' }}</span>
        <p>{{ check.detail }}</p>
      </section>
    </div>

    <p class="link-ledger-note">
      <template v-if="emptyMode === 'repository'">
        当前仓库操作默认联动 {{ task.id }}。PR 是代码成果进入审核的入口，提交柜台负责登记课程任务成果。
      </template>
      <template v-else>
        PR 是代码成果进入审核的入口；平台会检查它是否关联当前任务、分支、commit 和提交柜台记录。
      </template>
    </p>

    <div v-if="pullRequests.length > 0" class="linked-pr-list">
      <button
        v-for="pr in pullRequests"
        :key="pr.id"
        class="linked-pr-row"
        type="button"
        @click="emit('open-pr', pr)"
      >
        <div>
          <strong>{{ pr.id }} · {{ pr.title }}</strong>
          <small v-if="emptyMode === 'repository'">{{ resolvePullRequestTask(pr)?.id ?? '未关联任务' }} · {{ pr.checks }}</small>
          <small v-else>{{ pr.checks }}</small>
        </div>
        <span>{{ pr.status }}</span>
      </button>
    </div>
    <p v-else class="empty-state-note">
      <template v-if="emptyMode === 'repository'">
        这个仓库当前没有可展示的 PR。可以先选择关联任务，再在 Git 操作区创建 PR。
      </template>
      <template v-else>
        当前任务还没有关联 PR。请先在 Git 操作区创建分支、上传 commit，再发起 PR。
      </template>
    </p>
  </article>

  <article v-else class="detail-card repository-link-card link-ledger-card">
    <div class="link-ledger-head">
      <div>
        <p class="kicker">PR × Quest Ledger</p>
        <h3>PR 与任务联动</h3>
      </div>
      <span class="status-pill warning">无关联任务</span>
    </div>
    <p class="empty-state-note">
      当前仓库还没有和工作台任务建立关联。Git 操作区只能查看仓库，不能创建任务分支、commit 或 PR。
    </p>
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

.link-ledger-card {
  gap: 14px;
  border-color: rgba(238, 184, 91, 0.34);
  background:
    linear-gradient(135deg, rgba(72, 38, 14, 0.62), rgba(11, 6, 3, 0.46)),
    radial-gradient(circle at 92% 0%, rgba(255, 217, 138, 0.13), transparent 0 30%, transparent 58%);
}

.link-ledger-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.link-ledger-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 9px;
  margin: 0;
}

.link-ledger-grid div {
  min-width: 0;
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 5px;
  padding: 9px 10px;
  background: rgba(7, 4, 2, 0.28);
}

.link-ledger-grid dd {
  overflow-wrap: anywhere;
}

.link-check-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.link-check-row {
  display: grid;
  gap: 5px;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 10px;
  background: rgba(7, 4, 2, 0.3);
}

.link-check-row.passed {
  border-color: rgba(111, 158, 87, 0.58);
  background: rgba(44, 73, 36, 0.24);
}

.link-check-row.blocked {
  border-color: rgba(204, 95, 65, 0.58);
  background: rgba(88, 31, 23, 0.28);
}

.link-check-row strong {
  color: #ffe8b9;
  line-height: 1.2;
}

.link-check-row span {
  width: fit-content;
  border: 1px solid rgba(238, 184, 91, 0.36);
  border-radius: 999px;
  padding: 2px 8px;
  color: #ffe4ad;
  font-size: 0.7rem;
  background: rgba(80, 43, 18, 0.42);
}

.link-check-row.passed span {
  border-color: rgba(129, 184, 98, 0.64);
  color: #dcf4c2;
  background: rgba(44, 91, 36, 0.42);
}

.link-check-row.blocked span {
  border-color: rgba(238, 120, 82, 0.66);
  color: #ffd7c9;
  background: rgba(110, 42, 36, 0.46);
}

.link-check-row p,
.link-ledger-note {
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.4;
}

.repository-task-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.linked-pr-list {
  display: grid;
  gap: 8px;
}

.linked-pr-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 9px 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(7, 4, 2, 0.32);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.linked-pr-row:hover,
.linked-pr-row:focus-visible {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.5);
  transform: translateY(-1px);
}

.linked-pr-row div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.linked-pr-row span {
  flex: 0 0 auto;
  color: rgba(255, 231, 183, 0.68);
  font-size: 0.78rem;
}

.linked-pr-row small {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.76rem;
  line-height: 1.32;
}

.empty-state-note {
  border: 1px dashed rgba(240, 198, 118, 0.28);
  border-radius: 5px;
  margin: 0;
  padding: 10px;
  color: rgba(255, 231, 183, 0.72);
  background: rgba(7, 4, 2, 0.24);
}

@media (max-width: 940px) {
  .link-ledger-grid,
  .link-check-list {
    grid-template-columns: 1fr;
  }
}
</style>
