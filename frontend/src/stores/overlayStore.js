import { reactive } from 'vue'

export const overlayStore = reactive({
  activeOverlay: null,
})

export function openOverlay(name) {
  overlayStore.activeOverlay = name
}

export function closeOverlay(name = null) {
  if (!name || overlayStore.activeOverlay === name) {
    overlayStore.activeOverlay = null
  }
}

export function isOverlayOpen(name) {
  return overlayStore.activeOverlay === name
}
