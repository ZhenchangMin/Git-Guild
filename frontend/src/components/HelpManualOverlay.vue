<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { closeHelpManual, useHelpManual } from '../composables/useHelpManual'
import { helpTopicRouteMap, helpTopics } from '../data/helpManual'
import { TUTORIAL_LAUNCH_QUERY } from '../data/tutorials'

const route = useRoute()
const router = useRouter()
const { helpManualState } = useHelpManual()

const manualRef = ref(null)
const selectedTopicId = ref('')

const isReading = computed(() => Boolean(selectedTopicId.value))
const suggestedTopicId = computed(() => helpTopicRouteMap[helpManualState.sourceRoute] ?? '')
const selectedTopic = computed(() => helpTopics.find((topic) => topic.id === selectedTopicId.value) ?? null)
const selectedTopicNumber = computed(() => {
  const index = helpTopics.findIndex((topic) => topic.id === selectedTopicId.value)
  return index >= 0 ? String(index + 1).padStart(2, '0') : ''
})
const canStartTutorial = computed(() => Boolean(helpManualState.tutorialId))

function selectTopic(topicId) {
  selectedTopicId.value = selectedTopicId.value === topicId ? '' : topicId
}

function resetManual() {
  selectedTopicId.value = ''
  nextTick(() => manualRef.value?.focus({ preventScroll: true }))
}

function handleClose() {
  closeHelpManual()
}

function startCurrentPageTutorial() {
  if (!canStartTutorial.value) return
  const query = {
    ...route.query,
    [TUTORIAL_LAUNCH_QUERY]: helpManualState.tutorialId,
  }

  closeHelpManual()
  router.replace({
    name: route.name,
    params: route.params,
    query,
    hash: route.hash,
  })
}

function handleKeydown(event) {
  if (!helpManualState.isOpen) return
  if (event.key === 'Escape') {
    event.preventDefault()
    handleClose()
  }
}

watch(
  () => helpManualState.openedAt,
  () => {
    if (helpManualState.isOpen) resetManual()
  },
)

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="help-manual-layer">
      <div
        v-if="helpManualState.isOpen"
        class="help-manual-layer"
        role="presentation"
        @click.self="handleClose"
      >
        <section
          ref="manualRef"
          class="help-manual"
          :class="{ 'is-reading': isReading }"
          role="dialog"
          aria-modal="true"
          aria-labelledby="help-manual-title"
          tabindex="-1"
        >
          <button class="manual-close" type="button" aria-label="关闭使用帮助" @click="handleClose">×</button>

          <aside class="manual-directory" aria-label="使用帮助目录">
            <header class="manual-head">
              <p class="manual-kicker">使用手册</p>
              <h2 id="help-manual-title">Git Guild 使用帮助</h2>
              <p>选择一个主题，查看操作说明、常见问题与平台信息。</p>
            </header>

            <div class="manual-topic-list">
              <button
                v-for="(topic, index) in helpTopics"
                :key="topic.id"
                class="manual-topic-card"
                :class="{
                  'is-active': topic.id === selectedTopicId,
                  'is-suggested': topic.id === suggestedTopicId,
                }"
                type="button"
                @click="selectTopic(topic.id)"
              >
                <span class="manual-topic-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="manual-topic-copy">
                  <span class="manual-topic-eyebrow">{{ topic.eyebrow }}</span>
                  <strong>{{ topic.title }}</strong>
                  <small>{{ topic.summary }}</small>
                </span>
              </button>
            </div>

            <footer class="manual-directory-foot">
              <button
                class="manual-tutorial-button"
                type="button"
                :disabled="!canStartTutorial"
                @click="startCurrentPageTutorial"
              >
                查看本页面教程
              </button>
            </footer>
          </aside>

          <Transition name="manual-detail" mode="out-in">
            <article v-if="selectedTopic" :key="selectedTopic.id" class="manual-detail">
              <header class="manual-detail-head">
                <div>
                  <p class="manual-kicker">{{ selectedTopic.eyebrow }}</p>
                  <h3>{{ selectedTopic.title }}</h3>
                  <p>{{ selectedTopic.summary }}</p>
                </div>
                <span class="manual-page-mark">{{ selectedTopicNumber }}</span>
              </header>

              <section class="manual-section">
                <h4>适用场景</h4>
                <p>{{ selectedTopic.scenario }}</p>
              </section>

              <section v-if="selectedTopic.steps?.length" class="manual-section">
                <h4>操作步骤</h4>
                <ol class="manual-step-list">
                  <li v-for="step in selectedTopic.steps" :key="step">{{ step }}</li>
                </ol>
              </section>

              <section class="manual-section">
                <h4>相关入口</h4>
                <div class="manual-entry-list">
                  <span v-for="entry in selectedTopic.entrances" :key="entry">{{ entry }}</span>
                </div>
              </section>

              <section v-if="selectedTopic.profile?.length" class="manual-section">
                <h4>小组与平台信息</h4>
                <dl class="manual-profile-list">
                  <div v-for="item in selectedTopic.profile" :key="item.label">
                    <dt>{{ item.label }}</dt>
                    <dd>{{ item.value }}</dd>
                  </div>
                </dl>
              </section>

              <section class="manual-section">
                <h4>常见问题</h4>
                <div class="manual-faq-list">
                  <details v-for="faq in selectedTopic.faqs" :key="faq.q">
                    <summary>{{ faq.q }}</summary>
                    <p>{{ faq.a }}</p>
                  </details>
                </div>
              </section>
            </article>
          </Transition>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.help-manual-layer {
  position: fixed;
  inset: 0;
  z-index: 4300;
  display: grid;
  place-items: center;
  padding: 48px 64px;
  background:
    radial-gradient(circle at 50% 42%, rgba(173, 107, 34, 0.08), transparent 0 34%),
    rgba(5, 2, 1, 0.74);
}

.help-manual {
  position: relative;
  display: grid;
  width: min(800px, calc(100vw - 128px));
  height: min(820px, calc(100svh - 96px));
  max-height: calc(100svh - 96px);
  border: 1px solid rgba(238, 184, 91, 0.58);
  border-radius: 8px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(24, 11, 4, 0.98), rgba(7, 3, 1, 0.99)),
    radial-gradient(circle at 22% 12%, rgba(255, 214, 135, 0.1), transparent 0 32%);
  box-shadow:
    0 30px 90px rgba(0, 0, 0, 0.62),
    0 0 0 1px rgba(255, 224, 154, 0.12),
    inset 0 1px 0 rgba(255, 232, 185, 0.14);
  outline: none;
  transition:
    width 320ms cubic-bezier(0.2, 0.84, 0.22, 1),
    grid-template-columns 320ms cubic-bezier(0.2, 0.84, 0.22, 1),
    box-shadow 220ms ease;
}

.help-manual.is-reading {
  grid-template-columns: 300px minmax(0, 1fr);
  width: min(1080px, calc(100vw - 128px));
}

.manual-close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 2;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid rgba(238, 184, 91, 0.52);
  border-radius: 50%;
  padding: 0;
  color: #ffe8b8;
  background: rgba(12, 6, 3, 0.72);
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.3);
  font-size: 24px;
  line-height: 1;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    background 160ms ease,
    box-shadow 160ms ease;
}

.manual-close:hover,
.manual-close:focus-visible {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.82);
  box-shadow: 0 0 0 7px rgba(255, 204, 105, 0.12), 0 0 24px rgba(255, 190, 82, 0.28);
  transform: translateY(-1px);
}

.manual-directory {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 18px;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  padding: 28px;
  background:
    linear-gradient(180deg, rgba(52, 27, 10, 0.72), rgba(21, 9, 3, 0.88)),
    radial-gradient(circle at 0 0, rgba(238, 184, 91, 0.16), transparent 0 38%);
  transition:
    padding 320ms cubic-bezier(0.2, 0.84, 0.22, 1),
    border-color 220ms ease,
    background 220ms ease;
}

.help-manual.is-reading .manual-directory {
  border-right: 1px solid rgba(238, 184, 91, 0.32);
  padding: 24px 18px;
  background:
    linear-gradient(180deg, rgba(58, 31, 12, 0.92), rgba(24, 10, 4, 0.97)),
    radial-gradient(circle at 0 0, rgba(238, 184, 91, 0.18), transparent 0 36%);
}

.manual-head {
  display: grid;
  gap: 8px;
  padding-right: 44px;
}

.manual-kicker {
  margin: 0;
  color: rgba(231, 180, 93, 0.78);
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.manual-head h2,
.manual-detail h3 {
  margin: 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  line-height: 1.04;
}

.manual-head h2 {
  font-size: clamp(2rem, 4vw, 3.2rem);
}

.help-manual.is-reading .manual-head h2 {
  font-size: 1.55rem;
}

.manual-head p,
.manual-detail-head p,
.manual-section p {
  margin: 0;
  color: rgba(255, 232, 190, 0.76);
  line-height: 1.6;
  text-wrap: pretty;
}

.manual-topic-list {
  display: grid;
  align-content: start;
  grid-auto-rows: max-content;
  gap: 7px;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px 10px 0 0;
  scrollbar-gutter: stable;
}

.manual-topic-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 9px;
  align-items: start;
  align-self: start;
  width: 100%;
  min-height: 68px;
  border: 1px solid rgba(238, 184, 91, 0.26);
  border-radius: 8px;
  padding: 8px 10px;
  color: #ffe8b9;
  text-align: left;
  background:
    linear-gradient(135deg, rgba(76, 39, 13, 0.58), rgba(11, 5, 2, 0.42)),
    radial-gradient(circle at 92% 16%, rgba(255, 208, 111, 0.1), transparent 0 32%);
  transition:
    transform 180ms ease,
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease;
}

.manual-topic-card:hover,
.manual-topic-card:focus-visible,
.manual-topic-card.is-active {
  border-color: rgba(255, 214, 135, 0.76);
  background:
    linear-gradient(135deg, rgba(105, 58, 20, 0.78), rgba(20, 9, 3, 0.6)),
    radial-gradient(circle at 92% 16%, rgba(255, 208, 111, 0.16), transparent 0 34%);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255, 225, 166, 0.08);
  outline: none;
  transform: translateY(-1px);
}

.manual-topic-card.is-suggested:not(.is-active) {
  border-color: rgba(216, 154, 50, 0.58);
}

.manual-topic-index {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(238, 184, 91, 0.36);
  border-radius: 50%;
  color: #e7b45d;
  background: rgba(10, 5, 2, 0.42);
  font-size: 0.82rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.manual-topic-copy {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.manual-topic-eyebrow {
  color: rgba(231, 180, 93, 0.72);
  font-size: 0.68rem;
  font-weight: 800;
}

.manual-topic-copy strong {
  color: #fff0c2;
  font-size: 0.98rem;
}

.manual-topic-copy small {
  color: rgba(255, 232, 190, 0.68);
  font-size: 0.8rem;
  line-height: 1.36;
}

.help-manual.is-reading .manual-topic-copy small {
  display: none;
}

.help-manual.is-reading .manual-topic-card {
  min-height: 62px;
}

.manual-directory-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.manual-tutorial-button {
  min-height: 38px;
  border: 1px solid rgba(238, 184, 91, 0.58);
  border-radius: 5px;
  padding: 0 14px;
  color: #ffe8b8;
  background: rgba(17, 8, 3, 0.58);
  font-weight: 800;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    background 160ms ease,
    filter 160ms ease;
}

.manual-tutorial-button:hover:not(:disabled),
.manual-tutorial-button:focus-visible:not(:disabled) {
  border-color: var(--gold-bright);
  background: rgba(89, 51, 18, 0.78);
  transform: translateY(-1px);
}

.manual-tutorial-button:disabled {
  cursor: default;
  filter: grayscale(0.25);
  opacity: 0.48;
}

.manual-detail {
  min-height: 0;
  overflow: auto;
  padding: 30px 32px 48px;
  scroll-padding-bottom: 42px;
  color: #f9e3b8;
  background:
    linear-gradient(180deg, rgba(15, 6, 2, 0.99), rgba(5, 2, 1, 0.99)),
    radial-gradient(circle at 72% 8%, rgba(255, 198, 103, 0.08), transparent 0 30%);
}

.manual-detail-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: start;
  border-bottom: 1px solid rgba(238, 184, 91, 0.22);
  padding: 0 48px 20px 0;
}

.manual-detail h3 {
  margin-top: 4px;
  font-size: 2.35rem;
}

.manual-page-mark {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 50%;
  color: #e7b45d;
  background: rgba(10, 5, 2, 0.48);
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-variant-numeric: tabular-nums;
}

.manual-section {
  display: grid;
  gap: 12px;
  border-bottom: 1px solid rgba(238, 184, 91, 0.14);
  padding: 22px 0;
}

.manual-section h4 {
  margin: 0;
  color: #ffe8b9;
  font-size: 1rem;
}

.manual-step-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
  counter-reset: manual-step;
}

.manual-step-list li {
  position: relative;
  min-height: 38px;
  border: 1px solid rgba(238, 184, 91, 0.16);
  border-radius: 6px;
  padding: 10px 12px 10px 48px;
  color: rgba(255, 232, 190, 0.82);
  background: rgba(7, 3, 1, 0.3);
  line-height: 1.5;
  counter-increment: manual-step;
}

.manual-step-list li::before {
  position: absolute;
  left: 12px;
  top: 10px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: #1d0d04;
  background: linear-gradient(180deg, #f0c270, #a7631c);
  font-size: 0.78rem;
  font-weight: 900;
  content: counter(manual-step);
}

.manual-entry-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.manual-entry-list span {
  border: 1px solid rgba(238, 184, 91, 0.26);
  border-radius: 999px;
  padding: 6px 10px;
  color: #f5c878;
  background: rgba(12, 6, 3, 0.38);
  font-size: 0.84rem;
  font-weight: 800;
}

.manual-profile-list {
  display: grid;
  gap: 10px;
  margin: 0;
}

.manual-profile-list div {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 14px;
  border: 1px solid rgba(238, 184, 91, 0.14);
  border-radius: 6px;
  padding: 10px 12px;
  background: rgba(7, 3, 1, 0.24);
}

.manual-profile-list dt {
  color: rgba(231, 180, 93, 0.78);
  font-weight: 800;
}

.manual-profile-list dd {
  margin: 0;
  color: rgba(255, 232, 190, 0.82);
}

.manual-faq-list {
  display: grid;
  gap: 10px;
}

.manual-faq-list details {
  border: 1px solid rgba(238, 184, 91, 0.16);
  border-radius: 6px;
  background: rgba(7, 3, 1, 0.26);
}

.manual-faq-list summary {
  padding: 12px 14px;
  color: #ffe8b9;
  font-weight: 800;
  cursor: pointer;
}

.manual-faq-list p {
  margin: 0;
  padding: 0 14px 14px;
}

.help-manual-layer-enter-active,
.help-manual-layer-leave-active {
  transition: opacity 180ms ease;
}

.help-manual-layer-enter-active .help-manual,
.help-manual-layer-leave-active .help-manual {
  transition:
    opacity 220ms ease,
    transform 220ms cubic-bezier(0.2, 0.84, 0.22, 1);
}

.help-manual-layer-enter-from,
.help-manual-layer-leave-to {
  opacity: 0;
}

.help-manual-layer-enter-from .help-manual,
.help-manual-layer-leave-to .help-manual {
  opacity: 0;
  transform: translateY(18px) scale(0.98);
}

.manual-detail-enter-active,
.manual-detail-leave-active {
  transition:
    opacity 200ms ease,
    transform 240ms cubic-bezier(0.2, 0.84, 0.22, 1);
}

.manual-detail-enter-from,
.manual-detail-leave-to {
  opacity: 0;
  transform: translateX(22px);
}

@media (prefers-reduced-motion: reduce) {
  .help-manual,
  .manual-directory,
  .manual-close,
  .manual-topic-card,
  .manual-tutorial-button,
  .help-manual-layer-enter-active,
  .help-manual-layer-leave-active,
  .help-manual-layer-enter-active .help-manual,
  .help-manual-layer-leave-active .help-manual,
  .manual-detail-enter-active,
  .manual-detail-leave-active {
    transition: none;
  }
}
</style>
