<script setup>
defineProps({
  leaving: {
    type: Boolean,
    default: false,
  },
  statusText: {
    type: String,
    default: '正在点亮公会大厅...',
  },
})

const dustMotes = [
  { left: 18, top: 62, delay: 120, size: 4 },
  { left: 29, top: 39, delay: 420, size: 3 },
  { left: 39, top: 70, delay: 760, size: 5 },
  { left: 52, top: 31, delay: 260, size: 3 },
  { left: 63, top: 58, delay: 620, size: 4 },
  { left: 73, top: 42, delay: 980, size: 3 },
  { left: 82, top: 67, delay: 540, size: 5 },
]
</script>

<template>
  <section
    class="hall-entry-transition"
    :class="{ 'is-leaving': leaving }"
    role="status"
    aria-live="polite"
    aria-label="正在进入 GitGuild 公会大厅"
  >
    <div class="hall-entry-glow" aria-hidden="true"></div>
    <div class="hall-entry-rays" aria-hidden="true"></div>
    <div class="hall-entry-dust" aria-hidden="true">
      <i
        v-for="mote in dustMotes"
        :key="`${mote.left}-${mote.top}`"
        :style="{
          left: `${mote.left}%`,
          top: `${mote.top}%`,
          width: `${mote.size}px`,
          height: `${mote.size}px`,
          animationDelay: `${mote.delay}ms`,
        }"
      ></i>
    </div>

    <div class="hall-entry-copy">
      <p class="hall-entry-kicker">Guild Gate</p>
      <h1>GitGuild</h1>
      <p class="hall-entry-line">Every expert was once a beginner.</p>
      <span class="hall-entry-status">{{ statusText }}</span>
    </div>
  </section>
</template>

<style scoped>
.hall-entry-transition {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: #fff0c4;
  background:
    radial-gradient(circle at 50% 48%, rgba(255, 207, 122, 0.2), transparent 0 44%),
    radial-gradient(circle at 50% 58%, rgba(88, 45, 15, 0.74), rgba(24, 12, 5, 0.94) 74%),
    #160a03;
  isolation: isolate;
  pointer-events: all;
  transition: opacity 620ms ease, filter 620ms ease;
}

.hall-entry-transition::before {
  position: absolute;
  inset: 0;
  z-index: -1;
  content: '';
  background:
    radial-gradient(circle at 50% 50%, rgba(255, 239, 184, 0.76) 0 6%, rgba(255, 206, 113, 0.54) 16%, rgba(214, 126, 35, 0.4) 34%, rgba(142, 75, 20, 0.28) 58%, transparent 82%),
    linear-gradient(90deg, rgba(255, 215, 139, 0.54), rgba(255, 207, 124, 0.16) 25%, rgba(91, 43, 12, 0.12) 43%, rgba(91, 43, 12, 0.12) 57%, rgba(255, 207, 124, 0.16) 75%, rgba(255, 215, 139, 0.54));
  animation: hallEntryWash 1800ms cubic-bezier(0.18, 0.72, 0.2, 1) forwards;
  mix-blend-mode: screen;
}

.hall-entry-transition::after {
  position: absolute;
  inset: 0;
  z-index: -1;
  content: '';
  background:
    radial-gradient(circle at 50% 50%, transparent 0 24%, rgba(31, 14, 4, 0.08) 54%, rgba(7, 3, 1, 0.34) 100%),
    linear-gradient(90deg, rgba(12, 5, 1, 0.42), rgba(35, 16, 5, 0.08) 31%, rgba(35, 16, 5, 0.08) 69%, rgba(12, 5, 1, 0.42));
  opacity: 0;
  animation: hallEntryVignette 1500ms ease 480ms forwards;
}

.hall-entry-transition.is-leaving {
  opacity: 0;
  filter: brightness(1.12) blur(3px);
}

.hall-entry-glow {
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(86vmax, 1200px);
  height: min(86vmax, 1200px);
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(255, 236, 184, 0.62) 0 8%, rgba(255, 205, 112, 0.42) 22%, rgba(201, 112, 31, 0.24) 48%, transparent 76%);
  filter: blur(22px);
  opacity: 0.76;
  transform: translate(-50%, -50%) scale(0.7);
  animation: hallEntryGlowSettle 2200ms cubic-bezier(0.16, 0.82, 0.2, 1) forwards;
  mix-blend-mode: screen;
}

.hall-entry-rays {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 170vmax;
  height: 170vmax;
  background:
    repeating-conic-gradient(
      from -8deg at 50% 50%,
      transparent 0deg 8deg,
      rgba(255, 223, 149, 0.16) 8deg 10deg
    );
  -webkit-mask-image: radial-gradient(circle, rgba(0, 0, 0, 0.75) 0 18%, rgba(0, 0, 0, 0.28) 42%, transparent 68%);
  mask-image: radial-gradient(circle, rgba(0, 0, 0, 0.75) 0 18%, rgba(0, 0, 0, 0.28) 42%, transparent 68%);
  opacity: 0.56;
  transform: translate(-50%, -50%) rotate(-8deg);
  animation: hallEntryRays 3600ms ease-in-out infinite alternate;
  mix-blend-mode: screen;
}

.hall-entry-dust {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hall-entry-dust i {
  position: absolute;
  border-radius: 50%;
  background: #ffe7a8;
  box-shadow: 0 0 16px rgba(255, 214, 131, 0.9);
  opacity: 0;
  animation: hallEntryDust 2800ms ease-in-out infinite;
}

.hall-entry-copy {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 10px;
  width: min(680px, calc(100vw - 44px));
  text-align: center;
  text-wrap: pretty;
  opacity: 0;
  transform: translateY(18px) scale(0.98);
  animation: hallEntryCopyReveal 760ms cubic-bezier(0.18, 0.84, 0.24, 1) 360ms forwards;
}

.hall-entry-kicker {
  margin: 0;
  color: rgba(255, 235, 180, 0.66);
  font-size: 0.78rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hall-entry-copy h1 {
  margin: 0;
  color: #fff4cc;
  font-family: var(--font-display);
  font-size: clamp(3.3rem, 12vw, 8.2rem);
  font-weight: 700;
  line-height: 0.86;
  text-shadow:
    0 0 18px rgba(255, 236, 182, 0.74),
    0 0 54px rgba(255, 186, 72, 0.58),
    0 12px 28px rgba(35, 13, 2, 0.7);
}

.hall-entry-line {
  margin: 4px 0 0;
  color: rgba(255, 234, 190, 0.84);
  font-size: clamp(1rem, 2.4vw, 1.35rem);
  line-height: 1.5;
}

.hall-entry-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  margin-top: 10px;
  border: 1px solid rgba(255, 220, 145, 0.36);
  border-radius: 999px;
  padding: 0 16px;
  color: #ffe6ad;
  background: rgba(22, 10, 3, 0.38);
  box-shadow: inset 0 1px 0 rgba(255, 244, 209, 0.16), 0 0 28px rgba(255, 190, 82, 0.18);
  backdrop-filter: blur(8px);
  font-size: 0.92rem;
  animation: hallEntryStatusPulse 1600ms ease-in-out infinite;
}

@keyframes hallEntryWash {
  0% {
    opacity: 1;
    filter: brightness(1.06) blur(0);
    transform: scale(1);
  }

  58% {
    opacity: 0.72;
  }

  100% {
    opacity: 0.42;
    filter: brightness(1) blur(18px);
    transform: scale(1.22);
  }
}

@keyframes hallEntryVignette {
  to {
    opacity: 1;
  }
}

@keyframes hallEntryGlowSettle {
  0% {
    opacity: 0.82;
    transform: translate(-50%, -50%) scale(0.58);
  }

  100% {
    opacity: 0.58;
    transform: translate(-50%, -50%) scale(1.14);
  }
}

@keyframes hallEntryRays {
  from {
    opacity: 0.42;
    transform: translate(-50%, -50%) rotate(-10deg) scale(0.96);
  }

  to {
    opacity: 0.62;
    transform: translate(-50%, -50%) rotate(10deg) scale(1.04);
  }
}

@keyframes hallEntryCopyReveal {
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes hallEntryDust {
  0% {
    opacity: 0;
    transform: translate3d(0, 22px, 0) scale(0.7);
  }

  35% {
    opacity: 0.8;
  }

  100% {
    opacity: 0;
    transform: translate3d(10px, -48px, 0) scale(1.08);
  }
}

@keyframes hallEntryStatusPulse {
  0%,
  100% {
    opacity: 0.72;
    box-shadow: inset 0 1px 0 rgba(255, 244, 209, 0.16), 0 0 22px rgba(255, 190, 82, 0.14);
  }

  50% {
    opacity: 1;
    box-shadow: inset 0 1px 0 rgba(255, 244, 209, 0.22), 0 0 34px rgba(255, 190, 82, 0.26);
  }
}

@media (max-width: 640px) {
  .hall-entry-copy {
    gap: 8px;
  }

  .hall-entry-status {
    max-width: 100%;
    min-height: 38px;
    padding: 0 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hall-entry-transition,
  .hall-entry-transition::before,
  .hall-entry-transition::after,
  .hall-entry-glow,
  .hall-entry-rays,
  .hall-entry-dust i,
  .hall-entry-copy,
  .hall-entry-status {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
