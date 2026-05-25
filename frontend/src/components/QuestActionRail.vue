<script setup>
import { nextTick, ref, watch } from 'vue'

const props = defineProps({
  // Raw workflow state, e.g. 'available' / 'in-progress' / 'completed'.
  status: {
    type: String,
    default: 'available',
  },
  // workflowConfig: { status, next, primary, secondary }
  config: {
    type: Object,
    required: true,
  },
  // Transient feedback line shown after an action.
  notice: {
    type: String,
    default: '',
  },
  // Highlight + autofocus when the user arrived intending to accept.
  acceptIntent: {
    type: Boolean,
    default: false,
  },
  // Single most-relevant exception hint, or null.
  exceptionHint: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['primary', 'secondary'])

const railRef = ref(null)
const primaryRef = ref(null)

// When the page is opened with accept intent, draw the eye to the rail
// and put the keyboard focus on the primary action.
watch(
  () => props.acceptIntent,
  async (intent) => {
    if (!intent) return
    await nextTick()
    railRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    primaryRef.value?.focus({ preventScroll: true })
  },
  { immediate: true },
)
</script>

<template>
  <section
    ref="railRef"
    class="quest-action-rail"
    :class="{ 'accept-intent': acceptIntent, completed: status === 'completed' }"
    aria-label="任务操作"
  >
    <header class="rail-head">
      <p class="kicker">下一步</p>
      <span class="rail-state">{{ config.status }}</span>
    </header>

    <p class="rail-next">{{ config.next }}</p>

    <div class="rail-actions">
      <button ref="primaryRef" class="primary-action" type="button" @click="emit('primary')">
        {{ config.primary }}
      </button>
      <button class="quiet-action" type="button" @click="emit('secondary')">
        {{ config.secondary }}
      </button>
    </div>

    <p v-if="acceptIntent" class="rail-accept-note">确认任务背景和完成标准后再接取该任务。</p>

    <Transition name="notice" mode="out-in">
      <p v-if="notice" :key="notice" class="rail-notice" aria-live="polite">{{ notice }}</p>
    </Transition>

    <Transition name="notice">
      <div v-if="exceptionHint" class="rail-exception" role="note">
        <strong>{{ exceptionHint.title }}</strong>
        <span>{{ exceptionHint.body }}</span>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.quest-action-rail {
  position: sticky;
  top: 78px;
  display: grid;
  gap: 12px;
  border: 1px solid rgba(238, 184, 91, 0.58);
  border-radius: var(--radius);
  padding: 16px;
  color: #ffe7b5;
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.86), rgba(15, 8, 4, 0.8)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.16), transparent 58%);
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.36), inset 0 1px 0 rgba(255, 229, 163, 0.14);
  backdrop-filter: blur(6px);
  transition: border-color 200ms ease, box-shadow 200ms ease;
}

.quest-action-rail.accept-intent {
  border-color: rgba(242, 192, 111, 0.86);
  box-shadow:
    inset 0 0 0 1px rgba(255, 217, 138, 0.2),
    0 0 0 6px rgba(255, 204, 105, 0.1),
    0 18px 42px rgba(0, 0, 0, 0.34);
}

.rail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.rail-head .kicker {
  margin: 0;
}

.rail-state {
  border: 1px solid rgba(238, 184, 91, 0.46);
  border-radius: 999px;
  padding: 4px 11px;
  color: #ffe4ad;
  background: rgba(80, 43, 18, 0.46);
  font-size: 0.82rem;
  font-family: var(--font-display);
  white-space: nowrap;
}

.completed .rail-state {
  border-color: rgba(129, 184, 98, 0.6);
  color: #f1ffd6;
  background: rgba(67, 97, 58, 0.6);
}

.rail-next {
  margin: 0;
  color: rgba(255, 231, 183, 0.82);
  line-height: 1.46;
}

.rail-actions {
  display: grid;
  gap: 9px;
}

.rail-actions .primary-action,
.rail-actions .quiet-action {
  width: 100%;
  min-height: 42px;
}

.rail-actions .primary-action:active,
.rail-actions .quiet-action:active {
  transform: translateY(1px);
  filter: brightness(0.96);
}

.completed .rail-actions .primary-action {
  border-color: rgba(129, 184, 98, 0.7);
  color: #18230f;
  background: linear-gradient(180deg, #b7d98a, #5d8443);
}

.rail-accept-note {
  margin: 0;
  border-left: 3px solid rgba(242, 192, 111, 0.9);
  padding-left: 10px;
  color: #ffe8b9;
  line-height: 1.42;
}

.rail-notice {
  margin: 0;
  border: 1px solid rgba(67, 97, 58, 0.56);
  border-radius: 5px;
  padding: 10px;
  color: #f2ffd9;
  background: rgba(67, 97, 58, 0.24);
  line-height: 1.42;
}

.rail-exception {
  display: grid;
  gap: 4px;
  border-left: 3px solid #d66a48;
  border-radius: 4px;
  padding: 9px 11px;
  background: rgba(110, 42, 36, 0.22);
}

.rail-exception strong {
  color: #ffe0da;
  font-size: 0.88rem;
}

.rail-exception span {
  color: rgba(255, 231, 183, 0.82);
  font-size: 0.84rem;
  line-height: 1.4;
}

/* Feedback transition: slide + fade. */
.notice-enter-active,
.notice-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}

.notice-enter-from,
.notice-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 980px) {
  /* Single-column flow — a normal in-flow card is more predictable than a
     bottom bar that would otherwise sit at the very end of the page. */
  .quest-action-rail {
    position: static;
  }
}
</style>
