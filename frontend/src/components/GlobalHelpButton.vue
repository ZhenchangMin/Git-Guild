<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { openHelpManual } from '../composables/useHelpManual'
import { routeTutorialMap } from '../data/tutorials'

const route = useRoute()

const routeName = computed(() => (typeof route.name === 'string' ? route.name : ''))
const tutorialId = computed(() => routeTutorialMap[routeName.value] ?? '')
const shouldShow = computed(() => routeName.value !== 'help')

function openHelp() {
  openHelpManual({
    sourceRoute: routeName.value,
    tutorialId: tutorialId.value,
  })
}
</script>

<template>
  <button
    v-if="shouldShow"
    class="help-orb"
    type="button"
    aria-label="打开 Git Guild 使用手册"
    :data-tutorial="routeName === 'hall' ? 'hall-help' : undefined"
    @click="openHelp"
  >?</button>
</template>
