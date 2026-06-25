<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { routeTutorialMap } from '../data/tutorials'

const route = useRoute()
const router = useRouter()

const routeName = computed(() => (typeof route.name === 'string' ? route.name : ''))
const tutorialId = computed(() => routeTutorialMap[routeName.value] ?? '')
const shouldShow = computed(() => routeName.value !== 'help')

function openHelp() {
  const query = {
    returnTo: route.fullPath,
  }
  if (tutorialId.value) query.tutorialId = tutorialId.value
  router.push({ name: 'help', query }).catch(() => {})
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
