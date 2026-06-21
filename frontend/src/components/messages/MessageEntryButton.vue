<script setup>
import { computed, onMounted } from 'vue'

import {
  loadMessageThreads,
  messageStore,
  messageUnreadCount,
  openMessageCenter,
} from '../../stores/messageStore'
import { overlayStore } from '../../stores/overlayStore'

const props = defineProps({
  variant: {
    type: String,
    default: 'orb',
  },
})

const isOpen = computed(() => overlayStore.activeOverlay === 'message')
const badgeText = computed(() => (messageUnreadCount.value > 99 ? '99+' : String(messageUnreadCount.value)))
const buttonClass = computed(() => [
  'message-entry-button',
  props.variant === 'circle' ? 'message-circle-button' : 'back-orb message-orb',
  { 'is-open': isOpen.value },
])

onMounted(loadMessageThreads)
</script>

<template>
  <button
    :class="buttonClass"
    type="button"
    aria-label="打开站内信笺"
    :aria-expanded="isOpen"
    :disabled="messageStore.loadingThreads && messageStore.threads.length === 0"
    @click="openMessageCenter()"
  >
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 7h16v11H4z" />
      <path d="m4 7 8 6 8-6" />
      <path d="M8 5h8" />
    </svg>
    <span>信笺</span>
    <em v-if="messageUnreadCount > 0" class="message-badge" aria-hidden="true">{{ badgeText }}</em>
  </button>
</template>

<style scoped>
.message-entry-button {
  position: relative;
}

.message-orb.is-open span {
  display: none;
}

.message-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  background: #d84d35;
  color: #fff;
  font-size: 0.66rem;
  font-style: normal;
  font-weight: 800;
  line-height: 1;
  box-shadow: 0 0 0 2px rgba(17, 9, 5, 0.7);
}

.message-circle-button {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 1px solid rgba(238, 184, 91, 0.62);
  border-radius: 50%;
  color: #ffe4ad;
  background: rgba(17, 9, 5, 0.44);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.32);
  cursor: pointer;
  transition: background 160ms ease, border-color 160ms ease, transform 160ms ease;
}

.message-circle-button:hover,
.message-circle-button:focus-visible,
.message-circle-button.is-open {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.72);
  transform: translateY(-1px);
}

.message-circle-button svg {
  width: 24px;
  height: 24px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.message-circle-button span {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
</style>
