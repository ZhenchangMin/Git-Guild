<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import questBoardImg from '../../assets/quest board.png'
import QuestDetail from '../../components/QuestDetail.vue'
import { questCommissions } from '../../data/questBoard'

const route = useRoute()
const router = useRouter()

const activeQuest = computed(() => {
  const questId = String(route.params.questId || 'QST-0427')
  return questCommissions.find((quest) => quest.id === questId) ?? questCommissions[0]
})

const activeQuestIntent = computed(() => (route.query.intent === 'accept' ? 'accept' : 'view'))

function backToQuestBoard() {
  router.push({ name: 'quest-board' })
}

function openWorkbench() {
  router.push({ name: 'adventurer-workbench' })
}

function openSubmission(questId = activeQuest.value.id) {
  router.push({
    name: 'submission-counter',
    query: { questId },
  })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene quest-detail-mode" :style="{ backgroundImage: `url(${questBoardImg})` }">
      <button class="back-orb" type="button" aria-label="返回悬赏任务板" @click="backToQuestBoard">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回悬赏任务板</span>
      </button>

      <QuestDetail
        :quest="activeQuest"
        :intent="activeQuestIntent"
        @open-workbench="openWorkbench"
        @open-submission="openSubmission"
      />
    </section>
  </main>
</template>
