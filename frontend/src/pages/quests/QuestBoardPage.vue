<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import questBoardImg from '../../assets/quest board.png'
import { questCommissions, questFilterGroups } from '../../data/questBoard'

const router = useRouter()
const questSearch = ref('')
const questPage = ref(1)
const questPageSize = 4
const selectedQuestFilters = ref({
  category: [],
  tag: [],
  difficulty: [],
  stack: [],
  status: [],
})

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
        <div class="quest-search-band">
          <label for="quest-search">搜索委托</label>
          <input
            id="quest-search"
            v-model="questSearch"
            type="search"
            autocomplete="off"
            placeholder="输入任务、技术栈、完成标准..."
          />
          <span>{{ rankedQuestCommissions.length }} 条匹配</span>
        </div>

        <aside class="quest-filter-rail" aria-label="筛选条件">
          <div class="filter-rail-head">
            <div>
              <p class="kicker">筛选条件</p>
              <h2>筛选条件</h2>
            </div>
            <button type="button" @click="clearQuestFilters">清空</button>
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
          <header class="commission-panel-head">
            <div>
              <p class="kicker">悬赏任务列表</p>
              <h1>委托清单</h1>
            </div>
            <span>按关联度排序</span>
          </header>

          <div v-if="pagedQuestCommissions.length > 0" class="commission-grid">
            <article v-for="quest in pagedQuestCommissions" :key="quest.id" class="commission-card">
              <div class="commission-card-top">
                <span>{{ quest.id }}</span>
                <strong>{{ quest.reward }}</strong>
              </div>
              <h2>{{ quest.title }}</h2>
              <p>{{ quest.summary }}</p>
              <div class="commission-tags">
                <span>{{ quest.category }}</span>
                <span v-for="tag in quest.tags.slice(0, 2)" :key="tag">{{ tag }}</span>
              </div>
              <dl>
                <div>
                  <dt>分类</dt>
                  <dd>{{ quest.category }}</dd>
                </div>
                <div>
                  <dt>难度</dt>
                  <dd>{{ quest.difficulty }}</dd>
                </div>
                <div>
                  <dt>技术栈</dt>
                  <dd>{{ quest.stack }}</dd>
                </div>
                <div>
                  <dt>状态</dt>
                  <dd>{{ quest.status }}</dd>
                </div>
              </dl>
              <ul>
                <li v-for="line in quest.criteria.slice(0, 2)" :key="line">{{ line }}</li>
              </ul>
              <div class="commission-actions">
                <button class="primary-action" type="button" @click="openQuestDetail(quest.id, 'accept')">接取</button>
                <button class="quiet-action" type="button" @click="openQuestDetail(quest.id, 'view')">详情</button>
              </div>
            </article>
          </div>

          <div v-else class="commission-empty">
            <p class="kicker">没有匹配</p>
            <h2>没有符合条件的委托</h2>
            <p>调整搜索内容或关闭部分筛选条件后，右侧清单会同步刷新。</p>
          </div>
        </section>

        <nav class="quest-pagination" aria-label="委托分页">
          <button type="button" :disabled="questPage === 1" @click="prevQuestPage">‹</button>
          <span>{{ questPage }} / {{ questPageCount }}</span>
          <button type="button" :disabled="questPage === questPageCount" @click="nextQuestPage">›</button>
        </nav>
      </div>
    </section>
  </main>
</template>
