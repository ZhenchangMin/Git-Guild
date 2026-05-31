<script setup>
import { computed } from 'vue'

const props = defineProps({
  reviews: {
    type: Array,
    required: true,
  },
  selectedReviewId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['select'])

const queueSummary = computed(() => {
  const pending = props.reviews.filter((review) => review.status === '待审核').length
  const returned = props.reviews.filter((review) => review.status.includes('修改')).length
  return { pending, returned, total: props.reviews.length }
})
</script>

<template>
  <aside class="maintainer-review-queue">
    <div class="queue-head">
      <p class="kicker">Review Queue</p>
      <h2>成果审核队列</h2>
      <p>先确认 PR 状态，再逐项核对完成标准，最后给出通过、退回修改或驳回结论。</p>
    </div>

    <div class="queue-summary" aria-label="审核队列统计">
      <span>{{ queueSummary.total }} 个提交</span>
      <span>{{ queueSummary.pending }} 待审核</span>
      <span>{{ queueSummary.returned }} 已退回</span>
    </div>

    <div class="queue-list">
      <button
        v-for="review in reviews"
        :key="review.id"
        class="queue-card"
        :class="[{ active: selectedReviewId === review.id }, review.statusTone]"
        type="button"
        @click="emit('select', review.id)"
      >
        <div>
          <span>{{ review.status }}</span>
          <strong>{{ review.id }}</strong>
        </div>
        <h3>{{ review.questId }} · {{ review.questTitle }}</h3>
        <p>{{ review.submitter }} · {{ review.pullRequest }} · {{ review.submittedAt }}</p>
        <small>{{ review.repository }}</small>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.maintainer-review-queue {
  display: grid;
  align-content: start;
  gap: 14px;
  min-height: 0;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 10px;
  padding: 16px;
  background:
    linear-gradient(180deg, rgba(37, 20, 9, 0.78), rgba(10, 5, 2, 0.62)),
    radial-gradient(circle at 18% 0%, rgba(255, 216, 133, 0.12), transparent 0 32%);
  box-shadow: 0 20px 55px rgba(0, 0, 0, 0.35);
  overflow: auto;
}

.queue-head h2 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
}

.queue-head p:last-child {
  margin: 8px 0 0;
  color: rgba(255, 231, 183, 0.72);
  line-height: 1.5;
  text-wrap: pretty;
}

.queue-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.queue-summary span {
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 999px;
  padding: 7px 8px;
  color: #ffe2a0;
  text-align: center;
  font-size: 0.78rem;
  background: rgba(7, 4, 2, 0.36);
}

.queue-list {
  display: grid;
  gap: 10px;
}

.queue-card {
  display: grid;
  gap: 7px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 8px;
  padding: 12px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(8, 4, 2, 0.34);
  transition: border-color 160ms ease, background 160ms ease, transform 160ms ease;
}

.queue-card:hover,
.queue-card:focus-visible,
.queue-card.active {
  border-color: rgba(255, 224, 157, 0.78);
  background: rgba(84, 45, 16, 0.54);
  transform: translateY(-1px);
}

.queue-card.warning {
  border-color: rgba(226, 132, 88, 0.35);
}

.queue-card div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.queue-card span {
  border-radius: 999px;
  padding: 3px 8px;
  color: #261306;
  background: #e6bd72;
  font-size: 0.76rem;
  font-weight: 800;
}

.queue-card.warning span {
  background: #f0a06d;
}

.queue-card strong,
.queue-card h3,
.queue-card p,
.queue-card small {
  margin: 0;
}

.queue-card h3 {
  font-family: var(--font-display);
  font-size: 1.02rem;
  line-height: 1.25;
}

.queue-card p,
.queue-card small {
  color: rgba(255, 231, 183, 0.68);
  line-height: 1.42;
}
</style>
