<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import deskImg from '../../assets/desk.webp'
import HomeOrb from '../../components/HomeOrb.vue'
import { questApi } from '../../api/questApi'
import { repositoryApi } from '../../api/repositoryApi'
import { submissionApi } from '../../api/submissionApi'
import { sessionStore } from '../../stores/sessionStore'
import { toBrowsableGiteaUrl } from '../../utils/giteaUrl'

const router = useRouter()

const maintainerName = computed(
  () => sessionStore.user?.displayName || sessionStore.user?.username || '委托人',
)

// 待审计数（null = 未知/隐藏徽标）。审核队列会返回全状态提交，这里只统计待批阅项。
const pendingReviews = ref(null)
const repos = ref([])
const reposLoading = ref(true)
// 正在删除的仓库 ID（用于行内按钮禁用/文案）；null = 无进行中删除。
const deletingRepoId = ref(null)

// 我发布的委托（含所有状态，让维护者看到 DRAFT/待审核 等"未上架"的委托）。
const myQuests = ref([])
const myQuestsLoading = ref(true)

const QUEST_STATUS = {
  DRAFT: { label: '草稿', tone: 'draft' },
  PENDING_ADMIN_REVIEW: { label: '待管理员审核', tone: 'pending' },
  PUBLISHED: { label: '已上架', tone: 'published' },
  IN_PROGRESS: { label: '进行中', tone: 'active' },
  IN_REVIEW: { label: '审核中', tone: 'pending' },
  COMPLETED: { label: '已完成', tone: 'done' },
  REJECTED: { label: '被退回', tone: 'rejected' },
  CLOSED: { label: '已下架', tone: 'rejected' },
  // 提交级状态：委托人把冒险家的提交退回要求修改（Quest 仍停留在 IN_REVIEW，靠 latestSubmissionStatus 体现）
  CHANGES_REQUESTED: { label: '已要求修改', tone: 'changes' },
}
function questStatus(status) {
  return QUEST_STATUS[status] ?? { label: status || '未知', tone: 'draft' }
}

// 展示用的有效状态：委托人退回提交时 Quest 仍是 IN_REVIEW，但语义上应显示为「已要求修改」，
// 因此最近一次提交为 CHANGES_REQUESTED 时优先以提交状态展示。
function effectiveStatus(quest) {
  if (quest?.latestSubmissionStatus === 'CHANGES_REQUESTED') return 'CHANGES_REQUESTED'
  return quest?.status
}

// 状态筛选：'ALL' 表示不过滤。chip 列表只展示当前委托里实际出现过的状态，避免空选项。
const questStatusFilter = ref('ALL')
const questStatusFilterOptions = computed(() => {
  const present = new Set(myQuests.value.map((q) => effectiveStatus(q)))
  return Object.keys(QUEST_STATUS).filter((status) => present.has(status))
})
const filteredMyQuests = computed(() => {
  if (questStatusFilter.value === 'ALL') return myQuests.value
  return myQuests.value.filter((q) => effectiveStatus(q) === questStatusFilter.value)
})
function setQuestStatusFilter(status) {
  questStatusFilter.value = status
}

// 已驳回委托的驳回原因弹窗状态。
// { quest, loading, reason, error } — null 表示弹窗关闭。
const rejectionModal = ref(null)

function isRejected(status) {
  return status === 'REJECTED' || status === 'CLOSED'
}

// 点击「已驳回」徽标：打开弹窗并拉取该委托最近一条管理员审核意见。
async function openRejection(quest) {
  rejectionModal.value = { quest, loading: true, reason: '', error: '' }
  try {
    const res = await questApi.reviews(quest.questId)
    const records = Array.isArray(res?.data) ? res.data : []
    // 取最近一条驳回/关闭决策的原因；接口已按时间倒序返回。
    const latest = records.find((r) => r.decision && r.reason) ?? records[0]
    if (!rejectionModal.value) return
    rejectionModal.value.reason = latest?.reason ?? ''
    rejectionModal.value.loading = false
  } catch (error) {
    if (!rejectionModal.value) return
    rejectionModal.value.error = error?.message ?? '驳回原因读取失败，请稍后再试。'
    rejectionModal.value.loading = false
  }
}

function closeRejection() {
  rejectionModal.value = null
}

// 重新发布委托：关闭弹窗并前往发布页（带上原仓库，便于复用）。
function republishFromRejection() {
  const repoId = rejectionModal.value?.quest?.repository?.repositoryId
  closeRejection()
  router.push(repoId ? { name: 'maintainer-publish', query: { repoId } } : { name: 'maintainer-publish' })
}

// 返回上一页（保留用户来路）；无站内历史（直接深链进入）时兜底回大厅。
function goBack() {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push({ name: 'hall' })
  }
}
function goPublish() {
  router.push({ name: 'maintainer-publish' })
}
function goReviews() {
  router.push({ name: 'maintainer-review' })
}
function goAdventureWorkbench() {
  router.push({ name: 'adventurer-workbench' })
}
function goRepoSync() {
  router.push({ name: 'repository-sync' })
}
function openRepo(repo) {
  const url = toBrowsableGiteaUrl(repo.sourceUrl || repo.giteaUrl)
  if (url) window.open(url, '_blank', 'noopener,noreferrer')
}

async function loadRepos({ silent = false } = {}) {
  if (!silent) reposLoading.value = true
  try {
    const res = await repositoryApi.myRepositories()
    const data = res?.data
    repos.value = Array.isArray(data?.items) ? data.items : Array.isArray(data) ? data : []
  } catch {
    if (!silent) repos.value = []
  } finally {
    reposLoading.value = false
  }
}

// 删除仓库二次确认弹窗：null = 不显示；否则为 { repo, error }
const deleteConfirm = ref(null)

function requestDeleteRepo(repo) {
  if (deletingRepoId.value) return
  deleteConfirm.value = { repo, error: '' }
}

function cancelDeleteRepo() {
  if (deletingRepoId.value) return
  deleteConfirm.value = null
}

// 删除已接入仓库：二次确认后调用后端级联删除，成功则刷新列表与「我发布的委托」。
async function confirmDeleteRepo() {
  const repo = deleteConfirm.value?.repo
  if (!repo || deletingRepoId.value) return

  deletingRepoId.value = repo.repositoryId
  try {
    await repositoryApi.remove(repo.repositoryId)
    deleteConfirm.value = null
    await Promise.all([loadRepos(), loadMyQuests()])
  } catch (err) {
    if (deleteConfirm.value) deleteConfirm.value.error = err?.message || '删除失败，请稍后重试。'
  } finally {
    deletingRepoId.value = null
  }
}

async function loadMyQuests({ silent = false } = {}) {
  if (!silent) myQuestsLoading.value = true
  try {
    const res = await questApi.myPublished()
    myQuests.value = Array.isArray(res?.data) ? res.data : []
  } catch {
    if (!silent) myQuests.value = []
  } finally {
    myQuestsLoading.value = false
  }
}

function isPendingReviewSubmission(item) {
  return item?.status === 'PENDING_REVIEW'
}

async function loadPendingReviews() {
  try {
    const res = await submissionApi.reviewQueue()
    const items = Array.isArray(res?.data) ? res.data : []
    pendingReviews.value = items.filter(isPendingReviewSubmission).length
  } catch {
    pendingReviews.value = null
  }
}

// 统一刷新工作台数据。silent=true 用于后台自动刷新，不闪现骨架屏。
async function refreshWorkbench({ silent = false } = {}) {
  await Promise.all([
    loadPendingReviews(),
    loadRepos({ silent }),
    loadMyQuests({ silent }),
  ])
}

// 管理员在别处改了委托状态后，委托人这边需要无刷新同步：回到本页（可见/聚焦）即静默拉取，
// 另加一个轻量轮询兜底，避免一直停在本页时状态长期不更新。
let pollTimer = null
function handleVisibility() {
  if (document.visibilityState === 'visible') refreshWorkbench({ silent: true })
}

onMounted(async () => {
  await refreshWorkbench()
  document.addEventListener('visibilitychange', handleVisibility)
  window.addEventListener('focus', handleVisibility)
  pollTimer = window.setInterval(() => refreshWorkbench({ silent: true }), 20000)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibility)
  window.removeEventListener('focus', handleVisibility)
  if (pollTimer) window.clearInterval(pollTimer)
})
</script>

<template>
  <main class="app-shell">
    <section class="scene office-scene" :style="{ backgroundImage: `url(${deskImg})` }">
      <HomeOrb />
      <button class="back-orb" type="button" aria-label="返回上一页" @click="goBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回</span>
      </button>

      <div class="office-hall">
        <header class="office-head">
          <p class="kicker">公会事务所 · Commission Office</p>
          <h1>委托人 {{ maintainerName }}</h1>
          <p class="office-sub">在此发布委托、批阅冒险家呈交的成果。</p>
        </header>

        <div class="office-portals">
          <button class="office-portal" type="button" data-tutorial="maintainer-publish-entry" @click="goPublish">
            <span class="office-portal-glyph" aria-hidden="true">✎</span>
            <span class="office-portal-body">
              <strong>发布委托</strong>
              <small>起草新任务，提交管理员审核后上架悬赏板</small>
            </span>
            <span class="office-portal-arrow" aria-hidden="true">→</span>
          </button>

          <button class="office-portal" type="button" data-tutorial="maintainer-review-entry" @click="goReviews">
            <span class="office-portal-glyph" aria-hidden="true">
              ⚖
              <span v-if="pendingReviews" class="office-portal-badge">{{ pendingReviews }}</span>
            </span>
            <span class="office-portal-body">
              <strong>审核台</strong>
              <small>{{ pendingReviews ? `${pendingReviews} 份成果待批阅` : '批阅冒险家呈交的成果' }}</small>
            </span>
            <span class="office-portal-arrow" aria-hidden="true">→</span>
          </button>

          <button class="office-portal" type="button" data-tutorial="maintainer-repo-sync-entry" @click="goRepoSync">
            <span class="office-portal-glyph" aria-hidden="true">⤓</span>
            <span class="office-portal-body">
              <strong>新建或导入仓库</strong>
              <small>从 GitHub / Gitee 等导入或同步受托仓库，作为发布委托的来源。</small>
            </span>
            <span class="office-portal-arrow" aria-hidden="true">→</span>
          </button>

          <button
            class="office-portal office-portal-adventure"
            type="button"
            data-tutorial="maintainer-adventure-entry"
            @click="goAdventureWorkbench"
          >
            <span class="office-portal-glyph" aria-hidden="true">◆</span>
            <span class="office-portal-body">
              <strong>完成委托</strong>
              <small>进入冒险家工作台，查看已接取任务、提交成果并追踪成长档案。</small>
            </span>
            <span class="office-portal-arrow" aria-hidden="true">→</span>
          </button>
        </div>

        <section class="office-myquests" aria-label="我发布的委托" data-tutorial="maintainer-my-quests">
          <header class="office-repos-head">
            <p class="kicker">我发布的委托 · My Commissions</p>
            <button class="quiet-action office-sync-btn" type="button" @click="goPublish">发布新委托</button>
          </header>

          <div v-if="questStatusFilterOptions.length > 1" class="quest-filter-options" aria-label="按状态筛选">
            <button
              type="button"
              class="quest-filter-chip"
              :class="{ active: questStatusFilter === 'ALL' }"
              :aria-pressed="questStatusFilter === 'ALL'"
              @click="setQuestStatusFilter('ALL')"
            >
              全部 · {{ myQuests.length }}
            </button>
            <button
              v-for="status in questStatusFilterOptions"
              :key="status"
              type="button"
              class="quest-filter-chip"
              :class="{ active: questStatusFilter === status }"
              :aria-pressed="questStatusFilter === status"
              @click="setQuestStatusFilter(status)"
            >
              {{ questStatus(status).label }}
            </button>
          </div>

          <ul v-if="myQuestsLoading" class="office-repo-list" aria-hidden="true">
            <li v-for="n in 2" :key="n" class="office-repo-row is-skeleton">
              <span class="sk sk-name"></span>
              <span class="sk sk-meta"></span>
            </li>
          </ul>

          <p v-else-if="!myQuests.length" class="office-repos-empty">
            还没有发布过委托。点
            <button class="office-link" type="button" @click="goPublish">发布委托</button>
            起草第一个任务。
          </p>

          <p v-else-if="!filteredMyQuests.length" class="office-repos-empty">
            没有符合该状态的委托。
          </p>

          <ul v-else class="office-quest-list">
            <li v-for="q in filteredMyQuests" :key="q.questId" class="office-quest-row">
              <span class="office-quest-title">{{ q.title }}</span>
              <span class="office-quest-repo">{{ q.repository?.name || '—' }}</span>
              <button
                v-if="isRejected(q.status)"
                type="button"
                class="office-quest-badge is-clickable"
                :class="questStatus(q.status).tone"
                title="点击查看被退回的原因"
                @click="openRejection(q)"
              >
                {{ questStatus(q.status).label }}
                <span class="office-quest-badge-peek" aria-hidden="true">查看原因 ›</span>
              </button>
              <span v-else class="office-quest-badge" :class="questStatus(effectiveStatus(q)).tone">
                {{ questStatus(effectiveStatus(q)).label }}
              </span>
            </li>
          </ul>
        </section>

        <section class="office-repos" aria-label="受托仓库" data-tutorial="maintainer-repositories">
          <header class="office-repos-head">
            <p class="kicker">受托仓库 · Repositories</p>
            <button class="quiet-action office-sync-btn" type="button" @click="goRepoSync">
              新建 / 导入
            </button>
          </header>

          <ul v-if="reposLoading" class="office-repo-list" aria-hidden="true">
            <li v-for="n in 2" :key="n" class="office-repo-row is-skeleton">
              <span class="sk sk-name"></span>
            </li>
          </ul>

          <p v-else-if="!repos.length" class="office-repos-empty">
            尚未接入受托仓库。
            <button class="office-link" type="button" @click="goRepoSync">前往新建或导入仓库</button>
            ，之后即可基于其发布委托。
          </p>

          <ul v-else class="office-repo-list">
            <li v-for="r in repos" :key="r.repositoryId" class="office-repo-row">
              <span class="office-repo-name">{{ r.name }}</span>
              <span class="office-repo-actions">
                <button
                  v-if="r.sourceUrl || r.giteaUrl"
                  class="office-link"
                  type="button"
                  @click="openRepo(r)"
                >
                  在 Gitea 打开 ↗
                </button>
                <button
                  class="office-link office-link-danger"
                  type="button"
                  :disabled="deletingRepoId === r.repositoryId"
                  @click="requestDeleteRepo(r)"
                >
                  {{ deletingRepoId === r.repositoryId ? '删除中…' : '删除' }}
                </button>
              </span>
            </li>
          </ul>
        </section>
      </div>
    </section>

    <!-- 已驳回委托：查看驳回原因弹窗。返回工作台只关闭弹窗，不跳转。 -->
    <transition name="reject-pop">
      <div
        v-if="rejectionModal"
        class="reject-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="reject-title"
        @click.self="closeRejection"
      >
        <div class="reject-card">
          <button class="reject-close" type="button" aria-label="关闭" @click="closeRejection">×</button>
          <span class="reject-icon" aria-hidden="true">⊘</span>
          <p class="kicker">Commission Returned</p>
          <h2 id="reject-title">委托被驳回</h2>
          <p class="reject-quest-title">{{ rejectionModal.quest?.title }}</p>

          <div class="reject-reason-box">
            <p v-if="rejectionModal.loading" class="reject-reason muted">正在读取驳回原因…</p>
            <p v-else-if="rejectionModal.error" class="reject-reason danger">{{ rejectionModal.error }}</p>
            <p v-else-if="rejectionModal.reason" class="reject-reason">{{ rejectionModal.reason }}</p>
            <p v-else class="reject-reason muted">管理员未留下具体说明。</p>
          </div>

          <div class="reject-actions">
            <button class="quiet-action" type="button" @click="closeRejection">返回工作台</button>
            <button class="primary-action" type="button" @click="republishFromRejection">重新发布委托 →</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 删除仓库二次确认：列出仓库名称与级联影响范围，取消在左、确认在右。 -->
    <transition name="reject-pop">
      <div
        v-if="deleteConfirm"
        class="delete-repo-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="delete-repo-title"
        @click.self="cancelDeleteRepo"
      >
        <div class="delete-repo-card">
          <button class="reject-close" type="button" aria-label="关闭" :disabled="!!deletingRepoId" @click="cancelDeleteRepo">×</button>
          <span class="reject-icon" aria-hidden="true">⊘</span>
          <p class="kicker">Delete Repository</p>
          <h2 id="delete-repo-title">确认删除这个仓库？</h2>
          <p class="reject-quest-title">{{ deleteConfirm.repo?.name }}</p>

          <div class="reject-reason-box">
            <p class="reject-reason">
              该仓库下的所有委托、提交、审核记录及平台 Gitea 副本都会被一并删除，且<strong>不可恢复</strong>。
            </p>
            <p v-if="deleteConfirm.error" class="reject-reason danger">{{ deleteConfirm.error }}</p>
          </div>

          <div class="reject-actions">
            <button class="quiet-action" type="button" :disabled="!!deletingRepoId" @click="cancelDeleteRepo">取消</button>
            <button class="danger-action" type="button" :disabled="!!deletingRepoId" @click="confirmDeleteRepo">
              {{ deletingRepoId ? '删除中…' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </main>
</template>

<style scoped>
.office-scene {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(24px, 6vh, 72px) 18px;
  /* desk.webp 的暖棕主色兜底：图片下载完成前先铺同色调底，避免「近黑 → 暖色木桌」的突兀切换。 */
  background-color: #241710;
  background-repeat: no-repeat;
}

.office-hall {
  width: min(880px, 100%);
  display: grid;
  gap: clamp(20px, 3.4vh, 34px);
  padding: clamp(26px, 4vw, 44px);
  border: 1px solid var(--line);
  border-radius: 14px;
  background: linear-gradient(165deg, rgba(28, 16, 8, 0.82), rgba(14, 8, 4, 0.86));
  box-shadow: 0 26px 60px var(--shadow), inset 0 1px 0 rgba(255, 224, 166, 0.08);
  backdrop-filter: blur(7px);
}

.office-head h1 {
  margin: 2px 0 6px;
  color: #ffe9c4;
  font-size: clamp(1.7rem, 3.4vw, 2.3rem);
  letter-spacing: -0.01em;
}
.office-sub {
  margin: 0;
  max-width: 52ch;
  color: rgba(255, 230, 190, 0.66);
  font-size: 0.95rem;
  line-height: 1.55;
}

/* ── 两个主入口 ─────────────────────────────────────────── */
.office-portals {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: clamp(14px, 2vw, 20px);
}
.office-portal {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  border: 1px solid var(--line);
  border-radius: 12px;
  text-align: left;
  color: #ffe9c4;
  background: linear-gradient(160deg, rgba(80, 43, 18, 0.4), rgba(24, 13, 7, 0.5));
  cursor: pointer;
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease,
    background 160ms ease;
}
.office-portal:hover,
.office-portal:focus-visible {
  transform: translateY(-2px);
  border-color: var(--gold-bright);
  background: linear-gradient(160deg, rgba(110, 60, 24, 0.52), rgba(34, 19, 9, 0.58));
  box-shadow: 0 0 0 6px rgba(255, 204, 105, 0.1), 0 16px 32px rgba(0, 0, 0, 0.42);
  outline: none;
}
.office-portal:active {
  transform: translateY(0) scale(0.99);
}
/* 四个入口排成 2×2 网格，「完成委托」不再独占整行。 */
.office-portal-glyph {
  position: relative;
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border: 1px solid rgba(244, 192, 111, 0.55);
  border-radius: 50%;
  background: radial-gradient(circle at 34% 30%, rgba(255, 224, 166, 0.32), transparent 60%),
    rgba(16, 9, 4, 0.5);
  color: var(--gold-bright);
  font-size: 1.5rem;
  line-height: 1;
}
.office-portal-badge {
  position: absolute;
  top: -7px;
  right: -7px;
  min-width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  padding: 0 5px;
  border: 1px solid rgba(255, 214, 196, 0.5);
  border-radius: 999px;
  background: var(--wine);
  color: #ffe7d6;
  font-size: 0.74rem;
  font-weight: 700;
  font-family: var(--font-body);
}
.office-portal-body {
  display: grid;
  gap: 4px;
  min-width: 0;
}
.office-portal-body strong {
  font-family: var(--font-display);
  font-size: 1.18rem;
  letter-spacing: 0.01em;
}
.office-portal-body small {
  color: rgba(255, 230, 190, 0.6);
  font-size: 0.82rem;
  line-height: 1.4;
}
.office-portal-arrow {
  color: rgba(244, 192, 111, 0.7);
  font-size: 1.2rem;
  transition: transform 160ms ease;
}
.office-portal:hover .office-portal-arrow,
.office-portal:focus-visible .office-portal-arrow {
  transform: translateX(3px);
}

/* ── 我发布的委托 ───────────────────────────────────────── */
.office-myquests {
  display: grid;
  gap: 12px;
  padding-top: 4px;
  border-top: 1px solid rgba(238, 184, 91, 0.22);
}
.office-quest-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.office-quest-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(238, 184, 91, 0.24);
  border-radius: 8px;
  background: rgba(20, 11, 6, 0.42);
}
.office-quest-title {
  font-family: var(--font-display);
  font-size: 1.02rem;
  color: #ffe9c4;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.office-quest-repo {
  color: rgba(255, 230, 190, 0.55);
  font-size: 0.82rem;
}
.office-quest-badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 700;
  border: 1px solid transparent;
  white-space: nowrap;
}
.office-quest-badge.draft {
  color: #d9c7a6;
  background: rgba(120, 100, 70, 0.28);
  border-color: rgba(200, 170, 120, 0.3);
}
.office-quest-badge.pending {
  color: #ffd98a;
  background: rgba(150, 100, 20, 0.3);
  border-color: rgba(240, 184, 104, 0.4);
}
.office-quest-badge.published {
  color: #2a1605;
  background: linear-gradient(180deg, #ffd98a, #d89a32);
  border-color: rgba(255, 233, 178, 0.7);
  box-shadow: 0 0 0 1px rgba(255, 217, 138, 0.18);
}
.office-quest-badge.active {
  color: #cfe6ad;
  background: rgba(60, 110, 40, 0.3);
  border-color: rgba(169, 208, 123, 0.4);
}
.office-quest-badge.done {
  color: #a9d07b;
  background: rgba(43, 74, 28, 0.45);
  border-color: rgba(169, 208, 123, 0.45);
}
.office-quest-badge.rejected {
  color: #f0a890;
  background: rgba(110, 42, 36, 0.3);
  border-color: rgba(220, 130, 110, 0.4);
}
.office-quest-badge.changes {
  color: #ffc98a;
  background: rgba(150, 78, 20, 0.32);
  border-color: rgba(240, 160, 90, 0.42);
}

/* 可点击的「已驳回」徽标：悬停时浮现「查看原因 ›」并加深底色，暗示可交互。 */
.office-quest-badge.is-clickable {
  display: inline-flex;
  align-items: center;
  gap: 0;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, box-shadow 150ms ease;
}
.office-quest-badge.is-clickable:hover,
.office-quest-badge.is-clickable:focus-visible {
  background: rgba(150, 56, 48, 0.42);
  border-color: rgba(240, 150, 128, 0.6);
  box-shadow: 0 0 0 4px rgba(220, 130, 110, 0.12);
  outline: none;
}
.office-quest-badge-peek {
  max-width: 0;
  overflow: hidden;
  opacity: 0;
  white-space: nowrap;
  transition: max-width 200ms ease, opacity 150ms ease, margin-left 200ms ease;
}
.office-quest-badge.is-clickable:hover .office-quest-badge-peek,
.office-quest-badge.is-clickable:focus-visible .office-quest-badge-peek {
  max-width: 88px;
  margin-left: 6px;
  opacity: 0.92;
}

/* ── 驳回原因弹窗 ───────────────────────────────────────── */
.reject-modal {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(10, 5, 3, 0.62);
  backdrop-filter: blur(2px);
}
.reject-card {
  position: relative;
  width: min(460px, 100%);
  padding: 34px 30px 26px;
  text-align: center;
  border: 1px solid rgba(220, 130, 110, 0.42);
  border-radius: 14px;
  color: #ffe9c4;
  background: linear-gradient(168deg, rgba(60, 26, 20, 0.96), rgba(28, 14, 9, 0.98));
  box-shadow: 0 26px 64px rgba(0, 0, 0, 0.55);
}
.reject-close {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 200, 184, 0.7);
  font-size: 1.4rem;
  line-height: 1;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease;
}
.reject-close:hover {
  background: rgba(220, 130, 110, 0.18);
  color: #ffd0c2;
}
.reject-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 8px;
  border-radius: 50%;
  background: rgba(150, 56, 48, 0.34);
  color: #f3a691;
  font-size: 1.5rem;
}
.reject-card h2 {
  margin: 8px 0 4px;
  font-family: var(--font-display);
  color: #ffe1d6;
}
.reject-quest-title {
  margin: 0 0 16px;
  color: rgba(255, 230, 190, 0.7);
  font-size: 0.92rem;
}
.reject-reason-box {
  text-align: left;
  padding: 14px 16px;
  border: 1px solid rgba(220, 130, 110, 0.28);
  border-radius: 10px;
  background: rgba(15, 8, 5, 0.5);
}
.reject-reason {
  margin: 0;
  font-size: 0.94rem;
  line-height: 1.7;
  color: #ffe3d2;
  white-space: pre-wrap;
  word-break: break-word;
}
.reject-reason.muted {
  color: rgba(255, 220, 200, 0.55);
}
.reject-reason.danger {
  color: #ff9f86;
}
.reject-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
}
.reject-pop-enter-active,
.reject-pop-leave-active {
  transition: opacity 180ms ease;
}
.reject-pop-enter-active .reject-card,
.reject-pop-leave-active .reject-card {
  transition: transform 180ms cubic-bezier(0.2, 0.9, 0.3, 1.2), opacity 180ms ease;
}
.reject-pop-enter-from,
.reject-pop-leave-to {
  opacity: 0;
}
.reject-pop-enter-from .reject-card,
.reject-pop-leave-to .reject-card {
  transform: translateY(12px) scale(0.96);
  opacity: 0;
}

/* ── 删除仓库二次确认弹窗（与驳回原因弹窗同款式，红色基调强调破坏性） ── */
.delete-repo-modal {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(10, 5, 3, 0.62);
  backdrop-filter: blur(2px);
}
.delete-repo-card {
  position: relative;
  width: min(460px, 100%);
  padding: 34px 30px 26px;
  text-align: center;
  border: 1px solid rgba(220, 130, 110, 0.42);
  border-radius: 14px;
  color: #ffe9c4;
  background: linear-gradient(168deg, rgba(60, 26, 20, 0.96), rgba(28, 14, 9, 0.98));
  box-shadow: 0 26px 64px rgba(0, 0, 0, 0.55);
}
.danger-action {
  min-height: 42px;
  border: 1px solid rgba(238, 120, 82, 0.62);
  border-radius: 4px;
  padding: 0 16px;
  color: #ffe7d2;
  background: linear-gradient(180deg, #d8634a, #962f1f);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.36);
  cursor: pointer;
  transition: transform 150ms ease, filter 150ms ease;
}
.danger-action:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
}
.danger-action:disabled {
  cursor: not-allowed;
  filter: grayscale(0.3);
  opacity: 0.65;
  transform: none;
}

/* ── 受托仓库 ───────────────────────────────────────────── */
.office-repos {
  display: grid;
  gap: 12px;
  padding-top: 4px;
  border-top: 1px solid rgba(238, 184, 91, 0.22);
}
.office-repos-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.office-repos-head .kicker {
  margin: 0;
}
.office-sync-btn {
  min-height: 34px;
  font-size: 0.82rem;
}
.office-repo-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.office-repo-row {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(238, 184, 91, 0.24);
  border-radius: 8px;
  background: rgba(20, 11, 6, 0.42);
}
.office-repo-name {
  font-family: var(--font-display);
  font-size: 1.02rem;
  color: #ffe9c4;
}
.office-link {
  border: none;
  background: transparent;
  color: var(--gold-bright);
  font-size: 0.82rem;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.office-link:hover {
  color: #ffe1a6;
}
.office-repo-actions {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  white-space: nowrap;
}
.office-link-danger {
  color: #f0a48c;
}
.office-link-danger:hover {
  color: #ff8f6f;
}
.office-link-danger:disabled {
  color: rgba(240, 164, 140, 0.45);
  cursor: progress;
  text-decoration: none;
}
.office-repos-empty {
  margin: 0;
  padding: 16px 14px;
  border: 1px dashed rgba(238, 184, 91, 0.32);
  border-radius: 8px;
  color: rgba(255, 230, 190, 0.66);
  font-size: 0.9rem;
  line-height: 1.6;
  background: rgba(20, 11, 6, 0.3);
}

/* 骨架行 */
.office-repo-row.is-skeleton {
  grid-template-columns: 1fr auto;
}
.sk {
  height: 12px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    rgba(255, 224, 166, 0.08),
    rgba(255, 224, 166, 0.18),
    rgba(255, 224, 166, 0.08)
  );
  background-size: 200% 100%;
  animation: office-shimmer 1.3s ease-in-out infinite;
}
.sk-name {
  width: 42%;
}
@keyframes office-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@media (max-width: 640px) {
  .office-portals {
    grid-template-columns: 1fr;
  }
  .office-repo-row,
  .office-repo-row.is-skeleton {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}
</style>
