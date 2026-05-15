<script setup>
import { computed, ref } from 'vue'

import parchmentFormImg from '../assets/submission-form-parchment-v0-clean.png'
import {
  linkedSubmission,
  reviewerSlot,
  submissionChecks,
  submissionFields,
  submissionReviewSteps,
} from '../data/submissionCounter'

const isFormOpen = ref(false)

const props = defineProps({
  quest: {
    type: Object,
    default: null,
  },
})

const evidenceItems = ['运行截图', '测试日志', '设计稿链接']

const displayLinkedSubmission = computed(() => {
  if (!props.quest) return linkedSubmission

  return {
    quest: `${props.quest.id} · ${props.quest.title}`,
    meta: `${props.quest.stack} · 难度 ${props.quest.difficulty} · ${props.quest.reward}`,
  }
})

const displaySubmissionFields = computed(() => {
  if (!props.quest) return submissionFields

  const repositoryName = props.quest.detail?.repository?.name ?? 'git-guild / frontend'
  const branchName = props.quest.detail?.repository?.branch
    ? `feature/${props.quest.id.toLowerCase()}`
    : 'feature/refactor-submission-flow'
  const pullRequest = props.quest.detail?.pr?.number
  const hasPullRequest = pullRequest && pullRequest !== 'Not created'

  return submissionFields.map((field) => {
    if (field.id === 'repository') return { ...field, value: repositoryName }
    if (field.id === 'branch') return { ...field, value: branchName }
    if (field.id === 'pull-request') {
      return {
        ...field,
        value: hasPullRequest ? pullRequest : '',
        placeholder: hasPullRequest ? field.placeholder : '先在工作台发起 PR，再粘贴 Pull Request 链接',
      }
    }
    if (field.id === 'submission-note') {
      return {
        ...field,
        value: `准备提交 ${props.quest.id} 的成果审核。请说明本次修改、测试结果和完成标准自检情况。`,
      }
    }

    return field
  })
})
</script>

<template>
  <div class="submission-counter-workspace" :class="{ active: isFormOpen }" aria-label="委托提交处">
    <button class="submission-paper-hotspot" type="button" aria-label="打开委托提交单" @click="isFormOpen = true">
      <span class="paper-hotspot-glow" aria-hidden="true"></span>
      <span class="sr-only">打开委托提交单</span>
    </button>

    <Transition name="submission-sheet">
      <div v-if="isFormOpen" class="submission-modal-layer" role="dialog" aria-modal="true" aria-label="委托提交单">
        <button class="submission-modal-scrim" type="button" aria-label="关闭委托提交单" @click="isFormOpen = false"></button>

        <section class="submission-modal-sheet" :style="{ '--sheet-image': `url(${parchmentFormImg})` }">
          <button class="submission-sheet-close" type="button" aria-label="关闭委托提交单" @click="isFormOpen = false">
            ×
          </button>

          <header class="submission-sheet-head">
            <div>
              <p class="kicker">Submission Counter</p>
              <h1>委托提交单</h1>
            </div>
            <span>草稿</span>
          </header>

          <div class="submission-sheet-body">
            <section class="submission-quest-summary" aria-label="关联委托">
              <p class="kicker">Linked Quest</p>
              <h2>{{ displayLinkedSubmission.quest }}</h2>
              <p>{{ displayLinkedSubmission.meta }}</p>
            </section>

            <form class="submission-form-panel">
              <label v-for="field in displaySubmissionFields" :key="field.id" :class="{ 'wide-field': field.wide }">
                <span>{{ field.label }}</span>
                <textarea
                  v-if="field.type === 'textarea'"
                  :rows="field.rows"
                  :value="field.value"
                  :aria-label="field.ariaLabel"
                ></textarea>
                <input v-else :value="field.value" :placeholder="field.placeholder" :aria-label="field.ariaLabel" />
              </label>
            </form>

            <section class="submission-check-panel" aria-label="提交前核对">
              <div>
                <p class="kicker">Counter Check</p>
                <h2>提交前核对</h2>
              </div>
              <label v-for="item in submissionChecks" :key="item">
                <input type="checkbox" checked />
                <span>{{ item }}</span>
              </label>
            </section>

            <section class="submission-evidence-panel" aria-label="附件与证据">
              <div>
                <p class="kicker">Evidence</p>
                <h2>附件与证据</h2>
              </div>
              <button v-for="item in evidenceItems" :key="item" type="button">
                <span>+</span>
                {{ item }}
              </button>
            </section>

            <section class="submission-review-panel" aria-label="审核流转">
              <div class="review-route" aria-label="审核流转">
                <div v-for="step in submissionReviewSteps" :key="step.label" class="review-step" :class="step.state">
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

          <footer class="submission-actions">
            <button class="quiet-action" type="button">保存草稿</button>
            <button class="quiet-action" type="button">查看 PR</button>
            <button class="primary-action" type="button">提交审核</button>
          </footer>
        </section>
      </div>
    </Transition>
  </div>
</template>
