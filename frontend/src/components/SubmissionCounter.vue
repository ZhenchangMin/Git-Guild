<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

import parchmentFormImg from '../assets/submission-form-parchment-v0-clean.webp'
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

const emit = defineEmits(['submitted', 'back-to-workbench'])

// ── Stage machine ──────────────────────────────────────────────────────────
// draft       — user is editing; everything is interactive
// submitting  — request in-flight; UI locks, bell rings
// submitted   — clerk stamped a receipt; sheet locks, receipt overlay shows
// error       — last submit failed; back to draft-like edit, banner visible
const stage = ref('draft')
const isFormOpen = ref(false)
const errorMessage = ref('')
// 提交失败的结构化诊断：{ reason, hints[], detail }，null = 无后端错误
const submitError = ref(null)
const ringingBell = ref(false)
const receipt = ref(null)

const blankForm = () => ({
  repository: '',
  branch: '',
  note: '',
})
const form = reactive(blankForm())
const checks = reactive(Object.fromEntries(submissionChecks.map((label) => [label, false])))
const evidence = ref([])

const firstInput = ref(null)
const fileInput = ref(null)

// 佐证文件上限：数量与单文件大小，超出用文字提示用户
const MAX_EVIDENCE_FILES = 5
const MAX_EVIDENCE_SIZE = 5 * 1024 * 1024 // 单个文件 5MB

// ── Draft persistence ──────────────────────────────────────────────────────
// Drafts are keyed by (owner, questId) so multiple users on the same browser
// don't trample each other's drafts. `owner` falls back to "guest" when the
// session has no real user (e.g. the dev shortcut we use to bypass login).
const draftKey = computed(() => {
  const owner = sessionStore.user?.username || sessionStore.user?.displayName || 'guest'
  const questId = props.quest?.id || 'unknown'
  return `git-guild:submission-draft:${owner}:${questId}`
})

const numericQuestId = computed(() => {
  if (props.quest?.numericId) return Number(props.quest.numericId)
  const match = String(props.quest?.id ?? '').match(/\d+/)
  return match ? Number(match[0]) : null
})

function quietlySeedFromQuest() {
  const q = props.quest
  if (!q) return
  // Only fill empty fields; never overwrite something the user already typed.
  // 仓库/分支为服务端派生（分支即 task branch），此处只做兜底占位，真正取值见 fetchDraftFromBackend。
  if (!form.repository) form.repository = q.detail?.repository?.name ?? 'git-guild / frontend'
}

// 提交说明输入框的占位提示——只是引导性的底层灰字，不写入 form.note，
// 否则用户没动过这段文字也会被当成「已填写」的真实内容一起提交。
const notePlaceholder = computed(() =>
  props.quest?.id
    ? `准备提交 ${props.quest.id} 的成果。请说明本次修改、测试结果，以及完成标准的逐项自检情况。`
    : '本次修改、测试结果、完成标准自检情况',
)

function hydrateFromDraft() {
  // Reset state so switching between quests doesn't leak fields.
  Object.assign(form, blankForm())
  for (const k of Object.keys(checks)) checks[k] = false
  evidence.value = []
  stage.value = 'draft'
  receipt.value = null
  errorMessage.value = ''
  submitError.value = null

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
      // 佐证为本地文件（File / dataUrl），无法跨刷新可靠恢复，故草稿不持久化附件，需重新选择
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
  fetchDraftFromBackend()
}

// Load the draft from GET /quests/{questId}/submission-draft.
// 仓库与“任务分支”由后端权威给出（branch 即 task branch，提交时平台据此自动建 PR）。
async function fetchDraftFromBackend() {
  if (!numericQuestId.value) return

  try {
    const response = await submissionApi.getDraft(numericQuestId.value)
    const data = response?.data
    if (!data) return
    if (data.repository?.name) {
      form.repository = data.repository.name
    }
    if (data.branch) {
      form.branch = data.branch
    }
    // 维护者退回要求修改后：旧回执仍存在 localStorage 里会把柜台锁在「已交付」态，
    // 导致无法重新提交。后端回传最近一次提交为 CHANGES_REQUESTED 时，解锁回到草稿态，
    // 并覆盖掉带 receipt 的旧草稿，避免下次开页又被锁。
    if (data.latestSubmissionStatus === 'CHANGES_REQUESTED' && stage.value === 'submitted') {
      receipt.value = null
      stage.value = 'draft'
      errorMessage.value = ''
      submitError.value = null
      persistDraft()
    }
  } catch {
    // Silently ignore — local draft is sufficient
  }
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
  if (!numericQuestId.value) e.quest = '当前提交柜台没有关联后端任务。'
  // 仓库与任务分支由后端派生、提交即自动建 PR，这里不再做 PR/分支必填校验。
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
  submitError.value = null
  nextTick(() => {
    if (stage.value === 'draft') firstInput.value?.focus()
  })
}

function closeSheet() {
  if (stage.value === 'submitting') return
  isFormOpen.value = false
}

// ── Behaviour: draft / submit ──────────────────────────────────────────────
function persistDraft(extra = {}) {
  const payload = {
    form: { ...form },
    checks: { ...checks },
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

function generateReceiptId() {
  const stamp = Date.now().toString(36).toUpperCase().slice(-6)
  return `RCP-${stamp}`
}

async function submitForReview() {
  if (!canSubmit.value) {
    // Surface the first concrete error so the user knows what to fix without
    // having to scan the whole sheet.
    errorMessage.value =
      errors.value.note ||
      errors.value.checks ||
      errors.value.quest ||
      '请完成必填项后再提交。'
    return
  }

  stage.value = 'submitting'
  errorMessage.value = ''
  submitError.value = null
  ringingBell.value = true
  // Bell rings for ~900ms regardless of network — the animation completes even
  // if the request resolves instantly, so the moment doesn't feel rushed.
  setTimeout(() => (ringingBell.value = false), 900)

  try {
    const response = await submissionApi.create({
      questId: numericQuestId.value,
      description: form.note.trim(),
      checklist: Object.entries(checks)
        .filter(([, checked]) => checked)
        .map(([label]) => label),
      // 佐证文件随提交内联上传（文件名 + MIME + base64），后端持久化后供委托人在审核台查看。
      evidenceFiles: evidence.value
        .filter((item) => item.content)
        .map((item) => ({
          name: item.name,
          mimeType: item.mimeType || 'application/octet-stream',
          content: item.content,
        })),
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
    errorMessage.value = ''
    submitError.value = diagnoseSubmitError(err)
  }
}

/**
 * 把后端提交错误尽量翻译成「人话原因 + 可操作建议 + 技术细节」，避免用户只看到
 * 一句笼统的「请求参数不合法」。优先识别最常见的根因（任务分支没有改动）。
 */
function diagnoseSubmitError(err) {
  const code = err?.code ?? ''
  const status = err?.status
  const detail = err?.details || ''
  const rawDetail = detail.toLowerCase()

  // 最常见根因：任务分支没有任何改动，空分支无法建 PR
  if (code === 'TASK_BRANCH_EMPTY') {
    return {
      reason: '任务分支还没有任何改动，平台无法基于一个「空分支」创建 Pull Request，所以提交被拦下了。',
      hints: [
        '先把本次完成的代码 commit 并 push 到你接取任务时分配的 task 分支',
        '确认改动确实推送到了该分支（可在仓库里核对分支最新提交）',
        '推送成功后再回到提交柜台重新提交',
      ],
      detail: detail || err?.message || '',
    }
  }

  if (code === 'VALIDATION_FAILED' || status === 400) {
    if (/evidenceurl|512/.test(rawDetail)) {
      return {
        reason: '佐证内容过长，超出了单条佐证的长度上限，无法随这次提交一起上传。',
        hints: ['减少佐证文件数量', '或改用更短的文件名后重试'],
        detail,
      }
    }
    if (/description/.test(rawDetail)) {
      return {
        reason: '成果说明不能为空。',
        hints: ['在「提交说明」里写清本次修改、测试结果与完成标准的逐项自检情况'],
        detail,
      }
    }
    if (/questid/.test(rawDetail)) {
      return {
        reason: '没能识别当前委托编号，提交无法定位到具体任务。',
        hints: ['返回工作台，从该委托重新进入提交柜台后再提交'],
        detail,
      }
    }
    return {
      reason: `提交内容未通过校验：${detail || '请检查必填项是否完整。'}`,
      hints: ['按上面的字段提示补全后重试'],
      detail,
    }
  }

  if (code === 'FORBIDDEN' || status === 403) {
    return {
      reason: '你不是该委托当前的承接人，或暂时没有提交权限。',
      hints: ['确认你已接取该委托且接取仍然有效', '若已被退回或取消，请重新接取后再提交'],
      detail,
    }
  }

  if (code === 'QUEST_NOT_ACCEPTABLE') {
    return {
      reason: '该委托当前状态不接受提交（可能已完成、已关闭或尚未开始）。',
      hints: ['回到工作台查看该委托的最新状态'],
      detail,
    }
  }

  if (code === 'SUBMISSION_NOT_REVIEWABLE') {
    return {
      reason: '该委托已经有一份正在等待审核的提交，不能重复提交。',
      hints: ['等待维护者审核当前提交', '若需修改，等退回后再重新提交'],
      detail,
    }
  }

  if (code === 'CODE_HOST_UNAVAILABLE' || status === 502 || status === 503 || status === 504) {
    return {
      reason: '平台代码托管服务（Gitea）暂时不可用，未能创建 PR。',
      hints: ['稍后重试', '若持续失败请联系管理员检查 Gitea 服务状态'],
      detail,
    }
  }

  if (!status || err?.name === 'TypeError') {
    return {
      reason: '网络请求失败，无法连接到后端服务。',
      hints: ['检查网络连接', '确认后端服务正在运行后重试'],
      detail,
    }
  }

  // 兜底：原样展示后端 message + details，至少不丢失任何排查信息
  return {
    reason: err?.message || '提交失败，请稍后再试。',
    hints: [],
    detail,
  }
}

// "Reopen the draft" — used after a submit error to step back into edit mode
// without forcing the user to refresh.
function returnToDraft() {
  stage.value = 'draft'
  errorMessage.value = ''
  submitError.value = null
}

// Receipt actions
function backToWorkbench() {
  // Component is route-agnostic; let the page handle navigation by emitting
  // a dedicated event — reusing 'submitted' here would be indistinguishable
  // from the actual submit-success emit and the page would just store the
  // receipt again instead of navigating.
  emit('back-to-workbench', receipt.value)
  closeSheet()
}

// ── Evidence file upload ───────────────────────────────────────────────────
// 把字节数格式化为人类可读大小，用于上限提示与每个附件的大小展示。
function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

// 把文件读成 base64 data URL，以便随提交内联上传，委托人在审核台可直接预览/下载。
function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

// 点击「选择文件上传」→ 触发隐藏的原生文件浏览器
function pickEvidenceFiles() {
  if (isLocked.value) return
  if (evidence.value.length >= MAX_EVIDENCE_FILES) {
    showToast(`最多上传 ${MAX_EVIDENCE_FILES} 个佐证文件`)
    return
  }
  fileInput.value?.click()
}

async function onEvidenceFilesPicked(event) {
  const picked = Array.from(event.target.files ?? [])
  // 重置 input，确保再次选择同一文件也能触发 change
  event.target.value = ''
  if (!picked.length) return

  const remaining = MAX_EVIDENCE_FILES - evidence.value.length
  let tooLarge = 0
  let overflow = 0
  const accepted = []
  for (const file of picked) {
    if (accepted.length >= remaining) {
      overflow += 1
      continue
    }
    if (file.size > MAX_EVIDENCE_SIZE) {
      tooLarge += 1
      continue
    }
    accepted.push(file)
  }

  for (const file of accepted) {
    try {
      const content = await readFileAsDataUrl(file)
      evidence.value = [
        ...evidence.value,
        {
          id: `EV-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 6)}`.toUpperCase(),
          type: file.type.startsWith('image/') ? 'screenshot' : 'doc',
          name: file.name,
          size: file.size,
          mimeType: file.type || 'application/octet-stream',
          content,
        },
      ]
    } catch {
      showToast(`「${file.name}」读取失败，请重试`)
    }
  }

  if (tooLarge) {
    showToast(`${tooLarge} 个文件超过 ${formatSize(MAX_EVIDENCE_SIZE)}，已跳过`)
  } else if (overflow) {
    showToast(`最多 ${MAX_EVIDENCE_FILES} 个文件，多余的已跳过`)
  }
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
              <!-- 仓库与任务分支均为服务端派生、只读展示；提交时平台基于任务分支自动创建 PR。 -->
              <label>
                <span>所属仓库</span>
                <input
                  ref="firstInput"
                  v-model.trim="form.repository"
                  placeholder="git-guild / frontend"
                  readonly
                />
              </label>

              <label>
                <span>任务分支（自动）</span>
                <input
                  v-model.trim="form.branch"
                  placeholder="接取后自动创建"
                  readonly
                />
              </label>

              <p class="wide-field submission-auto-pr-hint">
                提交即由平台基于上方任务分支自动创建 / 复用 Pull Request，无需手动填写 PR 链接。
              </p>

              <label class="wide-field" :class="{ 'has-error': errors.note }">
                <span>提交说明</span>
                <textarea
                  v-model="form.note"
                  rows="4"
                  :placeholder="notePlaceholder"
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
                <h2>附上佐证（运行截图或文档说明）</h2>
                <p class="evidence-hint">
                  点击下方按钮从本地选择文件上传 · 最多 {{ MAX_EVIDENCE_FILES }} 个，单个不超过 {{ formatSize(MAX_EVIDENCE_SIZE) }}
                </p>
              </div>

              <ul v-if="evidence.length" class="evidence-list">
                <li v-for="item in evidence" :key="item.id" class="evidence-chip">
                  <span class="evidence-glyph" :data-type="item.type" aria-hidden="true">{{ evidenceGlyph(item.type) }}</span>
                  <div class="evidence-body">
                    <strong>{{ item.name }}</strong>
                    <span class="evidence-meta">{{ evidenceLabelFor(item.type) }} · {{ formatSize(item.size) }}</span>
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

              <input
                ref="fileInput"
                class="evidence-file-input"
                type="file"
                multiple
                accept="image/*,.pdf,.txt,.md,.log,.doc,.docx,.zip"
                @change="onEvidenceFilesPicked"
              />
              <button
                type="button"
                class="evidence-upload-btn"
                :disabled="isLocked || evidence.length >= MAX_EVIDENCE_FILES"
                @click="pickEvidenceFiles"
              >
                <span aria-hidden="true">⤓</span>
                {{ evidence.length >= MAX_EVIDENCE_FILES ? `已达上限（${MAX_EVIDENCE_FILES} 个）` : '选择文件上传' }}
              </button>
              <p class="evidence-count-hint">已添加 {{ evidence.length }} / {{ MAX_EVIDENCE_FILES }} 个文件</p>
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
            <div
              v-if="submitError"
              class="submission-error-banner detailed"
              role="alert"
            >
              <div class="error-head">
                <strong>提交未成功</strong>
                <button type="button" class="quiet-action" @click="returnToDraft">返回修改</button>
              </div>
              <p class="error-reason">{{ submitError.reason }}</p>
              <ul v-if="submitError.hints.length" class="error-hints">
                <li v-for="hint in submitError.hints" :key="hint">{{ hint }}</li>
              </ul>
              <details v-if="submitError.detail" class="error-detail">
                <summary>技术细节</summary>
                <code>{{ submitError.detail }}</code>
              </details>
            </div>
          </Transition>

          <Transition name="submission-error-bar">
            <p
              v-if="!submitError && errorMessage"
              class="submission-error-banner"
              role="alert"
            >
              <span>{{ errorMessage }}</span>
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
                  <button class="quiet-action" type="button" @click="closeSheet">返回</button>
                  <button
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
