import { readonly, reactive } from 'vue'

const state = reactive({
  isOpen: false,
  sourceRoute: '',
  tutorialId: '',
  openedAt: 0,
})

export function openHelpManual({ sourceRoute = '', tutorialId = '' } = {}) {
  state.isOpen = true
  state.sourceRoute = sourceRoute
  state.tutorialId = tutorialId
  state.openedAt = Date.now()
}

export function closeHelpManual() {
  state.isOpen = false
}

export function useHelpManual() {
  return {
    helpManualState: readonly(state),
    openHelpManual,
    closeHelpManual,
  }
}
