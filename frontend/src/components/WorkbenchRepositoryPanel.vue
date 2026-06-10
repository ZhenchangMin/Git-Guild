<script setup>
const props = defineProps({
  repository: {
    type: Object,
    required: true,
  },
  task: {
    type: Object,
    default: null,
  },
  variant: {
    type: String,
    default: 'task',
  },
})

const isWarning = () => props.repository.syncStatus?.toLowerCase().includes('warning')
</script>

<template>
  <template v-if="variant === 'repository'">
    <article class="repository-overview-card">
      <div>
        <p class="kicker">Repository Workspace</p>
        <h3>{{ repository.name }}</h3>
        <p>用于查看仓库状态、创建分支、上传文件生成 commit，并从这里发起 PR。这里不提供在线代码编辑器。</p>
      </div>
      <div class="repository-sync-card" :class="{ warning: isWarning() }">
        <span>同步状态</span>
        <strong>{{ repository.syncStatus }}</strong>
      </div>
    </article>

    <div class="repo-metric-grid" aria-label="仓库关键指标">
      <article class="repo-metric">
        <span>默认分支</span>
        <strong>{{ repository.defaultBranch }}</strong>
      </article>
      <article class="repo-metric">
        <span>分支</span>
        <strong>{{ repository.branches }}</strong>
      </article>
      <article class="repo-metric">
        <span>Issue</span>
        <strong>{{ repository.issues }}</strong>
      </article>
      <article class="repo-metric">
        <span>PR</span>
        <strong>{{ repository.pullRequests }}</strong>
      </article>
      <article class="repo-metric">
        <span>最近 commit</span>
        <strong>{{ repository.lastCommit }}</strong>
      </article>
    </div>
  </template>

  <article v-else class="detail-card wide repository-context-card">
    <div class="link-ledger-head">
      <div>
        <p class="kicker">Repository Context</p>
        <h3>当前任务仓库</h3>
      </div>
      <span class="status-pill" :class="{ warning: isWarning() }">
        {{ repository.syncStatus }}
      </span>
    </div>
    <p class="section-helper">
      这里确认本次任务所在仓库和同步状态。工作台只上传文件并生成 commit，不提供网页代码编辑器。
    </p>
    <p v-if="isWarning()" class="empty-state-note warning-note">
      当前仓库同步存在预警，Issue 或 PR 状态可能不是最新。建议先打开仓库视图并手动同步，再继续提交链路。
    </p>
    <dl class="link-ledger-grid">
      <div>
        <dt>仓库</dt>
        <dd>{{ repository.name }}</dd>
      </div>
      <div>
        <dt>默认分支</dt>
        <dd>{{ repository.defaultBranch }}</dd>
      </div>
      <div>
        <dt>关联 Issue</dt>
        <dd>{{ task?.issue ?? '未关联' }}</dd>
      </div>
      <div>
        <dt>最近 commit</dt>
        <dd>{{ repository.lastCommit }}</dd>
      </div>
      <div>
        <dt>分支 / Issue / PR</dt>
        <dd>{{ repository.branches }} / {{ repository.issues }} / {{ repository.pullRequests }}</dd>
      </div>
      <div>
        <dt>下一步</dt>
        <dd>{{ task?.nextStep ?? '等待选择任务' }}</dd>
      </div>
    </dl>
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

.repository-overview-card {
  grid-area: overview;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(170px, 220px);
  gap: 14px;
  align-items: stretch;
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 7px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.56), rgba(11, 6, 3, 0.44)),
    radial-gradient(circle at 90% 10%, rgba(255, 217, 138, 0.16), transparent 0 28%, transparent 54%);
}

.repository-overview-card h3 {
  margin: 2px 0 8px;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: 1.42rem;
}

.repository-overview-card p,
.section-helper {
  margin: 0;
  color: rgba(255, 231, 183, 0.75);
  line-height: 1.48;
}

.repository-sync-card {
  display: grid;
  align-content: center;
  gap: 8px;
  border: 1px solid rgba(238, 184, 91, 0.32);
  border-radius: 6px;
  padding: 14px;
  background: rgba(7, 4, 2, 0.34);
}

.repository-sync-card.warning {
  border-color: rgba(204, 95, 65, 0.68);
  background: rgba(79, 30, 24, 0.38);
}

.repository-sync-card span,
.repo-metric span {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.76rem;
}

.repository-sync-card strong,
.repo-metric strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.08rem;
}

.repo-metric-grid {
  grid-area: metrics;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.repo-metric {
  display: grid;
  gap: 5px;
  min-width: 0;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 11px 12px;
  background: rgba(11, 6, 3, 0.34);
}

.repo-metric strong,
.link-ledger-grid dd {
  overflow-wrap: anywhere;
}

.repository-context-card {
  border-color: rgba(129, 184, 98, 0.3);
  background:
    linear-gradient(135deg, rgba(37, 58, 33, 0.46), rgba(11, 6, 3, 0.42)),
    radial-gradient(circle at 90% 0%, rgba(154, 214, 112, 0.12), transparent 0 30%, transparent 58%);
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

.empty-state-note {
  border: 1px dashed rgba(240, 198, 118, 0.28);
  border-radius: 5px;
  margin: 0;
  padding: 10px;
  color: rgba(255, 231, 183, 0.72);
  background: rgba(7, 4, 2, 0.24);
}

.empty-state-note.warning-note {
  border-color: rgba(204, 95, 65, 0.62);
  color: #ffd7c9;
  background: rgba(88, 31, 23, 0.3);
}

@media (max-width: 940px) {
  .repository-overview-card {
    grid-template-columns: 1fr;
  }

  .repo-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .link-ledger-grid {
    grid-template-columns: 1fr;
  }
}
</style>
