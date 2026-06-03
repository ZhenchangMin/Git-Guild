<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  data: { type: Array, required: true },
})

const DIFF_MAP = { D: 1, C: 2, B: 3, A: 4 }
const DIFF_LABELS = { 1: 'D 入门', 2: 'C 初级', 3: 'B 中级', 4: 'A 高级' }
const DIFF_SHORT = { 1: 'D', 2: 'C', 3: 'B', 4: 'A' }

// ─── chart geometry ─────────────────────────────────
const W = 520
const H = 260
const PAD = { top: 28, right: 24, bottom: 16, left: 44 }
const plotW = W - PAD.left - PAD.right
const plotH = H - PAD.top - PAD.bottom

// ─── range state ────────────────────────────────────
const sorted = computed(() =>
  [...props.data].sort((a, b) => a.date.localeCompare(b.date)),
)

const totalRange = computed(() => {
  if (sorted.value.length === 0) return { min: 0, max: 0 }
  const dates = sorted.value.map(d => new Date(d.date).getTime())
  return { min: Math.min(...dates), max: Math.max(...dates) }
})

const rangeStart = ref(0)
const rangeEnd = ref(1)

onMounted(() => {
  rangeStart.value = 0
  rangeEnd.value = 1
})

const visibleData = computed(() => {
  const { min, max } = totalRange.value
  const span = max - min || 1
  return sorted.value.filter(d => {
    const t = new Date(d.date).getTime()
    const ratio = (t - min) / span
    return ratio >= rangeStart.value - 0.001 && ratio <= rangeEnd.value + 0.001
  })
})

// ─── point positions ────────────────────────────────
function dateRatio(dateStr) {
  const { min, max } = totalRange.value
  const span = max - min || 1
  return (new Date(dateStr).getTime() - min) / span
}

function xPos(dateStr) {
  const ratio = dateRatio(dateStr)
  const localSpan = rangeEnd.value - rangeStart.value || 1
  const localRatio = (ratio - rangeStart.value) / localSpan
  return PAD.left + localRatio * plotW
}

function yPos(difficulty) {
  const val = DIFF_MAP[difficulty] || 1
  return PAD.top + plotH - ((val - 0.5) / 4) * plotH
}

const points = computed(() =>
  visibleData.value.map(d => ({
    x: xPos(d.date),
    y: yPos(d.difficulty),
    ...d,
  })),
)

const linePath = computed(() => {
  if (points.value.length < 2) return ''
  return points.value
    .map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`)
    .join(' ')
})

const areaPath = computed(() => {
  if (points.value.length < 2) return ''
  const bottom = PAD.top + plotH
  const line = points.value
    .map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`)
    .join(' ')
  const last = points.value[points.value.length - 1]
  const first = points.value[0]
  return `${line} L${last.x.toFixed(1)},${bottom} L${first.x.toFixed(1)},${bottom} Z`
})

// ─── grid lines ─────────────────────────────────────
const gridLines = [
  { value: 1, label: 'D' },
  { value: 2, label: 'C' },
  { value: 3, label: 'B' },
  { value: 4, label: 'A' },
]

// ─── current level ──────────────────────────────────
const currentLevel = computed(() => {
  if (points.value.length === 0) return ''
  const last = points.value[points.value.length - 1]
  return DIFF_LABELS[DIFF_MAP[last.difficulty]]
})

// ─── tooltip ────────────────────────────────────────
const tooltip = ref(null)
const tooltipVisible = ref(false)

function showTooltip(pt, evt) {
  tooltip.value = {
    title: pt.questTitle,
    date: pt.date,
    difficulty: DIFF_LABELS[DIFF_MAP[pt.difficulty]],
    xp: pt.xp,
    x: pt.x,
    y: pt.y,
  }
  tooltipVisible.value = true
}

function hideTooltip() {
  tooltipVisible.value = false
}

// ─── slider drag ────────────────────────────────────
const trackEl = ref(null)
const dragging = ref(null) // 'start' | 'end' | null

function getTrackRatio(evt) {
  if (!trackEl.value) return 0
  const rect = trackEl.value.getBoundingClientRect()
  return Math.max(0, Math.min(1, (evt.clientX - rect.left) / rect.width))
}

function onPointerDown(handle, evt) {
  dragging.value = handle
  evt.preventDefault()
}

function onPointerMove(evt) {
  if (!dragging.value) return
  const ratio = getTrackRatio(evt)
  if (dragging.value === 'start') {
    rangeStart.value = Math.min(ratio, rangeEnd.value - 0.05)
  } else {
    rangeEnd.value = Math.max(ratio, rangeStart.value + 0.05)
  }
}

function onPointerUp() {
  dragging.value = null
}

onMounted(() => {
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
})

// ─── date labels on track ───────────────────────────
const trackDateLabels = computed(() => {
  const { min, max } = totalRange.value
  const span = max - min || 1
  const allDates = sorted.value.map(d => d.date)
  if (allDates.length <= 4) return allDates
  // pick ~4 evenly spaced labels
  const step = Math.max(1, Math.floor(allDates.length / 4))
  const labels = []
  for (let i = 0; i < allDates.length; i += step) labels.push(allDates[i])
  if (!labels.includes(allDates[allDates.length - 1])) labels.push(allDates[allDates.length - 1])
  return labels
})
</script>

<template>
  <div class="dtc-wrap">
    <svg :viewBox="`0 0 ${W} ${H}`" class="dtc-svg" aria-label="难度攀升曲线">
      <defs>
        <linearGradient id="dtc-line-grad" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#c8851a" />
          <stop offset="50%" stop-color="#f1b756" />
          <stop offset="100%" stop-color="#ffe0a3" />
        </linearGradient>
        <linearGradient id="dtc-area-grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="rgba(241,183,86,0.28)" />
          <stop offset="100%" stop-color="rgba(241,183,86,0)" />
        </linearGradient>
        <filter id="dtc-glow">
          <feGaussianBlur stdDeviation="3" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      <!-- grid lines -->
      <line
        v-for="g in gridLines"
        :key="g.value"
        :x1="PAD.left"
        :y1="PAD.top + plotH - ((g.value - 0.5) / 4) * plotH"
        :x2="PAD.left + plotW"
        :y2="PAD.top + plotH - ((g.value - 0.5) / 4) * plotH"
        stroke="rgba(238,184,91,0.15)"
        stroke-dasharray="4 4"
      />

      <!-- Y axis labels -->
      <text
        v-for="g in gridLines"
        :key="'yl-' + g.value"
        :x="PAD.left - 8"
        :y="PAD.top + plotH - ((g.value - 0.5) / 4) * plotH + 4"
        text-anchor="end"
        fill="rgba(255,232,190,0.5)"
        font-size="11"
        font-family="var(--font-display)"
      >{{ g.label }}</text>

      <!-- area fill -->
      <path v-if="areaPath" :d="areaPath" fill="url(#dtc-area-grad)" />

      <!-- line -->
      <path
        v-if="linePath"
        :d="linePath"
        fill="none"
        stroke="url(#dtc-line-grad)"
        stroke-width="2.5"
        stroke-linejoin="round"
        stroke-linecap="round"
      />

      <!-- data points -->
      <g v-for="(pt, i) in points" :key="i">
        <!-- pulse ring -->
        <circle
          :cx="pt.x"
          :cy="pt.y"
          r="8"
          fill="none"
          stroke="rgba(241,183,86,0.4)"
          stroke-width="1.5"
          class="dtc-pulse"
        />
        <!-- solid dot -->
        <circle
          :cx="pt.x"
          :cy="pt.y"
          r="4"
          fill="#f1b756"
          stroke="#2a180d"
          stroke-width="1.5"
          class="dtc-dot"
          @pointerenter="showTooltip(pt, $event)"
          @pointerleave="hideTooltip"
        />
      </g>

      <!-- X axis date labels (visible range) -->
      <text
        v-for="(pt, i) in points"
        :key="'xl-' + i"
        :x="pt.x"
        :y="PAD.top + plotH + 14"
        text-anchor="middle"
        fill="rgba(255,232,190,0.45)"
        font-size="10"
      >{{ pt.date.slice(5) }}</text>
    </svg>

    <!-- tooltip -->
    <div
      v-if="tooltipVisible && tooltip"
      class="dtc-tooltip"
      :style="{
        left: tooltip.x + 'px',
        top: (tooltip.y - 10) + 'px',
      }"
    >
      <strong>{{ tooltip.title }}</strong>
      <span>{{ tooltip.date }} · {{ tooltip.difficulty }} · +{{ tooltip.xp }} XP</span>
    </div>

    <!-- current level text -->
    <p class="dtc-current" v-if="currentLevel">当前挑战水平：{{ currentLevel }}</p>

    <!-- dual slider -->
    <div class="dtc-slider-wrap">
      <div class="dtc-track" ref="trackEl">
        <div
          class="dtc-track-highlight"
          :style="{
            left: (rangeStart * 100) + '%',
            width: ((rangeEnd - rangeStart) * 100) + '%',
          }"
        />
        <!-- data density dots on track -->
        <span
          v-for="(d, i) in sorted"
          :key="'dot-' + i"
          class="dtc-track-dot"
          :style="{ left: (dateRatio(d.date) * 100) + '%' }"
        />
        <!-- left handle -->
        <span
          class="dtc-handle dtc-handle-start"
          :style="{ left: (rangeStart * 100) + '%' }"
          @pointerdown="onPointerDown('start', $event)"
        />
        <!-- right handle -->
        <span
          class="dtc-handle dtc-handle-end"
          :style="{ left: (rangeEnd * 100) + '%' }"
          @pointerdown="onPointerDown('end', $event)"
        />
      </div>
      <div class="dtc-track-labels">
        <span v-for="label in trackDateLabels" :key="label">{{ label.slice(5) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dtc-wrap {
  position: relative;
}

.dtc-svg {
  display: block;
  width: 100%;
  height: auto;
}

.dtc-pulse {
  animation: dtc-pulse 2.4s ease-in-out infinite;
}

@keyframes dtc-pulse {
  0%, 100% { opacity: 0.4; r: 8; }
  50%      { opacity: 0;   r: 14; }
}

.dtc-dot {
  cursor: pointer;
  transition: r 0.15s ease;
}

.dtc-dot:hover {
  r: 6;
}

.dtc-tooltip {
  position: absolute;
  transform: translate(-50%, -100%);
  padding: 8px 12px;
  border: 1px solid rgba(241,183,86,0.5);
  border-radius: var(--radius, 6px);
  background: rgba(12,7,4,0.92);
  color: #ffe2a0;
  font-size: 0.82rem;
  white-space: nowrap;
  pointer-events: none;
  z-index: 10;
  display: grid;
  gap: 2px;
}

.dtc-tooltip strong {
  font-family: var(--font-display);
  font-size: 0.9rem;
}

.dtc-tooltip span {
  color: rgba(255,232,190,0.7);
  font-size: 0.78rem;
}

.dtc-current {
  margin: 8px 0 0;
  text-align: center;
  color: rgba(255,232,190,0.7);
  font-size: 0.85rem;
  font-family: var(--font-display);
}

/* ─── slider ──────────────────────────────────────── */

.dtc-slider-wrap {
  margin-top: 12px;
  padding: 0 4px;
}

.dtc-track {
  position: relative;
  height: 28px;
  background: rgba(255,232,190,0.06);
  border-radius: 999px;
  cursor: pointer;
}

.dtc-track-highlight {
  position: absolute;
  top: 0;
  height: 100%;
  background: rgba(241,183,86,0.18);
  border-radius: 999px;
  pointer-events: none;
}

.dtc-track-dot {
  position: absolute;
  top: 50%;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(241,183,86,0.35);
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.dtc-handle {
  position: absolute;
  top: 50%;
  width: 18px;
  height: 18px;
  border: 2px solid #f1b756;
  border-radius: 50%;
  background: #2a180d;
  transform: translate(-50%, -50%);
  cursor: ew-resize;
  z-index: 2;
  transition: box-shadow 0.15s ease;
  touch-action: none;
}

.dtc-handle:hover,
.dtc-handle:active {
  box-shadow: 0 0 10px rgba(241,183,86,0.6);
}

.dtc-track-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  padding: 0 2px;
  color: rgba(255,232,190,0.4);
  font-size: 0.7rem;
}
</style>
