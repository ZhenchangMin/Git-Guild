<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { questApi } from '../../api/questApi'
import questBoardImg from '../../assets/quest board.png'
import { questFilterGroups } from '../../data/questBoard'
import { sessionStore } from '../../stores/sessionStore'

const router = useRouter()
const questSearch = ref('')
const questPage = ref(1)
const questPageSize = 18
const recommendationLimit = 20
const loadingSkeletonCards = Array.from({ length: 6 }, (_, index) => index)
const questSource = ref([])
const taxonomyFilterOptions = ref({
  category: [],
  tag: [],
})
const recommendationMeta = ref(null)
const recommendationError = ref('')
const isRecommendationLoading = ref(true)
const usingRecommendedSource = ref(false)
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

const STATUS_LABELS = {
  PUBLISHED: '可接取',
  IN_PROGRESS: '进行中',
  PENDING_ADMIN_REVIEW: '待审核',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
  DRAFT: '草稿',
}

function statusTone(status) {
  return STATUS_TONE[status] || 'open'
}

function unwrapApiData(payload) {
  return payload?.data ?? payload ?? {}
}

function unique(values) {
  return [...new Set(values.filter(Boolean))]
}

function normalizeTaxonomyOptions(items) {
  return (items ?? [])
    .filter((item) => item?.enabled !== false)
    .map((item) => item?.name)
    .filter(Boolean)
}

function questLookupKeys(quest) {
  const keys = [quest.id, quest.routeId].filter(Boolean).map(String)
  const qstMatch = String(quest.id ?? '').match(/^QST-0*(\d+)$/i)
  if (qstMatch) keys.push(qstMatch[1])
  return unique(keys)
}

function buildQuestMap(quests) {
  const map = new Map()
  quests.forEach((quest) => {
    questLookupKeys(quest).forEach((key) => map.set(key, quest))
  })
  return map
}

function normalizeReward(rewardXp, fallback = '') {
  if (rewardXp === undefined || rewardXp === null || rewardXp === '') return fallback || '待定 XP'
  return String(rewardXp).includes('XP') ? String(rewardXp) : `${rewardXp} XP`
}

function normalizeStatus(status, fallback = '可接取') {
  return STATUS_LABELS[status] ?? status ?? fallback
}

function normalizeCriteria(value) {
  if (!value) return ['查看任务详情', '提交关联 PR', '等待委托人审核']
  if (Array.isArray(value)) return value
  return String(value)
    .split(/\r?\n|；|;/)
    .map((line) => line.trim())
    .filter(Boolean)
}

function normalizeQuestSummary(quest) {
  const techStack = Array.isArray(quest.techStack) ? quest.techStack : []
  const tagNames = Array.isArray(quest.tags)
    ? quest.tags.map((tag) => tag.name ?? tag).filter(Boolean)
    : []
  const routeId = String(quest.questId ?? quest.id ?? '')

  return {
    id: routeId,
    routeId,
    title: quest.title ?? '未命名委托',
    issuer: quest.publisher?.username
      ? `委托人 · ${quest.publisher.username}`
      : quest.repository?.name
        ? `委托人 · ${quest.repository.name}`
        : '委托人 · 推荐系统',
    category: quest.category?.name ?? '推荐',
    difficulty: quest.difficulty ?? 'C',
    stack: techStack.length > 0 ? techStack.join(' / ') : '待补充',
    techStack,
    status: normalizeStatus(quest.status),
    tags: tagNames,
    reward: normalizeReward(quest.rewardXp),
    summary: quest.descriptionPreview ?? quest.description ?? '查看详情了解这份委托的背景和完成要求。',
    criteria: normalizeCriteria(quest.completionCriteria),
  }
}

function normalizeRecommendationItem(item, questMap, index) {
  const brief = item.quest ?? {}
  const routeId = String(brief.questId ?? '')
  const fallback = questMap.get(routeId) ?? questMap.get(`QST-${routeId.padStart(4, '0')}`) ?? {}
  const techStack = Array.isArray(brief.techStack) && brief.techStack.length > 0
    ? brief.techStack
    : fallback.techStack ?? []
  const reasons = Array.isArray(item.reasons) ? item.reasons : []

  return {
    ...fallback,
    id: fallback.id ?? routeId,
    routeId,
    title: brief.title ?? fallback.title ?? '未命名委托',
    issuer: brief.repositoryName ? `委托人 · ${brief.repositoryName}` : fallback.issuer ?? '委托人 · 推荐系统',
    category: fallback.category ?? '推荐',
    difficulty: brief.difficulty ?? fallback.difficulty ?? 'C',
    stack: techStack.length > 0 ? techStack.join(' / ') : fallback.stack ?? '待补充',
    techStack,
    status: fallback.status ?? '可接取',
    tags: unique([...(fallback.tags ?? []), ...reasons]),
    reward: normalizeReward(brief.rewardXp, fallback.reward),
    summary: fallback.summary ?? '推荐算法根据你的技术栈、难度偏好和成长阶段计算出的委托。',
    criteria: fallback.criteria ?? ['查看任务详情', '确认实现范围', '提交关联 PR'],
    recommendationRank: index,
    recommendationScore: Number(item.score ?? 0),
    strongMatch: Boolean(item.strongMatch),
    recommendationReasons: reasons,
  }
}

const activeFilterCount = computed(() =>
  Object.values(selectedQuestFilters.value).reduce((total, list) => total + list.length, 0),
)

const hasQuestSearch = computed(() => questSearch.value.trim().length > 0)

const visibleQuestFilterGroups = computed(() => {
  const dynamicValues = {
    category: [
      ...taxonomyFilterOptions.value.category,
      ...questSource.value.map((quest) => quest.category),
    ],
    tag: [
      ...taxonomyFilterOptions.value.tag,
      ...questSource.value.flatMap((quest) => quest.tags ?? []),
    ],
    difficulty: questSource.value.map((quest) => quest.difficulty),
    stack: questSource.value.flatMap((quest) => quest.techStack ?? []),
    status: questSource.value.map((quest) => quest.status),
  }

  return questFilterGroups.map((group) => ({
    ...group,
    options: unique([...(group.options ?? []), ...(dynamicValues[group.id] ?? [])]),
  }))
})

const recommendationStatusText = computed(() => {
  if (isRecommendationLoading.value) return '正在根据你的成长画像计算推荐'
  if (recommendationError.value) return recommendationError.value
  if (recommendationMeta.value?.user) {
    const { level, totalXp } = recommendationMeta.value.user
    return `Lv ${level} · ${totalXp} XP`
  }
  return usingRecommendedSource.value ? '由推荐算法实时计算' : '当前显示默认委托清单'
})

const emptyDescription = computed(() => {
  if (isRecommendationLoading.value) return '推荐算法会根据技术栈、难度和成长阶段生成默认列表。'
  if (hasQuestSearch.value && activeFilterCount.value > 0) return '可以先清除搜索词，或减少左侧筛选条件后再查看结果。'
  if (hasQuestSearch.value) return `当前搜索“${questSearch.value.trim()}”没有命中任务标题、技术栈或验收标准。`
  if (activeFilterCount.value > 0) return '当前筛选组合过窄，可以移除部分条件或清空筛选。'
  return '调整搜索词或清空部分筛选条件，悬赏板会立即刷新。'
})

const rankedQuestCommissions = computed(() => {
  const selected = selectedQuestFilters.value
  const query = questSearch.value.trim().toLowerCase()

  return questSource.value
    .map((quest) => {
      const groupMatches = {
        category: selected.category.length === 0 || selected.category.includes(quest.category),
        tag: selected.tag.length === 0 || selected.tag.some((tag) => quest.tags?.includes(tag)),
        difficulty: selected.difficulty.length === 0 || selected.difficulty.includes(quest.difficulty),
        stack: selected.stack.length === 0 || selected.stack.some((stack) => quest.techStack?.includes(stack)),
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
        ...(quest.tags ?? []),
        ...(quest.techStack ?? []),
        ...(quest.criteria ?? []),
        ...(quest.recommendationReasons ?? []),
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
          if (quest.tags?.some((tag) => tag.toLowerCase().includes(token))) score += 3
          if (quest.criteria?.some((line) => line.toLowerCase().includes(token))) score += 3
          if (quest.recommendationReasons?.some((reason) => reason.toLowerCase().includes(token))) score += 3
          if (searchableText.includes(token)) score += 1
          return score
        }, 0)
      }

      return { ...quest, relevance, filterMatched }
    })
    .filter((quest) => quest.filterMatched && (!query || quest.relevance > 0))
    .sort((a, b) => {
      if (query) return b.relevance - a.relevance || a.id.localeCompare(b.id)
      if (usingRecommendedSource.value) {
        return (
          (a.recommendationRank ?? Number.MAX_SAFE_INTEGER) -
          (b.recommendationRank ?? Number.MAX_SAFE_INTEGER)
        )
      }
      return b.relevance - a.relevance || a.id.localeCompare(b.id)
    })
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

function applyRecommendationOrder(baseQuests, recommendedItems) {
  const questMap = buildQuestMap(baseQuests)
  const recommendationByQuestId = new Map()

  recommendedItems.forEach((item, index) => {
    const routeId = String(item.quest?.questId ?? '')
    if (!routeId) return
    recommendationByQuestId.set(routeId, normalizeRecommendationItem(item, questMap, index))
  })

  return baseQuests.map((quest) => {
    const recommendation = questLookupKeys(quest)
      .map((key) => recommendationByQuestId.get(key))
      .find(Boolean)

    if (!recommendation) {
      return {
        ...quest,
        recommendationRank: Number.MAX_SAFE_INTEGER,
        recommendationScore: 0,
        strongMatch: false,
        recommendationReasons: [],
      }
    }

    return {
      ...quest,
      recommendationRank: recommendation.recommendationRank,
      recommendationScore: recommendation.recommendationScore,
      strongMatch: recommendation.strongMatch,
      recommendationReasons: recommendation.recommendationReasons,
      tags: unique([...(quest.tags ?? []), ...(recommendation.recommendationReasons ?? [])]),
    }
  })
}

async function loadRecommendedQuests() {
  isRecommendationLoading.value = true
  recommendationError.value = ''

  try {
    const questListData = unwrapApiData(
      await questApi.list({ page: 1, size: 100, sortBy: 'createdAt', sortOrder: 'desc' }),
    )
    const questListItems = Array.isArray(questListData.items) ? questListData.items : []
    // 只展示后端真实委托；无数据即真实空态（模板已有“空空如也”空态）。
    const baseQuests = questListItems.map(normalizeQuestSummary)
    const recommendationPayload = await questApi
      .recommendations({
        strategy: 'tech-difficulty',
        beginnerFriendly: true,
        excludeAccepted: false,
        limit: recommendationLimit,
      })
      .catch(() => null)
    const recommendationData = unwrapApiData(recommendationPayload)
    const recommendedItems = Array.isArray(recommendationData.items) ? recommendationData.items : []

    questSource.value = applyRecommendationOrder(baseQuests, recommendedItems)
    recommendationMeta.value = recommendationData
    usingRecommendedSource.value = true
    if (!recommendationPayload) {
      recommendationError.value = '推荐接口暂时不可用，当前按默认排序显示'
    }
  } catch {
    questSource.value = []
    recommendationMeta.value = null
    usingRecommendedSource.value = false
    recommendationError.value = '悬赏板加载失败，请稍后重试。'
  } finally {
    isRecommendationLoading.value = false
  }
}

async function loadTaxonomyFilterOptions() {
  try {
    const [categoryPayload, tagPayload] = await Promise.all([
      questApi.categories(),
      questApi.tags({ size: 100 }),
    ])
    const categoryData = unwrapApiData(categoryPayload)
    const tagData = unwrapApiData(tagPayload)
    taxonomyFilterOptions.value = {
      category: normalizeTaxonomyOptions(Array.isArray(categoryData) ? categoryData : categoryData.items),
      tag: normalizeTaxonomyOptions(Array.isArray(tagData) ? tagData : tagData.items),
    }
  } catch {
    taxonomyFilterOptions.value = {
      category: [],
      tag: [],
    }
  }
}

function openQuestDetail(questId, intent = 'view') {
  // Guests can browse but must sign in before accepting a commission — send
  // them to the gate, preserving the accept intent so the detail page picks
  // up where they left off after login.
  if (intent === 'accept' && sessionStore.role === 'VISITOR') {
    router.push({
      name: 'login',
      query: { redirect: `/quests/${questId}?intent=accept` },
    })
    return
  }
  router.push({
    name: 'quest-detail',
    params: { questId },
    query: intent === 'accept' ? { intent } : {},
  })
}

// Visitors never had a hall to come from — the board is their entry point. Send
// them back to the login gate instead, mirroring the accept-quest flow.
const isVisitor = computed(() => sessionStore.role === 'VISITOR')
const backLabel = computed(() => (isVisitor.value ? '返回登录' : '返回公会大厅'))

function goBack() {
  if (isVisitor.value) {
    router.push({ name: 'login' })
    return
  }
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

onMounted(() => {
  loadTaxonomyFilterOptions()
  loadRecommendedQuests()
})
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene quest-mode" :style="{ backgroundImage: `url(${questBoardImg})` }">
      <button class="back-orb" type="button" :aria-label="backLabel" @click="goBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>{{ backLabel }}</span>
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
              <section v-for="group in visibleQuestFilterGroups" :key="group.id" class="quest-filter-group">
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
            <div class="commission-panel-head recommendation-head">
              <div>
                <p class="kicker">为您推荐</p>
                <h2>推荐委托</h2>
              </div>
              <span>{{ recommendationStatusText }}</span>
            </div>

            <div v-if="isRecommendationLoading" class="commission-grid loading" aria-label="正在加载委托">
              <article
                v-for="item in loadingSkeletonCards"
                :key="item"
                class="commission-card commission-skeleton-card"
                aria-hidden="true"
              >
                <span class="skeleton-line short"></span>
                <span class="skeleton-line title"></span>
                <span class="skeleton-line"></span>
                <span class="skeleton-line"></span>
                <span class="skeleton-block"></span>
                <span class="skeleton-tags"></span>
              </article>
            </div>

            <div v-else-if="pagedQuestCommissions.length > 0" class="commission-grid">
              <article
                v-for="quest in pagedQuestCommissions"
                :key="quest.routeId ?? quest.id"
                class="commission-card"
                :class="{ 'is-strong-match': quest.strongMatch }"
              >
                <span class="commission-seal" :class="`tone-${statusTone(quest.status)}`">{{ quest.status }}</span>

                <div class="commission-card-top">
                  <span class="commission-id">{{ quest.id }}</span>
                  <span class="commission-rank" :title="`难度 ${quest.difficulty} 阶`">{{ quest.difficulty }} 阶</span>
                </div>

                <h3 class="commission-title">{{ quest.title }}</h3>
                <p class="commission-issuer">{{ quest.issuer }}</p>
                <p class="commission-summary">{{ quest.summary }}</p>

                <div v-if="quest.recommendationReasons?.length" class="commission-reasons">
                  <p class="commission-label">推荐理由</p>
                  <div>
                    <span v-for="reason in quest.recommendationReasons" :key="reason">{{ reason }}</span>
                  </div>
                </div>

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
                    <button class="quiet-action" type="button" @click="openQuestDetail(quest.routeId ?? quest.id, 'view')">详情</button>
                    <button class="primary-action" type="button" @click="openQuestDetail(quest.routeId ?? quest.id, 'accept')">接取委托</button>
                  </div>
                </footer>
              </article>
            </div>

            <div v-else class="commission-empty">
              <p class="kicker">空空如也</p>
              <p>{{ emptyDescription }}</p>
              <button v-if="activeFilterCount > 0" class="quiet-action" type="button" @click="clearQuestFilters">清空筛选</button>
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
