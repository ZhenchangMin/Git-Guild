<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

import parchmentFormImg from '../assets/submission-form-parchment-v0-clean.png'
import { submissionApi } from '../api/submissionApi'
import { sessionStore } from '../stores/sessionStore'
import {
  evidenceTypes,
  reviewerSlot,
  stageRibbon,
  submissionChecks,
  submissionReviewSteps,
} from '../data/submissionCounter'

const props = defineProps({
  quest: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['submitted'])

// ── Stage machine ──────────────────────────────────────────────────────────
// draft       — user is editing; everything is interactive
// submitting  — request in-flight; UI locks, bell rings
// submitted   — clerk stamped a receipt; sheet locks, receipt overlay shows
// error       — last submit failed; back to draft-like edit, banner visible
const stage = ref('draft')
const isFormOpen = ref(false)
const errorMessage = ref('')
const ringingBell = ref(false)
const receipt = ref(null)

// Accept the loose GitHub / Gitea / GitLab PR/MR URL shape. We intentionally
// keep it lenient — strict origin checks belong on the backend. The keyword
// must precede the digits with an explicit "/" (GitHub uses /pull/, Gitea
// uses /pulls/, GitLab uses /merge_requests/).
const PR_URL_PATTERN = /^https?:\/\/\S+\/(?:pull|pulls|merge_requests)\/\d+(?:[\/?#]\S*)?$/i

const blankForm = () => ({
  repository: '',
  branch: '',
  pullRequest: '',
  note: '',
})
const form = reactive(blankForm())
const checks = reactive(Object.fromEntries(submissionChecks.map((label) => [label, false])))
const evidence = ref([])

const firstInput = ref(null)
const evidenceLabelInput = ref(null)

// ── Draft persistence ──────────────────────────────────────────────────────
// Drafts are keyed by (owner, questId) so multiple users on the same browser
// don't trample each other's drafts. `owner` falls back to "guest" when the
// session has no real user (e.g. the dev shortcut we use to bypass login).
const draftKey = computed(() => {
  const owner = sessionStore.user?.username || sessionStore.user?.displayName || 'guest'
  const questId = props.quest?.id || 'unknown'
  return `git-guild:submission-draft:${owner}:${questId}`
})

function quietlySeedFromQuest() {
  const q = props.quest
  if (!q) return
  // Only fill empty fields; never overwrite something the user already typed.
  if (!form.repository) form.repository = q.detail?.repository?.name ?? 'git-guild / frontend'
  if (!form.branch) form.branch = `feature/${(q.id || 'task').toLowerCase()}`
  const prn = q.detail?.pr?.number
  if (!form.pullRequest && prn && prn !== 'Not created' && /^https?:/i.test(prn)) {
    form.pullRequest = prn
  }
  if (!form.note) {
    form.note = `准备提交 ${q.id} 的成果。请说明本次修改、测试结果，以及完成标准的逐项自检情况。`
  }
}

function hydrateFromDraft() {
  // Reset state so switching between quests doesn't leak fields.
  Object.assign(form, blankForm())
  for (const k of Object.keys(checks)) checks[k] = false
  evidence.value = []
  stage.value = 'draft'
  receipt.value = null
  errorMessage.value = ''

  try {
    const raw = localStorage.getItem(draftKey.value)
    if (raw) {
      const draft = JSON.parse(raw)
      Object.assign(form, draft.form ?? {})
      if (draft.checks) {
        for (const k of Object.keys(checks)) {
          if (k in draft.checks) checks[k] = Boolean(draft.checks[k])
        }
      }
      if (Array.isArray(draft.evidence)) evidence.value = draft.evidence
      if (draft.receipt) {
        // A previously submitted draft — restore lock + receipt so the user
        // re-opening the counter sees the same wax seal, not a blank form.
        receipt.value = {
          ...draft.receipt,
          submittedAt: new Date(draft.receipt.submittedAt),
        }
        stage.value = 'submitted'
      }
    }
  } catch {
    // Corrupt draft — log to console and start fresh. We don't surface this
    // to the user because the visible result (empty form) already tells the
    // story.
    console.warn('[SubmissionCounter] failed to parse draft, ignoring.')
  }

  quietlySeedFromQuest()
}

watch(
  () => props.quest?.id,
  () => hydrateFromDraft(),
  { immediate: true },
)

// ── Display helpers ────────────────────────────────────────────────────────
const displayLinkedSubmission = computed(() => {
  if (!props.quest) {
    return {
      quest: 'QST-???? · 未关联任务',
      meta: '先在任务详情接取委托，再回到柜台呈递成果',
    }
  }
  return {
    quest: `${props.quest.id} · ${props.quest.title}`,
    meta: `${props.quest.stack} · 难度 ${props.quest.difficulty} · ${props.quest.reward}`,
  }
})

const evidenceGlyph = (typeId) =>
  evidenceTypes.find((t) => t.id === typeId)?.glyph ?? '✚'
const evidenceLabelFor = (typeId) =>
  evidenceTypes.find((t) => t.id === typeId)?.label ?? '附件'

// ── Validation ─────────────────────────────────────────────────────────────
const allChecksDone = computed(() => Object.values(checks).every(Boolean))

const errors = computed(() => {
  const e = {}
  if (!form.repository.trim()) e.repository = '请填写仓库名。'
  if (!form.branch.trim()) e.branch = '请填写分支名。'
  if (!form.pullRequest.trim()) {
    e.pullRequest = '请粘贴 PR 链接。'
  } else if (!PR_URL_PATTERN.test(form.pullRequest.trim())) {
    e.pullRequest = '链接需指向 PR / Merge Request 详情页。'
  }
  if (form.note.trim().length < 8) e.note = '提交说明至少 8 个字。'
  if (!allChecksDone.value) e.checks = '请完成提交前核对清单。'
  return e
})

const canSubmit = computed(
  () => Object.keys(errors.value).length === 0 && stage.value === 'draft',
)

const isLocked = computed(() => stage.value !== 'draft')

// ── Stage-driven UI bits ───────────────────────────────────────────────────
const ribbon = computed(() => stageRibbon[stage.value] ?? stageRibbon.draft)

// Walk the static stepper forward as the live stage advances. The data file
// owns the labels, this just maps stage → which step is `active`.
const dynamicReviewSteps = computed(() => {
  const advanceTo = {
    draft: 0,
    submitting: 1,
    submitted: 2,
    error: 0,
  }[stage.value] ?? 0
  return submissionReviewSteps.map((step, i) => {
    if (i < advanceTo) return { ...step, state: 'done' }
    if (i === advanceTo) return { ...step, state: 'active' }
    return { ...step, state: 'next' }
  })
})

const submitterName = computed(
  () => sessionStore.user?.displayName || sessionStore.user?.username || '冒险家',
)

// ── Behaviour: open / close ────────────────────────────────────────────────
function openSheet() {
  isFormOpen.value = true
  errorMessage.value = ''
  nextTick(() => {
    if (stage.value === 'draft') firstInput.value?.focus()
  })
}

function closeSheet() {
  if (stage.value === 'submitting') return
  isFormOpen.value = false
  isAddingEvidence.value = false
}

// ── Behaviour: draft / submit ──────────────────────────────────────────────
function persistDraft(extra = {}) {
  const payload = {
    form: { ...form },
    checks: { ...checks },
    evidence: evidence.value,
    savedAt: new Date().toISOString(),
    ...extra,
  }
  localStorage.setItem(draftKey.value, JSON.stringify(payload))
}

function saveDraft() {
  if (isLocked.value) return
  persistDraft()
  showToast('已存为草稿，下次回到柜台自动恢复')
}

function openPullRequest() {
  const url = form.pullRequest.trim()
  if (!url) {
    showToast('还没填 PR 链接')
    return
  }
  if (!PR_URL_PATTERN.test(url)) {
    showToast('PR 链接格式不对，无法打开')
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

function generateReceiptId() {
  const stamp = Date.now().toString(36).toUpperCase().slice(-6)
  return `RCP-${stamp}`
}

async function submitForReview() {
  if (!canSubmit.value) {
    // Surface the first concrete error so the user knows what to fix without
    // having to scan the whole sheet.
    errorMessage.value =
      errors.value.repository ||
      errors.value.branch ||
      errors.value.pullRequest ||
      errors.value.note ||
      errors.value.checks ||
      '请完成必填项后再提交。'
    return
  }

  stage.value = 'submitting'
  errorMessage.value = ''
  ringingBell.value = true
  // Bell rings for ~900ms regardless of network — the animation completes even
  // if the request resolves instantly, so the moment doesn't feel rushed.
  setTimeout(() => (ringingBell.value = false), 900)

  try {
    const response = await submissionApi.create({
      questId: props.quest?.id ?? null,
      repository: form.repository.trim(),
      branch: form.branch.trim(),
      pullRequest: form.pullRequest.trim(),
      note: form.note.trim(),
      checks: { ...checks },
      evidence: evidence.value,
    })
    const submittedAt = new Date()
    const newReceipt = {
      id: response?.data?.submissionId ?? generateReceiptId(),
      submittedAt,
      submitter: submitterName.value,
      questId: props.quest?.id,
    }
    receipt.value = newReceipt
    stage.value = 'submitted'
    // Preserve the locked-in form + receipt so reopening the page shows the
    // same stamped state instead of an editable blank.
    persistDraft({
      receipt: {
        ...newReceipt,
        submittedAt: submittedAt.toISOString(),
      },
    })
    emit('submitted', newReceipt)
  } catch (err) {
    stage.value = 'error'
    errorMessage.value = err?.message || '提交失败，请稍后再试。'
  }
}

// "Reopen the draft" — used after a submit error to step back into edit mode
// without forcing the user to refresh.
function returnToDraft() {
  stage.value = 'draft'
  errorMessage.value = ''
}

// Receipt actions
function backToWorkbench() {
  // Component is route-agnostic; let the page handle navigation by emitting.
  emit('submitted', receipt.value)
  closeSheet()
}

// ── Evidence add / remove ─────────────────────────────────────────────────
const isAddingEvidence = ref(false)
const evidenceDraft = reactive({ type: 'screenshot', label: '', url: '' })

function startAddingEvidence(typeId) {
  if (isLocked.value) return
  Object.assign(evidenceDraft, { type: typeId, label: '', url: '' })
  isAddingEvidence.value = true
  nextTick(() => evidenceLabelInput.value?.focus())
}

function commitEvidence() {
  const label = evidenceDraft.label.trim()
  if (!label) return
  const url = evidenceDraft.url.trim()
  // URL is optional, but if provided it should look like a URL — otherwise
  // we'd accept "asdf" as a link and embarrass the reviewer.
  if (url && !/^https?:\/\/\S+$/i.test(url)) {
    showToast('链接需以 http:// 或 https:// 开头')
    return
  }
  evidence.value = [
    ...evidence.value,
    {
      id: `EV-${Date.now().toString(36).toUpperCase()}`,
      type: evidenceDraft.type,
      label,
      url,
    },
  ]
  isAddingEvidence.value = false
}

function removeEvidence(id) {
  if (isLocked.value) return
  evidence.value = evidence.value.filter((it) => it.id !== id)
}

// ── Transient toast ───────────────────────────────────────────────────────
const toast = ref('')
let toastTimer = 0
function showToast(text) {
  toast.value = text
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => (toast.value = ''), 2200)
}

// ── Keyboard ───────────────────────────────────────────────────────────────
function onKeydown(event) {
  if (!isFormOpen.value || stage.value === 'submitting') return
  if (event.key === 'Escape') {
    if (isAddingEvidence.value) {
      isAddingEvidence.value = false
      event.preventDefault()
      return
    }
    closeSheet()
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  window.clearTimeout(toastTimer)
})

function formatDateTime(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<template>
  <div class="submission-counter-workspace" :class="{ active: isFormOpen }" aria-label="委托提交处">
    <button
      class="submission-paper-hotspot"
      type="button"
      :aria-label="stage === 'submitted' ? '查看回执' : '打开委托提交单'"
      @click="openSheet"
    >
      <span class="paper-hotspot-glow" aria-hidden="true"></span>
      <span class="paper-hotspot-label">
        <strong>{{ stage === 'submitted' ? '查看回执' : '呈递提交单' }}</strong>
        <small>{{ stage === 'submitted' ? '已盖审核章 · 待维护者批阅' : '点击展开柜台上的羊皮纸' }}</small>
      </span>
    </button>

    <Transition name="submission-sheet">
      <div
        v-if="isFormOpen"
        class="submission-modal-layer"
        role="dialog"
        aria-modal="true"
        aria-label="委托提交单"
      >
        <button
          class="submission-modal-scrim"
          type="button"
          aria-label="关闭委托提交单"
          @click="closeSheet"
        ></button>

        <section
          class="submission-modal-sheet"
          :class="{ 'is-locked': isLocked, 'is-busy': stage === 'submitting' }"
          :style="{ '--sheet-image': `url(${parchmentFormImg})` }"
        >
          <button
            class="submission-sheet-close"
            type="button"
            aria-label="关闭委托提交单"
            @click="closeSheet"
          >×</button>

          <header class="submission-sheet-head">
            <div>
              <p class="kicker">提交柜台</p>
              <h1>委托提交单</h1>
            </div>
            <span class="submission-status-ribbon" :data-tone="ribbon.tone">{{ ribbon.label }}</span>
          </header>

          <div class="submission-sheet-body">
            <section class="submission-quest-summary" aria-label="关联委托">
              <p class="kicker">关联悬赏任务</p>
              <h2>{{ displayLinkedSubmission.quest }}</h2>
              <p>{{ displayLinkedSubmission.meta }}</p>
            </section>

            <form class="submission-form-panel" novalidate @submit.prevent="submitForReview">
              <!-- Repository + branch share row 1 (both narrow); PR + note are
                   wide rows so URLs and prose have horizontal room to breathe. -->
              <label :class="{ 'has-error': errors.repository }">
                <span>所属仓库</span>
                <input
                  ref="firstInput"
                  v-model.trim="form.repository"
                  placeholder="git-guild / frontend"
                  :disabled="isLocked"
                />
                <small v-if="errors.repository" class="submission-field-error">{{ errors.repository }}</small>
              </label>

              <label :class="{ 'has-error': errors.branch }">
                <span>提交分支</span>
                <input
                  v-model.trim="form.branch"
                  placeholder="feature/your-branch"
                  :disabled="isLocked"
                />
                <small v-if="errors.branch" class="submission-field-error">{{ errors.branch }}</small>
              </label>

              <label class="wide-field" :class="{ 'has-error': errors.pullRequest }">
                <span>PR / MR 链接</span>
                <input
                  v-model.trim="form.pullRequest"
                  placeholder="https://github.com/owner/repo/pull/123"
                  :disabled="isLocked"
                />
                <small v-if="errors.pullRequest" class="submission-field-error">{{ errors.pullRequest }}</small>
              </label>

              <label class="wide-field" :class="{ 'has-error': errors.note }">
                <span>提交说明</span>
                <textarea
                  v-model="form.note"
                  rows="4"
                  placeholder="本次修改、测试结果、完成标准自检情况"
                  :disabled="isLocked"
                ></textarea>
                <small v-if="errors.note" class="submission-field-error">{{ errors.note }}</small>
              </label>
            </form>

            <section class="submission-check-panel" aria-label="提交前核对">
              <div>
                <p class="kicker">Counter Check</p>
                <h2>提交前核对</h2>
              </div>
              <label
                v-for="item in submissionChecks"
                :key="item"
                :class="{ checked: checks[item] }"
              >
                <input
                  type="checkbox"
                  v-model="checks[item]"
                  :disabled="isLocked"
                />
                <span>{{ item }}</span>
              </label>
              <p v-if="errors.checks && stage === 'draft'" class="submission-field-error subtle">
                {{ errors.checks }}
              </p>
            </section>

            <section class="submission-evidence-panel" aria-label="附件与证据">
              <div>
                <p class="kicker">附件与证据</p>
                <h2>附上佐证</h2>
              </div>

              <ul v-if="evidence.length" class="evidence-list">
                <li v-for="item in evidence" :key="item.id" class="evidence-chip">
                  <span class="evidence-glyph" :data-type="item.type" aria-hidden="true">{{ evidenceGlyph(item.type) }}</span>
                  <div class="evidence-body">
                    <strong>{{ item.label }}</strong>
                    <span class="evidence-meta">{{ evidenceLabelFor(item.type) }}</span>
                    <a
                      v-if="item.url"
                      :href="item.url"
                      target="_blank"
                      rel="noopener noreferrer"
                    >{{ item.url }}</a>
                  </div>
                  <button
                    v-if="!isLocked"
                    type="button"
                    class="evidence-remove"
                    aria-label="移除"
                    @click="removeEvidence(item.id)"
                  >×</button>
                </li>
              </ul>

              <div v-if="isAddingEvidence" class="evidence-draft">
                <select v-model="evidenceDraft.type" aria-label="附件类型">
                  <option v-for="t in evidenceTypes" :key="t.id" :value="t.id">{{ t.label }}</option>
                </select>
                <input
                  ref="evidenceLabelInput"
                  v-model.trim="evidenceDraft.label"
                  placeholder="名称（必填）"
                  @keydown.enter.prevent="commitEvidence"
                />
                <input
                  v-model.trim="evidenceDraft.url"
                  placeholder="链接（可选）"
                  @keydown.enter.prevent="commitEvidence"
                />
                <div class="evidence-draft-actions">
                  <button type="button" class="quiet-action" @click="isAddingEvidence = false">取消</button>
                  <button
                    type="button"
                    class="primary-action"
                    :disabled="!evidenceDraft.label.trim()"
                    @click="commitEvidence"
                  >登记</button>
                </div>
              </div>
              <template v-else>
                <button
                  v-for="type in evidenceTypes"
                  :key="type.id"
                  type="button"
                  :disabled="isLocked"
                  :title="type.hint"
                  @click="startAddingEvidence(type.id)"
                >
                  <span>+</span>{{ type.label }}
                </button>
              </template>
            </section>

            <section class="submission-review-panel" aria-label="审核流转">
              <div class="review-route" aria-label="审核流转">
                <div
                  v-for="step in dynamicReviewSteps"
                  :key="step.label"
                  class="review-step"
                  :class="step.state"
                >
                  <span></span>
                  <div>
                    <strong>{{ step.label }}</strong>
                    <small>{{ step.note }}</small>
                  </div>
                </div>
              </div>

              <div class="review-note">
                <p class="kicker">{{ reviewerSlot.eyebrow }}</p>
                <h2>{{ reviewerSlot.title }}</h2>
                <p>{{ reviewerSlot.body }}</p>
              </div>
            </section>
          </div>

          <Transition name="submission-error-bar">
            <p
              v-if="errorMessage"
              class="submission-error-banner"
              role="alert"
            >
              <span>{{ errorMessage }}</span>
              <button
                v-if="stage === 'error'"
                type="button"
                class="quiet-action"
                @click="returnToDraft"
              >返回修改</button>
            </p>
          </Transition>

          <footer class="submission-actions">
            <button
              class="quiet-action"
              type="button"
              :disabled="isLocked"
              @click="saveDraft"
            >保存草稿</button>
            <button
              class="quiet-action"
              type="button"
              @click="openPullRequest"
            >查看 PR</button>
            <button
              class="primary-action submit-cta"
              :class="{ ringing: ringingBell, sealed: stage === 'submitted' }"
              type="button"
              :disabled="!canSubmit && stage === 'draft' || isLocked"
              @click="submitForReview"
            >
              <span class="bell-glyph" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                  <path d="M12 3a1.3 1.3 0 0 0-1.3 1.3v.46A6.5 6.5 0 0 0 5.5 11v3.2L4 16.6v.9h16v-.9l-1.5-2.4V11a6.5 6.5 0 0 0-5.2-6.24v-.46A1.3 1.3 0 0 0 12 3Zm-1.7 16.6a1.7 1.7 0 0 0 3.4 0Z" />
                </svg>
              </span>
              <span>{{ stage === 'submitting' ? '正在敲钟…' : stage === 'submitted' ? '已盖章' : '敲钟提交' }}</span>
            </button>
          </footer>

          <Transition name="submission-receipt">
            <div
              v-if="stage === 'submitted' && receipt"
              class="submission-receipt-overlay"
              aria-live="polite"
            >
              <div class="submission-receipt-card">
                <div class="wax-seal" aria-hidden="true">
                  <span class="wax-rune">審</span>
                  <span class="wax-glow"></span>
                </div>
                <p class="kicker">受理回执 · Counter Receipt</p>
                <h2>已交付柜台 · 维护者将依队列审阅</h2>
                <dl class="receipt-meta">
                  <div><dt>回执编号</dt><dd>{{ receipt.id }}</dd></div>
                  <div><dt>呈递时间</dt><dd>{{ formatDateTime(receipt.submittedAt) }}</dd></div>
                  <div><dt>提交者</dt><dd>{{ receipt.submitter || submitterName }}</dd></div>
                  <div><dt>当前状态</dt><dd>审核中</dd></div>
                </dl>
                <p class="receipt-hint">
                  收据已留存。返回工作台等待维护者反馈，反馈会同步回到本柜台。
                </p>
                <div class="receipt-actions">
                  <button class="quiet-action" type="button" @click="closeSheet">关闭柜台</button>
                  <a
                    v-if="form.pullRequest"
                    class="primary-action"
                    :href="form.pullRequest"
                    target="_blank"
                    rel="noopener noreferrer"
                  >前往 PR</a>
                  <button
                    v-else
                    class="primary-action"
                    type="button"
                    @click="backToWorkbench"
                  >回到工作台</button>
                </div>
              </div>
            </div>
          </Transition>
        </section>
      </div>
    </Transition>

    <Transition name="submission-toast">
      <p v-if="toast" class="submission-toast" role="status">{{ toast }}</p>
    </Transition>
  </div>
</template>
