import { computed, nextTick, ref } from 'vue'

import { assistantApi } from '../api/assistantApi'
import { sessionStore } from '../stores/sessionStore'

let idCounter = 0

const QUICK_QUESTIONS = {
  VISITOR: [
    'Git Guild 是什么？',
    '冒险家如何接取委托？',
    '委托人如何发布委托？',
    '有哪些演示账号？',
  ],
  ADVENTURER: [
    '我的委托下一步该做什么？',
    '如何提交成果？',
    '如何查看我的 PR？',
    '我被退回后怎么修改？',
  ],
  MAINTAINER: [
    '哪些提交需要我审核？',
    '如何发布新委托？',
    '如何合并 PR？',
    '我发布了哪些委托？',
  ],
}

export function useAssistantChat({ page }) {
  const messages = ref([])
  const draft = ref('')
  const loading = ref(false)
  const messageList = ref(null)

  const pageValue = typeof page === 'function' ? page : () => page

  const quickQuestions = computed(() => {
    const role = sessionStore.role
    if (role === 'ADVENTURER') return QUICK_QUESTIONS.ADVENTURER
    if (role === 'MAINTAINER') return QUICK_QUESTIONS.MAINTAINER
    return QUICK_QUESTIONS.VISITOR
  })

  const canSend = computed(() => draft.value.trim().length > 0 && !loading.value)

  const welcomeMessage = computed(() => {
    const role = sessionStore.role
    if (role === 'ADVENTURER') return '欢迎回来，冒险家！有什么可以帮你的？'
    if (role === 'MAINTAINER') return '欢迎回来，委托人！有什么可以帮你的？'
    return '欢迎来到 Git Guild 公会大厅！我是前台向导，可以帮你了解平台使用方法。'
  })

  function scrollToBottom() {
    nextTick(() => {
      const el = messageList.value
      if (el) el.scrollTop = el.scrollHeight
    })
  }

  async function send(messageText) {
    const text = (messageText ?? draft.value).trim()
    if (!text || loading.value) return

    messages.value.push({ id: ++idCounter, role: 'user', text })
    draft.value = ''
    loading.value = true
    scrollToBottom()

    try {
      const res = await assistantApi.chat({ message: text, page: pageValue() })
      const data = res?.data ?? res
      messages.value.push({
        id: ++idCounter,
        role: 'npc',
        text: data.answer ?? '',
        source: data.source ?? 'FALLBACK',
        suggestedQuestions: data.suggestedQuestions ?? [],
        actions: data.actions ?? [],
      })
    } catch {
      messages.value.push({
        id: ++idCounter,
        role: 'npc',
        text: '前台向导暂时无法回答。你可以查看帮助页面了解平台使用方法，或稍后再试。',
        source: 'FALLBACK',
        suggestedQuestions: ['冒险家怎么接任务？', '委托人怎么审核提交？'],
        actions: [],
      })
    } finally {
      loading.value = false
      scrollToBottom()
    }
  }

  function onKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      send()
    }
  }

  function reset() {
    messages.value = []
    draft.value = ''
    loading.value = false
  }

  return {
    messages,
    draft,
    loading,
    messageList,
    quickQuestions,
    canSend,
    welcomeMessage,
    send,
    onKeydown,
    reset,
    scrollToBottom,
  }
}
