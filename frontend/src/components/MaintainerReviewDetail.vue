<script setup>
import { ref } from 'vue'

const props = defineProps({
  review: {
    type: Object,
    required: true,
  },
  merging: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits(['merge-pr'])

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

// 佐证文件是否为图片：是则在审核台直接出缩略图预览，否则按文档走下载。
function isImageEvidence(file) {
  return (file?.mimeType || '').startsWith('image/')
}

// 把 base64 的 data: URL 还原成 Blob。
// 直接用 <a target="_blank" href="data:..."> 打开会被现代浏览器拦截（落到 about:blank），
// 必须先转成 blob: URL 再打开 / 下载。
function dataUrlToBlob(dataUrl) {
  const [header, base64 = ''] = String(dataUrl).split(',')
  const mimeMatch = header.match(/data:([^;]+)/)
  const mime = mimeMatch ? mimeMatch[1] : 'application/octet-stream'
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i)
  }
  return new Blob([bytes], { type: mime })
}

// 打开佐证文件：图片在新标签预览，文档触发下载。统一走 blob: URL 规避 data: 拦截。
function openEvidence(file) {
  if (!file?.content) return

  const content = String(file.content)
  // 兼容历史数据：若已是普通链接（http/blob），直接打开。
  if (!content.startsWith('data:')) {
    window.open(content, '_blank', 'noopener')
    return
  }

  let objectUrl
  try {
    objectUrl = URL.createObjectURL(dataUrlToBlob(content))
  } catch {
    return
  }

  if (isImageEvidence(file)) {
    window.open(objectUrl, '_blank', 'noopener')
  } else {
    const anchor = document.createElement('a')
    anchor.href = objectUrl
    anchor.download = file.name || 'evidence'
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
  }

  // 留足新标签 / 下载读取时间后回收，避免内存泄漏。
  setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
}

// 合并 PR 二次确认弹窗开关
const mergeConfirmOpen = ref(false)

function requestMerge() {
  if (props.merging) return
  mergeConfirmOpen.value = true
}

function cancelMerge() {
  if (props.merging) return
  mergeConfirmOpen.value = false
}

function confirmMerge() {
  if (props.merging) return
  mergeConfirmOpen.value = false
  emit('merge-pr')
}
</script>

<template>
  <section class="maintainer-review-detail">
    <div class="detail-hero" data-tutorial="review-overview" data-tutorial-boundary="viewport">
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
          <div class="gitea-link-target" data-tutorial="review-links" data-tutorial-boundary="viewport">
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
      </div>
      <aside>
        <span>{{ review.status }}</span>
        <strong>{{ review.rewardXp }} XP</strong>
      </aside>
    </div>

    <div class="detail-grid">
      <article class="detail-card deliverable-card" data-tutorial="review-deliverables" data-tutorial-boundary="viewport">
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
        <div v-if="review.evidenceFiles?.length" class="deliverable-block">
          <span class="deliverable-label">冒险家上传的佐证文件（{{ review.evidenceFiles.length }}）</span>
          <ul class="evidence-file-grid">
            <li v-for="(file, idx) in review.evidenceFiles" :key="idx" class="evidence-file">
              <button
                type="button"
                class="evidence-file-link"
                :title="isImageEvidence(file) ? `点击查看大图：${file.name}` : `点击下载：${file.name}`"
                @click="openEvidence(file)"
              >
                <img
                  v-if="isImageEvidence(file)"
                  class="evidence-thumb"
                  :src="file.content"
                  :alt="file.name"
                />
                <span v-else class="evidence-doc-glyph" aria-hidden="true">⤓</span>
                <span class="evidence-file-name">{{ file.name }}</span>
              </button>
            </li>
          </ul>
        </div>
      </article>

      <article class="detail-card pr-card" data-tutorial="review-pr-status" data-tutorial-boundary="viewport">
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
            <button
              class="pr-merge-btn"
              type="button"
              data-tutorial="review-merge"
              data-tutorial-boundary="viewport"
              :disabled="merging"
              @click="requestMerge"
            >
              {{ merging ? '合并中…' : '合并 PR' }}
            </button>
            <small>接受提交不会自动合并；是否合并由你决定。</small>
          </template>
          <small v-else>无可合并的 PR。</small>
        </div>
      </article>
    </div>

    <!-- 合并 PR 二次确认：取消在左、确认在右，站内统一弹窗风格 -->
    <transition name="reject-pop">
      <div
        v-if="mergeConfirmOpen"
        class="confirm-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="merge-confirm-title"
        @click.self="cancelMerge"
      >
        <div class="confirm-card">
          <button class="confirm-close" type="button" aria-label="关闭" :disabled="merging" @click="cancelMerge">×</button>
          <span class="confirm-icon" aria-hidden="true">⤥</span>
          <p class="kicker">Confirm Merge</p>
          <h2 id="merge-confirm-title">确认合并这个 PR？</h2>
          <p class="confirm-quest">{{ review.pullRequest }} · {{ review.pullRequestTitle }}</p>

          <div class="confirm-body-box">
            <p class="confirm-body">
              合并后 <strong>{{ review.branch }}</strong> 的改动将进入目标分支，该操作不可在平台撤销，请确认无误。
            </p>
          </div>

          <div class="confirm-actions">
            <button class="quiet-action" type="button" :disabled="merging" @click="cancelMerge">取消</button>
            <button class="primary-action" type="button" :disabled="merging" @click="confirmMerge">
              {{ merging ? '合并中…' : '确认合并' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
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
  margin-top: 14px;
}

.gitea-link-target {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 20px;
  width: fit-content;
  max-width: 100%;
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

.evidence-file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(112px, 1fr));
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.evidence-file-link {
  display: grid;
  gap: 6px;
  justify-items: center;
  width: 100%;
  padding: 8px;
  border: 1px solid rgba(240, 198, 118, 0.28);
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
  font: inherit;
  cursor: pointer;
  background: rgba(7, 4, 2, 0.34);
  transition: border-color 150ms ease, transform 120ms ease, box-shadow 150ms ease;
}

.evidence-file-link:hover,
.evidence-file-link:focus-visible {
  border-color: rgba(255, 224, 157, 0.7);
  transform: translateY(-1px);
  outline: none;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.36);
}

.evidence-thumb {
  width: 100%;
  height: 72px;
  object-fit: cover;
  border-radius: 5px;
  background: rgba(0, 0, 0, 0.3);
}

.evidence-doc-glyph {
  display: grid;
  place-items: center;
  width: 100%;
  height: 72px;
  border-radius: 5px;
  color: #ffe4ad;
  font-size: 1.6rem;
  background: rgba(80, 43, 18, 0.5);
}

.evidence-file-name {
  width: 100%;
  color: rgba(255, 233, 187, 0.86);
  font-size: 0.74rem;
  line-height: 1.3;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

/* ── 合并 PR 二次确认弹窗（站内统一风格） ── */
.confirm-modal {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(10, 5, 3, 0.62);
  backdrop-filter: blur(2px);
}

.confirm-card {
  position: relative;
  width: min(460px, 100%);
  padding: 34px 30px 26px;
  text-align: center;
  border: 1px solid rgba(238, 184, 91, 0.46);
  border-radius: 14px;
  color: #ffe9c4;
  background: linear-gradient(168deg, rgba(54, 30, 12, 0.96), rgba(24, 13, 6, 0.98));
  box-shadow: 0 26px 64px rgba(0, 0, 0, 0.55);
}

.confirm-close {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 224, 178, 0.7);
  font-size: 1.4rem;
  line-height: 1;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease;
}

.confirm-close:hover:not(:disabled) {
  background: rgba(240, 198, 118, 0.18);
  color: #ffe0b0;
}

.confirm-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 8px;
  border-radius: 50%;
  background: rgba(86, 120, 60, 0.34);
  color: #c4e29a;
  font-size: 1.5rem;
}

.kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: rgba(255, 224, 178, 0.6);
}

.confirm-card h2 {
  margin: 8px 0 4px;
  font-family: var(--font-display);
  color: #ffe8c8;
}

.confirm-quest {
  margin: 0 0 16px;
  color: rgba(255, 230, 190, 0.7);
  font-size: 0.92rem;
  overflow-wrap: anywhere;
}

.confirm-body-box {
  text-align: left;
  padding: 14px 16px;
  border: 1px solid rgba(240, 198, 118, 0.28);
  border-radius: 10px;
  background: rgba(15, 8, 5, 0.5);
}

.confirm-body {
  margin: 0;
  font-size: 0.94rem;
  line-height: 1.7;
  color: #ffe6cf;
}

.confirm-body strong {
  color: #ffd79a;
  overflow-wrap: anywhere;
}

.confirm-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
}

.confirm-actions button {
  min-height: 42px;
}

.reject-pop-enter-active,
.reject-pop-leave-active {
  transition: opacity 180ms ease;
}

.reject-pop-enter-active .confirm-card,
.reject-pop-leave-active .confirm-card {
  transition: transform 180ms cubic-bezier(0.2, 0.9, 0.3, 1.2), opacity 180ms ease;
}

.reject-pop-enter-from,
.reject-pop-leave-to {
  opacity: 0;
}

.reject-pop-enter-from .confirm-card,
.reject-pop-leave-to .confirm-card {
  transform: translateY(12px) scale(0.96);
  opacity: 0;
}

@media (max-width: 980px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }
}
</style>
