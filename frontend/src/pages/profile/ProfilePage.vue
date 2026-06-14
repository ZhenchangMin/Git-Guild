<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { growthApi } from '../../api/growthApi'
import { questApi } from '../../api/questApi'
import { userApi } from '../../api/userApi'
import { sessionStore, updateSessionUser } from '../../stores/sessionStore'
import profileArchiveBg from '../../assets/profile-archive-bg.webp'
import DifficultyTrendChart from '../../components/DifficultyTrendChart.vue'
import {
  profileIdentity as profileIdentityFallback,
  growthFallback,
  titleThresholds,
  profileStats,
  badgeShowcase,
} from '../../data/profileData'

const router = useRouter()

// 身份卡初始值取自当前登录会话（已在内存），而非演示兜底，
// 避免 /me 返回前闪现别人的演示身份（如 "Minera Dawn"）。
// 缺失字段用中性空值占位，由 loadProfilePage 拿到真实资料后覆盖。
function initialIdentity() {
  const u = sessionStore.user
  if (!u) return { ...profileIdentityFallback }
  return {
    userId: u.userId ?? u.id ?? 0,
    name: u.displayName ?? u.username ?? u.email ?? '',
    avatarUrl: u.avatarUrl ?? '',
    motto: u.motto ?? '',
    createdAt: u.createdAt ?? '',
  }
}

// ─── growth data ────────────────────────────────────
const profileIdentity = reactive(initialIdentity())
// 初始为中性空值而非演示兜底，避免真实数据到达前闪现"别人的 XP"；
// 仅当成长接口真正失败时才在 catch 中回退到 growthFallback。
const growth = ref({ level: 0, totalXp: 0, nextLevelXp: 0, completedQuestCount: 0 })
const badgeCards = ref([...badgeShowcase])
// 真实贡献数据（来自 /users/me/contributions）与衍生指标
const contributions = ref([])
const repoCount = ref(0)
const pendingReviewCount = ref(0)
const isLoading = ref(true)
const isSavingProfile = ref(false)
const loadError = ref('')
const avatarInput = ref(null)
const mottoTextarea = ref(null)
const isEditingMotto = ref(false)
const mottoDraft = ref(profileIdentity.motto)
const mottoError = ref('')
const MAX_MOTTO_LENGTH = 200

function unwrapApiData(payload) {
  return payload?.data ?? payload ?? {}
}

function normalizeUser(payload) {
  const d = unwrapApiData(payload)
  const hasDisplayBadgeId = Object.prototype.hasOwnProperty.call(d, 'displayBadgeId')
  return {
    userId: d.userId ?? d.id ?? profileIdentityFallback.userId,
    name: d.username ?? d.displayName ?? d.email ?? profileIdentityFallback.name,
    avatarUrl: d.avatarUrl ?? '',
    motto: d.motto ?? profileIdentityFallback.motto,
    createdAt: d.createdAt ?? profileIdentityFallback.createdAt,
    displayBadgeId: hasDisplayBadgeId ? d.displayBadgeId : undefined,
  }
}

function normalizeGrowth(payload) {
  const d = unwrapApiData(payload)
  return {
    level: d.level ?? growthFallback.level,
    totalXp: d.totalXp ?? d.xp ?? growthFallback.totalXp,
    nextLevelXp: d.nextLevelXp ?? d.nextLevel ?? growthFallback.nextLevelXp,
    completedQuestCount: d.completedQuestCount ?? d.completedQuests ?? growthFallback.completedQuestCount,
  }
}

function normalizeBadges(payload) {
  const d = unwrapApiData(payload)
  const items = Array.isArray(d.items) ? d.items : Array.isArray(d) ? d : []
  if (items.length === 0) return [...badgeShowcase]

  return items.map(item => ({
    id: item.badgeId ?? item.id,
    name: item.name ?? '未命名徽章',
    earned: Boolean(item.earned),
    hint: item.description ?? item.condition ?? '',
    earnedAt: item.earnedAt ? String(item.earnedAt).slice(0, 10) : '',
    progress: item.progress,
    target: item.target,
  }))
}

function applyUser(payload) {
  const user = normalizeUser(payload)
  Object.assign(profileIdentity, {
    userId: user.userId,
    name: user.name,
    avatarUrl: user.avatarUrl,
    motto: user.motto,
    createdAt: user.createdAt,
  })
  mottoDraft.value = user.motto
  if (user.displayBadgeId !== undefined) {
    displayBadgeId.value = user.displayBadgeId
  }
  updateSessionUser({
    userId: user.userId,
    id: user.userId,
    displayName: user.name,
    avatarUrl: user.avatarUrl,
    motto: user.motto,
    createdAt: user.createdAt,
  })
}

async function loadProfilePage() {
  isLoading.value = true
  loadError.value = ''
  const failures = []

  // 五个接口相互独立，并行发起：总耗时从"五个串行相加（约 5s）"降到"最慢的一个"。
  // 用 allSettled 而非 all，保证单个失败不影响其余结果，仍各自走原有兜底逻辑。
  const [meRes, summaryRes, badgesRes, contribRes, assignRes] = await Promise.allSettled([
    userApi.me(),
    growthApi.summary(),
    growthApi.badges(),
    growthApi.contributions(),
    questApi.myAssignments(),
  ])

  if (meRes.status === 'fulfilled') {
    applyUser(meRes.value)
  } else {
    failures.push('用户资料')
    Object.assign(profileIdentity, { ...profileIdentityFallback })
  }

  if (summaryRes.status === 'fulfilled') {
    growth.value = normalizeGrowth(summaryRes.value)
  } else {
    failures.push('成长摘要')
    growth.value = { ...growthFallback }
  }

  if (badgesRes.status === 'fulfilled') {
    badgeCards.value = normalizeBadges(badgesRes.value)
  } else {
    failures.push('徽章')
    badgeCards.value = [...badgeShowcase]
  }

  if (contribRes.status === 'fulfilled') {
    const data = unwrapApiData(contribRes.value)
    contributions.value = Array.isArray(data.items) ? data.items : []
    repoCount.value = data.repoCount ?? 0
  } else {
    failures.push('贡献历程')
    contributions.value = []
    repoCount.value = 0
  }

  // 待审核 = 当前用户处于 IN_REVIEW 的接取数（真实统计）
  if (assignRes.status === 'fulfilled') {
    const stats = unwrapApiData(assignRes.value).stats ?? {}
    pendingReviewCount.value = stats.inReview ?? 0
  } else {
    pendingReviewCount.value = 0
  }

  if (failures.length > 0) {
    loadError.value = `${failures.join('、')}暂时无法读取，请确认后端服务已启动。`
  }
  isLoading.value = false
}

onMounted(loadProfilePage)

async function editProfileMotto() {
  mottoDraft.value = profileIdentity.motto ?? ''
  mottoError.value = ''
  isEditingMotto.value = true
  await nextTick()
  mottoTextarea.value?.focus()
}

function cancelProfileMottoEdit() {
  mottoDraft.value = profileIdentity.motto ?? ''
  mottoError.value = ''
  isEditingMotto.value = false
}

async function saveProfileMotto() {
  const nextMotto = mottoDraft.value.trim()
  if (nextMotto.length > MAX_MOTTO_LENGTH) {
    mottoError.value = `座右铭最多 ${MAX_MOTTO_LENGTH} 个字。`
    return
  }

  if (nextMotto === (profileIdentity.motto ?? '')) {
    cancelProfileMottoEdit()
    return
  }

  isSavingProfile.value = true
  try {
    applyUser(await userApi.updateProfile({ motto: nextMotto }))
    isEditingMotto.value = false
  } catch {
    profileIdentity.motto = nextMotto
    mottoDraft.value = nextMotto
    isEditingMotto.value = false
    loadError.value = '资料接口暂未就绪，座右铭已在当前页面保留为演示状态。'
  } finally {
    isSavingProfile.value = false
  }
}

function openAvatarPicker() {
  avatarInput.value?.click()
}

async function uploadAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    loadError.value = '头像文件必须是图片格式。'
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    loadError.value = '头像文件不能超过 2MB。'
    return
  }

  isSavingProfile.value = true
  try {
    applyUser(await userApi.uploadAvatar(file))
  } catch (error) {
    loadError.value = error.message || '头像上传失败，请确认后端服务已启动并稍后重试。'
  } finally {
    isSavingProfile.value = false
  }
}

// ─── license helpers ────────────────────────────────
const licenseNo = computed(() =>
  profileIdentity.userId
    ? 'No. ' + String(profileIdentity.userId).padStart(5, '0')
    : 'No. —',
)

const guildTitle = computed(() => {
  const lv = growth.value.level
  for (const t of titleThresholds) {
    if (lv >= t.minLevel) return t.title
  }
  return '见习冒险者'
})

const levelProgress = computed(() => {
  const t = growth.value.totalXp
  const n = growth.value.nextLevelXp || 1
  return Math.min(100, Math.round((t / n) * 100))
})

const codeAge = computed(() => {
  const created = new Date(profileIdentity.createdAt)
  if (!profileIdentity.createdAt || Number.isNaN(created.getTime())) return '—'
  const now = new Date()
  const days = Math.floor((now - created) / 86400000)
  return days + ' 天'
})

const avatarInitial = computed(() => profileIdentity.name.charAt(0).toUpperCase())
const mottoChars = computed(() => mottoDraft.value.trim().length)

const growthPathPoints = [
  { step: '1', label: '启程', x: '70.1%', y: '72.0%', state: 'done', side: 'bottomRight' },
  { step: '2', label: '首个 PR', x: '76.0%', y: '61.9%', state: 'done', side: 'right' },
  { step: '3', label: '代码审查', x: '73.5%', y: '40.5%', state: 'done', side: 'right' },
  { step: '4', label: '稳定贡献', x: '82.0%', y: '51.5%', state: 'current', side: 'right' },
  { step: '5', label: '协作熟练', x: '80.1%', y: '30.3%', state: 'next', side: 'right' },
  { step: '6', label: '公会认证', x: '84.6%', y: '20.6%', state: 'locked', side: 'left' },
]

const STAT_CLOUD_LAYOUT = {
  totalXp: { cloudClass: 'cloud-main', rotation: -2 },
  completedQuestCount: { cloudClass: 'cloud-west', rotation: 3 },
  repoCount: { cloudClass: 'cloud-east', rotation: -4 },
  pendingReview: { cloudClass: 'cloud-south', rotation: 2 },
}

// ─── derived real data ─────────────────────────────
const hasActivity = computed(() => contributions.value.length > 0)

// 四项成长指标全部来自真实接口：等级/XP/完成数取自成长摘要，仓库数与待审核取自贡献与接取统计
const statValues = computed(() => ({
  completedQuestCount: growth.value.completedQuestCount,
  totalXp: growth.value.totalXp,
  repoCount: repoCount.value,
  pendingReview: pendingReviewCount.value,
}))

// 贡献时间线：真实贡献记录映射为卡片所需字段（无 PR / 标签字段，留空不渲染）
const contributionList = computed(() =>
  contributions.value.map(c => ({
    id: c.recordId,
    questTitle: c.questTitle,
    repository: c.repository,
    difficulty: c.difficulty,
    xp: c.xp,
    completedAt: (c.completedAt || '').slice(0, 10),
    summary: c.summary,
  })),
)

// 难度攀升曲线：直接由真实贡献的 完成日期 / 难度 / XP 构成
const difficultyTrend = computed(() =>
  contributions.value.map(c => ({
    date: (c.completedAt || '').slice(0, 10),
    difficulty: c.difficulty,
    questTitle: c.questTitle,
    xp: c.xp,
  })),
)

// 技能标签：按真实贡献所在仓库聚合，贡献次数作为权重（无贡献则为空）
const derivedSkillTags = computed(() => {
  const counts = new Map()
  for (const c of contributions.value) {
    if (!c.repository) continue
    counts.set(c.repository, (counts.get(c.repository) || 0) + 1)
  }
  return [...counts.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
})

// 成长里程碑：从真实数据派生 —— 首次贡献 + 已获得徽章，按时间排序
const derivedMilestones = computed(() => {
  const events = []
  const first = [...contributions.value]
    .filter(c => c.completedAt)
    .sort((a, b) => String(a.completedAt).localeCompare(String(b.completedAt)))[0]
  if (first) {
    events.push({
      icon: 'scroll',
      title: '首次冒险',
      description: `完成「${first.questTitle}」，获得 ${first.xp} XP`,
      date: String(first.completedAt).slice(0, 10),
    })
  }
  for (const badge of badgeCards.value) {
    if (badge.earned) {
      events.push({
        icon: 'badge',
        title: `徽章达成 — ${badge.name}`,
        description: badge.hint,
        date: badge.earnedAt || '',
      })
    }
  }
  return events.sort((a, b) => (a.date || '').localeCompare(b.date || ''))
})

// ─── stats word-cloud ──────────────────────────────
const statCards = computed(() => {
  const raw = profileStats.map(s => {
    const numeric = statValues.value[s.key] ?? 0
    // 全部指标均来自真实接口，加载完成前以占位符显示，避免闪现旧值
    return {
      ...s,
      value: isLoading.value ? '—' : numeric,
      wv: numeric * s.weight,
    }
  })
  const maxWv = Math.max(...raw.map(s => s.wv), 1)
  return raw.map(s => ({
    ...s,
    ...(STAT_CLOUD_LAYOUT[s.key] ?? { cloudClass: 'cloud-small', rotation: 0 }),
    fontSize: 1.05 + (s.wv / maxWv) * 2.35,
  }))
})

// ─── displayed badge ────────────────────────────────
const displayBadgeId = ref(
  badgeShowcase.find(b => b.earned)?.id ?? null,
)

const displayBadge = computed(() =>
  badgeCards.value.find(b => b.id === displayBadgeId.value) ?? null,
)

async function wearBadge(id) {
  const nextBadgeId = displayBadgeId.value === id ? null : id
  const previousBadgeId = displayBadgeId.value
  displayBadgeId.value = nextBadgeId
  try {
    applyUser(await userApi.setDisplayBadge(nextBadgeId))
  } catch {
    loadError.value = previousBadgeId === nextBadgeId
      ? ''
      : '徽章佩戴接口暂未就绪，当前选择已作为页面演示状态保留。'
  }
}

// ─── tabs ──────────────────────────────────────────
const activeTab = ref('contributions')

const tabs = [
  { key: 'contributions', label: '贡献历程', icon: 'scroll' },
  { key: 'badges',        label: '成就徽章', icon: 'badge' },
  { key: 'milestones',    label: '成长里程碑', icon: 'flag' },
]

// ─── skill tag quality ─────────────────────────────
function tagQuality(count) {
  if (count >= 5) return 'gold'
  if (count >= 3) return 'silver'
  return 'common'
}

// ─── difficulty label ───────────────────────────────
const DIFF_LABEL = { A: '高级', B: '中级', C: '初级', D: '入门' }

// ─── navigation ────────────────────────────────────
function backToHall() { router.push({ name: 'hall' }) }
function openQuestBoard() { router.push({ name: 'quest-board' }) }
function openWorkbench() { router.push({ name: 'adventurer-workbench' }) }
function openLeaderboard() { router.push({ name: 'leaderboard' }) }

// ─── SVG icon paths ────────────────────────────────
const icons = {
  scroll: 'M4 4a2 2 0 0 1 2-2h4l2 2h6a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4z',
  star: 'M12 2l2.4 7.4H22l-6 4.4 2.3 7.2L12 16.6 5.7 21l2.3-7.2-6-4.4h7.6z',
  grid: 'M3 3h7v7H3V3zm11 0h7v7h-7V3zM3 14h7v7H3v-7zm11 0h7v7h-7v-7z',
  clock: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 4v6l4 2',
  badge: 'M12 2l1.8 3.6L18 6.4l-3 2.9.7 4.1L12 11.4 8.3 13.4l.7-4.1-3-2.9 4.2-.8z',
  'arrow-up': 'M12 19V5m-7 7 7-7 7 7',
  flame: 'M12 2c0 4-4 6-4 10a4 4 0 0 0 8 0c0-4-4-6-4-10z',
  branch: 'M6 3v6m0 6v3m12-12v6a3 3 0 0 1-3 3H9',
  flag: 'M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1v12zM4 22v-7',
  edit: 'M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z',
}
</script>

<template>
  <main class="app-shell">
    <section class="scene profile-scene" :style="{ '--profile-archive-bg': `url(${profileArchiveBg})` }">
      <!-- back button -->
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 6 9 12l6 6" /></svg>
        <span>返回公会大厅</span>
      </button>

      <aside class="growth-map-points" aria-label="成长路径点">
        <span
          v-for="point in growthPathPoints"
          :key="point.label"
          class="growth-map-point"
          :class="['point-' + point.state, 'label-' + point.side]"
          :style="{ '--point-x': point.x, '--point-y': point.y }"
        >
          <i aria-hidden="true">{{ point.step }}</i>
          <span>{{ point.label }}</span>
        </span>
      </aside>

      <div class="profile-shell">

        <!-- ═══════ §3.1 冒险者执照 ═══════ -->
        <section class="profile-license glass-ledger">
          <div class="license-header">
            <span class="license-title">冒险者执照</span>
            <span class="license-no">{{ licenseNo }}</span>
          </div>

          <div class="license-body">
            <!-- left: avatar -->
            <div class="license-left">
              <div
                class="license-avatar"
                :class="{ 'has-img': profileIdentity.avatarUrl }"
              >
                <img v-if="profileIdentity.avatarUrl" :src="profileIdentity.avatarUrl" alt="头像" />
                <span v-else class="avatar-initial">{{ avatarInitial }}</span>
              </div>
              <input
                ref="avatarInput"
                class="avatar-input"
                type="file"
                accept="image/*"
                @change="uploadAvatar"
              />
              <button
                class="avatar-upload-btn"
                type="button"
                :disabled="isSavingProfile"
                @click="openAvatarPicker"
              >
                {{ profileIdentity.avatarUrl ? '更换头像' : '上传头像' }}
              </button>
              <span class="license-name-left">{{ profileIdentity.name }}</span>
            </div>

            <!-- right: info -->
            <div class="license-right">
              <div class="license-top-row">
                <h1>{{ profileIdentity.name }}</h1>
                <button
                  class="edit-btn"
                  type="button"
                  aria-label="编辑资料"
                  :disabled="isSavingProfile"
                  @click="editProfileMotto"
                >
                  <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path :d="icons.edit" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  <span>编辑</span>
                </button>
              </div>

              <p class="license-title-line">
                <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true"><path :d="icons.star" fill="currentColor" opacity="0.7"/></svg>
                {{ isLoading ? '—' : guildTitle }}
              </p>

              <div v-if="isEditingMotto" class="motto-editor">
                <label class="sr-only" for="profile-motto">座右铭</label>
                <textarea
                  id="profile-motto"
                  ref="mottoTextarea"
                  v-model="mottoDraft"
                  :maxlength="MAX_MOTTO_LENGTH"
                  :disabled="isSavingProfile"
                  rows="3"
                  placeholder="写一句能代表你当前冒险状态的话"
                />
                <div class="motto-editor-foot">
                  <span :class="{ warn: mottoChars > MAX_MOTTO_LENGTH * 0.9 }">
                    {{ mottoChars }} / {{ MAX_MOTTO_LENGTH }}
                  </span>
                  <div class="motto-actions">
                    <button type="button" class="ghost-btn" :disabled="isSavingProfile" @click="cancelProfileMottoEdit">
                      取消
                    </button>
                    <button type="button" class="save-btn" :disabled="isSavingProfile" @click="saveProfileMotto">
                      {{ isSavingProfile ? '保存中' : '保存' }}
                    </button>
                  </div>
                </div>
                <p v-if="mottoError" class="motto-error">{{ mottoError }}</p>
              </div>
              <blockquote v-else class="license-motto">
                "{{ profileIdentity.motto || 'Every expert was once a beginner.' }}"
              </blockquote>

              <!-- displayed badge -->
              <div class="license-badge" v-if="displayBadge">
                <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true"><path :d="icons.badge" fill="currentColor" opacity="0.8"/></svg>
                {{ displayBadge.name }}
              </div>

              <!-- XP progress -->
              <div class="license-xp">
                <div class="xp-labels">
                  <span>Lv {{ isLoading ? '—' : growth.level }}</span>
                  <span>Lv {{ isLoading ? '—' : growth.level + 1 }}</span>
                </div>
                <div class="xp-bar">
                  <i :style="{ width: (isLoading ? 0 : levelProgress) + '%' }"></i>
                </div>
                <p class="xp-num">{{ isLoading ? '— / —' : `${growth.totalXp} / ${growth.nextLevelXp}` }} XP</p>
              </div>

              <!-- stats row -->
              <div class="license-stats">
                <div>
                  <span class="stat-label">累计 XP</span>
                  <strong>{{ isLoading ? '—' : growth.totalXp }}</strong>
                </div>
                <div>
                  <span class="stat-label">已完成任务</span>
                  <strong>{{ isLoading ? '—' : growth.completedQuestCount }}</strong>
                </div>
                <div>
                  <span class="stat-label">Code 龄</span>
                  <strong>{{ codeAge }}</strong>
                </div>
              </div>
            </div>
          </div>
        </section>

        <p v-if="loadError" class="api-note">{{ loadError }}</p>
        <p v-else-if="isLoading" class="api-note">正在读取成长摘要...</p>

        <!-- ═══════ §3.2 统计指标卡片 ═══════ -->
        <section class="stat-cloud" aria-label="成长指标">
          <span class="cloud-noise cloud-noise-a">Quest</span>
          <span class="cloud-noise cloud-noise-b">Review</span>
          <span class="cloud-noise cloud-noise-c">PR</span>
          <span class="cloud-noise cloud-noise-d">XP</span>
          <article
            v-for="s in statCards"
            :key="s.key"
            class="stat-card"
            :class="s.cloudClass"
            :style="{ '--stat-size': s.fontSize + 'rem', '--stat-rotate': s.rotation + 'deg' }"
          >
            <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true" class="stat-icon">
              <path :d="icons[s.icon]" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <strong>{{ s.value }}</strong>
            <span class="stat-label-text">{{ s.label }}</span>
          </article>
        </section>

        <!-- ═══════ §3.3 趣味数据双栏 ═══════ -->
        <section class="fun-grid">
          <article class="glass-ledger fun-card">
            <p class="kicker">难度攀升曲线</p>
            <h2>挑战轨迹</h2>
            <DifficultyTrendChart v-if="hasActivity" :data="difficultyTrend" />
            <p v-else class="section-empty">完成你的第一个任务后，这里会绘制你的难度攀升曲线。</p>
          </article>
          <article class="glass-ledger fun-card">
            <p class="kicker">技能雷达</p>
            <h2>能力分布</h2>
            <p class="section-empty">技能雷达将在平台引入技能评估模型后开放，暂不展示推测数据。</p>
          </article>
        </section>

        <!-- ═══════ §3.4 技能标签区 ═══════ -->
        <section class="glass-ledger skill-section">
          <p class="kicker">技能图谱</p>
          <h2>冒险中积累的技术栈</h2>
          <div v-if="derivedSkillTags.length > 0" class="skill-tags">
            <span
              v-for="tag in derivedSkillTags"
              :key="tag.name"
              class="skill-tag"
              :class="'quality-' + tagQuality(tag.count)"
            >
              {{ tag.name }}
              <em>{{ tag.count }}</em>
            </span>
          </div>
          <p v-else class="section-empty">完成任务后，会按贡献仓库自动积累你的技术栈标签。</p>
        </section>

        <!-- ═══════ §3.5 Tab 切换内容区 ═══════ -->
        <section class="glass-ledger tab-section">
          <div class="tab-bar" role="tablist">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              role="tab"
              :aria-selected="activeTab === tab.key"
              :class="{ active: activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                <path :d="icons[tab.icon]" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>{{ tab.label }}</span>
            </button>
          </div>

          <!-- Tab 1: 贡献历程 -->
          <div v-if="activeTab === 'contributions'" class="timeline">
            <article v-for="(rec, i) in contributionList" :key="rec.id" class="timeline-item">
              <div class="timeline-dot-col">
                <span class="timeline-dot" />
                <span v-if="i < contributionList.length - 1" class="timeline-line" />
              </div>
              <div class="timeline-card">
                <div class="tc-head">
                  <span class="tc-date">{{ rec.completedAt }}</span>
                  <span class="tc-xp">+{{ rec.xp }} XP</span>
                </div>
                <h3>{{ rec.questTitle }}</h3>
                <p class="tc-repo">{{ rec.repository }}<span v-if="rec.difficulty"> · 难度 {{ rec.difficulty }}</span></p>
                <p class="tc-summary">{{ rec.summary }}</p>
              </div>
            </article>
            <p v-if="contributionList.length === 0" class="ms-empty">
              你还没有已完成的贡献。完成第一个任务后，这里会记录你的贡献历程。
            </p>
          </div>

          <!-- Tab 2: 成就徽章 -->
          <div v-else-if="activeTab === 'badges'" class="badge-grid">
            <article
              v-for="badge in badgeCards"
              :key="badge.id"
              class="badge-card"
              :class="{ locked: !badge.earned }"
            >
              <div class="badge-head">
                <!-- star icon for earned, lock for locked -->
                <svg v-if="badge.earned" viewBox="0 0 24 24" width="40" height="40" aria-hidden="true">
                  <path :d="icons.badge" fill="#f2c06f" stroke="#2a180d" stroke-width="1"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="40" height="40" aria-hidden="true">
                  <circle cx="12" cy="12" r="10" fill="none" stroke="rgba(255,232,190,0.2)" stroke-dasharray="3 3"/>
                  <path d="M8 11V7a4 4 0 0 1 8 0v4M7 11h10v8H7z" fill="none" stroke="rgba(255,232,190,0.25)" stroke-width="1.5"/>
                </svg>
                <!-- wear button -->
                <button
                  v-if="badge.earned"
                  class="wear-btn"
                  :class="{ worn: displayBadgeId === badge.id }"
                  type="button"
                  @click="wearBadge(badge.id)"
                >
                  {{ displayBadgeId === badge.id ? '✓ 佩戴中' : '佩戴' }}
                </button>
              </div>
              <strong>{{ badge.name }}</strong>
              <p>{{ badge.hint }}</p>
              <span v-if="badge.earned" class="badge-date">{{ badge.earnedAt }}</span>
            </article>
          </div>

          <!-- Tab 3: 成长里程碑 -->
          <div v-else class="milestone-list">
            <article v-for="(ms, i) in derivedMilestones" :key="i" class="milestone-item">
              <div class="ms-dot-col">
                <span class="ms-icon-wrap">
                  <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                    <path :d="icons[ms.icon]" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </span>
                <span v-if="i < derivedMilestones.length - 1" class="ms-line" />
              </div>
              <div class="ms-content">
                <h3>{{ ms.title }}</h3>
                <p>{{ ms.description }}</p>
                <span class="ms-date">{{ ms.date }}</span>
              </div>
            </article>
            <p v-if="derivedMilestones.length === 0" class="ms-empty">
              完成你的第一个任务，开启冒险旅程。
            </p>
          </div>
        </section>

        <!-- ═══════ §3.6 快捷操作栏 ═══════ -->
        <section class="quick-actions" aria-label="快捷操作">
          <button type="button" class="action-btn glass-ledger" @click="openQuestBoard">
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path :d="icons.scroll" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <span>浏览悬赏任务</span>
          </button>
          <button type="button" class="action-btn glass-ledger" @click="openWorkbench">
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path :d="icons.grid" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <span>进入工作台</span>
          </button>
          <button type="button" class="action-btn glass-ledger" @click="openLeaderboard">
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M18 20V10M12 20V4M6 20v-6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <span>排行榜</span>
          </button>
        </section>

      </div>
    </section>
  </main>
</template>

<style scoped>
/* ═══════ scene ════════════════════════════════════ */
.profile-scene {
  position: relative;
  min-height: 100svh;
  overflow: auto;
  padding: 84px 36px 44px;
  background: #0c0502;
  isolation: isolate;
}

.profile-scene::before,
.profile-scene::after {
  position: fixed;
  inset: 0;
  pointer-events: none;
  content: '';
}

.profile-scene::before {
  z-index: -3;
  background-image: var(--profile-archive-bg);
  background-position: right top;
  background-repeat: no-repeat;
  background-size: cover;
  filter: saturate(1.08) brightness(1.08);
}

.profile-scene::after {
  z-index: -2;
  background:
    linear-gradient(90deg, rgba(6,2,1,0.7) 0%, rgba(8,3,1,0.54) 50%, rgba(6,2,1,0.2) 68%, rgba(5,2,1,0.34) 100%),
    linear-gradient(180deg, rgba(4,1,0,0.3), rgba(8,3,1,0.08) 36%, rgba(4,1,0,0.66)),
    radial-gradient(circle at 83% 28%, rgba(242,192,111,0.16), transparent 0 18%, transparent 42%);
}

.profile-shell {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 16px;
  --profile-left-gutter: clamp(36px, 5vw, 96px);
  width: calc(66.666vw - var(--profile-left-gutter));
  margin-left: var(--profile-left-gutter);
  margin-right: auto;
}

.profile-shell::before {
  position: absolute;
  inset: -18px;
  z-index: -1;
  border: 1px solid rgba(238,184,91,0.08);
  border-radius: 10px;
  content: '';
  background:
    radial-gradient(circle at 18% 0%, rgba(242,192,111,0.14), transparent 0 28%),
    linear-gradient(180deg, rgba(6,2,1,0.24), rgba(6,2,1,0.5));
  box-shadow: 0 34px 90px rgba(0,0,0,0.36);
  backdrop-filter: blur(2px);
}

.growth-map-points {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.growth-map-point {
  position: fixed;
  left: var(--point-x);
  top: var(--point-y);
  display: block;
  width: 24px;
  height: 24px;
  font-family: var(--font-display);
  font-size: clamp(0.64rem, 0.7vw, 0.8rem);
  letter-spacing: 0.02em;
  text-shadow: 0 1px 4px rgba(0,0,0,0.78);
  transform: translate(-50%, -50%);
}

.growth-map-point i {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  border: 1px solid rgba(255,226,160,0.56);
  border-radius: 50%;
  color: #2a1807;
  font-style: normal;
  font-size: 0.72rem;
  font-weight: 800;
  background: #f3bf62;
  box-shadow: 0 0 0 4px rgba(242,192,111,0.14), 0 0 18px rgba(242,192,111,0.78);
}

.growth-map-point span {
  position: absolute;
  top: 50%;
  width: max-content;
  max-width: 88px;
  padding: 3px 7px;
  border: 1px solid rgba(255,226,160,0.36);
  border-radius: 999px;
  color: rgba(255,232,190,0.86);
  background: rgba(10,5,2,0.56);
  box-shadow: 0 0 14px rgba(242,192,111,0.18), inset 0 1px 0 rgba(255,235,180,0.14);
  transform: translateY(-50%);
  backdrop-filter: blur(2px);
}

.growth-map-point.label-right span {
  left: calc(100% + 6px);
}

.growth-map-point.label-left span {
  right: calc(100% + 6px);
}

.growth-map-point.label-bottomRight span {
  top: calc(100% + 6px);
  left: calc(100% + 2px);
  transform: none;
}

.growth-map-point.point-done {
  color: rgba(224,238,192,0.8);
}

.growth-map-point.point-done i {
  background: #9fc56e;
  box-shadow: 0 0 0 4px rgba(141,178,99,0.14), 0 0 18px rgba(141,178,99,0.7);
}

.growth-map-point.point-current {
  width: 26px;
  height: 26px;
  color: #ffe2a0;
}

.growth-map-point.point-current i {
  border-color: rgba(255,226,160,0.86);
  box-shadow: 0 0 0 5px rgba(242,192,111,0.18), 0 0 24px rgba(242,192,111,0.92);
}

.growth-map-point.point-current span {
  border-color: rgba(255,226,160,0.68);
  color: #ffe2a0;
  background: rgba(45,23,6,0.62);
}

.growth-map-point.point-locked {
  color: rgba(255,232,190,0.52);
}

.growth-map-point.point-locked i {
  background: rgba(255,232,190,0.54);
  box-shadow: 0 0 0 4px rgba(255,232,190,0.08), 0 0 16px rgba(255,232,190,0.28);
}

.api-note {
  margin: 0;
  padding: 10px 14px;
  border: 1px solid rgba(242,192,111,0.3);
  border-radius: var(--radius);
  color: rgba(255,232,190,0.82);
  background: rgba(8,4,2,0.42);
  font-size: 0.85rem;
}

/* ═══════ §3.1 冒险者执照 ══════════════════════════ */
.profile-license {
  padding: 0;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(30,14,6,0.76), rgba(8,3,1,0.7)),
    radial-gradient(circle at 84% 12%, rgba(242,192,111,0.1), transparent 0 32%);
  backdrop-filter: blur(5px);
}

.license-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: rgba(12,7,4,0.6);
  border-bottom: 1px solid rgba(238,184,91,0.2);
}

.license-title {
  font-family: var(--font-display);
  font-size: 0.85rem;
  color: rgba(255,232,190,0.6);
  letter-spacing: 0.1em;
}

.license-no {
  font-family: var(--font-display);
  font-size: 0.8rem;
  color: rgba(255,232,190,0.4);
  letter-spacing: 0.05em;
}

.license-body {
  display: grid;
  grid-template-columns: minmax(150px, 0.72fr) minmax(0, 2fr);
  gap: 24px;
  padding: 24px 28px;
}

.license-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px;
}

.license-avatar {
  width: 72px;
  height: 72px;
  padding: 0;
  border-radius: 50%;
  border: 2px solid rgba(241,183,86,0.7);
  overflow: hidden;
  display: grid;
  place-items: center;
  background: rgba(12,7,4,0.6);
}

.avatar-input {
  display: none;
}

.avatar-upload-btn {
  min-height: 30px;
  border: 1px solid rgba(238,184,91,0.28);
  border-radius: 999px;
  padding: 0 12px;
  color: rgba(255,232,190,0.66);
  background: rgba(7,4,2,0.3);
  font-size: 0.74rem;
  transition: border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.avatar-upload-btn:hover:not(:disabled) {
  border-color: rgba(255,226,160,0.68);
  color: #ffe2a0;
  box-shadow: 0 0 18px rgba(242,192,111,0.12);
}

.avatar-upload-btn:disabled {
  cursor: wait;
  opacity: 0.58;
}

.license-avatar.has-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-initial {
  font-family: var(--font-display);
  font-size: 1.8rem;
  color: #ffe2a0;
}

.license-name-left {
  font-family: var(--font-display);
  font-size: 0.85rem;
  color: rgba(255,232,190,0.6);
  text-align: center;
}

.license-right {
  display: grid;
  gap: 8px;
  padding: 4px 0;
  align-content: start;
}

.license-top-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.license-top-row h1 {
  font-family: var(--font-display);
  font-size: clamp(1.6rem, 4vw, 2.4rem);
  color: #ffe2a0;
  margin: 0;
  line-height: 1.1;
}

.edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: rgba(255,232,190,0.4);
  font-size: 0.8rem;
  padding: 4px 8px;
  border-radius: var(--radius);
  transition: color 0.2s;
}

.edit-btn:hover {
  color: #f2c06f;
}

.edit-btn:disabled {
  cursor: wait;
  opacity: 0.55;
}

.edit-btn:focus-visible,
.tab-bar button:focus-visible,
.wear-btn:focus-visible,
.action-btn:focus-visible,
.avatar-upload-btn:focus-visible,
.ghost-btn:focus-visible,
.save-btn:focus-visible {
  outline: 2px solid rgba(255,226,160,0.84);
  outline-offset: 3px;
}

.license-title-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  color: rgba(255,232,190,0.7);
  font-size: 0.9rem;
}

.license-motto {
  margin: 4px 0 0;
  padding-left: 12px;
  border-left: 2px solid rgba(242,192,111,0.35);
  color: rgba(255,232,190,0.55);
  font-style: italic;
  font-size: 0.88rem;
  line-height: 1.5;
}

.motto-editor {
  display: grid;
  gap: 8px;
  margin-top: 4px;
  padding-left: 12px;
  border-left: 2px solid rgba(242,192,111,0.35);
}

.motto-editor textarea {
  width: 100%;
  min-height: 78px;
  resize: vertical;
  border: 1px solid rgba(238,184,91,0.28);
  border-radius: var(--radius);
  color: rgba(255,232,190,0.82);
  background: rgba(8,4,2,0.46);
  box-shadow: inset 0 1px 0 rgba(255,235,180,0.08);
  font: inherit;
  font-size: 0.88rem;
  line-height: 1.5;
  padding: 10px 12px;
}

.motto-editor textarea::placeholder {
  color: rgba(255,232,190,0.34);
}

.motto-editor textarea:focus {
  outline: none;
  border-color: rgba(255,226,160,0.62);
  box-shadow: 0 0 0 3px rgba(242,192,111,0.12), inset 0 1px 0 rgba(255,235,180,0.1);
}

.motto-editor textarea:disabled {
  cursor: wait;
  opacity: 0.72;
}

.motto-editor-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: rgba(255,232,190,0.42);
  font-size: 0.74rem;
}

.motto-editor-foot .warn {
  color: #f2c06f;
}

.motto-actions {
  display: inline-flex;
  gap: 8px;
}

.ghost-btn,
.save-btn {
  min-height: 30px;
  border-radius: 999px;
  padding: 0 12px;
  font-size: 0.76rem;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.ghost-btn {
  border: 1px solid rgba(238,184,91,0.2);
  color: rgba(255,232,190,0.58);
  background: transparent;
}

.save-btn {
  border: 1px solid rgba(241,183,86,0.56);
  color: #ffe2a0;
  background: rgba(241,183,86,0.11);
}

.ghost-btn:hover:not(:disabled),
.save-btn:hover:not(:disabled) {
  border-color: rgba(255,226,160,0.68);
  color: #ffe2a0;
}

.ghost-btn:disabled,
.save-btn:disabled {
  cursor: wait;
  opacity: 0.58;
}

.motto-error {
  margin: 0;
  color: #f2c06f;
  font-size: 0.75rem;
}

.license-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding: 4px 10px;
  border: 1px solid rgba(241,183,86,0.35);
  border-radius: 999px;
  color: #ffe2a0;
  font-size: 0.78rem;
  width: fit-content;
  background: rgba(241,183,86,0.06);
}

/* XP progress */
.license-xp {
  margin-top: 8px;
}

.xp-labels {
  display: flex;
  justify-content: space-between;
  color: rgba(255,232,190,0.5);
  font-size: 0.75rem;
  margin-bottom: 4px;
}

.xp-bar {
  height: 10px;
  background: rgba(255,232,190,0.1);
  border-radius: 999px;
  overflow: hidden;
}

.xp-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #c8851a, #f2c06f);
  box-shadow: 0 0 12px rgba(242,192,111,0.3);
  transition: width 0.6s ease;
}

.xp-num {
  margin: 4px 0 0;
  color: rgba(255,232,190,0.5);
  font-size: 0.78rem;
  text-align: right;
}

/* license stats */
.license-stats {
  display: flex;
  gap: 24px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.license-stats div {
  display: grid;
  gap: 2px;
}

.stat-label {
  color: rgba(255,232,190,0.5);
  font-size: 0.75rem;
}

.license-stats strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.15rem;
}

/* ═══════ §3.2 统计指标卡片 ════════════════════════ */
.stat-cloud {
  position: relative;
  min-height: 194px;
  overflow: hidden;
  border: 1px solid rgba(238,184,91,0.52);
  border-radius: var(--radius);
  background:
    radial-gradient(circle at 50% 48%, rgba(241,183,86,0.15), transparent 0 34%, transparent 58%),
    radial-gradient(circle at 22% 28%, rgba(67,97,58,0.18), transparent 0 24%, transparent 48%),
    linear-gradient(135deg, rgba(31,15,6,0.8), rgba(9,4,2,0.66));
  box-shadow: 0 22px 70px var(--shadow), inset 0 1px 0 rgba(255,235,180,0.16);
  backdrop-filter: blur(4px);
}

.stat-card {
  position: absolute;
  display: grid;
  grid-template-columns: auto auto;
  align-items: center;
  gap: 6px 10px;
  width: max-content;
  max-width: 46%;
  padding: 8px 10px;
  border: 0;
  border-radius: 999px;
  color: rgba(255,232,190,0.76);
  text-align: left;
  transform: translate(-50%, -50%) rotate(var(--stat-rotate));
  transition: color 0.2s ease, filter 0.2s ease, transform 0.2s ease;
}

.stat-card:hover {
  color: #ffe2a0;
  filter: drop-shadow(0 0 14px rgba(241,183,86,0.26));
  transform: translate(-50%, -50%) rotate(0deg) scale(1.04);
}

.stat-icon {
  grid-row: 1 / span 2;
  width: clamp(16px, calc(var(--stat-size) * 0.56), 34px);
  height: clamp(16px, calc(var(--stat-size) * 0.56), 34px);
  color: rgba(241,183,86,0.56);
}

.stat-card strong {
  font-family: var(--font-display);
  color: #ffe2a0;
  font-size: var(--stat-size);
  line-height: 1;
  text-shadow: 0 0 22px rgba(241,183,86,0.28);
}

.stat-label-text {
  color: rgba(255,232,190,0.58);
  font-size: clamp(0.72rem, calc(var(--stat-size) * 0.32), 1rem);
  white-space: nowrap;
}

.cloud-main {
  left: 50%;
  top: 48%;
  z-index: 3;
}

.cloud-west {
  left: 22%;
  top: 38%;
}

.cloud-east {
  left: 76%;
  top: 34%;
}

.cloud-south {
  left: 62%;
  top: 74%;
}

.cloud-noise {
  position: absolute;
  color: rgba(255,232,190,0.18);
  font-family: var(--font-display);
  font-weight: 700;
  pointer-events: none;
}

.cloud-noise-a {
  left: 11%;
  top: 66%;
  font-size: 0.95rem;
  transform: rotate(-8deg);
}

.cloud-noise-b {
  left: 31%;
  top: 18%;
  font-size: 1.1rem;
  color: rgba(67,97,58,0.36);
}

.cloud-noise-c {
  right: 17%;
  bottom: 18%;
  font-size: 1.35rem;
  transform: rotate(8deg);
}

.cloud-noise-d {
  right: 8%;
  top: 58%;
  font-size: 0.9rem;
  color: rgba(241,183,86,0.22);
}

/* ═══════ §3.3 趣味数据双栏 ════════════════════════ */
.fun-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.04fr) minmax(0, 0.96fr);
  gap: 16px;
}

.fun-card {
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(24,11,5,0.76), rgba(7,3,1,0.68)),
    radial-gradient(circle at 88% 10%, rgba(242,192,111,0.08), transparent 0 32%);
  backdrop-filter: blur(5px);
}

.fun-card h2 {
  margin: 2px 0 14px;
  font-family: var(--font-display);
  font-size: 1.1rem;
  color: #ffe2a0;
}

/* ═══════ §3.4 技能标签区 ══════════════════════════ */
.skill-section {
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(24,11,5,0.75), rgba(7,3,1,0.68)),
    radial-gradient(circle at 14% 0%, rgba(67,97,58,0.12), transparent 0 38%);
  backdrop-filter: blur(5px);
}

.skill-section h2 {
  margin: 2px 0 14px;
  font-family: var(--font-display);
  font-size: 1.1rem;
  color: #ffe2a0;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: 999px;
  font-size: 0.82rem;
  transition: all 0.2s;
}

.skill-tag em {
  font-style: normal;
  font-size: 0.72rem;
  opacity: 0.7;
}

/* common (1-2) */
.quality-common {
  border: 1px solid rgba(238,184,91,0.2);
  background: rgba(255,226,160,0.03);
  color: rgba(255,231,183,0.55);
}

/* silver (3-4) */
.quality-silver {
  border: 1px solid rgba(192,192,192,0.5);
  background: rgba(192,192,192,0.06);
  color: rgba(220,220,230,0.8);
}

/* gold (5+) */
.quality-gold {
  border: 1px solid rgba(241,183,86,0.6);
  background: rgba(241,183,86,0.1);
  color: #ffe2a0;
  box-shadow: 0 0 8px rgba(241,183,86,0.1);
}

/* ═══════ §3.5 Tab 切换内容区 ═══════════════════════ */
.tab-section {
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(24,11,5,0.78), rgba(7,3,1,0.72)),
    radial-gradient(circle at 86% 4%, rgba(242,192,111,0.08), transparent 0 30%);
  backdrop-filter: blur(5px);
}

.tab-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.tab-bar button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 16px;
  border: 1px solid rgba(238,184,91,0.25);
  border-radius: 999px;
  color: rgba(255,232,190,0.6);
  background: rgba(7,4,2,0.3);
  transition: all 0.2s;
}

.tab-bar button:hover {
  border-color: rgba(238,184,91,0.5);
  color: rgba(255,232,190,0.85);
}

.tab-bar button.active {
  border-color: rgba(241,183,86,0.7);
  color: #ffe2a0;
  background: rgba(241,183,86,0.1);
}

.tab-bar button svg {
  stroke: currentColor;
}

/* ─── Tab 1: 贡献历程 ────────────────────────────── */
.timeline {
  display: grid;
  gap: 0;
}

.timeline-item {
  display: grid;
  grid-template-columns: 32px 1fr;
  gap: 14px;
}

.timeline-dot-col {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeline-dot {
  width: 14px;
  height: 14px;
  border: 2px solid #f2c06f;
  border-radius: 50%;
  background: rgba(12,7,4,0.8);
  box-shadow: 0 0 0 4px rgba(242,192,111,0.08);
  flex-shrink: 0;
  margin-top: 4px;
}

.timeline-line {
  width: 2px;
  flex: 1;
  background: rgba(242,192,111,0.15);
  min-height: 16px;
}

.timeline-card {
  padding: 12px 16px 16px;
  border: 1px solid rgba(240,198,118,0.12);
  border-radius: var(--radius);
  background: rgba(7,4,2,0.25);
  margin-bottom: 14px;
  transition: border-color 0.2s;
}

.timeline-card:hover {
  border-color: rgba(240,198,118,0.3);
}

.tc-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tc-date {
  color: rgba(255,232,190,0.45);
  font-size: 0.78rem;
}

.tc-xp {
  color: #f2c06f;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 0.9rem;
}

.timeline-card h3 {
  margin: 6px 0 0;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.1rem;
}

.tc-repo {
  margin: 4px 0 0;
  color: rgba(255,232,190,0.45);
  font-size: 0.78rem;
}

.tc-summary {
  margin: 8px 0 0;
  color: rgba(255,232,190,0.7);
  font-size: 0.88rem;
  line-height: 1.55;
}

.tc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.tc-tags span {
  border: 1px solid rgba(238,184,91,0.2);
  border-radius: 999px;
  padding: 2px 10px;
  color: rgba(255,232,190,0.6);
  font-size: 0.72rem;
  background: rgba(255,232,190,0.04);
}

/* ─── Tab 2: 成就徽章 ────────────────────────────── */
.badge-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.badge-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 12px;
  border: 1px solid rgba(240,198,118,0.15);
  border-radius: var(--radius);
  background: rgba(7,4,2,0.25);
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
}

.badge-card:not(.locked):hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(242,192,111,0.12);
}

.badge-card.locked {
  opacity: 0.4;
  filter: grayscale(0.6);
}

.badge-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.badge-card strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1rem;
}

.badge-card p {
  margin: 0;
  color: rgba(255,232,190,0.65);
  font-size: 0.82rem;
  line-height: 1.4;
}

.badge-date {
  color: rgba(255,232,190,0.35);
  font-size: 0.72rem;
}

.wear-btn {
  border: 1px solid rgba(238,184,91,0.3);
  border-radius: 999px;
  padding: 3px 12px;
  color: rgba(255,232,190,0.6);
  background: transparent;
  font-size: 0.72rem;
  transition: all 0.2s;
}

.wear-btn:hover {
  border-color: rgba(241,183,86,0.6);
  color: #ffe2a0;
}

.wear-btn.worn {
  border-color: rgba(241,183,86,0.7);
  color: #ffe2a0;
  background: rgba(241,183,86,0.12);
}

/* ─── Tab 3: 成长里程碑 ──────────────────────────── */
.milestone-list {
  display: grid;
  gap: 0;
}

.milestone-item {
  display: grid;
  grid-template-columns: 40px 1fr;
  gap: 12px;
}

.ms-dot-col {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.ms-icon-wrap {
  width: 36px;
  height: 36px;
  border: 2px solid rgba(241,183,86,0.5);
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(12,7,4,0.7);
  color: #f2c06f;
  flex-shrink: 0;
}

.ms-line {
  width: 2px;
  flex: 1;
  background: rgba(242,192,111,0.12);
  min-height: 12px;
}

.ms-content {
  padding-bottom: 16px;
}

.ms-content h3 {
  margin: 0;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1rem;
}

.ms-content p {
  margin: 4px 0 0;
  color: rgba(255,232,190,0.65);
  font-size: 0.85rem;
  line-height: 1.5;
}

.ms-date {
  color: rgba(255,232,190,0.35);
  font-size: 0.72rem;
}

.ms-empty {
  text-align: center;
  color: rgba(255,232,190,0.5);
  padding: 30px 0;
}

/* ═══════ §3.6 快捷操作栏 ══════════════════════════ */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 48px;
  border: 1px solid rgba(238,184,91,0.25);
  border-radius: var(--radius);
  color: rgba(255,232,190,0.7);
  background: rgba(7,4,2,0.3);
  transition: all 0.2s;
  cursor: pointer;
}

.action-btn svg {
  stroke: currentColor;
}

.action-btn:hover {
  border-color: rgba(241,183,86,0.6);
  color: #ffe2a0;
}

/* ═══════ empty states ═════════════════════════════ */
.section-empty {
  margin: 8px 0 0;
  padding: 18px 4px;
  color: rgba(255,232,190,0.5);
  font-size: 0.86rem;
  line-height: 1.6;
  text-align: center;
}

/* ═══════ kicker ═══════════════════════════════════ */
.kicker {
  margin: 0;
  color: rgba(255,232,190,0.5);
  font-family: var(--font-display);
  font-size: 0.75rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

/* ═══════ responsive ════════════════════════════════ */
@media (max-width: 1180px) {
  .growth-map-points {
    display: none;
  }

  .profile-shell {
    width: min(880px, calc(100vw - 72px));
    margin: 0 auto;
  }
}

@media (max-width: 768px) {
  .profile-scene {
    padding: 92px 14px 28px;
  }

  .profile-scene::before {
    background-position: right top;
    background-size: cover;
  }

  .profile-scene::after {
    background:
      linear-gradient(90deg, rgba(5,2,1,0.86), rgba(7,3,1,0.5), rgba(5,2,1,0.86)),
      linear-gradient(180deg, rgba(4,1,0,0.72), rgba(7,3,1,0.28) 36%, rgba(4,1,0,0.88));
  }

  .profile-shell {
    width: 100%;
    margin: 0;
  }

  .profile-shell::before {
    inset: -8px;
    backdrop-filter: blur(1px);
  }

  .license-body {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
    padding: 22px 18px;
  }

  .license-left {
    flex-direction: row;
    gap: 14px;
  }

  .license-name-left {
    display: none;
  }

  .license-motto {
    border-left: none;
    padding-left: 0;
  }

  .license-stats {
    justify-content: center;
  }

  .fun-grid {
    grid-template-columns: 1fr;
  }

  .stat-cloud {
    min-height: auto;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
    gap: 8px 12px;
    padding: 18px 12px;
  }

  .stat-cloud .stat-card {
    position: relative;
    left: auto;
    top: auto;
    max-width: 100%;
    transform: none;
  }

  .cloud-main {
    flex-basis: 100%;
    justify-content: center;
  }

  .cloud-noise {
    display: none;
  }

  .badge-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stat-card,
  .timeline-card,
  .badge-card,
  .action-btn,
  .xp-bar i,
  .skill-tag {
    transition: none !important;
  }
}
</style>
