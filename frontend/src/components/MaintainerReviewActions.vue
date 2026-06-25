<script setup>
import { computed, reactive, ref, watch } from 'vue'

import { maintainerReviewDecisionOptions } from '../data/maintainerReview'

const props = defineProps({
  review: {
    type: Object,
    required: true,
  },
  result: {
    type: Object,
    default: null,
  },
  busy: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['submit-review', 'save-draft'])

const form = reactive({
  decision: 'CHANGES_REQUESTED',
  summary: '',
  approveConfirmed: false,
})

// 二次确认弹窗开关
const confirmOpen = ref(false)

const selectedDecision = computed(
  () => maintainerReviewDecisionOptions.find((option) => option.decision === form.decision) ?? maintainerReviewDecisionOptions[0],
)

const isApprove = computed(() => form.decision === 'APPROVED')
const isReject = computed(() => form.decision === 'REJECTED')

// 意见框文案：退回修改填「修改意见」，驳回提交填「驳回原因」
const opinionLabel = computed(() => (isReject.value ? '驳回原因' : '修改意见'))
// 占位提示：优先用系统给出的建议总结作为底层提示，缺省再用通用文案
const opinionPlaceholder = computed(() => {
  if (props.review.suggestedSummary?.trim()) return props.review.suggestedSummary.trim()
  return isReject.value
    ? '说明驳回该提交的原因，冒险家会看到这条说明'
    : '写明需要修改的地方，冒险家会据此修改后重新提交'
})

// 主操作是否可提交：通过需勾选确认，退回/驳回需填写意见
const canSubmit = computed(() => {
  if (isApprove.value) return form.approveConfirmed
  return Boolean(form.summary.trim())
})

const confirmTitle = computed(() => {
  if (isApprove.value) return '确认通过这条提交？'
  if (isReject.value) return '确认驳回这条提交？'
  return '确认退回修改？'
})

const confirmBody = computed(() => {
  if (isApprove.value) {
    return '通过后任务将标记完成并发放 XP；PR 不会自动合并，如需合并请在左侧详情区点「合并 PR」。'
  }
  if (isReject.value) {
    return '驳回后该提交将被关闭，请确认驳回原因已写清楚。冒险家会看到这条原因。'
  }
  return '将把提交退回给冒险家并附上你的修改意见，等待其修改后重新提交。'
})

function resetForm(review) {
  form.decision = review.completionCriteria.every((item) => item.passed) ? 'APPROVED' : 'CHANGES_REQUESTED'
  // 不再预填总结文字：改为以建议总结作为文本框底层提示（placeholder），保持输入框为空
  form.summary = ''
  form.approveConfirmed = false
}

function selectDecision(decision) {
  form.decision = decision
}

function buildPayload() {
  // 后端要求 summary 非空：通过时若未额外填写，则落一句默认确认语。
  const summary = isApprove.value
    ? form.summary.trim() || '已确认 PR 与 Issue，审核通过。'
    : form.summary.trim()
  return {
    decision: form.decision,
    summary,
    items: props.review.completionCriteria.map((item) => ({
      checkpoint: item.checkpoint,
      passed: isApprove.value ? true : item.passed,
      comment: item.comment,
    })),
  }
}

function requestConfirm() {
  if (!canSubmit.value || props.busy) return
  confirmOpen.value = true
}

function cancelConfirm() {
  if (props.busy) return
  confirmOpen.value = false
}

function confirmSubmit() {
  if (props.busy) return
  confirmOpen.value = false
  emit('submit-review', buildPayload())
}

watch(
  () => props.review.id,
  () => {
    resetForm(props.review)
    confirmOpen.value = false
  },
  { immediate: true },
)
</script>

<template>
  <aside class="maintainer-review-actions">
    <div class="action-head">
      <h2>审核反馈</h2>
    </div>

    <div class="decision-list" role="radiogroup" aria-label="审核结论" data-tutorial="review-decision">
      <button
        v-for="option in maintainerReviewDecisionOptions"
        :key="option.decision"
        type="button"
        role="radio"
        :aria-checked="form.decision === option.decision"
        :class="{ active: form.decision === option.decision, danger: option.decision === 'REJECTED' }"
        @click="selectDecision(option.decision)"
      >
        <strong>{{ option.label }}</strong>
        <small>{{ option.helper }}</small>
      </button>
    </div>

    <!-- 审核通过：不再保留下方各框，仅确认是否已查看 PR 与 Issue -->
    <div v-if="isApprove" class="approve-confirm-box" data-tutorial="review-feedback">
      <p class="approve-hint">通过前请确认你已经查看过对应的 <strong>PR</strong> 与 <strong>Issue</strong>。</p>
      <label class="approve-check">
        <input v-model="form.approveConfirmed" type="checkbox" />
        <span>我已确认 PR 与 Issue 无误</span>
      </label>
    </div>

    <!-- 退回修改 / 驳回提交：一个意见框 -->
    <label v-else class="opinion-field" :class="{ danger: isReject }" data-tutorial="review-feedback">
      <span>{{ opinionLabel }}<em>必填</em></span>
      <textarea v-model="form.summary" rows="6" :placeholder="opinionPlaceholder"></textarea>
    </label>

    <div class="action-buttons">
      <div class="action-button-target" data-tutorial="review-submit-action">
      <button class="quiet-action" type="button" :disabled="busy" @click="emit('save-draft', buildPayload())">
        保存草稿
      </button>
      <button
        class="primary-action"
        :class="{ 'danger-action': isReject }"
        type="button"
        :disabled="busy || !canSubmit"
        @click="requestConfirm"
      >
        {{ busy ? '提交中...' : selectedDecision.label }}
      </button>
      </div>
    </div>

    <section v-if="result" class="review-result" :class="result.tone">
      <strong>{{ result.title }}</strong>
      <p>{{ result.body }}</p>
    </section>

    <!-- 二次确认弹窗：取消在左、确认在右；驳回用红色基调 -->
    <transition name="reject-pop">
      <div
        v-if="confirmOpen"
        class="confirm-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="review-confirm-title"
        @click.self="cancelConfirm"
      >
        <div class="confirm-card" :class="{ danger: isReject }">
          <button class="confirm-close" type="button" aria-label="关闭" :disabled="busy" @click="cancelConfirm">×</button>
          <span class="confirm-icon" aria-hidden="true">{{ isApprove ? '✓' : isReject ? '⊘' : '✎' }}</span>
          <p class="kicker">{{ isApprove ? 'Confirm Approval' : isReject ? 'Confirm Reject' : 'Confirm Return' }}</p>
          <h2 id="review-confirm-title">{{ confirmTitle }}</h2>
          <p class="confirm-quest">{{ review.questTitle }}</p>

          <div class="confirm-body-box">
            <p class="confirm-body">{{ confirmBody }}</p>
            <p v-if="!isApprove" class="confirm-opinion">{{ opinionLabel }}：{{ form.summary.trim() }}</p>
          </div>

          <div class="confirm-actions">
            <button class="quiet-action" type="button" :disabled="busy" @click="cancelConfirm">取消</button>
            <button
              class="primary-action"
              :class="{ 'danger-action': isReject }"
              type="button"
              :disabled="busy"
              @click="confirmSubmit"
            >
              {{ busy ? '提交中...' : selectedDecision.label }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </aside>
</template>

<style scoped>
.maintainer-review-actions {
  display: grid;
  align-content: start;
  gap: 14px;
  min-height: 0;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 10px;
  padding: 16px;
  background:
    linear-gradient(180deg, rgba(35, 18, 8, 0.8), rgba(9, 4, 2, 0.64)),
    radial-gradient(circle at 90% 0%, rgba(255, 216, 133, 0.13), transparent 0 32%);
  box-shadow: 0 20px 55px rgba(0, 0, 0, 0.35);
  overflow: auto;
}

.action-head h2 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
}

.decision-list {
  display: grid;
  gap: 9px;
}

.decision-list button {
  display: grid;
  gap: 5px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.22);
  border-radius: 8px;
  padding: 11px;
  color: rgba(255, 231, 183, 0.74);
  text-align: left;
  background: rgba(7, 4, 2, 0.34);
  transition: border-color 160ms ease, background 160ms ease, transform 160ms ease;
}

.decision-list button:hover,
.decision-list button:focus-visible,
.decision-list button.active {
  border-color: rgba(255, 224, 157, 0.78);
  color: #ffe8b9;
  background: rgba(84, 45, 16, 0.54);
  transform: translateY(-1px);
}

.decision-list button.danger.active,
.decision-list button.danger:hover,
.decision-list button.danger:focus-visible {
  border-color: rgba(232, 132, 104, 0.82);
  background: rgba(96, 38, 26, 0.58);
}

.decision-list strong {
  color: #ffe2a0;
}

.decision-list button.danger.active strong {
  color: #ffcdbb;
}

.decision-list small {
  line-height: 1.38;
}

/* ── 审核通过：确认 PR / Issue ── */
.approve-confirm-box {
  display: grid;
  gap: 12px;
  border: 1px solid rgba(169, 208, 123, 0.34);
  border-radius: 10px;
  padding: 14px 15px;
  background: rgba(28, 40, 18, 0.4);
}

.approve-hint {
  margin: 0;
  color: rgba(231, 244, 205, 0.86);
  line-height: 1.6;
}

.approve-hint strong {
  color: #d7eeb0;
}

.approve-check {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #e7f4cd;
  cursor: pointer;
}

.approve-check input {
  width: 17px;
  height: 17px;
  accent-color: #8fbf62;
  cursor: pointer;
}

/* ── 退回修改 / 驳回提交：意见框 ── */
.opinion-field {
  display: grid;
  gap: 7px;
}

.opinion-field span {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: rgba(255, 231, 183, 0.78);
}

.opinion-field em {
  font-style: normal;
  font-size: 0.74rem;
  color: rgba(240, 160, 109, 0.92);
}

.opinion-field.danger em {
  color: #ff9f86;
}

.opinion-field textarea {
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 8px;
  padding: 10px 11px;
  color: #ffe8b9;
  font: inherit;
  line-height: 1.45;
  resize: vertical;
  background: rgba(7, 4, 2, 0.42);
}

.opinion-field textarea:focus {
  border-color: rgba(255, 224, 157, 0.78);
  outline: none;
}

.opinion-field.danger textarea {
  border-color: rgba(220, 130, 110, 0.42);
}

.opinion-field.danger textarea:focus {
  border-color: rgba(232, 132, 104, 0.85);
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
}

.action-button-target {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 9px;
  width: fit-content;
  max-width: 100%;
}

.action-buttons button {
  min-height: 40px;
}

/* 红色破坏性按钮（驳回提交） */
.danger-action {
  border: 1px solid rgba(238, 120, 82, 0.62);
  border-radius: 8px;
  padding: 0 16px;
  color: #ffe7d2;
  background: linear-gradient(180deg, #d8634a, #962f1f);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.36);
  cursor: pointer;
  transition: transform 150ms ease, filter 150ms ease;
}

.danger-action:hover:not(:disabled),
.danger-action:focus-visible:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.danger-action:disabled {
  cursor: not-allowed;
  filter: grayscale(0.3);
  opacity: 0.65;
  transform: none;
}

.review-result {
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 8px;
  padding: 12px;
  background: rgba(7, 4, 2, 0.3);
}

.review-result.approved {
  border-color: rgba(169, 208, 123, 0.46);
}

.review-result.warning {
  border-color: rgba(240, 160, 109, 0.5);
}

.review-result strong {
  color: #ffe2a0;
}

.review-result p {
  margin: 7px 0 0;
  color: rgba(255, 231, 183, 0.72);
  line-height: 1.5;
}

/* ── 二次确认弹窗（站内统一风格） ── */
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

.confirm-card.danger {
  border-color: rgba(220, 130, 110, 0.42);
  background: linear-gradient(168deg, rgba(60, 26, 20, 0.96), rgba(28, 14, 9, 0.98));
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
  background: rgba(150, 110, 48, 0.34);
  color: #f3d091;
  font-size: 1.5rem;
}

.confirm-card.danger .confirm-icon {
  background: rgba(150, 56, 48, 0.34);
  color: #f3a691;
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
}

.confirm-body-box {
  text-align: left;
  padding: 14px 16px;
  border: 1px solid rgba(240, 198, 118, 0.28);
  border-radius: 10px;
  background: rgba(15, 8, 5, 0.5);
}

.confirm-card.danger .confirm-body-box {
  border-color: rgba(220, 130, 110, 0.28);
}

.confirm-body {
  margin: 0;
  font-size: 0.94rem;
  line-height: 1.7;
  color: #ffe6cf;
}

.confirm-opinion {
  margin: 10px 0 0;
  padding-top: 10px;
  border-top: 1px dashed rgba(240, 198, 118, 0.24);
  font-size: 0.9rem;
  line-height: 1.6;
  color: rgba(255, 226, 190, 0.78);
  white-space: pre-wrap;
  word-break: break-word;
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
</style>
