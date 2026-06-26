<script setup>
import { RouterView } from 'vue-router'

import GlobalHelpButton from './components/GlobalHelpButton.vue'
import HelpManualOverlay from './components/HelpManualOverlay.vue'
import MessageCenter from './components/messages/MessageCenter.vue'
import NotificationCenter from './components/NotificationCenter.vue'
import TutorialCoachmark from './components/TutorialCoachmark.vue'
</script>

<template>
  <RouterView />
  <GlobalHelpButton />
  <HelpManualOverlay />
  <MessageCenter />
  <NotificationCenter />
  <TutorialCoachmark />
</template>
