<script setup>
import { computed, ref } from 'vue'

import {
  maintainerNotifications,
  maintainerPublishedQuests,
  maintainerWorkbenchStats,
  notifications,
  pullRequests,
  recentContributions,
  repositories,
  reviewQueue,
  reviewFeedbacks,
  taskGroups,
  workbenchEmails,
  workbenchStats,
  workbenchUser,
} from '../data/workbench'

const emit = defineEmits(['open-submission', 'open-id-card'])

const workbenchView = ref('adventurer')
const maintainerReviews = ref(reviewQueue.map((review) => ({ ...review, checklist: [...review.checklist] })))
const mailboxMessages = ref(workbenchEmails.map((email) => ({ ...email, body: [...email.body] })))
const isMailboxOpen = ref(false)
const selectedTaskId = ref(null)
const selectedEmailId = ref(null)
const selectedRepositoryName = ref(null)
const selectedNotificationText = ref(null)
const selectedFeedbackId = ref(null)
const selectedReviewId = ref(reviewQueue[0]?.id ?? null)
const isGrowthDetailOpen = ref(false)
const operationResult = ref({
  title: 'Git 操作',
  body: '选择创建分支、上传提交或发起 PR 后，这里会展示模拟结果。',
})

const allTasks = computed(() =>
  taskGroups.flatMap((group) =>
    group.tasks.map((task) => ({
      ...task,
      statusLabel: group.label,
    })),
  ),
)
const selectedTask = computed(() => allTasks.value.find((task) => task.id === selectedTaskId.value) ?? null)
const selectedEmail = computed(() => mailboxMessages.value.find((email) => email.id === selectedEmailId.value) ?? null)
const selectedNotification = computed(() =>
  notifications.find((notification) => notification.text === selectedNotificationText.value) ?? null,
)
const selectedFeedback = computed(() =>
  reviewFeedbacks.find((feedback) => feedback.id === selectedFeedbackId.value) ?? null,
)
const unreadMailCount = computed(() => mailboxMessages.value.filter((email) => email.unread).length)
const displayStats = computed(() =>
  workbenchView.value === 'guild-master'
    ? maintainerWorkbenchStats
    : workbenchStats.map((stat) => (stat.label === '未读邮件' ? { ...stat, value: unreadMailCount.value } : stat)),
)
const selectedTaskRepository = computed(() =>
  repositories.find((repository) => repository.name === selectedTask.value?.repository),
)
const selectedRepository = computed(() =>
  repositories.find((repository) => repository.name === selectedRepositoryName.value) ?? null,
)
const selectedTaskPullRequests = computed(() => {
  if (!selectedTask.value) return []

  const prId = selectedTask.value.prStatus.match(/PR #\d+/)?.[0]
  return pullRequests.filter((pr) => pr.id === prId || pr.title.includes(selectedTask.value.id))
})
const selectedFeedbackTask = computed(() =>
  allTasks.value.find((task) => task.id === selectedFeedback.value?.questId) ?? null,
)
const xpProgress = computed(() => `${Math.round((workbenchUser.xpCurrent / workbenchUser.xpTarget) * 100)}%`)
const selectedReview = computed(() =>
  maintainerReviews.value.find((review) => review.id === selectedReviewId.value) ?? maintainerReviews.value[0] ?? null,
)
const activeRoleLabel = computed(() => (workbenchView.value === 'guild-master' ? 'Guild Master' : workbenchUser.role))

function switchWorkbenchView(view) {
  workbenchView.value = view
  isMailboxOpen.value = false
  operationResult.value =
    view === 'guild-master'
      ? {
          title: '维护者工作台已打开',
          body: '审核队列、提交详情和反馈操作已准备好。点击队列中的提交可以查看逐项检查。',
        }
      : {
          title: '冒险家工作台已打开',
          body: '我的待办、仓库操作、邮件通知和成长记录已恢复。',
        }
}

function selectReview(review) {
  selectedReviewId.value = review.id
  operationResult.value = {
    title: `${review.id} 已选中`,
    body: `正在查看 ${review.questId} 的提交详情、完成标准和审核建议。`,
  }
}

function runReviewAction(action, review = selectedReview.value) {
  if (!review) return

  const resultMap = {
    approve: ['审核已通过', `${review.questId} 已通过审核，冒险家侧将收到成长记录和完成通知。`],
    changes: ['修改请求已发送', `${review.questId} 的逐项反馈已发送，冒险家工作台将出现待修改事项。`],
    reject: ['提交已驳回', `${review.questId} 已标记为驳回，任务仍需维护者后续处理。`],
    draft: ['草稿已保存', `${review.questId} 的审核意见已保存为本地模拟草稿。`],
    pr: ['PR 状态已打开', `${review.pullRequest} 的检查结果和 Review 状态已定位。`],
  }

  const [title, body] = resultMap[action] ?? ['审核操作已记录', `${review.questId} 的模拟操作已完成。`]
  operationResult.value = { title, body }
}

function selectTask(task) {
  selectedTaskId.value = task.id
  selectedEmailId.value = null
  selectedRepositoryName.value = null
  selectedNotificationText.value = null
  selectedFeedbackId.value = null
  isGrowthDetailOpen.value = false
  operationResult.value = {
    title: `${task.id} 已选中`,
    body: `中间区域正在显示 ${task.title} 的仓库、Issue、PR 状态和下一步操作。`,
  }
}

function selectEmail(email) {
  selectedEmailId.value = email.id
  selectedTaskId.value = null
  selectedRepositoryName.value = null
  selectedNotificationText.value = null
  selectedFeedbackId.value = null
  isGrowthDetailOpen.value = false
  isMailboxOpen.value = false
  email.unread = false
  operationResult.value = {
    title: '邮件已打开',
    body: `${email.subject} 已显示在中间详情区。`,
  }
}

function selectRepository(repository) {
  selectedRepositoryName.value = repository.name
  selectedTaskId.value = null
  selectedEmailId.value = null
  selectedNotificationText.value = null
  selectedFeedbackId.value = null
  isGrowthDetailOpen.value = false
  operationResult.value = {
    title: `${repository.name} 已打开`,
    body: '中间区域正在显示该仓库的同步状态、分支、Issue、PR 和常用 Git 操作。',
  }
}

function selectNotification(notification) {
  selectedNotificationText.value = notification.text
  selectedTaskId.value = null
  selectedEmailId.value = null
  selectedRepositoryName.value = null
  selectedFeedbackId.value = null
  isGrowthDetailOpen.value = false
  isMailboxOpen.value = false
  operationResult.value = {
    title: '通知已打开',
    body: `${notification.type}：${notification.text}`,
  }
}

function showGrowthDetails(source = '个人成长档案') {
  isGrowthDetailOpen.value = true
  selectedTaskId.value = null
  selectedEmailId.value = null
  selectedRepositoryName.value = null
  selectedNotificationText.value = null
  selectedFeedbackId.value = null
  operationResult.value = {
    title: '成长详情已打开',
    body: `${source} 已显示 Level、XP、完成任务数和最近贡献记录。`,
  }
}

function openIdCard() {
  emit('open-id-card')
}

function resolveNotificationAction(notification) {
  const actionMap = {
    查看反馈: 'feedback',
    '查看 PR': 'pr-view',
    查看任务: 'feedback',
    处理异常: 'exception',
  }
  return { type: actionMap[notification.action] ?? 'feedback', feedbackId: notification.feedbackId }
}

function runEmailAction(action, email) {
  if (!email) return

  if (action === 'read') {
    email.unread = false
    operationResult.value = { title: '邮件已标记为已读', body: email.subject }
    return
  }

  if (action === 'unread') {
    email.unread = true
    operationResult.value = { title: '邮件已标记为未读', body: email.subject }
    return
  }

  const resultMap = {
    reply: ['回复草稿已创建', `正在回复 ${email.from}：${email.subject}`],
    archive: ['邮件已归档', `${email.subject} 已移动到归档箱。`],
    delete: ['删除确认已触发', `${email.subject} 已进入删除确认流程。`],
  }
  const [title, body] = resultMap[action] ?? ['邮件操作已记录', email.subject]
  operationResult.value = { title, body }
}

function runAction(action, source = '当前事项') {
  if (action.type === 'submit') {
    emit('open-submission', action.questId ?? selectedFeedback.value?.questId ?? selectedTask.value?.id)
    return
  }

  if (action.type === 'feedback') {
    openFeedback(action.feedbackId ?? selectedTask.value?.feedbackId, source)
    return
  }

  if (action.type === 'growth') {
    showGrowthDetails(source)
    return
  }

  if (action.type === 'repository' && selectedTaskRepository.value && !selectedRepository.value) {
    selectRepository(selectedTaskRepository.value)
    return
  }

  const resultMap = {
    branch: ['已创建任务分支', `${source} 已生成 feature/qst-workbench-demo 分支。`],
    commit: ['上传提交已记录', `${source} 已模拟上传文件并生成 commit c0ffee1。`],
    'pull-request': ['Pull Request 已准备', `${source} 已模拟创建 PR 草稿，可继续到提交柜台关联成果。`],
    feedback: ['反馈已打开', `${source} 的退回意见已定位到审核反馈区。`],
    repository: ['仓库视图已打开', `${source} 的分支、提交、Issue 和 PR 摘要已定位。`],
    history: ['提交记录已打开', `${source} 的最近提交记录已定位。`],
    'pr-view': ['PR 状态已打开', `${source} 的 PR 检查状态已定位。`],
    contribution: ['贡献记录已打开', `${source} 已完成并写入个人贡献记录。`],
    growth: ['成长结果已打开', `${source} 对应 XP 和等级进度已更新。`],
    exception: ['异常处理入口已打开', `${source} 已进入同步异常处理流程。`],
  }
  const [title, body] = resultMap[action.type] ?? ['操作已记录', `${source} 的模拟操作已完成。`]
  operationResult.value = { title, body }
}

function openFeedback(feedbackId, source = '审核反馈') {
  const fallbackId = reviewFeedbacks.find((feedback) => feedback.questId === selectedTask.value?.id)?.id
  selectedFeedbackId.value = feedbackId ?? fallbackId ?? reviewFeedbacks[0]?.id ?? null
  isGrowthDetailOpen.value = false
  isMailboxOpen.value = false
  operationResult.value = {
    title: '反馈详情已打开',
    body: `${source} 的逐项审核意见已显示。请先在工作台更新 PR，再到提交柜台重新提交成果。`,
  }
}
</script>

<template>
  <div class="workbench-workspace" aria-label="工作台">
    <header class="workbench-statusbar">
      <div class="workbench-user">
        <p class="kicker">Workbench</p>
        <h1>工作台</h1>
        <div class="user-identity">
          <span class="user-avatar" aria-hidden="true">{{ workbenchUser.name.slice(0, 1) }}</span>
          <div>
            <strong>{{ workbenchUser.name }}</strong>
            <span>{{ activeRoleLabel }}</span>
          </div>
        </div>
        <div class="view-switch" aria-label="工作台视图切换">
          <button
            type="button"
            :class="{ active: workbenchView === 'adventurer' }"
            @click="switchWorkbenchView('adventurer')"
          >
            Adventurer View
          </button>
          <button
            type="button"
            :class="{ active: workbenchView === 'guild-master' }"
            @click="switchWorkbenchView('guild-master')"
          >
            Guild Master View
          </button>
        </div>
      </div>

      <div class="workbench-level">
        <div class="level-head">
          <div>
            <strong>Level {{ workbenchUser.level }}</strong>
            <span>{{ workbenchUser.xpCurrent }} / {{ workbenchUser.xpTarget }} XP</span>
          </div>
          <button class="quiet-action detail-link" type="button" @click="openIdCard">
            查看详细信息
          </button>
        </div>
        <div class="xp-track" aria-label="XP 进度">
          <span :style="{ width: xpProgress }"></span>
        </div>
      </div>

      <dl class="workbench-stat-grid">
        <div v-for="stat in displayStats" :key="stat.label">
          <dt>{{ stat.label }}</dt>
          <dd>{{ stat.value }}</dd>
        </div>
      </dl>

      <div class="repository-shortcuts" aria-label="仓库快捷入口">
        <span>仓库</span>
        <button
          v-for="repository in repositories"
          :key="repository.name"
          class="repository-chip"
          :class="{ active: selectedRepositoryName === repository.name }"
          type="button"
          @click="selectRepository(repository)"
        >
          <strong>{{ repository.name.replace('git-guild / ', '') }}</strong>
          <small :class="{ warning: repository.syncStatus.includes('warning') }">{{ repository.syncStatus }}</small>
        </button>
      </div>

      <div class="mailbox-area">
        <button
          class="mail-button"
          type="button"
          aria-label="打开邮箱"
          :aria-expanded="isMailboxOpen"
          @click="isMailboxOpen = !isMailboxOpen"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h16v11H4z" />
            <path d="m4 7 8 6 8-6" />
          </svg>
          <span v-if="unreadMailCount > 0" class="mail-alert" aria-label="有未读邮件">!</span>
        </button>

        <section v-if="isMailboxOpen" class="mailbox-popover" aria-label="收到的邮件">
          <div class="mailbox-head">
            <div>
              <p class="kicker">Mailbox</p>
              <h2>收到的邮件</h2>
            </div>
            <span>{{ unreadMailCount }} 未读</span>
          </div>

          <button
            v-for="email in mailboxMessages"
            :key="email.id"
            class="mail-preview"
            :class="{ unread: email.unread, active: selectedEmailId === email.id }"
            type="button"
            @click="selectEmail(email)"
          >
            <span>{{ email.from }} · {{ email.receivedAt }}</span>
            <strong>{{ email.subject }}</strong>
            <small>{{ email.preview }}</small>
          </button>

          <div class="mailbox-section-title">系统通知</div>
          <button
            v-for="notice in notifications"
            :key="notice.text"
            class="mail-preview notice-preview"
            :class="{ active: selectedNotificationText === notice.text }"
            type="button"
            @click="selectNotification(notice)"
          >
            <span>{{ notice.type }}</span>
            <strong>{{ notice.text }}</strong>
            <small>操作：{{ notice.action }}</small>
          </button>
        </section>
      </div>
    </header>

    <aside v-if="workbenchView === 'adventurer'" class="workbench-panel todo-panel">
      <div class="panel-head">
        <p class="kicker">My Todo</p>
        <h2>我的待办</h2>
      </div>

      <div class="todo-group-list">
        <section v-for="group in taskGroups" :key="group.id" class="todo-group">
          <header>
            <h3>{{ group.label }}</h3>
            <span>{{ group.tasks.length }}</span>
          </header>

          <button
            v-for="task in group.tasks"
            :key="task.id"
            class="todo-task"
            :class="{ active: selectedTaskId === task.id }"
            type="button"
            @click="selectTask(task)"
          >
            <span class="status-pill">{{ group.label }}</span>
            <strong>{{ task.id }} · {{ task.title }}</strong>
            <small>下一步：{{ task.nextStep }}</small>
          </button>
        </section>
      </div>
    </aside>

    <aside v-else class="workbench-panel todo-panel review-queue-panel">
      <div class="panel-head">
        <p class="kicker">Review Queue</p>
        <h2>审核队列</h2>
      </div>

      <div class="todo-group-list">
        <button
          v-for="review in maintainerReviews"
          :key="review.id"
          class="todo-task review-queue-item"
          :class="{ active: selectedReview?.id === review.id }"
          type="button"
          @click="selectReview(review)"
        >
          <span class="status-pill" :class="{ warning: review.status !== '待审核' }">{{ review.status }}</span>
          <strong>{{ review.id }} · {{ review.questId }}</strong>
          <small>{{ review.questTitle }}</small>
          <small>提交人：{{ review.submitter }} · {{ review.pullRequest }}</small>
          <small>提交时间：{{ review.submittedAt }}</small>
        </button>
      </div>
    </aside>

    <main class="workbench-main">
      <section v-if="workbenchView === 'guild-master'" class="workbench-panel task-detail-panel maintainer-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Guild Master Review</p>
            <h2>{{ selectedReview?.questId }} · {{ selectedReview?.questTitle }}</h2>
          </div>
          <span class="status-pill" :class="{ warning: selectedReview?.status !== '待审核' }">
            {{ selectedReview?.status }}
          </span>
        </div>

        <div v-if="selectedReview" class="maintainer-detail-grid">
          <article class="maintainer-brief-card">
            <div>
              <p class="kicker">Submission Detail</p>
              <h3>{{ selectedReview.summary }}</h3>
            </div>
            <dl>
              <div>
                <dt>提交编号</dt>
                <dd>{{ selectedReview.id }}</dd>
              </div>
              <div>
                <dt>任务</dt>
                <dd>{{ selectedReview.questId }} · {{ selectedReview.questTitle }}</dd>
              </div>
              <div>
                <dt>提交人</dt>
                <dd>{{ selectedReview.submitter }}</dd>
              </div>
              <div>
                <dt>关联仓库</dt>
                <dd>{{ selectedReview.repository }}</dd>
              </div>
              <div>
                <dt>关联 PR</dt>
                <dd>{{ selectedReview.pullRequest }}</dd>
              </div>
              <div>
                <dt>提交时间</dt>
                <dd>{{ selectedReview.submittedAt }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card maintainer-check-card">
            <h3>完成标准逐项检查</h3>
            <div class="feedback-check-list">
              <section
                v-for="item in selectedReview.checklist"
                :key="item.item"
                class="feedback-check-row"
                :class="{ passed: item.passed, failed: !item.passed }"
              >
                <div>
                  <strong>{{ item.item }}</strong>
                  <span>{{ item.passed ? '通过' : '需修改' }}</span>
                </div>
                <p>{{ item.comment }}</p>
              </section>
            </div>
          </article>

          <article class="detail-card maintainer-feedback-card">
            <h3>审核总结</h3>
            <p>{{ selectedReview.summary }}</p>
            <h3>必须修改</h3>
            <ul class="feedback-list urgent">
              <li v-for="item in selectedReview.requiredChanges" :key="item">{{ item }}</li>
            </ul>
            <h3>学习建议</h3>
            <ul class="feedback-list">
              <li v-for="item in selectedReview.learningSuggestions" :key="item">{{ item }}</li>
            </ul>
          </article>

          <article class="detail-card maintainer-action-card">
            <h3>审核操作</h3>
            <div class="card-actions detail-actions">
              <button class="primary-action" type="button" @click="runReviewAction('approve')">通过</button>
              <button class="quiet-action" type="button" @click="runReviewAction('changes')">请求修改</button>
              <button class="quiet-action danger" type="button" @click="runReviewAction('reject')">驳回</button>
              <button class="quiet-action" type="button" @click="runReviewAction('pr')">查看 PR</button>
              <button class="quiet-action" type="button" @click="runReviewAction('draft')">保存草稿</button>
            </div>
            <p>这里仅做静态模拟，不保存真实审核结果；通过或请求修改后，冒险家侧会在工作台看到反馈或成长状态。</p>
          </article>

          <article class="detail-card maintainer-quest-card">
            <h3>我发布的任务</h3>
            <div class="maintainer-list">
              <section v-for="quest in maintainerPublishedQuests" :key="quest.id">
                <strong>{{ quest.id }} · {{ quest.title }}</strong>
                <span>{{ quest.status }} · {{ quest.assignee }}</span>
              </section>
            </div>
          </article>

          <article class="detail-card maintainer-repository-card">
            <h3>仓库状态</h3>
            <div class="maintainer-list">
              <section v-for="repository in repositories" :key="repository.name">
                <strong>{{ repository.name }}</strong>
                <span :class="{ warning: repository.syncStatus.includes('warning') }">
                  {{ repository.syncStatus }} · Issue {{ repository.issues }} · PR {{ repository.pullRequests }}
                </span>
              </section>
            </div>
          </article>

          <article class="detail-card maintainer-notice-card">
            <h3>维护者通知</h3>
            <div class="maintainer-list">
              <section v-for="notice in maintainerNotifications" :key="notice.text">
                <strong>{{ notice.type }}</strong>
                <span>{{ notice.text }}</span>
              </section>
            </div>
          </article>

          <article class="detail-card maintainer-operation-card">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <template v-else>
      <section v-if="selectedFeedback" class="workbench-panel task-detail-panel feedback-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Review Feedback</p>
            <h2>{{ selectedFeedback.questId }} · {{ selectedFeedback.questTitle }}</h2>
          </div>
          <span class="status-pill warning">{{ selectedFeedback.conclusion }}</span>
        </div>

        <div class="feedback-detail-grid">
          <article class="feedback-brief-card">
            <div>
              <p class="kicker">Feedback Archive</p>
              <h3>{{ selectedFeedback.summary }}</h3>
            </div>
            <dl>
              <div>
                <dt>任务编号与标题</dt>
                <dd>{{ selectedFeedback.questId }} · {{ selectedFeedback.questTitle }}</dd>
              </div>
              <div>
                <dt>关联仓库</dt>
                <dd>{{ selectedFeedback.repository }}</dd>
              </div>
              <div>
                <dt>关联 PR</dt>
                <dd>{{ selectedFeedback.pullRequest }} · {{ selectedFeedback.pullRequestTitle }}</dd>
              </div>
              <div>
                <dt>审核人</dt>
                <dd>{{ selectedFeedback.reviewer }}</dd>
              </div>
              <div>
                <dt>审核结论</dt>
                <dd>{{ selectedFeedback.conclusion }}</dd>
              </div>
              <div>
                <dt>审核时间</dt>
                <dd>{{ selectedFeedback.reviewedAt }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card feedback-check-card">
            <h3>逐项检查结果</h3>
            <div class="feedback-check-list">
              <section
                v-for="item in selectedFeedback.checks"
                :key="item.checkpoint"
                class="feedback-check-row"
                :class="{ passed: item.passed, failed: !item.passed }"
              >
                <div>
                  <strong>{{ item.checkpoint }}</strong>
                  <span>{{ item.passed ? '通过' : '需修改' }}</span>
                </div>
                <p>{{ item.comment }}</p>
              </section>
            </div>
          </article>

          <article class="detail-card action-note-card">
            <h3>工作台与提交柜台分工</h3>
            <p>代码文件上传、commit 和 PR 更新仍在工作台完成；完成修改后，成果记录需要到 Submission Counter / 提交柜台重新提交。</p>
          </article>

          <article class="detail-card required-change-card">
            <h3>必须修改</h3>
            <ul class="feedback-list urgent">
              <li v-for="item in selectedFeedback.requiredChanges" :key="item">{{ item }}</li>
            </ul>
          </article>

          <article class="detail-card learning-tip-card">
            <h3>学习建议</h3>
            <ul class="feedback-list">
              <li v-for="item in selectedFeedback.learningTips" :key="item">{{ item }}</li>
            </ul>
          </article>

          <article class="detail-card feedback-action-card">
            <h3>下一步操作</h3>
            <div class="card-actions detail-actions">
              <button
                class="quiet-action"
                type="button"
                @click="runAction({ type: 'pull-request' }, `${selectedFeedback.questId} ${selectedFeedback.questTitle}`)"
              >
                进入工作台更新 PR
              </button>
              <button
                class="primary-action"
                type="button"
                @click="runAction({ type: 'submit', questId: selectedFeedback.questId }, `${selectedFeedback.questId} ${selectedFeedback.questTitle}`)"
              >
                重新提交成果
              </button>
              <button
                class="quiet-action"
                type="button"
                @click="runAction({ type: 'pr-view' }, `${selectedFeedback.pullRequest} ${selectedFeedback.pullRequestTitle}`)"
              >
                查看 PR
              </button>
            </div>
            <p v-if="selectedFeedbackTask">当前待办状态：{{ selectedFeedbackTask.statusLabel }} · {{ selectedFeedbackTask.nextStep }}</p>
          </article>

          <article class="detail-card feedback-operation-card">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="selectedEmail" class="workbench-panel task-detail-panel email-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Mail Detail</p>
            <h2>{{ selectedEmail.subject }}</h2>
          </div>
          <span class="status-pill">{{ selectedEmail.unread ? '未读' : '已读' }}</span>
        </div>

        <div class="email-detail-body">
          <article class="detail-card wide">
            <h3>邮件信息</h3>
            <dl>
              <div>
                <dt>发件人</dt>
                <dd>{{ selectedEmail.from }}</dd>
              </div>
              <div>
                <dt>收件人</dt>
                <dd>{{ selectedEmail.to }}</dd>
              </div>
              <div>
                <dt>时间</dt>
                <dd>{{ selectedEmail.receivedAt }}</dd>
              </div>
              <div>
                <dt>关联对象</dt>
                <dd>{{ selectedEmail.related }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card wide mail-content-card">
            <h3>正文</h3>
            <p v-for="paragraph in selectedEmail.body" :key="paragraph">{{ paragraph }}</p>
          </article>

          <article class="detail-card wide">
            <h3>邮件操作</h3>
            <div class="card-actions detail-actions">
              <button
                v-if="selectedEmail.feedbackId"
                class="primary-action"
                type="button"
                @click="runAction({ type: 'feedback', feedbackId: selectedEmail.feedbackId }, selectedEmail.related)"
              >
                查看审核反馈
              </button>
              <button class="quiet-action" type="button" @click="runEmailAction('read', selectedEmail)">标记已读</button>
              <button class="quiet-action" type="button" @click="runEmailAction('unread', selectedEmail)">标记未读</button>
              <button class="quiet-action" type="button" @click="runEmailAction('reply', selectedEmail)">回复</button>
              <button class="quiet-action" type="button" @click="runEmailAction('archive', selectedEmail)">归档</button>
              <button class="quiet-action" type="button" @click="runEmailAction('delete', selectedEmail)">删除</button>
            </div>
          </article>

          <article class="detail-card wide">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="selectedNotification" class="workbench-panel task-detail-panel notice-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Notice Detail</p>
            <h2>{{ selectedNotification.type }}</h2>
          </div>
          <span class="status-pill">{{ selectedNotification.action }}</span>
        </div>

        <div class="detail-grid">
          <article class="detail-card wide">
            <h3>通知内容</h3>
            <p>{{ selectedNotification.text }}</p>
          </article>

          <article class="detail-card">
            <h3>建议处理</h3>
            <p>该通知来自工作台消息中心，点击下方按钮会进入对应的模拟处理流程。</p>
            <div class="card-actions detail-actions">
              <button
                class="primary-action"
                type="button"
                @click="runAction(resolveNotificationAction(selectedNotification), selectedNotification.text)"
              >
                {{ selectedNotification.action }}
              </button>
            </div>
          </article>

          <article class="detail-card">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="selectedTask" class="workbench-panel task-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Quest Detail</p>
            <h2>{{ selectedTask.id }} · {{ selectedTask.title }}</h2>
          </div>
          <span class="status-pill">{{ selectedTask.statusLabel }}</span>
        </div>

        <div class="detail-grid">
          <article class="detail-card wide">
            <h3>任务信息</h3>
            <dl>
              <div>
                <dt>关联仓库</dt>
                <dd>{{ selectedTask.repository }}</dd>
              </div>
              <div>
                <dt>关联 Issue</dt>
                <dd>{{ selectedTask.issue }}</dd>
              </div>
              <div>
                <dt>PR 状态</dt>
                <dd>{{ selectedTask.prStatus }}</dd>
              </div>
              <div>
                <dt>下一步</dt>
                <dd>{{ selectedTask.nextStep }}</dd>
              </div>
            </dl>
          </article>

          <article v-if="selectedTaskRepository" class="detail-card">
            <h3>仓库摘要</h3>
            <dl>
              <div>
                <dt>同步状态</dt>
                <dd>{{ selectedTaskRepository.syncStatus }}</dd>
              </div>
              <div>
                <dt>默认分支</dt>
                <dd>{{ selectedTaskRepository.defaultBranch }}</dd>
              </div>
              <div>
                <dt>分支 / Issue / PR</dt>
                <dd>{{ selectedTaskRepository.branches }} / {{ selectedTaskRepository.issues }} / {{ selectedTaskRepository.pullRequests }}</dd>
              </div>
              <div>
                <dt>最近 commit</dt>
                <dd>{{ selectedTaskRepository.lastCommit }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <h3>PR 状态</h3>
            <div v-if="selectedTaskPullRequests.length > 0" class="linked-pr-list">
              <button
                v-for="pr in selectedTaskPullRequests"
                :key="pr.id"
                class="linked-pr-row"
                type="button"
                @click="runAction({ type: 'pr-view' }, `${pr.id} ${pr.title}`)"
              >
                <strong>{{ pr.id }}</strong>
                <span>{{ pr.status }}</span>
              </button>
            </div>
            <p v-else>{{ selectedTask.prStatus }}</p>
          </article>

          <article class="detail-card">
            <h3>可执行操作</h3>
            <div class="card-actions detail-actions">
              <button
                v-for="action in selectedTask.actions"
                :key="action.label"
                :class="action.primary ? 'primary-action' : 'quiet-action'"
                type="button"
                @click="runAction(action, `${selectedTask.id} ${selectedTask.title}`)"
              >
                {{ action.label }}
              </button>
            </div>
          </article>

          <article class="detail-card wide">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="selectedRepository" class="workbench-panel task-detail-panel repository-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Repository Detail</p>
            <h2>{{ selectedRepository.name }}</h2>
          </div>
          <span class="status-pill" :class="{ warning: selectedRepository.syncStatus.includes('warning') }">
            {{ selectedRepository.syncStatus }}
          </span>
        </div>

        <div class="repository-detail-grid">
          <article class="repository-overview-card">
            <div>
              <p class="kicker">Repository Workspace</p>
              <h3>{{ selectedRepository.name }}</h3>
              <p>用于查看仓库状态、创建分支、上传文件生成 commit，并从这里发起 PR。这里不提供在线代码编辑器。</p>
            </div>
            <div class="repository-sync-card" :class="{ warning: selectedRepository.syncStatus.includes('warning') }">
              <span>同步状态</span>
              <strong>{{ selectedRepository.syncStatus }}</strong>
            </div>
          </article>

          <div class="repo-metric-grid" aria-label="仓库关键指标">
            <article class="repo-metric">
              <span>默认分支</span>
              <strong>{{ selectedRepository.defaultBranch }}</strong>
            </article>
            <article class="repo-metric">
              <span>分支</span>
              <strong>{{ selectedRepository.branches }}</strong>
            </article>
            <article class="repo-metric">
              <span>Issue</span>
              <strong>{{ selectedRepository.issues }}</strong>
            </article>
            <article class="repo-metric">
              <span>PR</span>
              <strong>{{ selectedRepository.pullRequests }}</strong>
            </article>
            <article class="repo-metric">
              <span>最近 commit</span>
              <strong>{{ selectedRepository.lastCommit }}</strong>
            </article>
          </div>

          <article class="detail-card repository-actions-card">
            <h3>常用 Git 操作</h3>
            <div class="card-actions detail-actions">
              <button class="quiet-action" type="button" @click="runAction({ type: 'repository' }, selectedRepository.name)">
                查看仓库
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'branch' }, selectedRepository.name)">
                创建分支
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'commit' }, selectedRepository.name)">
                上传提交
              </button>
              <button class="primary-action" type="button" @click="runAction({ type: 'pull-request' }, selectedRepository.name)">
                发起 PR
              </button>
            </div>
          </article>

          <article class="detail-card repository-flow-card">
            <h3>推荐操作顺序</h3>
            <ol>
              <li>查看仓库、Issue 和最近提交，确认任务上下文。</li>
              <li>创建任务分支，上传文件生成 commit。</li>
              <li>发起 PR，再到提交柜台关联任务成果。</li>
            </ol>
          </article>

          <article class="detail-card repository-sync-detail">
            <h3>同步状态</h3>
            <p v-if="selectedRepository.syncStatus.includes('warning')">
              最近同步存在异常，Issue 或 PR 状态可能不是最新。可以先手动同步，再查看异常日志。
            </p>
            <p v-else>
              仓库数据已同步，分支、Issue、PR 和最近提交可用于任务协作。
            </p>
            <div class="card-actions detail-actions">
              <button class="quiet-action" type="button" @click="runAction({ type: 'repository' }, `${selectedRepository.name} 同步日志`)">
                查看同步日志
              </button>
              <button class="quiet-action" type="button" @click="runAction({ type: 'exception' }, `${selectedRepository.name} 手动同步`)">
                手动同步
              </button>
            </div>
          </article>

          <article class="detail-card repository-operation-card">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else-if="isGrowthDetailOpen" class="workbench-panel task-detail-panel growth-detail-panel">
        <div class="panel-head inline">
          <div>
            <p class="kicker">Growth Detail</p>
            <h2>成长详细信息</h2>
          </div>
          <span class="status-pill">Level {{ workbenchUser.level }}</span>
        </div>

        <div class="growth-detail-grid">
          <article class="growth-hero-card">
            <div>
              <span>当前等级</span>
              <strong>Level {{ workbenchUser.level }}</strong>
              <p>{{ workbenchUser.xpCurrent }} / {{ workbenchUser.xpTarget }} XP</p>
            </div>
            <div class="xp-track large">
              <span :style="{ width: xpProgress }"></span>
            </div>
          </article>

          <article class="detail-card">
            <h3>完成任务</h3>
            <p>已完成 {{ workbenchUser.completedQuests }} 个任务，最近贡献会写入个人成长记录。</p>
          </article>

          <article class="detail-card">
            <h3>最近贡献</h3>
            <ul class="contribution-list">
              <li v-for="record in recentContributions" :key="record">{{ record }}</li>
            </ul>
          </article>

          <article class="detail-card wide">
            <h3>{{ operationResult.title }}</h3>
            <p>{{ operationResult.body }}</p>
          </article>
        </div>
      </section>

      <section v-else class="workbench-panel task-detail-panel blank-detail" aria-hidden="true"></section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.workbench-workspace {
  position: absolute;
  inset: 74px 28px 28px;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(250px, 310px) minmax(560px, 1fr);
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  color: #ffe7b5;
  text-wrap: pretty;
}

.workbench-statusbar,
.workbench-panel {
  border: 1px solid rgba(238, 184, 91, 0.58);
  border-radius: var(--radius);
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.8), rgba(15, 8, 4, 0.76)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.16), transparent 58%);
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.36), inset 0 1px 0 rgba(255, 229, 163, 0.14);
  backdrop-filter: blur(6px);
}

.workbench-statusbar {
  position: relative;
  z-index: 60;
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns:
    minmax(210px, 0.95fr) minmax(230px, 0.85fr) minmax(340px, 1.15fr) minmax(230px, 0.85fr)
    auto;
  align-items: center;
  gap: 18px;
  min-height: 96px;
  padding: 16px 18px;
}

.workbench-user h1 {
  font-size: 2rem;
}

.workbench-user span,
.workbench-level span,
.panel-head span,
.operation-result {
  color: rgba(255, 231, 183, 0.74);
}

.user-identity {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border: 1px solid rgba(238, 184, 91, 0.6);
  border-radius: 50%;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.3rem;
  background:
    radial-gradient(circle at 38% 28%, rgba(255, 232, 176, 0.3), transparent 0 26%, transparent 50%),
    linear-gradient(145deg, #173154, #0d1a2e 48%, #44220e);
  box-shadow: inset 0 0 0 2px rgba(255, 225, 160, 0.18), 0 10px 24px rgba(0, 0, 0, 0.28);
}

.user-identity strong {
  display: block;
  color: #ffe8b9;
  font-size: 1rem;
  line-height: 1.2;
}

.user-identity div span {
  display: block;
  margin-top: 3px;
  font-size: 0.84rem;
}

.view-switch {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.view-switch button {
  min-height: 32px;
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 999px;
  padding: 0 11px;
  color: rgba(255, 231, 183, 0.78);
  font-size: 0.76rem;
  background: rgba(11, 6, 3, 0.36);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.view-switch button:hover,
.view-switch button:focus-visible,
.view-switch button.active {
  border-color: var(--gold-bright);
  color: #ffe8b9;
  background: rgba(82, 45, 16, 0.62);
  transform: translateY(-1px);
}

.workbench-level {
  display: grid;
  gap: 6px;
}

.level-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.workbench-level strong {
  display: block;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.2rem;
}

.detail-link {
  min-height: 34px;
  padding: 0 11px;
  white-space: nowrap;
  font-size: 0.82rem;
}

.mailbox-area {
  position: relative;
  z-index: 70;
  justify-self: end;
}

.mail-button {
  position: relative;
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 1px solid rgba(238, 184, 91, 0.62);
  border-radius: 50%;
  color: #ffe4ad;
  background: rgba(17, 9, 5, 0.44);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.32);
  transition: background 160ms ease, border-color 160ms ease, transform 160ms ease;
}

.mail-button:hover,
.mail-button:focus-visible {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.72);
  transform: translateY(-1px);
}

.mail-button svg {
  width: 24px;
  height: 24px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.mail-alert {
  position: absolute;
  right: -3px;
  top: -3px;
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border: 1px solid rgba(255, 226, 153, 0.9);
  border-radius: 50%;
  color: #fff6e5;
  font-size: 0.76rem;
  font-weight: 800;
  line-height: 1;
  background: #b5251d;
  box-shadow: 0 0 0 3px rgba(181, 37, 29, 0.22);
}

.mailbox-popover {
  position: absolute;
  right: 0;
  top: calc(100% + 12px);
  z-index: 1000;
  width: min(420px, calc(100vw - 56px));
  max-height: min(520px, 72svh);
  overflow: auto;
  border: 1px solid rgba(238, 184, 91, 0.68);
  border-radius: var(--radius);
  padding: 12px;
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.96), rgba(15, 8, 4, 0.94)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.18), transparent 58%);
  box-shadow: 0 24px 58px rgba(0, 0, 0, 0.52), inset 0 1px 0 rgba(255, 229, 163, 0.14);
}

.mailbox-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 10px;
}

.mailbox-head h2 {
  font-size: 1.1rem;
}

.mailbox-head > span {
  color: rgba(255, 231, 183, 0.72);
  font-size: 0.82rem;
  white-space: nowrap;
}

.mail-preview {
  display: grid;
  gap: 5px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  margin-top: 8px;
  padding: 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(11, 6, 3, 0.38);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.mail-preview:hover,
.mail-preview:focus-visible,
.mail-preview.active {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.58);
  transform: translateY(-1px);
}

.mail-preview.unread {
  border-color: rgba(245, 118, 82, 0.54);
}

.mail-preview span,
.mail-preview small {
  color: rgba(255, 231, 183, 0.7);
  font-size: 0.78rem;
}

.mail-preview strong {
  overflow-wrap: anywhere;
  line-height: 1.24;
}

.mailbox-section-title {
  border-top: 1px solid rgba(240, 198, 118, 0.18);
  margin: 12px 0 2px;
  padding-top: 10px;
  color: rgba(255, 231, 183, 0.68);
  font-size: 0.78rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.notice-preview {
  border-color: rgba(240, 198, 118, 0.24);
}

.xp-track {
  height: 8px;
  overflow: hidden;
  border: 1px solid rgba(238, 184, 91, 0.34);
  border-radius: 999px;
  background: rgba(7, 4, 2, 0.52);
}

.xp-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #b56c22, #ffd98a);
}

.xp-track.large {
  height: 11px;
  margin: 10px 0 0;
}

.workbench-stat-grid,
.growth-panel dl {
  display: grid;
  margin: 0;
}

.workbench-stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.repository-shortcuts {
  display: grid;
  grid-template-columns: auto repeat(2, minmax(0, 1fr));
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.repository-shortcuts > span {
  color: rgba(255, 231, 183, 0.66);
  font-size: 0.78rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  white-space: nowrap;
}

.repository-chip {
  display: grid;
  gap: 3px;
  min-width: 0;
  min-height: 48px;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 6px;
  padding: 8px 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(11, 6, 3, 0.34);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.repository-chip:hover,
.repository-chip:focus-visible,
.repository-chip.active {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.56);
  transform: translateY(-1px);
}

.repository-chip strong {
  overflow: hidden;
  font-size: 0.86rem;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.repository-chip small {
  color: rgba(255, 231, 183, 0.66);
  font-size: 0.72rem;
  line-height: 1;
}

.repository-chip small.warning {
  color: #ffd7c9;
}

.workbench-stat-grid div,
.notice-row,
.pr-row {
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  background: rgba(11, 6, 3, 0.34);
}

.workbench-stat-grid div {
  padding: 10px 12px;
}

dt {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.76rem;
}

dd {
  margin: 3px 0 0;
  color: #ffe2a0;
  font-weight: 700;
}

.workbench-panel {
  min-height: 0;
  padding: 14px;
  overflow: hidden;
}

.todo-panel,
.workbench-main {
  min-height: 0;
}

.todo-panel,
.workbench-main {
  display: grid;
  gap: 12px;
}

.workbench-main {
  grid-template-rows: minmax(0, 1fr);
}

.panel-head {
  display: grid;
  gap: 2px;
  margin-bottom: 12px;
}

.panel-head.inline {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
}

.panel-head h2 {
  font-size: 1.15rem;
}

.panel-caption {
  margin: -5px 0 10px;
  color: rgba(255, 231, 183, 0.68);
  font-size: 0.8rem;
  line-height: 1.35;
}

.todo-panel {
  overflow: auto;
}

.todo-group-list {
  display: grid;
  gap: 14px;
}

.todo-group {
  display: grid;
  gap: 8px;
}

.todo-group header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid rgba(240, 198, 118, 0.18);
  padding-bottom: 6px;
}

.todo-group h3 {
  margin: 0;
  color: rgba(255, 226, 160, 0.9);
  font-family: var(--font-display);
  font-size: 0.96rem;
}

.todo-group header span {
  display: grid;
  place-items: center;
  min-width: 24px;
  height: 24px;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 50%;
  color: #ffe4ad;
  font-size: 0.78rem;
  background: rgba(80, 43, 18, 0.44);
}

.todo-task {
  display: grid;
  gap: 7px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 10px;
  color: inherit;
  text-align: left;
  background: rgba(11, 6, 3, 0.34);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.todo-task:hover,
.todo-task:focus-visible,
.todo-task.active {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.56);
  transform: translateY(-1px);
}

.todo-task strong,
.detail-card h3 {
  display: block;
  color: #ffe8b9;
  line-height: 1.22;
}

.todo-task small,
.operation-panel p,
.notice-row p,
.detail-card p {
  margin: 0;
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.36;
}

.status-pill,
.notice-row span {
  display: inline-flex;
  width: fit-content;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 2px 8px;
  color: #ffe4ad;
  font-size: 0.72rem;
  background: rgba(80, 43, 18, 0.44);
}

.status-pill.warning {
  border-color: rgba(204, 95, 65, 0.72);
  color: #ffd7c9;
  background: rgba(110, 42, 36, 0.44);
}

.status-pill {
  margin-bottom: 0;
}

.task-detail-panel {
  display: grid;
  align-content: start;
  min-height: 100%;
}

.blank-detail {
  background:
    linear-gradient(180deg, rgba(32, 17, 8, 0.34), rgba(15, 8, 4, 0.28)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.06), transparent 58%);
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(260px, 0.9fr);
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.detail-card {
  display: grid;
  align-content: start;
  gap: 12px;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 13px;
  background: rgba(11, 6, 3, 0.34);
}

.detail-card.wide {
  grid-column: 1 / -1;
}

.detail-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px 12px;
  margin: 0;
}

.detail-card dl div {
  min-width: 0;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 6px;
}

.detail-card dd {
  overflow-wrap: anywhere;
}

.detail-actions {
  align-content: start;
}

.linked-pr-list {
  display: grid;
  gap: 8px;
}

.linked-pr-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 9px 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(7, 4, 2, 0.32);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.linked-pr-row:hover,
.linked-pr-row:focus-visible {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.5);
  transform: translateY(-1px);
}

.linked-pr-row span {
  flex: 0 0 auto;
  color: rgba(255, 231, 183, 0.68);
  font-size: 0.78rem;
}

.email-detail-body {
  display: grid;
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.mail-content-card {
  gap: 10px;
}

.mail-content-card p {
  line-height: 1.58;
}

.feedback-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.72fr);
  grid-template-areas:
    "brief brief"
    "checks note"
    "checks required"
    "tips actions"
    "operation operation";
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.feedback-brief-card {
  grid-area: brief;
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(320px, 1.05fr);
  gap: 16px;
  border: 1px solid rgba(240, 198, 118, 0.28);
  border-radius: 7px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.58), rgba(11, 6, 3, 0.46)),
    radial-gradient(circle at 92% 12%, rgba(255, 217, 138, 0.14), transparent 0 30%, transparent 56%);
}

.feedback-brief-card h3 {
  margin: 2px 0 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: 1.08rem;
  line-height: 1.38;
}

.feedback-brief-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin: 0;
}

.feedback-brief-card dl div {
  min-width: 0;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 6px;
}

.feedback-brief-card dd {
  overflow-wrap: anywhere;
}

.feedback-check-card {
  grid-area: checks;
}

.action-note-card {
  grid-area: note;
  border-color: rgba(238, 184, 91, 0.34);
  background:
    linear-gradient(180deg, rgba(39, 22, 10, 0.66), rgba(12, 7, 4, 0.46)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.12), transparent 58%);
}

.required-change-card {
  grid-area: required;
}

.learning-tip-card {
  grid-area: tips;
}

.feedback-action-card {
  grid-area: actions;
}

.feedback-operation-card {
  grid-area: operation;
}

.feedback-check-list {
  display: grid;
  gap: 9px;
}

.feedback-check-row {
  display: grid;
  gap: 7px;
  border: 1px solid rgba(240, 198, 118, 0.18);
  border-radius: 5px;
  padding: 10px;
  background: rgba(7, 4, 2, 0.3);
}

.feedback-check-row.passed {
  border-color: rgba(111, 158, 87, 0.58);
  background: rgba(44, 73, 36, 0.26);
}

.feedback-check-row.failed {
  border-color: rgba(204, 95, 65, 0.68);
  background: rgba(88, 31, 23, 0.32);
}

.feedback-check-row div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.feedback-check-row strong {
  color: #ffe8b9;
  line-height: 1.25;
}

.feedback-check-row span {
  flex: 0 0 auto;
  border: 1px solid rgba(238, 184, 91, 0.38);
  border-radius: 999px;
  padding: 2px 8px;
  color: #ffe4ad;
  font-size: 0.72rem;
  background: rgba(80, 43, 18, 0.44);
}

.feedback-check-row.passed span {
  border-color: rgba(129, 184, 98, 0.68);
  color: #dcf4c2;
  background: rgba(44, 91, 36, 0.44);
}

.feedback-check-row.failed span {
  border-color: rgba(238, 120, 82, 0.72);
  color: #ffd7c9;
  background: rgba(110, 42, 36, 0.5);
}

.feedback-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  color: rgba(255, 231, 183, 0.78);
  list-style: none;
}

.feedback-list li {
  position: relative;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding: 0 0 8px 17px;
  line-height: 1.42;
}

.feedback-list li::before {
  position: absolute;
  left: 0;
  top: 0.58em;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  content: '';
  background: rgba(238, 184, 91, 0.76);
}

.feedback-list.urgent li::before {
  background: #d66a48;
}

.feedback-action-card p {
  border-top: 1px solid rgba(240, 198, 118, 0.16);
  padding-top: 10px;
}

.review-queue-item {
  gap: 6px;
}

.maintainer-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.72fr);
  grid-template-areas:
    "brief brief"
    "checks feedback"
    "actions quests"
    "repos notices"
    "operation operation";
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.maintainer-brief-card {
  grid-area: brief;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(320px, 1.08fr);
  gap: 16px;
  border: 1px solid rgba(240, 198, 118, 0.28);
  border-radius: 7px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.58), rgba(11, 6, 3, 0.46)),
    radial-gradient(circle at 90% 14%, rgba(255, 217, 138, 0.14), transparent 0 30%, transparent 56%);
}

.maintainer-brief-card h3 {
  margin: 2px 0 0;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: 1.08rem;
  line-height: 1.38;
}

.maintainer-brief-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin: 0;
}

.maintainer-brief-card dl div {
  min-width: 0;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 6px;
}

.maintainer-brief-card dd {
  overflow-wrap: anywhere;
}

.maintainer-check-card {
  grid-area: checks;
}

.maintainer-feedback-card {
  grid-area: feedback;
}

.maintainer-action-card {
  grid-area: actions;
  border-color: rgba(238, 184, 91, 0.34);
  background:
    linear-gradient(180deg, rgba(39, 22, 10, 0.66), rgba(12, 7, 4, 0.46)),
    linear-gradient(135deg, rgba(216, 154, 50, 0.12), transparent 58%);
}

.maintainer-quest-card {
  grid-area: quests;
}

.maintainer-repository-card {
  grid-area: repos;
}

.maintainer-notice-card {
  grid-area: notices;
}

.maintainer-operation-card {
  grid-area: operation;
}

.maintainer-list {
  display: grid;
  gap: 8px;
}

.maintainer-list section {
  display: grid;
  gap: 4px;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 8px;
}

.maintainer-list strong {
  color: #ffe8b9;
  line-height: 1.25;
}

.maintainer-list span {
  color: rgba(255, 231, 183, 0.72);
  font-size: 0.8rem;
  line-height: 1.35;
}

.maintainer-list span.warning {
  color: #ffd7c9;
}

.quiet-action.danger {
  border-color: rgba(204, 95, 65, 0.68);
  color: #ffd7c9;
}

.quiet-action.danger:hover,
.quiet-action.danger:focus-visible {
  background: rgba(110, 42, 36, 0.48);
}

.repository-panel {
  overflow: auto;
}

.repository-row {
  display: grid;
  gap: 6px;
  width: 100%;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  margin-top: 7px;
  padding: 9px 10px;
  color: #ffe8b9;
  text-align: left;
  background: rgba(11, 6, 3, 0.34);
  transition: border-color 150ms ease, background 150ms ease, transform 150ms ease;
}

.repository-row:hover,
.repository-row:focus-visible,
.repository-row.active {
  border-color: var(--gold-bright);
  background: rgba(82, 45, 16, 0.56);
  transform: translateY(-1px);
}

.repository-row.active {
  box-shadow: inset 0 0 0 1px rgba(255, 217, 138, 0.16);
}

.repository-row div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.repository-row strong {
  overflow-wrap: anywhere;
  line-height: 1.2;
}

.repository-row span {
  flex: 0 0 auto;
  border: 1px solid rgba(238, 184, 91, 0.42);
  border-radius: 999px;
  padding: 2px 7px;
  color: #ffe4ad;
  font-size: 0.7rem;
  background: rgba(80, 43, 18, 0.44);
}

.repository-row span.warning {
  border-color: rgba(204, 95, 65, 0.72);
  color: #ffd7c9;
  background: rgba(110, 42, 36, 0.44);
}

.repository-row small {
  color: rgba(255, 231, 183, 0.68);
  font-size: 0.76rem;
  line-height: 1.35;
}

.repository-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(250px, 0.66fr);
  grid-template-areas:
    "overview overview"
    "metrics metrics"
    "actions flow"
    "sync operation";
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.repository-overview-card {
  grid-area: overview;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(170px, 220px);
  gap: 14px;
  align-items: stretch;
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 7px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.56), rgba(11, 6, 3, 0.44)),
    radial-gradient(circle at 90% 10%, rgba(255, 217, 138, 0.16), transparent 0 28%, transparent 54%);
}

.repository-overview-card h3 {
  margin: 2px 0 8px;
  color: #ffe8b9;
  font-family: var(--font-display);
  font-size: 1.42rem;
}

.repository-overview-card p {
  max-width: 62ch;
  margin: 0;
  color: rgba(255, 231, 183, 0.75);
  line-height: 1.48;
}

.repository-sync-card {
  display: grid;
  align-content: center;
  gap: 8px;
  border: 1px solid rgba(238, 184, 91, 0.32);
  border-radius: 6px;
  padding: 14px;
  background: rgba(7, 4, 2, 0.34);
}

.repository-sync-card.warning {
  border-color: rgba(204, 95, 65, 0.68);
  background: rgba(79, 30, 24, 0.38);
}

.repository-sync-card span,
.repo-metric span {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.76rem;
}

.repository-sync-card strong,
.repo-metric strong {
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.08rem;
}

.repo-metric-grid {
  grid-area: metrics;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.repo-metric {
  display: grid;
  gap: 5px;
  min-width: 0;
  border: 1px solid rgba(240, 198, 118, 0.2);
  border-radius: 5px;
  padding: 11px 12px;
  background: rgba(11, 6, 3, 0.34);
}

.repo-metric strong {
  overflow-wrap: anywhere;
}

.repository-actions-card {
  grid-area: actions;
}

.repository-flow-card {
  grid-area: flow;
}

.repository-sync-detail {
  grid-area: sync;
}

.repository-operation-card {
  grid-area: operation;
}

.repository-flow-card ol {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 1.15rem;
  color: rgba(255, 231, 183, 0.76);
  line-height: 1.42;
}

.growth-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.78fr);
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.growth-hero-card {
  grid-column: 1 / -1;
  display: grid;
  gap: 14px;
  border: 1px solid rgba(240, 198, 118, 0.24);
  border-radius: 7px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(91, 50, 19, 0.56), rgba(11, 6, 3, 0.44)),
    radial-gradient(circle at 88% 18%, rgba(255, 217, 138, 0.14), transparent 0 30%, transparent 56%);
}

.growth-hero-card span {
  color: rgba(255, 231, 183, 0.62);
  font-size: 0.78rem;
}

.growth-hero-card strong {
  display: block;
  margin-top: 4px;
  color: #ffe2a0;
  font-family: var(--font-display);
  font-size: 1.75rem;
}

.growth-hero-card p {
  margin: 4px 0 0;
  color: rgba(255, 231, 183, 0.74);
}

.contribution-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  color: rgba(255, 231, 183, 0.78);
  list-style: none;
}

.contribution-list li {
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 7px;
}

.pr-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pr-row span {
  color: rgba(255, 231, 183, 0.72);
  font-size: 0.78rem;
}

.growth-panel dl div {
  min-width: 0;
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 5px;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.card-actions .primary-action,
.card-actions .quiet-action,
.notice-row .quiet-action,
.pr-row .quiet-action {
  min-height: 32px;
  padding: 0 10px;
  font-size: 0.82rem;
}

.notification-panel,
.pr-panel,
.growth-panel,
.operation-panel {
  overflow: auto;
}

.notice-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  padding: 10px;
}

.pr-row {
  margin-top: 8px;
  padding: 10px;
}

.pr-row div {
  display: grid;
  min-width: 0;
}

.pr-row strong {
  color: #ffe2a0;
}

.growth-panel dl {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.growth-panel ul {
  display: grid;
  gap: 6px;
  margin: 12px 0 0;
  padding: 0;
  color: rgba(255, 231, 183, 0.78);
  font-size: 0.84rem;
  list-style: none;
}

.growth-panel li {
  border-bottom: 1px solid rgba(240, 198, 118, 0.16);
  padding-bottom: 6px;
}

@media (max-width: 1240px) {
  .workbench-workspace {
    grid-template-columns: minmax(230px, 280px) minmax(420px, 1fr);
  }

  .workbench-statusbar {
    grid-template-columns: 1fr auto;
  }

  .workbench-stat-grid {
    grid-column: 1 / -1;
  }

  .workbench-level {
    grid-column: 1 / -1;
  }

  .repository-shortcuts {
    grid-column: 1 / -1;
  }

  .mailbox-area {
    grid-column: 2;
    grid-row: 1;
  }
}

@media (max-width: 940px) {
  .workbench-workspace {
    inset: 68px 16px 18px;
    display: block;
    overflow: auto;
  }

  .workbench-statusbar,
  .workbench-panel,
  .workbench-main {
    margin-bottom: 12px;
  }

  .workbench-statusbar {
    display: grid;
    grid-template-columns: 1fr;
  }

  .mailbox-area {
    grid-column: auto;
    grid-row: auto;
    justify-self: start;
  }

  .mailbox-popover {
    left: 0;
    right: auto;
  }

  .workbench-stat-grid,
  .detail-card dl {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .level-head {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .growth-detail-grid {
    grid-template-columns: 1fr;
  }

  .feedback-detail-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "brief"
      "checks"
      "note"
      "required"
      "tips"
      "actions"
      "operation";
  }

  .feedback-brief-card {
    grid-template-columns: 1fr;
  }

  .feedback-brief-card dl {
    grid-template-columns: 1fr;
  }

  .maintainer-detail-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "brief"
      "checks"
      "feedback"
      "actions"
      "quests"
      "repos"
      "notices"
      "operation";
  }

  .maintainer-brief-card {
    grid-template-columns: 1fr;
  }

  .maintainer-brief-card dl {
    grid-template-columns: 1fr;
  }

  .repository-detail-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "overview"
      "metrics"
      "actions"
      "flow"
      "sync"
      "operation";
  }

  .repository-overview-card {
    grid-template-columns: 1fr;
  }

  .repo-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .repository-shortcuts {
    grid-template-columns: 1fr;
  }

  .panel-head.inline {
    align-items: start;
    flex-direction: column;
  }
}
</style>
