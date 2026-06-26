<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  TUTORIAL_CLOSE_EVENT,
  TUTORIAL_COMPLETE_STEP_EVENT,
  getTutorialStorageKey,
  routeTutorialMap,
  TUTORIAL_LAUNCH_QUERY,
  TUTORIAL_STEP_EVENT,
  tutorials,
} from '../data/tutorials'
import { sessionStore } from '../stores/sessionStore'

const route = useRoute()
const router = useRouter()

const activeTutorialId = ref('')
const currentStepIndex = ref(0)
const targetRect = ref(null)
const cardStyle = ref({})
const cardRef = ref(null)

let routeTimer = 0
let updateFrame = 0
let positionTimer = 0
let resizeObserver = null

const activeTutorial = computed(() => tutorials[activeTutorialId.value] ?? null)
const steps = computed(() => activeTutorial.value?.steps ?? [])
const activeStep = computed(() => steps.value[currentStepIndex.value] ?? null)
const isActive = computed(() => Boolean(activeTutorial.value && activeStep.value))
const isLastStep = computed(() => currentStepIndex.value >= steps.value.length - 1)
const progressText = computed(() => `${currentStepIndex.value + 1}/${steps.value.length}`)
const dismissLabel = computed(() => activeTutorial.value?.dismissLabel ?? '跳过教程')
const showDragGesture = computed(() => activeStep.value?.gesture === 'drag-horizontal' && !targetRect.value)

function fitSpotlightAxis(start, end, boundaryStart, boundaryEnd, padding) {
  let boxStart = start - padding
  let boxEnd = end + padding

  if (boxStart < boundaryStart) {
    boxEnd = Math.min(boundaryEnd, boxEnd + boundaryStart - boxStart)
    boxStart = boundaryStart
  }

  if (boxEnd > boundaryEnd) {
    boxStart = Math.max(boundaryStart, boxStart - (boxEnd - boundaryEnd))
    boxEnd = boundaryEnd
  }

  return {
    start: boxStart,
    size: Math.max(0, boxEnd - boxStart),
  }
}

const spotlightStyle = computed(() => {
  if (!targetRect.value) return {}
  const padding = targetRect.value.width < 72 || targetRect.value.height < 48 ? 10 : 8
  const boundaryLeft = targetRect.value.boundaryLeft ?? 8
  const boundaryTop = targetRect.value.boundaryTop ?? 8
  const boundaryRight = targetRect.value.boundaryRight ?? window.innerWidth - 8
  const boundaryBottom = targetRect.value.boundaryBottom ?? window.innerHeight - 8
  const horizontal = fitSpotlightAxis(
    targetRect.value.left,
    targetRect.value.right,
    boundaryLeft,
    boundaryRight,
    padding,
  )
  const vertical = fitSpotlightAxis(
    targetRect.value.top,
    targetRect.value.bottom,
    boundaryTop,
    boundaryBottom,
    padding,
  )
  return {
    left: `${horizontal.start}px`,
    top: `${vertical.start}px`,
    width: `${horizontal.size}px`,
    height: `${vertical.size}px`,
  }
})

// 「已看过」标记按当前登录账号隔离，避免同浏览器多账号共用一份记录。
function currentTutorialScope() {
  const user = sessionStore.user
  if (user?.userId != null) return `u${user.userId}`
  if (user?.username) return `n:${user.username}`
  return 'guest'
}

function hasSeenTutorial(tutorialId) {
  try {
    return window.localStorage.getItem(getTutorialStorageKey(tutorialId, currentTutorialScope())) === 'true'
  } catch {
    return false
  }
}

function markTutorialSeen(tutorialId) {
  try {
    window.localStorage.setItem(getTutorialStorageKey(tutorialId, currentTutorialScope()), 'true')
  } catch {
    // Local persistence is a convenience only; the tutorial should still work.
  }
}

function escapeTarget(value) {
  if (window.CSS?.escape) return window.CSS.escape(value)
  return String(value).replace(/["\\]/g, '\\$&')
}

function findTargetElement() {
  const target = activeStep.value?.target
  if (!target) return null
  return document.querySelector(`[data-tutorial="${escapeTarget(target)}"]`)
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value))
}

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
}

function isScrollableElement(element) {
  if (!element) return false
  const style = window.getComputedStyle(element)
  const canScrollY =
    /(auto|scroll|overlay)/.test(style.overflowY) && element.scrollHeight > element.clientHeight + 1
  const canScrollX =
    /(auto|scroll|overlay)/.test(style.overflowX) && element.scrollWidth > element.clientWidth + 1
  return canScrollY || canScrollX
}

function findScrollableParent(element) {
  let current = element?.parentElement
  while (current && current !== document.body && current !== document.documentElement) {
    if (isScrollableElement(current)) return current
    current = current.parentElement
  }
  return null
}

function getOverflowClipRect(element) {
  const clipRect = {
    left: 8,
    top: 8,
    right: window.innerWidth - 8,
    bottom: window.innerHeight - 8,
  }
  if (element?.dataset?.tutorialBoundary === 'viewport') return clipRect

  let current = element?.parentElement

  while (current && current !== document.body && current !== document.documentElement) {
    const style = window.getComputedStyle(current)
    const clipsX = style.overflowX !== 'visible'
    const clipsY = style.overflowY !== 'visible'
    if (clipsX || clipsY) {
      const rect = current.getBoundingClientRect()
      if (clipsX) {
        clipRect.left = Math.max(clipRect.left, rect.left)
        clipRect.right = Math.min(clipRect.right, rect.right)
      }
      if (clipsY) {
        clipRect.top = Math.max(clipRect.top, rect.top)
        clipRect.bottom = Math.min(clipRect.bottom, rect.bottom)
      }
    }
    current = current.parentElement
  }

  return clipRect
}

function getVisibleTargetRect(element) {
  const rect = element.getBoundingClientRect()
  const clipRect = getOverflowClipRect(element)
  const left = Math.max(rect.left, clipRect.left)
  const top = Math.max(rect.top, clipRect.top)
  const right = Math.min(rect.right, clipRect.right)
  const bottom = Math.min(rect.bottom, clipRect.bottom)

  if (right <= left || bottom <= top) return null

  return {
    left,
    top,
    right,
    bottom,
    width: right - left,
    height: bottom - top,
    boundaryLeft: clipRect.left,
    boundaryTop: clipRect.top,
    boundaryRight: clipRect.right,
    boundaryBottom: clipRect.bottom,
  }
}

function nudgeScrollContainerToTarget(element, scroller) {
  const rect = element.getBoundingClientRect()
  const containerRect = scroller.getBoundingClientRect()
  const topMargin = 24
  const bottomMargin = 56
  const leftMargin = 16
  const rightMargin = 16
  let nextTop = scroller.scrollTop
  let nextLeft = scroller.scrollLeft

  if (rect.height > containerRect.height - topMargin - bottomMargin || rect.top < containerRect.top + topMargin) {
    nextTop += rect.top - containerRect.top - topMargin
  } else if (rect.bottom > containerRect.bottom - bottomMargin) {
    nextTop += rect.bottom - containerRect.bottom + bottomMargin
  }

  if (rect.width > containerRect.width - leftMargin - rightMargin || rect.left < containerRect.left + leftMargin) {
    nextLeft += rect.left - containerRect.left - leftMargin
  } else if (rect.right > containerRect.right - rightMargin) {
    nextLeft += rect.right - containerRect.right + rightMargin
  }

  scroller.scrollTop = clamp(nextTop, 0, scroller.scrollHeight - scroller.clientHeight)
  scroller.scrollLeft = clamp(nextLeft, 0, scroller.scrollWidth - scroller.clientWidth)
}

function nudgeWindowToTarget(element) {
  const scrollParent = findScrollableParent(element)
  if (scrollParent) {
    nudgeScrollContainerToTarget(element, scrollParent)
  }

  const rect = element.getBoundingClientRect()
  const topMargin = 92
  const bottomMargin = 150
  if (rect.top >= topMargin && rect.bottom <= window.innerHeight - bottomMargin) return

  const nextTop = Math.max(0, window.scrollY + rect.top - topMargin)
  window.scrollTo({
    top: nextTop,
    behavior: prefersReducedMotion() ? 'auto' : 'smooth',
  })
}

function computeCardStyle(rect) {
  const gap = 16
  const width = Math.min(360, window.innerWidth - 32)
  const height = cardRef.value?.offsetHeight ?? 220

  if (!rect) {
    return {
      left: `${Math.max(16, (window.innerWidth - width) / 2)}px`,
      top: `${Math.max(16, (window.innerHeight - height) / 2)}px`,
      width: `${width}px`,
    }
  }

  const placement = activeStep.value?.placement
  if (placement === 'right-middle' || placement === 'left-middle') {
    const preferredLeft = placement === 'right-middle' ? rect.right + gap : rect.left - width - gap
    return {
      left: `${clamp(preferredLeft, 16, window.innerWidth - width - 16)}px`,
      top: `${clamp(rect.top + rect.height / 2 - height / 2, 16, window.innerHeight - height - 16)}px`,
      width: `${width}px`,
    }
  }

  const spaceBelow = window.innerHeight - rect.bottom
  const top = spaceBelow >= height + gap + 16 ? rect.bottom + gap : Math.max(16, rect.top - height - gap)
  const left = clamp(rect.left + rect.width / 2 - width / 2, 16, window.innerWidth - width - 16)

  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
  }
}

function disconnectObserver() {
  resizeObserver?.disconnect()
  resizeObserver = null
}

function updatePosition() {
  if (!isActive.value) return

  const element = findTargetElement()
  disconnectObserver()

  if (!element) {
    targetRect.value = null
    cardStyle.value = computeCardStyle(null)
    return
  }

  nudgeWindowToTarget(element)
  const rect = getVisibleTargetRect(element)
  const isVisible =
    rect &&
    rect.width > 0 &&
    rect.height > 0 &&
    rect.bottom > 0 &&
    rect.right > 0 &&
    rect.top < window.innerHeight &&
    rect.left < window.innerWidth

  targetRect.value = isVisible
    ? {
        left: rect.left,
        top: rect.top,
        width: rect.width,
        height: rect.height,
        bottom: rect.bottom,
        right: rect.right,
        boundaryLeft: rect.boundaryLeft,
        boundaryTop: rect.boundaryTop,
        boundaryRight: rect.boundaryRight,
        boundaryBottom: rect.boundaryBottom,
      }
    : null
  cardStyle.value = computeCardStyle(targetRect.value)

  if (window.ResizeObserver) {
    resizeObserver = new ResizeObserver(queuePositionUpdate)
    resizeObserver.observe(element)
  }
}

function queuePositionUpdate() {
  window.cancelAnimationFrame(updateFrame)
  updateFrame = window.requestAnimationFrame(updatePosition)
}

function announceStep() {
  if (!isActive.value) return
  window.dispatchEvent(
    new CustomEvent(TUTORIAL_STEP_EVENT, {
      detail: {
        tutorialId: activeTutorialId.value,
        stepId: activeStep.value.id,
        target: activeStep.value.target,
      },
    }),
  )
}

function focusCard() {
  nextTick(() => cardRef.value?.focus({ preventScroll: true }))
}

function refreshAfterStepChange() {
  announceStep()
  window.clearTimeout(positionTimer)
  positionTimer = window.setTimeout(updatePosition, 80)
  window.setTimeout(updatePosition, 260)
  focusCard()
}

function closeTutorial({ markSeen = false } = {}) {
  const tutorialId = activeTutorialId.value
  const stepId = activeStep.value?.id
  if (markSeen && tutorialId) markTutorialSeen(tutorialId)
  if (tutorialId) {
    window.dispatchEvent(
      new CustomEvent(TUTORIAL_CLOSE_EVENT, {
        detail: {
          tutorialId,
          stepId,
        },
      }),
    )
  }
  activeTutorialId.value = ''
  currentStepIndex.value = 0
  targetRect.value = null
  cardStyle.value = {}
  disconnectObserver()
}

function completeTutorial() {
  closeTutorial({ markSeen: true })
}

function goNext() {
  if (isLastStep.value) {
    completeTutorial()
    return
  }
  currentStepIndex.value += 1
}

function goPrevious() {
  if (currentStepIndex.value === 0) return
  currentStepIndex.value -= 1
}

function handleStepCompletion(event) {
  if (!isActive.value) return
  const detail = event.detail ?? {}
  const matchesTutorial = !detail.tutorialId || detail.tutorialId === activeTutorialId.value
  const matchesStep = !detail.stepId || detail.stepId === activeStep.value?.id
  if (matchesTutorial && matchesStep) goNext()
}

function handleTargetClick(event) {
  event.preventDefault()
  event.stopPropagation()
  goNext()
}

function startTutorial(tutorialId, { forced = false } = {}) {
  if (!tutorials[tutorialId]) return
  if (!forced && hasSeenTutorial(tutorialId)) return
  if (activeTutorialId.value === tutorialId) return

  activeTutorialId.value = tutorialId
  currentStepIndex.value = 0
  nextTick(refreshAfterStepChange)
}

function clearTutorialQuery() {
  if (!route.query[TUTORIAL_LAUNCH_QUERY]) return
  const query = { ...route.query }
  delete query[TUTORIAL_LAUNCH_QUERY]
  router.replace({ name: route.name, params: route.params, query, hash: route.hash })
}

function resolveRouteTutorial() {
  const manual = route.query[TUTORIAL_LAUNCH_QUERY]
  const manualId = Array.isArray(manual) ? manual[0] : manual
  if (manualId && tutorials[manualId]) {
    return { tutorialId: manualId, forced: true }
  }

  const routeName = typeof route.name === 'string' ? route.name : ''
  return { tutorialId: routeTutorialMap[routeName], forced: false }
}

function scheduleRouteTutorial() {
  window.clearTimeout(routeTimer)
  routeTimer = window.setTimeout(() => {
    const { tutorialId, forced } = resolveRouteTutorial()
    if (!tutorialId) {
      closeTutorial()
      return
    }
    if (activeTutorialId.value && activeTutorialId.value !== tutorialId) closeTutorial()
    startTutorial(tutorialId, { forced })
    if (forced) clearTutorialQuery()
  }, 360)
}

function handleKeydown(event) {
  if (!isActive.value) return
  if (event.key === 'Escape') {
    event.preventDefault()
    completeTutorial()
  } else if (event.key === 'ArrowRight') {
    event.preventDefault()
    goNext()
  } else if (event.key === 'ArrowLeft') {
    event.preventDefault()
    goPrevious()
  }
}

watch(() => route.fullPath, scheduleRouteTutorial, { immediate: true })
watch([activeTutorialId, currentStepIndex], () => {
  if (!isActive.value) return
  refreshAfterStepChange()
})

onMounted(() => {
  window.addEventListener('resize', queuePositionUpdate)
  window.addEventListener('scroll', queuePositionUpdate, true)
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener(TUTORIAL_COMPLETE_STEP_EVENT, handleStepCompletion)
})

onBeforeUnmount(() => {
  window.clearTimeout(routeTimer)
  window.clearTimeout(positionTimer)
  window.cancelAnimationFrame(updateFrame)
  window.removeEventListener('resize', queuePositionUpdate)
  window.removeEventListener('scroll', queuePositionUpdate, true)
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener(TUTORIAL_COMPLETE_STEP_EVENT, handleStepCompletion)
  disconnectObserver()
})
</script>

<template>
  <Teleport to="body">
    <Transition name="tutorial-fade">
      <div
        v-if="isActive"
        class="tutorial-layer"
        :class="{ 'is-pass-through': showDragGesture }"
        role="presentation"
      >
        <div class="tutorial-scrim" :class="{ 'has-target': targetRect }"></div>
        <div v-if="targetRect" class="tutorial-spotlight" :style="spotlightStyle"></div>
        <button
          v-if="targetRect"
          class="tutorial-target-hitbox"
          type="button"
          :style="spotlightStyle"
          :aria-label="activeStep.title"
          @pointerdown.stop
          @pointerup.stop
          @pointercancel.stop
          @click="handleTargetClick"
        ></button>
        <div v-if="showDragGesture" class="tutorial-drag-gesture" aria-hidden="true">
          <span class="tutorial-drag-arrow"></span>
        </div>
        <article
          ref="cardRef"
          class="tutorial-card"
          :class="{ 'is-centered': !targetRect }"
          :style="cardStyle"
          role="dialog"
          aria-modal="true"
          aria-labelledby="tutorial-card-title"
          tabindex="-1"
        >
          <header class="tutorial-card-head">
            <div>
              <p class="tutorial-kicker">{{ activeTutorial.title }}</p>
              <h2 id="tutorial-card-title">{{ activeStep.title }}</h2>
            </div>
            <span class="tutorial-progress" aria-label="教程进度">{{ progressText }}</span>
          </header>

          <p class="tutorial-body">{{ activeStep.body }}</p>

          <footer class="tutorial-actions">
            <button class="tutorial-button ghost" type="button" :disabled="currentStepIndex === 0" @click="goPrevious">
              上一步
            </button>
            <button class="tutorial-button quiet" type="button" @click="completeTutorial">
              {{ dismissLabel }}
            </button>
            <button class="tutorial-button primary" type="button" @click="goNext">
              {{ isLastStep ? '完成教程' : '下一步' }}
            </button>
          </footer>
        </article>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.tutorial-layer {
  position: fixed;
  inset: 0;
  z-index: 5000;
  pointer-events: auto;
}

.tutorial-layer.is-pass-through {
  pointer-events: none;
}

.tutorial-scrim {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% 42%, rgba(156, 93, 27, 0.1), transparent 0 28%),
    rgba(10, 6, 3, 0.58);
}

.tutorial-scrim.has-target {
  background: transparent;
}

.tutorial-spotlight {
  position: absolute;
  z-index: 1;
  border: 2px solid rgba(255, 214, 135, 0.92);
  border-radius: 8px;
  background: transparent;
  box-shadow:
    0 0 0 9999px rgba(5, 2, 1, 0.64),
    0 0 24px rgba(255, 190, 88, 0.5),
    inset 0 0 12px rgba(255, 190, 88, 0.1);
  pointer-events: none;
  transition:
    left 180ms ease,
    top 180ms ease,
    width 180ms ease,
    height 180ms ease;
}

.tutorial-target-hitbox {
  position: absolute;
  z-index: 2;
  border: 0;
  border-radius: 8px;
  padding: 0;
  appearance: none;
  background: transparent;
  cursor: pointer;
  pointer-events: auto;
  transition:
    left 180ms ease,
    top 180ms ease,
    width 180ms ease,
    height 180ms ease;
}

.tutorial-target-hitbox:focus-visible {
  outline: 2px solid rgba(255, 224, 154, 0.82);
  outline-offset: 4px;
}

.tutorial-drag-gesture {
  position: absolute;
  left: 50%;
  top: 36%;
  z-index: 1;
  width: min(460px, calc(100vw - 72px));
  height: 72px;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.tutorial-drag-arrow {
  position: absolute;
  top: 50%;
  left: 0;
  width: 58px;
  height: 58px;
  border: 1px solid rgba(210, 151, 59, 0.7);
  border-radius: 50%;
  background:
    radial-gradient(circle at 30% 22%, rgba(244, 190, 92, 0.2), transparent 0 42%),
    rgba(24, 12, 5, 0.92);
  color: #e7b45d;
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.36);
  transform: translateY(-50%);
  animation: tutorial-drag-arrow 2600ms cubic-bezier(0.24, 0.72, 0.22, 1) infinite;
}

.tutorial-drag-arrow::before,
.tutorial-drag-arrow::after {
  position: absolute;
  top: 50%;
  left: 50%;
  display: block;
  content: '';
}

.tutorial-drag-arrow::before {
  width: 25px;
  height: 5px;
  border-radius: 999px;
  background: currentColor;
  transform: translate(-58%, -50%);
  box-shadow: 0 0 10px rgba(231, 180, 93, 0.32);
}

.tutorial-drag-arrow::after {
  width: 15px;
  height: 15px;
  border-top: 5px solid currentColor;
  border-right: 5px solid currentColor;
  border-radius: 2px;
  transform: translate(-24%, -50%) rotate(45deg);
}

.tutorial-card {
  position: absolute;
  z-index: 3;
  display: grid;
  gap: 14px;
  max-width: calc(100vw - 32px);
  border: 1px solid rgba(255, 214, 135, 0.78);
  border-radius: 8px;
  padding: 16px;
  background:
    linear-gradient(180deg, rgba(18, 8, 4, 0.98), rgba(8, 3, 2, 0.98)),
    #0c0603;
  color: #f8dfad;
  box-shadow:
    0 28px 70px rgba(0, 0, 0, 0.62),
    0 0 0 1px rgba(255, 214, 135, 0.18),
    0 0 30px rgba(255, 175, 74, 0.16),
    inset 0 1px 0 rgba(255, 220, 150, 0.12);
  outline: none;
  pointer-events: auto;
  text-wrap: pretty;
}

.tutorial-card.is-centered {
  text-align: left;
}

.tutorial-card-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.tutorial-kicker {
  margin: 0 0 4px;
  color: rgba(231, 180, 93, 0.72);
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.tutorial-card h2 {
  margin: 0;
  color: #fff0c2;
  font-size: 1.05rem;
  line-height: 1.25;
}

.tutorial-progress {
  min-width: 44px;
  border: 1px solid rgba(210, 151, 59, 0.36);
  border-radius: 999px;
  padding: 4px 9px;
  background: rgba(9, 4, 2, 0.42);
  color: #e7b45d;
  font-size: 0.76rem;
  font-weight: 800;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.tutorial-body {
  margin: 0;
  color: rgba(255, 232, 185, 0.78);
  font-size: 0.92rem;
  line-height: 1.62;
}

.tutorial-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.tutorial-button {
  min-height: 36px;
  border: 1px solid rgba(210, 151, 59, 0.28);
  border-radius: 7px;
  padding: 8px 12px;
  font: inherit;
  font-size: 0.84rem;
  font-weight: 800;
  cursor: pointer;
  transition:
    transform 160ms ease,
    box-shadow 160ms ease,
    border-color 160ms ease,
    background 160ms ease;
}

.tutorial-button:hover:not(:disabled),
.tutorial-button:focus-visible:not(:disabled) {
  transform: translateY(-1px);
  outline: none;
}

.tutorial-button:active:not(:disabled) {
  transform: translateY(0);
}

.tutorial-button:disabled {
  cursor: default;
  opacity: 0.42;
}

.tutorial-button.ghost,
.tutorial-button.quiet {
  background: rgba(10, 5, 2, 0.34);
  color: rgba(255, 232, 185, 0.8);
}

.tutorial-button.ghost:hover:not(:disabled),
.tutorial-button.ghost:focus-visible:not(:disabled),
.tutorial-button.quiet:hover:not(:disabled),
.tutorial-button.quiet:focus-visible:not(:disabled) {
  border-color: rgba(117, 68, 18, 0.5);
  background: rgba(76, 42, 16, 0.58);
}

.tutorial-button.primary {
  border-color: rgba(238, 184, 91, 0.45);
  background: linear-gradient(180deg, #8f5b1c, #5a2f0d);
  color: #fff0c2;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.28);
}

.tutorial-button.primary:hover,
.tutorial-button.primary:focus-visible {
  box-shadow: 0 14px 26px rgba(0, 0, 0, 0.36);
}

@keyframes tutorial-drag-arrow {
  0% {
    left: 0;
    opacity: 0;
  }

  16% {
    left: 8%;
    opacity: 1;
  }

  76% {
    left: calc(92% - 58px);
    opacity: 1;
  }

  100% {
    left: calc(100% - 58px);
    opacity: 0;
  }
}

.tutorial-fade-enter-active,
.tutorial-fade-leave-active {
  transition: opacity 180ms ease;
}

.tutorial-fade-enter-from,
.tutorial-fade-leave-to {
  opacity: 0;
}

@media (max-width: 720px) {
  .tutorial-card {
    left: 12px !important;
    right: 12px;
    top: auto !important;
    bottom: 12px;
    width: auto !important;
    padding: 14px;
  }

  .tutorial-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .tutorial-button.primary {
    grid-column: 1 / -1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .tutorial-spotlight,
  .tutorial-target-hitbox,
  .tutorial-drag-arrow,
  .tutorial-button,
  .tutorial-fade-enter-active,
  .tutorial-fade-leave-active {
    transition: none;
    animation: none;
  }
}
</style>
