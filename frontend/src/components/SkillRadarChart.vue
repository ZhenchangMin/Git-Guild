<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  pool: { type: Array, required: true },
})

// ─── selection state ────────────────────────────────
const selectedNames = ref(
  props.pool.filter(s => s.defaultSelected).map(s => s.name),
)

const MIN_SELECT = 3
const MAX_SELECT = 8

// ─── toast hint ─────────────────────────────────────
const toastMsg = ref('')
let toastTimer = null

function showToast(msg) {
  toastMsg.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastMsg.value = '' }, 2000)
}

function toggle(name) {
  const idx = selectedNames.value.indexOf(name)
  if (idx >= 0) {
    if (selectedNames.value.length <= MIN_SELECT) {
      showToast(`至少需要选择 ${MIN_SELECT} 个技能`)
      return
    }
    selectedNames.value = selectedNames.value.filter(n => n !== name)
  } else {
    if (selectedNames.value.length >= MAX_SELECT) {
      showToast(`最多选择 ${MAX_SELECT} 个技能`)
      return
    }
    selectedNames.value = [...selectedNames.value, name]
  }
}

const isSelected = (name) => selectedNames.value.includes(name)

// ─── selected skill data ────────────────────────────
const selectedSkills = computed(() =>
  selectedNames.value
    .map(name => props.pool.find(s => s.name === name))
    .filter(Boolean),
)

// ─── radar geometry ─────────────────────────────────
const SIZE = 320
const CX = SIZE / 2
const CY = SIZE / 2
const R = 120
const RINGS = 4

const n = computed(() => selectedSkills.value.length)
const angleStep = computed(() => (2 * Math.PI) / n.value)

// vertex positions for a given value (0-100)
function vertex(index, value) {
  const angle = -Math.PI / 2 + index * angleStep.value
  const r = (value / 100) * R
  return {
    x: CX + r * Math.cos(angle),
    y: CY + r * Math.sin(angle),
  }
}

// label position (outside the outermost ring)
function labelPos(index) {
  const angle = -Math.PI / 2 + index * angleStep.value
  const lr = R + 22
  return {
    x: CX + lr * Math.cos(angle),
    y: CY + lr * Math.sin(angle),
  }
}

// text-anchor based on position
function anchor(index) {
  const angle = -Math.PI / 2 + index * angleStep.value
  const deg = ((angle * 180) / Math.PI + 360) % 360
  if (deg > 60 && deg < 120) return 'start'
  if (deg > 240 && deg < 300) return 'end'
  return 'middle'
}

// ─── animated polygon ───────────────────────────────
const animPoints = ref([])

function syncAnimPoints() {
  const target = selectedSkills.value.map((s, i) => vertex(i, s.value))
  // if lengths differ, jump; otherwise interpolate
  if (animPoints.value.length !== target.length) {
    animPoints.value = target
    return
  }
  animateTo(target)
}

let rafId = null

function animateTo(target) {
  if (rafId) cancelAnimationFrame(rafId)
  const start = animPoints.value.map(p => ({ ...p }))
  const startTime = performance.now()
  const duration = 400

  function step(now) {
    const t = Math.min(1, (now - startTime) / duration)
    const ease = t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t // easeInOut
    animPoints.value = start.map((s, i) => ({
      x: s.x + (target[i].x - s.x) * ease,
      y: s.y + (target[i].y - s.y) * ease,
    }))
    if (t < 1) rafId = requestAnimationFrame(step)
  }
  rafId = requestAnimationFrame(step)
}

watch(selectedSkills, syncAnimPoints, { immediate: true, deep: true })

onBeforeUnmount(() => {
  if (rafId) cancelAnimationFrame(rafId)
})

const polygonPath = computed(() => {
  if (animPoints.value.length < 3) return ''
  return animPoints.value
    .map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`)
    .join(' ') + ' Z'
})

// ─── ring paths ─────────────────────────────────────
function ringPath(r) {
  const pts = []
  const segs = 60
  for (let i = 0; i <= segs; i++) {
    const angle = (2 * Math.PI * i) / segs - Math.PI / 2
    pts.push(`${CX + r * Math.cos(angle)},${CY + r * Math.sin(angle)}`)
  }
  return `M${pts.join(' L')} Z`
}

// ─── axis lines ─────────────────────────────────────
function axisLine(index) {
  const angle = -Math.PI / 2 + index * angleStep.value
  return {
    x2: CX + R * Math.cos(angle),
    y2: CY + R * Math.sin(angle),
  }
}

// ─── summary ────────────────────────────────────────
const strongest = computed(() => {
  if (selectedSkills.value.length === 0) return null
  return selectedSkills.value.reduce((a, b) => (a.value >= b.value ? a : b))
})

const weakest = computed(() => {
  if (selectedSkills.value.length === 0) return null
  return selectedSkills.value.reduce((a, b) => (a.value <= b.value ? a : b))
})
</script>

<template>
  <div class="src-wrap">
    <svg :viewBox="`0 0 ${SIZE} ${SIZE}`" class="src-svg" aria-label="技能雷达图">
      <defs>
        <linearGradient id="src-fill-grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="rgba(241,183,86,0.25)" />
          <stop offset="100%" stop-color="rgba(241,183,86,0.05)" />
        </linearGradient>
      </defs>

      <!-- concentric rings -->
      <path
        v-for="i in RINGS"
        :key="'ring-' + i"
        :d="ringPath((R / RINGS) * i)"
        fill="none"
        stroke="rgba(238,184,91,0.12)"
        stroke-width="1"
      />

      <!-- axis lines + labels -->
      <g v-for="(skill, i) in selectedSkills" :key="'axis-' + skill.name">
        <line
          :x1="CX"
          :y1="CY"
          :x2="axisLine(i).x2"
          :y2="axisLine(i).y2"
          stroke="rgba(238,184,91,0.1)"
          stroke-width="1"
        />
        <text
          :x="labelPos(i).x"
          :y="labelPos(i).y + 4"
          :text-anchor="anchor(i)"
          fill="rgba(255,232,190,0.75)"
          font-size="11"
          font-family="var(--font-display)"
        >{{ skill.name }}</text>
      </g>

      <!-- data polygon -->
      <path
        v-if="polygonPath"
        :d="polygonPath"
        fill="url(#src-fill-grad)"
        stroke="#f1b756"
        stroke-width="2"
        stroke-linejoin="round"
      />

      <!-- data points -->
      <circle
        v-for="(pt, i) in animPoints"
        :key="'dp-' + i"
        :cx="pt.x"
        :cy="pt.y"
        r="4"
        fill="#ffe2a0"
        stroke="#2a180d"
        stroke-width="1.5"
      />
    </svg>

    <!-- summary -->
    <div class="src-summary" v-if="strongest && weakest && strongest.name !== weakest.name">
      <p><span class="src-tag gold">最强</span> {{ strongest.name }} {{ strongest.value }}%</p>
      <p><span class="src-tag dim">待提升</span> {{ weakest.name }} {{ weakest.value }}%</p>
    </div>

    <!-- label selector -->
    <div class="src-selector">
      <button
        v-for="skill in pool"
        :key="skill.name"
        type="button"
        class="src-tag-btn"
        :class="{ active: isSelected(skill.name) }"
        @click="toggle(skill.name)"
      >
        {{ skill.name }}
      </button>
    </div>

    <!-- toast hint -->
    <Transition name="src-toast">
      <p class="src-toast" v-if="toastMsg">{{ toastMsg }}</p>
    </Transition>
  </div>
</template>

<style scoped>
.src-wrap {
  display: grid;
  gap: 12px;
}

.src-svg {
  display: block;
  width: 100%;
  max-width: 340px;
  margin: 0 auto;
  height: auto;
}

.src-summary {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}

.src-summary p {
  margin: 0;
  color: rgba(255,232,190,0.8);
  font-size: 0.85rem;
}

.src-tag {
  display: inline-block;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 0.72rem;
  font-weight: 700;
  margin-right: 4px;
}

.src-tag.gold {
  color: #2a180d;
  background: #f2c06f;
}

.src-tag.dim {
  color: rgba(255,232,190,0.5);
  background: rgba(255,232,190,0.1);
}

/* ─── selector ──────────────────────────────────── */

.src-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.src-tag-btn {
  border: 1px solid rgba(238,184,91,0.2);
  border-radius: 999px;
  padding: 4px 12px;
  color: rgba(255,232,190,0.45);
  background: transparent;
  font-size: 0.78rem;
  transition: all 0.2s ease;
}

.src-tag-btn:hover {
  color: rgba(255,232,190,0.7);
  border-color: rgba(238,184,91,0.4);
}

.src-tag-btn.active {
  color: #ffe2a0;
  border-color: rgba(241,183,86,0.6);
  background: rgba(241,183,86,0.1);
  box-shadow: 0 0 8px rgba(241,183,86,0.15);
}

.src-hint {
  width: 100%;
  margin: 4px 0 0;
  text-align: center;
  color: rgba(255,232,190,0.5);
  font-size: 0.75rem;
}

/* ─── toast ──────────────────────────────────────── */

.src-toast {
  margin: 6px 0 0;
  padding: 6px 14px;
  border: 1px solid rgba(241,183,86,0.4);
  border-radius: 999px;
  color: #ffe2a0;
  background: rgba(12,7,4,0.85);
  font-size: 0.78rem;
  text-align: center;
  white-space: nowrap;
}

.src-toast-enter-active,
.src-toast-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.src-toast-enter-from,
.src-toast-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
