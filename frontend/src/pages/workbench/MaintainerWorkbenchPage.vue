<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import deskImg from '../../assets/desk.webp'
import { questApi } from '../../api/questApi'
import { repositoryApi } from '../../api/repositoryApi'
import { submissionApi } from '../../api/submissionApi'
import { sessionStore } from '../../stores/sessionStore'

const router = useRouter()

const maintainerName = computed(
  () => sessionStore.user?.displayName || sessionStore.user?.username || '委托人',
)

// 待审计数（null = 未知/隐藏徽标）。审核队列会返回全状态提交，这里只统计待批阅项。
const pendingReviews = ref(null)
const repos = ref([])
const reposLoading = ref(true)

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
  REJECTED: { label: '已驳回', tone: 'rejected' },
  CLOSED: { label: '已关闭', tone: 'rejected' },
}
function questStatus(status) {
  return QUEST_STATUS[status] ?? { label: status || '未知', tone: 'draft' }
}

function goHall() {
  router.push({ name: 'hall' })
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
  const url = repo.sourceUrl || repo.giteaUrl
  if (url) window.open(url, '_blank', 'noopener,noreferrer')
}

function isPendingReviewSubmission(item) {
  return item?.status === 'PENDING_REVIEW'
}

onMounted(async () => {
  try {
    const res = await submissionApi.reviewQueue()
    const items = Array.isArray(res?.data) ? res.data : []
    pendingReviews.value = items.filter(isPendingReviewSubmission).length
  } catch {
    pendingReviews.value = null
  }

  try {
    const res = await repositoryApi.myRepositories()
    const data = res?.data
    repos.value = Array.isArray(data?.items) ? data.items : Array.isArray(data) ? data : []
  } catch {
    repos.value = []
  } finally {
    reposLoading.value = false
  }

  try {
    const res = await questApi.myPublished()
    myQuests.value = Array.isArray(res?.data) ? res.data : []
  } catch {
    myQuests.value = []
  } finally {
    myQuestsLoading.value = false
  }
})
</script>

<template>
  <main class="app-shell">
    <section class="scene office-scene" :style="{ backgroundImage: `url(${deskImg})` }">
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="goHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回公会大厅</span>
      </button>

      <div class="office-hall">
        <header class="office-head">
          <p class="kicker">公会事务所 · Commission Office</p>
          <h1>委托人 {{ maintainerName }}</h1>
          <p class="office-sub">在此发布委托、批阅冒险家呈交的成果。</p>
        </header>

        <div class="office-portals">
          <button class="office-portal" type="button" @click="goPublish">
            <span class="office-portal-glyph" aria-hidden="true">✎</span>
            <span class="office-portal-body">
              <strong>发布委托</strong>
              <small>起草新任务，提交管理员审核后上架悬赏板</small>
            </span>
            <span class="office-portal-arrow" aria-hidden="true">→</span>
          </button>

          <button class="office-portal" type="button" @click="goReviews">
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

          <button class="office-portal office-portal-adventure" type="button" @click="goAdventureWorkbench">
            <span class="office-portal-glyph" aria-hidden="true">◆</span>
            <span class="office-portal-body">
              <strong>完成委托</strong>
              <small>进入冒险家工作台，查看已接取任务、提交成果并追踪成长档案。</small>
            </span>
            <span class="office-portal-arrow" aria-hidden="true">→</span>
          </button>
        </div>

        <section class="office-myquests" aria-label="我发布的委托">
          <header class="office-repos-head">
            <p class="kicker">我发布的委托 · My Commissions</p>
            <button class="quiet-action office-sync-btn" type="button" @click="goPublish">发布新委托</button>
          </header>

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

          <ul v-else class="office-quest-list">
            <li v-for="q in myQuests" :key="q.questId" class="office-quest-row">
              <span class="office-quest-title">{{ q.title }}</span>
              <span class="office-quest-repo">{{ q.repository?.name || '—' }}</span>
              <span class="office-quest-badge" :class="questStatus(q.status).tone">
                {{ questStatus(q.status).label }}
              </span>
            </li>
          </ul>
        </section>

        <section class="office-repos" aria-label="受托仓库">
          <header class="office-repos-head">
            <p class="kicker">受托仓库 · Repositories</p>
            <button class="quiet-action office-sync-btn" type="button" @click="goRepoSync">
              导入 / 同步
            </button>
          </header>

          <ul v-if="reposLoading" class="office-repo-list" aria-hidden="true">
            <li v-for="n in 2" :key="n" class="office-repo-row is-skeleton">
              <span class="sk sk-name"></span>
              <span class="sk sk-meta"></span>
            </li>
          </ul>

          <p v-else-if="!repos.length" class="office-repos-empty">
            尚未导入受托仓库。
            <button class="office-link" type="button" @click="goRepoSync">前往仓库同步</button>
            ，导入后即可基于其 Issue 发布委托。
          </p>

          <ul v-else class="office-repo-list">
            <li v-for="r in repos" :key="r.repositoryId" class="office-repo-row">
              <span class="office-repo-name">{{ r.name }}</span>
              <span class="office-repo-meta">
                {{ r.defaultBranch || 'main' }} · {{ r.syncStatus || 'SYNCED' }}
              </span>
              <button
                v-if="r.sourceUrl || r.giteaUrl"
                class="office-link"
                type="button"
                @click="openRepo(r)"
              >
                在 Gitea 打开 ↗
              </button>
            </li>
          </ul>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.office-scene {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(24px, 6vh, 72px) 18px;
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
.office-portal-adventure {
  grid-column: 1 / -1;
}
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
  color: #b7d6ff;
  background: rgba(40, 80, 140, 0.3);
  border-color: rgba(120, 170, 240, 0.4);
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
  grid-template-columns: 1fr auto auto;
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
.office-repo-meta {
  color: rgba(255, 230, 190, 0.55);
  font-size: 0.82rem;
  font-variant-numeric: tabular-nums;
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
.sk-meta {
  width: 88px;
  justify-self: end;
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
  .office-repo-meta {
    justify-self: start;
  }
}
</style>
