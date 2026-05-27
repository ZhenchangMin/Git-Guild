<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import questBoardImg from '../../assets/quest board.png'
import { questCommissions, questFilterGroups } from '../../data/questBoard'

const router = useRouter()
const questSearch = ref('')
const questPage = ref(1)
const questPageSize = 18
const selectedQuestFilters = ref({
  category: [],
  tag: [],
  difficulty: [],
  stack: [],
  status: [],
})

// Map each quest status to a seal colour so the board reads at a glance.
const STATUS_TONE = {
  可接取: 'open',
  进行中: 'active',
  'PR 已就绪': 'ready',
  待审核: 'review',
  退回修改: 'returned',
}

function statusTone(status) {
  return STATUS_TONE[status] || 'open'
}

const activeFilterCount = computed(() =>
  Object.values(selectedQuestFilters.value).reduce((total, list) => total + list.length, 0),
)

const rankedQuestCommissions = computed(() => {
  const selected = selectedQuestFilters.value
  const query = questSearch.value.trim().toLowerCase()

  return questCommissions
    .map((quest) => {
      const groupMatches = {
        category: selected.category.length === 0 || selected.category.includes(quest.category),
        tag: selected.tag.length === 0 || selected.tag.some((tag) => quest.tags.includes(tag)),
        difficulty: selected.difficulty.length === 0 || selected.difficulty.includes(quest.difficulty),
        stack: selected.stack.length === 0 || selected.stack.some((stack) => quest.techStack.includes(stack)),
        status: selected.status.length === 0 || selected.status.includes(quest.status),
      }
      const filterMatched = Object.values(groupMatches).every(Boolean)
      const searchableText = [
        quest.id,
        quest.title,
        quest.issuer,
        quest.category,
        quest.difficulty,
        quest.stack,
        quest.status,
        quest.reward,
        quest.summary,
        ...quest.tags,
        ...quest.techStack,
        ...quest.criteria,
      ]
        .join(' ')
        .toLowerCase()

      let relevance = 0
      if (!query) {
        relevance = 1
      } else {
        const tokens = query.split(/\s+/).filter(Boolean)
        relevance = tokens.reduce((score, token) => {
          if (quest.id.toLowerCase().includes(token)) score += 8
          if (quest.title.toLowerCase().includes(token)) score += 7
          if (quest.summary.toLowerCase().includes(token)) score += 5
          if (quest.stack.toLowerCase().includes(token)) score += 4
          if (quest.category.toLowerCase().includes(token)) score += 4
          if (quest.tags.some((tag) => tag.toLowerCase().includes(token))) score += 3
          if (quest.criteria.some((line) => line.toLowerCase().includes(token))) score += 3
          if (searchableText.includes(token)) score += 1
          return score
        }, 0)
      }

      return { ...quest, relevance, filterMatched }
    })
    .filter((quest) => quest.filterMatched && (!query || quest.relevance > 0))
    .sort((a, b) => b.relevance - a.relevance || a.id.localeCompare(b.id))
})

const questPageCount = computed(() => Math.max(1, Math.ceil(rankedQuestCommissions.value.length / questPageSize)))
const pagedQuestCommissions = computed(() => {
  const start = (questPage.value - 1) * questPageSize
  return rankedQuestCommissions.value.slice(start, start + questPageSize)
})

function toggleQuestFilter(groupId, option) {
  const current = selectedQuestFilters.value[groupId]
  if (!current) return
  selectedQuestFilters.value[groupId] = current.includes(option)
    ? current.filter((item) => item !== option)
    : [...current, option]
  questPage.value = 1
}

function isQuestFilterSelected(groupId, option) {
  return selectedQuestFilters.value[groupId]?.includes(option)
}

function clearQuestFilters() {
  selectedQuestFilters.value = {
    category: [],
    tag: [],
    difficulty: [],
    stack: [],
    status: [],
  }
  questPage.value = 1
}

function prevQuestPage() {
  questPage.value = Math.max(1, questPage.value - 1)
}

function nextQuestPage() {
  questPage.value = Math.min(questPageCount.value, questPage.value + 1)
}

function openQuestDetail(questId, intent = 'view') {
  router.push({
    name: 'quest-detail',
    params: { questId },
    query: intent === 'accept' ? { intent } : {},
  })
}

function backToHall() {
  router.push({ name: 'hall' })
}

watch(questSearch, () => {
  questPage.value = 1
})

watch(questPageCount, (pageCount) => {
  if (questPage.value > pageCount) {
    questPage.value = pageCount
  }
})
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene quest-mode" :style="{ backgroundImage: `url(${questBoardImg})` }">
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回公会大厅</span>
      </button>

      <div class="quest-board-workspace" aria-label="悬赏板">
        <div class="quest-board-top">
        <header class="board-header">
          <p class="kicker">冒险者公会 · 悬赏板</p>
          <h1>委托清单</h1>
          <p class="board-sub">挑选一份契约，开启你的冒险旅程。</p>
        </header>

        <div class="quest-toolbar">
          <label class="quest-search" for="quest-search">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
            <input
              id="quest-search"
              v-model="questSearch"
              type="search"
              autocomplete="off"
              placeholder="搜索任务、技术栈或完成标准…"
            />
          </label>
          <span class="quest-count">{{ rankedQuestCommissions.length }} 份委托</span>
        </div>
        </div>

        <div class="board-body">
          <aside class="quest-filter-rail" aria-label="筛选条件">
            <div class="filter-rail-head">
              <h2>筛选条件</h2>
              <button type="button" :disabled="activeFilterCount === 0" @click="clearQuestFilters">
                清空<template v-if="activeFilterCount"> · {{ activeFilterCount }}</template>
              </button>
            </div>

            <div class="quest-filter-list">
              <section v-for="group in questFilterGroups" :key="group.id" class="quest-filter-group">
                <h3>{{ group.title }}</h3>
                <div class="quest-filter-options">
                  <button
                    v-for="option in group.options"
                    :key="option"
                    type="button"
                    class="quest-filter-chip"
                    :class="{ active: isQuestFilterSelected(group.id, option) }"
                    :aria-pressed="isQuestFilterSelected(group.id, option)"
                    @click="toggleQuestFilter(group.id, option)"
                  >
                    {{ option }}
                  </button>
                </div>
              </section>
            </div>
          </aside>

          <section class="commission-panel" aria-live="polite">
            <div v-if="pagedQuestCommissions.length > 0" class="commission-grid">
              <article v-for="quest in pagedQuestCommissions" :key="quest.id" class="commission-card">
                <span class="commission-seal" :class="`tone-${statusTone(quest.status)}`">{{ quest.status }}</span>

                <div class="commission-card-top">
                  <span class="commission-id">{{ quest.id }}</span>
                  <span class="commission-rank" :title="`难度 ${quest.difficulty} 阶`">{{ quest.difficulty }} 阶</span>
                </div>

                <h3 class="commission-title">{{ quest.title }}</h3>
                <p class="commission-issuer">{{ quest.issuer }}</p>
                <p class="commission-summary">{{ quest.summary }}</p>

                <div class="commission-criteria">
                  <p class="commission-label">验收标准</p>
                  <ul>
                    <li v-for="line in quest.criteria" :key="line">{{ line }}</li>
                  </ul>
                </div>

                <div class="commission-stack">
                  <span v-for="tech in quest.techStack" :key="tech">{{ tech }}</span>
                </div>

                <footer class="commission-foot">
                  <span class="commission-reward">{{ quest.reward }}</span>
                  <div class="commission-actions">
                    <button class="quiet-action" type="button" @click="openQuestDetail(quest.id, 'view')">详情</button>
                    <button class="primary-action" type="button" @click="openQuestDetail(quest.id, 'accept')">接取委托</button>
                  </div>
                </footer>
              </article>
            </div>

            <div v-else class="commission-empty">
              <p class="kicker">空空如也</p>
              <h2>没有符合条件的委托</h2>
              <p>调整搜索词或清空部分筛选条件，悬赏板会立即刷新。</p>
              <button class="quiet-action" type="button" @click="clearQuestFilters">清空筛选</button>
            </div>

            <nav v-if="questPageCount > 1" class="quest-pagination" aria-label="委托分页">
              <button type="button" :disabled="questPage === 1" @click="prevQuestPage" aria-label="上一页">‹</button>
              <span>{{ questPage }} / {{ questPageCount }}</span>
              <button type="button" :disabled="questPage === questPageCount" @click="nextQuestPage" aria-label="下一页">›</button>
            </nav>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>
