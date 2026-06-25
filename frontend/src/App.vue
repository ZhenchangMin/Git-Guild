<script setup>
import { RouterView } from 'vue-router'

import MessageCenter from './components/messages/MessageCenter.vue'
import NotificationCenter from './components/NotificationCenter.vue'
import TutorialCoachmark from './components/TutorialCoachmark.vue'
</script>

<template>
  <RouterView />
  <MessageCenter />
  <NotificationCenter />
  <TutorialCoachmark />
</template>
