<script setup>
import QuestStepper from './QuestStepper.vue'

defineProps({
  workflowState: {
    type: String,
    default: 'available',
  },
})
</script>

<template>
  <section class="quest-journey" aria-label="贡献流程">
    <header class="journey-head">
      <p class="kicker">Contribution Journey</p>
      <h2>贡献流程</h2>
      <p class="journey-sub">先看清你在整条链路所处的位置；具体每一步的操作指引在工作台内提供。</p>
    </header>

    <QuestStepper :status="workflowState" />
  </section>
</template>

<style scoped>
.quest-journey {
  display: grid;
  gap: 18px;
  border: 1px solid rgba(238, 184, 91, 0.58);
  border-radius: var(--radius);
  padding: 18px;
  color: #ffe7b5;
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.82), rgba(15, 8, 4, 0.76)),
    radial-gradient(circle at 10% 0%, rgba(255, 231, 163, 0.14), transparent 0 32%),
    linear-gradient(135deg, rgba(216, 154, 50, 0.13), transparent 58%);
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.34), inset 0 1px 0 rgba(255, 229, 163, 0.14);
  backdrop-filter: blur(6px);
}

.journey-head h2 {
  font-size: 1.18rem;
}

.journey-sub {
  margin: 8px 0 0;
  color: rgba(255, 231, 183, 0.78);
  line-height: 1.48;
}
</style>
