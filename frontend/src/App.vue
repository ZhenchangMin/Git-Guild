<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'

import deskImg from './assets/desk.png'
import doorImg from './assets/door.png'
import hallImg from './assets/hall.png'
import idCardImg from './assets/ID card.png'
import leaderBoardWallImg from './assets/leader board wall.png'
import operationRoomImg from './assets/operation room.png'
import questBoardImg from './assets/quest board.png'
import submissionCounterImg from './assets/submission-counter-clerk-v0.png'
import workbenchImg from './assets/workbench.png'
import QuestDetail from './components/QuestDetail.vue'
import SubmissionCounter from './components/SubmissionCounter.vue'
import Workbench from './components/Workbench.vue'
import { questDetails } from './data/quests'

const screen = ref('door')
const activeRoom = ref(null)
const showIdCard = ref(false)
const selectedRole = ref('adventurer')
const signedRole = ref('visitor')
const showGuide = ref(false)
const viewHistory = ref([])
const hallViewport = ref(null)
const hallTrack = ref(null)
const hallOffset = ref(0)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartOffset = ref(0)
const questSearch = ref('')
const selectedQuestFilters = ref({
  category: [],
  tag: [],
  difficulty: [],
  stack: [],
  status: [],
})
const questPage = ref(1)
const questPageSize = 4
const activeQuestId = ref('QST-0427')
const activeQuestIntent = ref('view')
const activeSubmissionQuestId = ref('QST-0427')

const roles = [
  { id: 'adventurer', name: 'Adventurer', note: 'Browse quests and submit work' },
  { id: 'master', name: 'Guild Master', note: 'Publish quests and review results' },
  { id: 'admin', name: 'Administrator', note: 'Approve listings and handle incidents' },
]

const rooms = [
  {
    id: 'submission',
    label: 'Submission Counter',
    hint: 'Submitted work and review flow',
    image: submissionCounterImg,
    left: 7.2,
    top: 17,
    width: 11.8,
    height: 56,
  },
  {
    id: 'quest',
    label: 'Quest Board',
    hint: 'Issue-linked quests ready to claim',
    image: questBoardImg,
    left: 23.6,
    top: 16,
    width: 13.8,
    height: 45,
  },
  {
    id: 'desk',
    label: 'Desk',
    hint: 'Repositories, branches, commits, pull requests',
    image: deskImg,
    left: 43.8,
    top: 37,
    width: 16.2,
    height: 50,
  },
  {
    id: 'workbench',
    label: 'Workbench / 工作台',
    hint: 'Daily tasks, repositories, PRs, notifications',
    image: workbenchImg,
    left: 64.3,
    top: 18,
    width: 14.5,
    height: 53,
  },
  {
    id: 'leaderboard',
    label: 'Leader Board Wall',
    hint: 'Guild records and visible achievements',
    image: leaderBoardWallImg,
    left: 82.2,
    top: 17,
    width: 11.4,
    height: 56,
  },
]

const questCommissions = [
  {
    id: 'QST-0412',
    title: 'Issue sync status page',
    issuer: 'Guild Master · Repo Adapter',
    category: '接入',
    difficulty: 'C',
    stack: 'Vue / Spring Boot',
    techStack: ['Vue', 'Spring Boot'],
    status: 'Review',
    tags: ['Issue', '同步', '新手友好'],
    reward: '180 XP',
    summary: '展示仓库 Issue 同步状态，并给出失败后的下一步操作。',
    criteria: ['Issue 状态可读', 'Webhook 时间可见', '空态和错误态完整'],
  },
  {
    id: 'QST-0427',
    title: 'Refactor submission flow',
    issuer: 'Guild Master · Review Desk',
    category: '流程',
    difficulty: 'D',
    stack: 'Vue / REST API',
    techStack: ['Vue', 'REST API'],
    status: 'Open',
    tags: ['PR', '提交', '审核'],
    reward: '240 XP',
    summary: '重整成果提交表单，让任务、分支和 PR 关联更清楚。',
    criteria: ['PR 链接校验', '提交状态同步', '退回修改说明'],
  },
  {
    id: 'QST-0431',
    title: 'Repository import error view',
    issuer: 'Administrator · Import Hall',
    category: '界面',
    difficulty: 'C',
    stack: 'Vue / Gitea',
    techStack: ['Vue', 'Gitea'],
    status: 'PR Ready',
    tags: ['导入', '异常', '管理员'],
    reward: '160 XP',
    summary: '为仓库导入失败提供清晰错误页，帮助维护者重新接入项目。',
    criteria: ['错误原因分组', '重试入口', '权限不足提示'],
  },
  {
    id: 'QST-0438',
    title: 'Beginner guide checklist',
    issuer: 'Guild Master · Tutorial Shelf',
    category: '引导',
    difficulty: 'B',
    stack: 'Markdown / Vue',
    techStack: ['Markdown', 'Vue'],
    status: 'Open',
    tags: ['新手友好', '教程', '文档'],
    reward: '120 XP',
    summary: '把任务详情中的贡献步骤整理为新手可勾选的执行清单。',
    criteria: ['步骤少于七项', '含运行命令', '含示例 PR 链接'],
  },
  {
    id: 'QST-0440',
    title: 'Task filter persistence',
    issuer: 'Maintainer · Quest Board',
    category: '体验',
    difficulty: 'C',
    stack: 'Vue / Local State',
    techStack: ['Vue', 'Local State'],
    status: 'In Progress',
    tags: ['筛选', '状态', '任务板'],
    reward: '150 XP',
    summary: '记住用户最近使用的筛选条件，让返回任务板后保持上下文。',
    criteria: ['刷新后可恢复', '无条件时回到默认', '搜索词不污染筛选'],
  },
  {
    id: 'QST-0444',
    title: 'Review feedback archive',
    issuer: 'Guild Master · Submission Counter',
    category: '资料',
    difficulty: 'D',
    stack: 'Spring Boot / Vue',
    techStack: ['Spring Boot', 'Vue'],
    status: 'Changes',
    tags: ['反馈', '个人资料', '审核'],
    reward: '260 XP',
    summary: '在个人资料中展示最近审核意见，突出可学习的修改建议。',
    criteria: ['按任务归档', '区分通过与退回', '展示学习建议'],
  },
]

const questFilterGroups = [
  { id: 'category', title: '分类', options: ['接入', '流程', '界面', '引导', '体验', '资料'] },
  { id: 'tag', title: '标签', options: ['新手友好', 'Issue', 'PR', '审核', '同步', '异常', '反馈'] },
  { id: 'difficulty', title: '难度', options: ['B', 'C', 'D'] },
  { id: 'stack', title: '技术栈', options: ['Vue', 'Spring Boot', 'REST API', 'Gitea', 'Markdown', 'Local State'] },
  { id: 'status', title: '状态', options: ['Open', 'In Progress', 'PR Ready', 'Review', 'Changes'] },
]

const activeRoomImage = computed(() => rooms.find((room) => room.id === activeRoom.value)?.image)
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
const activeQuest = computed(
  () => questCommissions.find((quest) => quest.id === activeQuestId.value) ?? questCommissions[0],
)
const activeSubmissionQuest = computed(() => {
  const quest = questCommissions.find((item) => item.id === activeSubmissionQuestId.value) ?? activeQuest.value
  return {
    ...quest,
    detail: questDetails[quest.id],
  }
})
const hallBackHint = computed(() => getBackHint('入口'))
const operationBackHint = computed(() => getBackHint('入口'))
const questDetailBackHint = computed(() => getBackHint('任务板'))
const roomBackHint = computed(() => getBackHint('大厅'))

function describeView(view) {
  if (!view) return ''
  if (view.screen === 'door') return '入口'
  if (view.screen === 'hall') return '大厅'
  if (view.screen === 'operation') return '管理员操作台'
  if (view.screen === 'quest-detail') return '任务详情'
  if (view.screen === 'room') {
    const roomNames = {
      submission: '提交柜台',
      quest: '任务板',
      desk: '仓库桌面',
      workbench: '工作台',
      leaderboard: '排行榜墙',
    }
    return roomNames[view.activeRoom] ?? '上一房间'
  }
  return '上一页面'
}

function getBackHint(fallbackName) {
  const previousView = viewHistory.value[viewHistory.value.length - 1]
  const previousName = describeView(previousView)
  return previousName ? `返回上一页：${previousName}` : `返回至${fallbackName}`
}

function hasOwn(object, key) {
  return Object.prototype.hasOwnProperty.call(object, key)
}

function currentView() {
  return {
    screen: screen.value,
    activeRoom: activeRoom.value,
    activeQuestId: activeQuestId.value,
    activeQuestIntent: activeQuestIntent.value,
    activeSubmissionQuestId: activeSubmissionQuestId.value,
    hash: window.location.hash.replace('#', ''),
  }
}

function normalizeView(view) {
  return {
    screen: hasOwn(view, 'screen') ? view.screen : screen.value,
    activeRoom: hasOwn(view, 'activeRoom') ? view.activeRoom : activeRoom.value,
    activeQuestId: hasOwn(view, 'activeQuestId') ? view.activeQuestId : activeQuestId.value,
    activeQuestIntent: hasOwn(view, 'activeQuestIntent') ? view.activeQuestIntent : activeQuestIntent.value,
    activeSubmissionQuestId: hasOwn(view, 'activeSubmissionQuestId')
      ? view.activeSubmissionQuestId
      : activeSubmissionQuestId.value,
    hash: hasOwn(view, 'hash') ? view.hash : window.location.hash.replace('#', ''),
  }
}

function isSameView(first, second) {
  return (
    first.screen === second.screen &&
    first.activeRoom === second.activeRoom &&
    first.activeQuestId === second.activeQuestId &&
    first.activeQuestIntent === second.activeQuestIntent &&
    first.activeSubmissionQuestId === second.activeSubmissionQuestId &&
    first.hash === second.hash
  )
}

function applyView(view) {
  const nextView = normalizeView(view)
  screen.value = nextView.screen
  activeRoom.value = nextView.activeRoom
  activeQuestId.value = nextView.activeQuestId
  activeQuestIntent.value = nextView.activeQuestIntent
  activeSubmissionQuestId.value = nextView.activeSubmissionQuestId
  window.location.hash = nextView.hash
  if (nextView.screen === 'hall') {
    nextTick(centerHall)
  }
}

function navigateTo(view) {
  const previousView = currentView()
  const nextView = normalizeView(view)
  if (!isSameView(previousView, nextView)) {
    viewHistory.value.push(previousView)
  }
  applyView(nextView)
}

function goBack(fallbackView) {
  showIdCard.value = false
  const previousView = viewHistory.value.pop()
  applyView(previousView ?? fallbackView)
}

function enterGuild(roleId = selectedRole.value) {
  signedRole.value = roleId
  if (roleId === 'admin') {
    navigateTo({ screen: 'operation', activeRoom: null, hash: 'operation' })
    return
  }
  navigateTo({ screen: 'hall', activeRoom: null, hash: 'hall' })
}

function enterAsVisitor() {
  enterGuild('visitor')
}

function openRoom(id) {
  navigateTo({ screen: 'room', activeRoom: id, hash: id })
}

function openQuestDetail(questId, intent = 'view') {
  navigateTo({
    screen: 'quest-detail',
    activeRoom: null,
    activeQuestId: questId,
    activeQuestIntent: intent,
    hash: `quest-${questId}`,
  })
}

function backToQuestBoard() {
  goBack({ screen: 'room', activeRoom: 'quest', hash: 'quest' })
}

function openWorkbenchFromQuestDetail() {
  openRoom('workbench')
}

function openSubmissionFromQuestDetail(questId = activeQuestId.value) {
  activeSubmissionQuestId.value = questId
  navigateTo({ screen: 'room', activeRoom: 'submission', activeSubmissionQuestId: questId, hash: 'submission' })
}

function openSubmissionFromWorkbench(questId) {
  if (questId) {
    activeSubmissionQuestId.value = questId
  }
  navigateTo({
    screen: 'room',
    activeRoom: 'submission',
    activeSubmissionQuestId: questId ?? activeSubmissionQuestId.value,
    hash: 'submission',
  })
}

function backToHall() {
  goBack({ screen: 'hall', activeRoom: null, hash: 'hall' })
}

function backToDoor() {
  goBack({ screen: 'door', activeRoom: null, hash: '' })
}

function openIdCard() {
  showIdCard.value = true
}

function closeIdCard() {
  showIdCard.value = false
}

function openGuide() {
  showGuide.value = true
}

function closeGuide() {
  showGuide.value = false
}

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

function clampHallOffset(offset) {
  const viewport = hallViewport.value
  const track = hallTrack.value
  if (!viewport || !track) return offset
  const min = Math.min(0, viewport.clientWidth - track.offsetWidth)
  return Math.max(min, Math.min(0, offset))
}

function centerHall() {
  const viewport = hallViewport.value
  const track = hallTrack.value
  if (!viewport || !track) return
  hallOffset.value = clampHallOffset((viewport.clientWidth - track.offsetWidth) / 2)
}

function beginHallDrag(event) {
  if (event.target.closest('.hotspot')) return
  isDragging.value = true
  dragStartX.value = event.clientX
  dragStartOffset.value = hallOffset.value
  hallViewport.value?.setPointerCapture(event.pointerId)
}

function dragHall(event) {
  if (!isDragging.value) return
  const delta = event.clientX - dragStartX.value
  hallOffset.value = clampHallOffset(dragStartOffset.value + delta)
}

function endHallDrag(event) {
  if (!isDragging.value) return
  isDragging.value = false
  hallViewport.value?.releasePointerCapture(event.pointerId)
}

onMounted(() => {
  window.addEventListener('resize', centerHall)
  const hash = window.location.hash.replace('#', '')
  if (hash === 'operation') {
    signedRole.value = 'admin'
    screen.value = 'operation'
  } else if (hash === 'quest-detail') {
    activeQuestId.value = 'QST-0427'
    activeQuestIntent.value = 'view'
    screen.value = 'quest-detail'
  } else if (hash.startsWith('quest-')) {
    activeQuestId.value = hash.replace('quest-', '')
    activeQuestIntent.value = 'view'
    screen.value = 'quest-detail'
  } else if (hash === 'hall') {
    screen.value = 'hall'
    nextTick(centerHall)
  } else if (rooms.some((room) => room.id === hash)) {
    activeRoom.value = hash
    screen.value = 'room'
  } else {
    centerHall()
  }
})

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
    <button class="help-orb" type="button" aria-label="打开 Git Guild 使用教程" @click="openGuide">?</button>

    <Transition name="guide-card">
      <div v-if="showGuide" class="guide-backdrop" role="dialog" aria-modal="true" aria-label="Git Guild 使用教程">
        <section class="guide-panel">
          <header class="guide-head">
            <div>
              <p class="kicker">Quick Guide</p>
              <h1>Git Guild 使用教程</h1>
            </div>
            <button class="guide-close" type="button" aria-label="关闭使用教程" @click="closeGuide">×</button>
          </header>

          <div class="guide-grid">
            <article class="guide-card-block">
              <span>01</span>
              <h2>推荐演示流程</h2>
              <ol>
                <li>进入大厅，打开 Quest Board 浏览和筛选任务。</li>
                <li>查看任务详情，确认完成标准后接取任务。</li>
                <li>进入 Workbench 查看个人仓库、分支、提交和 PR 状态。</li>
                <li>在 Submission Counter 提交任务成果，等待维护者审核。</li>
                <li>切换维护者视角处理审核，再回到工作台查看反馈。</li>
              </ol>
            </article>

            <article class="guide-card-block">
              <span>02</span>
              <h2>页面分工</h2>
              <p>任务板负责找任务；任务详情负责说明目标、标准和接取；工作台负责仓库与 Git 协作；提交柜台只负责平台内任务成果提交。</p>
            </article>

            <article class="guide-card-block">
              <span>03</span>
              <h2>角色视角</h2>
              <p>冒险家完成任务和查看成长反馈；维护者发布任务、审核提交和处理 PR；管理员审核任务清晰度、合规性并处理导入或同步异常。</p>
            </article>

            <article class="guide-card-block">
              <span>04</span>
              <h2>操作边界</h2>
              <p>当前 Web 原型不做在线代码编辑器。用户通过上传或提交变更完成 commit、创建分支、发起 PR，并与任务状态联动。</p>
            </article>
          </div>

          <footer class="guide-note">
            左上角返回按钮会优先回到上一页面；如果是直接打开某个页面，则返回到该页面的默认上级入口。
          </footer>
        </section>
      </div>
    </Transition>

    <section v-if="screen === 'door'" class="scene door-scene" :style="{ backgroundImage: `url(${doorImg})` }">
      <aside class="login-panel" aria-label="Login panel">
        <p class="kicker">Git Guild Gate</p>
        <h1>Enter the Hall</h1>
        <div class="field-group">
          <label for="guild-name">Guild Name</label>
          <input id="guild-name" value="adventurer@relay" autocomplete="username" />
        </div>
        <div class="field-group">
          <label for="guild-key">Access Key</label>
          <input id="guild-key" value="••••••••" type="password" autocomplete="current-password" />
        </div>

        <div class="role-grid" aria-label="Select role">
          <button
            v-for="role in roles"
            :key="role.id"
            class="role-choice"
            :class="{ active: selectedRole === role.id }"
            type="button"
            @click="selectedRole = role.id"
          >
            <strong>{{ role.name }}</strong>
            <span>{{ role.note }}</span>
          </button>
        </div>

        <div class="login-actions">
          <button class="primary-action" type="button" @click="enterGuild()">Sign In</button>
          <button class="quiet-action" type="button" @click="enterAsVisitor">Visit Hall</button>
        </div>
      </aside>
    </section>

    <section v-else-if="screen === 'hall'" class="hall-scene">
      <button class="back-orb" type="button" :aria-label="hallBackHint" @click="backToDoor">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>{{ hallBackHint }}</span>
      </button>

      <div
        ref="hallViewport"
        class="hall-viewport"
        :class="{ dragging: isDragging }"
        @pointerdown="beginHallDrag"
        @pointermove="dragHall"
        @pointerup="endHallDrag"
        @pointercancel="endHallDrag"
      >
        <div ref="hallTrack" class="hall-track" :style="{ transform: `translateX(${hallOffset}px)` }">
          <img class="hall-image" :src="hallImg" alt="Git Guild Hall" draggable="false" @load="centerHall" />
          <button
            v-for="room in rooms"
            :key="room.id"
            class="hotspot"
            type="button"
            :aria-label="room.label"
            :style="{ left: `${room.left}%`, top: `${room.top}%`, width: `${room.width}%`, height: `${room.height}%` }"
            @click="openRoom(room.id)"
          >
            <span class="tooltip">
              <strong>{{ room.label }}</strong>
              <small>{{ room.hint }}</small>
            </span>
          </button>
        </div>
      </div>
    </section>

    <section
      v-else-if="screen === 'operation'"
      class="scene work-scene"
      :style="{ backgroundImage: `url(${operationRoomImg})` }"
    >
      <button class="back-orb" type="button" :aria-label="operationBackHint" @click="backToDoor">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>{{ operationBackHint }}</span>
      </button>

      <div class="room-layout admin-layout">
        <section class="parchment-panel tall-panel">
          <p class="kicker">Review Queue</p>
          <h1>Quest Clearance</h1>
          <div class="review-stack">
            <article class="sealed-row">
              <strong>QST-0431 · Repository import error view</strong>
              <span>Clear goal, issue linked, completion checklist present.</span>
              <div class="row-actions">
                <button type="button">Approve</button>
                <button type="button">Return</button>
              </div>
            </article>
            <article class="sealed-row warning">
              <strong>QST-0440 · Fix sync failure</strong>
              <span>Missing verification steps and expected result.</span>
              <div class="row-actions">
                <button type="button">Request Detail</button>
                <button type="button">Close</button>
              </div>
            </article>
          </div>
        </section>

        <section class="glass-ledger">
          <p class="kicker">Incident Desk</p>
          <h2>Common Exceptions</h2>
          <ul>
            <li><span>Import failed</span><button type="button">Retry</button></li>
            <li><span>Sync failed</span><button type="button">Resync</button></li>
            <li><span>Unclear quest returned</span><button type="button">Inspect</button></li>
            <li><span>PR link mismatch</span><button type="button">Resolve</button></li>
          </ul>
        </section>
      </div>
    </section>

    <section
      v-else-if="screen === 'quest-detail'"
      class="scene quest-detail-mode"
      :style="{ backgroundImage: `url(${questBoardImg})` }"
    >
      <button class="back-orb" type="button" :aria-label="questDetailBackHint" @click="backToQuestBoard">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>{{ questDetailBackHint }}</span>
      </button>

      <QuestDetail
        :quest="activeQuest"
        :intent="activeQuestIntent"
        @open-workbench="openWorkbenchFromQuestDetail"
        @open-submission="openSubmissionFromQuestDetail"
      />
    </section>

    <section
      v-else
      class="scene work-scene"
      :class="{
        'quest-mode': activeRoom === 'quest',
        'submission-mode': activeRoom === 'submission',
        'workbench-mode': activeRoom === 'workbench',
      }"
      :style="{ backgroundImage: `url(${activeRoomImage})` }"
    >
      <button class="back-orb" type="button" :aria-label="roomBackHint" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>{{ roomBackHint }}</span>
      </button>

      <div v-if="activeRoom === 'quest'" class="quest-board-workspace" aria-label="悬赏板">
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
              <p class="kicker">Filters</p>
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
              <p class="kicker">Available Quests</p>
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
            <p class="kicker">No Match</p>
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

      <SubmissionCounter v-else-if="activeRoom === 'submission'" :quest="activeSubmissionQuest" />

      <Workbench
        v-else-if="activeRoom === 'workbench'"
        @open-submission="openSubmissionFromWorkbench"
        @open-id-card="openIdCard"
      />

      <Transition name="id-card">
        <div v-if="showIdCard" class="id-card-backdrop" role="dialog" aria-modal="true" aria-label="个人身份卡">
          <button class="id-card-close" type="button" aria-label="关闭身份卡" @click="closeIdCard">×</button>
          <section class="id-card-frame">
            <img class="id-card-image" :src="idCardImg" alt="" />
            <div class="default-avatar" aria-label="Default avatar">
              <span class="avatar-head"></span>
              <span class="avatar-body"></span>
            </div>
            <button class="avatar-edit" type="button">编辑头像</button>

            <article class="identity-info">
              <p class="kicker">Adventurer Record</p>
              <h2>Minerva Dawn</h2>
              <dl>
                <div>
                  <dt>角色</dt>
                  <dd>Adventurer</dd>
                </div>
                <div>
                  <dt>等级</dt>
                  <dd>Level 3</dd>
                </div>
                <div>
                  <dt>经验</dt>
                  <dd>720 / 1000 XP</dd>
                </div>
                <div>
                  <dt>最近分支</dt>
                  <dd>feature/quest-flow</dd>
                </div>
              </dl>
            </article>

            <form class="profile-editor">
              <p class="kicker">编辑个人信息</p>
              <label>
                昵称
                <input value="Minerva Dawn" />
              </label>
              <label>
                技术栈
                <input value="Vue · Spring Boot" />
              </label>
              <label>
                简介
                <textarea rows="3">Learning through real quests and clean pull requests.</textarea>
              </label>
              <div class="editor-actions">
                <button class="primary-action" type="button">保存</button>
                <button class="quiet-action" type="button" @click="closeIdCard">取消</button>
              </div>
            </form>
          </section>
        </div>
      </Transition>
    </section>
  </main>
</template>
