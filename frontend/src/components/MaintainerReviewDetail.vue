<script setup>
defineProps({
  review: {
    type: Object,
    required: true,
  },
  merging: {
    type: Boolean,
    default: false,
  },
})
defineEmits(['merge-pr'])

const PR_STATE_LABELS = {
  OPEN: '待合并',
  MERGED: '已合并',
  CLOSED: '已关闭',
  DRAFT: '草稿',
  UNKNOWN: '未知',
}

function prStateLabel(state) {
  return PR_STATE_LABELS[state] ?? state ?? '未知'
}
</script>

<template>
  <section class="maintainer-review-detail">
    <div class="detail-hero">
      <div>
        <h1>{{ review.questTitle }}</h1>
        <dl class="hero-meta">
          <div>
            <dt>提交人</dt>
            <dd>{{ review.submitter }}</dd>
          </div>
          <div>
            <dt>受托仓库</dt>
            <dd>{{ review.repository }}</dd>
          </div>
          <div>
            <dt>提交时间</dt>
            <dd>{{ review.submittedAt }}</dd>
          </div>
        </dl>
        <div class="gitea-link-row">
          <a
            v-if="review.pullRequestUrl"
            class="gitea-btn"
            :href="review.pullRequestUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            前往 Gitea 查看 PR ↗
          </a>
          <a
            v-if="review.repositoryBranchUrl"
            class="gitea-btn"
            :href="review.repositoryBranchUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            前往分支查看提交 ↗
          </a>
        </div>
      </div>
      <aside>
        <span>{{ review.status }}</span>
        <strong>{{ review.rewardXp }} XP</strong>
      </aside>
    </div>

    <div class="detail-grid">
      <article class="detail-card deliverable-card">
        <h2>成果材料</h2>
        <div class="deliverable-block">
          <span class="deliverable-label">成果说明</span>
          <p class="deliverable-note">{{ review.summary }}</p>
        </div>
        <div v-if="review.evidence.length" class="deliverable-block">
          <span class="deliverable-label">佐证材料</span>
          <ul class="deliverable-evidence">
            <li v-for="item in review.evidence" :key="item">{{ item }}</li>
          </ul>
        </div>
      </article>

      <article class="detail-card pr-card">
        <h2>PR 状态</h2>
        <dl>
          <div>
            <dt>Pull Request</dt>
            <dd>{{ review.pullRequest }} · {{ review.pullRequestTitle }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ prStateLabel(review.prState) }}</dd>
          </div>
          <div>
            <dt>分支</dt>
            <dd>{{ review.branch }}</dd>
          </div>
        </dl>
        <div class="pr-merge-row">
          <span v-if="review.prState === 'MERGED'" class="pr-merged-badge">已合并</span>
          <template v-else-if="review.prState === 'OPEN'">
            <button class="pr-merge-btn" type="button" :disabled="merging" @click="$emit('merge-pr')">
              {{ merging ? '合并中…' : '合并 PR' }}
            </button>
            <small>接受提交不会自动合并；是否合并由你决定。</small>
          </template>
          <small v-else>无可合并的 PR。</small>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.maintainer-review-detail {
  display: grid;
  gap: 14px;
  min-width: 0;
  min-height: 0;
  overflow: auto;
}

.detail-hero,
.detail-card {
  border: 1px solid rgba(238, 184, 91, 0.36);
  border-radius: 10px;
  box-shadow: 0 18px 52px rgba(0, 0, 0, 0.32), inset 0 1px 0 rgba(255, 235, 180, 0.12);
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 176px;
  gap: 18px;
  padding: 20px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.72), rgba(11, 6, 3, 0.58)),
    radial-gradient(circle at 94% 12%, rgba(255, 217, 138, 0.16), transparent 0 31%);
}

.detail-hero h1 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: clamp(1.65rem, 2.6vw, 2.2rem);
  line-height: 1.12;
  letter-spacing: -0.012em;
  text-wrap: balance;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 30px;
  margin: 16px 0 0;
}

.hero-meta div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.hero-meta dt {
  color: rgba(255, 231, 183, 0.56);
  font-size: 0.76rem;
  letter-spacing: 0.03em;
}

.hero-meta dd {
  margin: 0;
  color: #ffe9bb;
  font-size: 1rem;
  line-height: 1.3;
  overflow-wrap: anywhere;
}

.gitea-link-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  margin-top: 14px;
}

.gitea-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 46px;
  padding: 0 22px;
  border: 1px solid rgba(238, 184, 91, 0.56);
  border-radius: 6px;
  color: #ffe4ad;
  font-family: var(--font-display);
  font-size: 0.94rem;
  white-space: nowrap;
  text-decoration: none;
  cursor: pointer;
  background: linear-gradient(180deg, rgba(80, 43, 18, 0.74), rgba(50, 24, 8, 0.84));
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.34);
  transition: filter 150ms ease, transform 120ms ease, box-shadow 150ms ease;
}

.gitea-btn:hover,
.gitea-btn:focus-visible {
  filter: brightness(1.14);
  transform: translateY(-1px);
  outline: none;
  box-shadow: 0 0 0 4px rgba(255, 204, 105, 0.14), 0 8px 20px rgba(0, 0, 0, 0.4);
}

.gitea-btn:active {
  transform: translateY(0) scale(0.98);
}

.detail-hero aside {
  display: grid;
  align-content: center;
  gap: 8px;
  border: 1px solid rgba(255, 222, 161, 0.24);
  border-radius: 12px;
  padding: 14px;
  background: rgba(7, 4, 2, 0.34);
}

.detail-hero aside span {
  color: rgba(255, 231, 183, 0.66);
  font-size: 0.9rem;
}

.detail-hero aside strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.7rem;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  grid-template-areas:
    "deliverable"
    "pr";
  gap: 14px;
}

.detail-card {
  padding: 15px;
  background: rgba(10, 5, 2, 0.5);
}

.deliverable-card {
  grid-area: deliverable;
}

.deliverable-block {
  margin-top: 12px;
}

.deliverable-label {
  display: inline-block;
  margin-bottom: 6px;
  color: rgba(255, 231, 183, 0.56);
  font-size: 0.78rem;
  letter-spacing: 0.03em;
}

.deliverable-note {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid rgba(240, 198, 118, 0.16);
  border-radius: 8px;
  color: rgba(255, 233, 187, 0.9);
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: rgba(7, 4, 2, 0.34);
}

.deliverable-evidence {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  color: rgba(255, 231, 183, 0.78);
  list-style: none;
}

.deliverable-evidence li {
  border-left: 2px solid rgba(240, 198, 118, 0.3);
  padding-left: 10px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.pr-card {
  grid-area: pr;
}

.pr-merge-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.pr-merge-row small {
  color: rgba(255, 231, 183, 0.58);
  font-size: 0.78rem;
}

.pr-merge-btn {
  border: 1px solid rgba(169, 208, 123, 0.6);
  border-radius: 8px;
  padding: 8px 16px;
  color: #18260c;
  background: linear-gradient(180deg, #c4e29a, #8fbf62);
  font-weight: 800;
  cursor: pointer;
  transition: transform 150ms ease, box-shadow 150ms ease, opacity 150ms ease;
}

.pr-merge-btn:hover:not(:disabled),
.pr-merge-btn:focus-visible:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.3);
}

.pr-merge-btn:disabled {
  opacity: 0.6;
  cursor: progress;
}

.pr-merged-badge {
  border: 1px solid rgba(169, 208, 123, 0.5);
  border-radius: 999px;
  padding: 5px 12px;
  color: #cfe6ad;
  background: rgba(43, 74, 28, 0.5);
  font-weight: 700;
  font-size: 0.82rem;
}

.criteria-card {
  grid-area: criteria;
}

.evidence-card {
  grid-area: evidence;
}

.detail-card h2 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: 1.18rem;
}

.detail-card dl {
  display: grid;
  gap: 9px;
  margin: 12px 0 0;
}

.detail-card dl div {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 12px;
}

.detail-card dt {
  color: rgba(255, 231, 183, 0.58);
}

.detail-card dd {
  margin: 0;
  color: #ffe2a0;
  overflow-wrap: anywhere;
}

@media (max-width: 980px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }
}
</style>
