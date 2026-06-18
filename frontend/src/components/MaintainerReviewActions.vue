<script setup>
import { computed, reactive, watch } from 'vue'

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

const emit = defineEmits(['submit-review', 'save-draft', 'open-pr'])

const form = reactive({
  decision: 'CHANGES_REQUESTED',
  summary: '',
  itemComments: {},
})

const selectedDecision = computed(
  () => maintainerReviewDecisionOptions.find((option) => option.decision === form.decision) ?? maintainerReviewDecisionOptions[0],
)

function resetForm(review) {
  form.decision = review.completionCriteria.every((item) => item.passed) ? 'APPROVED' : 'CHANGES_REQUESTED'
  form.summary = review.suggestedSummary
  form.itemComments = Object.fromEntries(
    review.completionCriteria.map((item) => [item.checkpoint, item.comment]),
  )
}

function buildPayload() {
  return {
    decision: form.decision,
    summary: form.summary.trim(),
    items: props.review.completionCriteria.map((item) => ({
      checkpoint: item.checkpoint,
      passed: form.decision === 'APPROVED' ? true : item.passed,
      comment: form.itemComments[item.checkpoint] ?? item.comment,
    })),
  }
}

watch(
  () => props.review.id,
  () => resetForm(props.review),
  { immediate: true },
)
</script>

<template>
  <aside class="maintainer-review-actions">
    <div class="action-head">
      <h2>审核反馈</h2>
    </div>

    <div class="decision-list" role="radiogroup" aria-label="审核结论">
      <button
        v-for="option in maintainerReviewDecisionOptions"
        :key="option.decision"
        type="button"
        :class="{ active: form.decision === option.decision }"
        @click="form.decision = option.decision"
      >
        <strong>{{ option.label }}</strong>
        <small>{{ option.helper }}</small>
      </button>
    </div>

    <label class="summary-field">
      <span>审核总结</span>
      <textarea v-model="form.summary" rows="5" placeholder="说明通过或退回修改的原因"></textarea>
    </label>

    <div class="item-feedback-list">
      <section v-for="item in review.completionCriteria" :key="item.checkpoint">
        <div>
          <strong>{{ item.checkpoint }}</strong>
          <span>{{ item.passed ? '默认通过' : '默认需修改' }}</span>
        </div>
        <textarea v-model="form.itemComments[item.checkpoint]" rows="3"></textarea>
      </section>
    </div>

    <div class="action-buttons">
      <button class="primary-action" type="button" :disabled="busy || !form.summary.trim()" @click="emit('submit-review', buildPayload())">
        {{ busy ? '提交中...' : selectedDecision.label }}
      </button>
      <button class="quiet-action" type="button" :disabled="busy" @click="emit('save-draft', buildPayload())">
        保存草稿
      </button>
      <button class="quiet-action" type="button" @click="emit('open-pr', review)">
        查看 PR
      </button>
    </div>

    <section v-if="result" class="review-result" :class="result.tone">
      <strong>{{ result.title }}</strong>
      <p>{{ result.body }}</p>
    </section>
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

.action-head p:last-child {
  margin: 8px 0 0;
  color: rgba(255, 231, 183, 0.66);
  line-height: 1.5;
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

.decision-list strong {
  color: #ffe2a0;
}

.decision-list small {
  line-height: 1.38;
}

.summary-field,
.item-feedback-list section {
  display: grid;
  gap: 7px;
}

.summary-field span {
  color: rgba(255, 231, 183, 0.76);
}

.summary-field textarea,
.item-feedback-list textarea {
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

.summary-field textarea:focus,
.item-feedback-list textarea:focus {
  border-color: rgba(255, 224, 157, 0.78);
  outline: none;
}

.item-feedback-list {
  display: grid;
  gap: 10px;
}

.item-feedback-list section {
  border: 1px solid rgba(240, 198, 118, 0.14);
  border-radius: 8px;
  padding: 10px;
  background: rgba(7, 4, 2, 0.24);
}

.item-feedback-list div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.item-feedback-list strong {
  color: #ffe8b9;
}

.item-feedback-list span {
  color: rgba(255, 231, 183, 0.56);
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.action-buttons button {
  min-height: 40px;
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
</style>
