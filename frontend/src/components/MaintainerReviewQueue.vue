<script setup>
import { computed, ref } from 'vue'

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

const activeSection = ref('pending')

function sortBySubmittedAtDesc(items) {
  return [...items].sort((left, right) => Number(right.submittedAtOrder ?? 0) - Number(left.submittedAtOrder ?? 0))
}

const pendingReviews = computed(() => sortBySubmittedAtDesc(props.reviews.filter((review) => review.status === '待审核')))
const reviewedReviews = computed(() => sortBySubmittedAtDesc(props.reviews.filter((review) => review.status !== '待审核')))

const sections = computed(() => [
  {
    id: 'pending',
    label: '待审核提交',
    count: pendingReviews.value.length,
    reviews: pendingReviews.value,
    empty: '当前没有待审核提交。',
  },
  {
    id: 'reviewed',
    label: '已审核提交',
    count: reviewedReviews.value.length,
    reviews: reviewedReviews.value,
    empty: '当前没有已审核提交。',
  },
])

function toggleSection(sectionId) {
  activeSection.value = activeSection.value === sectionId ? '' : sectionId
}
</script>

<template>
  <aside class="maintainer-review-queue" aria-label="委托提交审核队列">
    <header class="queue-head">
      <h2>审核队列</h2>
    </header>

    <div class="queue-sections">
      <section v-for="section in sections" :key="section.id" class="queue-section">
        <button
          class="section-toggle"
          type="button"
          :aria-expanded="activeSection === section.id"
          @click="toggleSection(section.id)"
        >
          <span>{{ section.label }}</span>
          <strong>{{ section.count }}</strong>
        </button>

        <div v-if="activeSection === section.id" class="submission-list">
          <button
            v-for="review in section.reviews"
            :key="review.id"
            class="submission-card"
            :class="[{ active: selectedReviewId === review.id }, review.statusTone]"
            type="button"
            :data-tutorial="selectedReviewId === review.id ? 'review-queue-card' : undefined"
            @click="emit('select', review.id)"
          >
            <strong>{{ review.questTitle }}</strong>
            <span>提交人：{{ review.submitter }}</span>
            <span>提交时间：{{ review.submittedAt }}</span>
            <em>{{ review.status }}</em>
          </button>

          <p v-if="section.reviews.length === 0" class="queue-empty">{{ section.empty }}</p>
        </div>
      </section>
    </div>
  </aside>
</template>

<style scoped>
.maintainer-review-queue {
  display: grid;
  align-content: start;
  gap: 14px;
  min-height: 0;
  border: 1px solid rgba(238, 184, 91, 0.36);
  border-radius: 14px;
  padding: 16px;
  background:
    linear-gradient(180deg, rgba(33, 18, 9, 0.86), rgba(9, 5, 3, 0.7)),
    radial-gradient(circle at 14% 0%, rgba(255, 216, 133, 0.12), transparent 0 34%);
  box-shadow: 0 20px 55px rgba(0, 0, 0, 0.34);
  overflow: auto;
}

.queue-head h2 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
}

.queue-head p:last-child {
  margin: 8px 0 0;
  color: rgba(255, 231, 183, 0.68);
  line-height: 1.5;
}

.queue-sections {
  display: grid;
  gap: 10px;
}

.queue-section {
  display: grid;
  gap: 9px;
}

.section-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 12px;
  padding: 13px 14px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(8, 4, 2, 0.38);
  cursor: pointer;
  transition: border-color 160ms ease, background 160ms ease, transform 160ms ease;
}

.section-toggle:hover,
.section-toggle:focus-visible,
.section-toggle[aria-expanded="true"] {
  border-color: rgba(255, 224, 157, 0.76);
  background: rgba(84, 45, 16, 0.52);
  transform: translateY(-1px);
}

.section-toggle span {
  font-weight: 700;
}

.section-toggle strong {
  display: grid;
  place-items: center;
  min-width: 28px;
  height: 28px;
  border-radius: 999px;
  color: #2a1506;
  background: #e6bd72;
  font-variant-numeric: tabular-nums;
}

.submission-list {
  display: grid;
  gap: 8px;
  animation: queue-reveal 180ms ease-out;
}

.submission-card {
  position: relative;
  display: grid;
  gap: 6px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 10px;
  padding: 12px;
  color: rgba(255, 231, 183, 0.72);
  text-align: left;
  background: rgba(7, 4, 2, 0.3);
  cursor: pointer;
  transition: border-color 160ms ease, background 160ms ease, transform 160ms ease;
}

.submission-card:hover,
.submission-card:focus-visible,
.submission-card.active {
  border-color: rgba(255, 224, 157, 0.78);
  color: #ffe8b9;
  background: rgba(84, 45, 16, 0.5);
  transform: translateY(-1px);
}

.submission-card strong {
  color: #ffe8b9;
  line-height: 1.28;
}

.submission-card span {
  font-size: 0.82rem;
  line-height: 1.35;
}

.submission-card em {
  justify-self: start;
  border-radius: 999px;
  padding: 4px 8px;
  color: #261306;
  background: #e6bd72;
  font-size: 0.76rem;
  font-style: normal;
  font-weight: 700;
}

.submission-card.approved em {
  background: #a9d07b;
}

.submission-card.warning em {
  background: #f0a06d;
}

.submission-card.danger em {
  color: #ffe5dd;
  background: #b3503c;
}

.submission-card.danger:hover,
.submission-card.danger:focus-visible,
.submission-card.danger.active {
  border-color: rgba(214, 138, 116, 0.7);
}

.queue-empty {
  margin: 0;
  border: 1px dashed rgba(240, 198, 118, 0.22);
  border-radius: 10px;
  padding: 14px;
  color: rgba(255, 231, 183, 0.58);
  text-align: center;
  background: rgba(7, 4, 2, 0.22);
}

@keyframes queue-reveal {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
