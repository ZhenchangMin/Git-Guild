<script setup>
import { useRouter } from 'vue-router'

import hallImg from '../../assets/hall.webp'

const router = useRouter()

// 三种身份：每个人进站后先认清自己的角色，再看对应的流程。
const roles = [
  {
    icon: '⚔',
    name: '冒险家',
    desc: '接取悬赏委托、提交成果、积累经验值与徽章。',
  },
  {
    icon: '📜',
    name: '委托人',
    desc: '把仓库 Issue 发布成悬赏，审核冒险家提交的成果。',
  },
  {
    icon: '🛡',
    name: '管理员',
    desc: '审核委托能否上架，维护分类、标签与平台秩序。',
  },
]

// 冒险家：从接取到结算的主线。每步标注「在哪里做」。
const adventurerSteps = [
  { where: '悬赏任务板', text: '按难度、技术栈或标签筛选，挑一个适合自己的委托。' },
  { where: '任务详情', text: '读清任务背景、关联 Issue 与完成标准，确认后点「接取委托」。' },
  { where: '工作台', text: '从默认分支创建任务分支，本地改完后上传 / 提交生成 commit。' },
  { where: '工作台', text: '用任务分支发起 PR，标题或描述里关联对应 Issue。' },
  { where: '提交柜台', text: '关联 PR、填写成果说明、上传佐证截图或文档，提交审核。' },
  { where: '成长档案', text: '审核通过后自动结算经验值与徽章，可在档案查看贡献记录。' },
]

// 委托人：发布与审核两条线。
const maintainerSteps = [
  { where: '仓库同步', text: '先把目标仓库导入或新建到平台，确保 Issue 可被引用。' },
  { where: '发布委托', text: '选定 Issue，填写完成标准、奖励、分类与技术栈，提交管理员审核。' },
  { where: '委托人工作台', text: '上架后等待冒险家接取；可随时跟踪进度与提交状态。' },
  { where: '审核台', text: '收到成果后核对 PR 与佐证，选择通过、退回修改或驳回。' },
]

// 三个工作区的分工——最容易混淆的地方，单独讲清楚。
const workspaces = [
  {
    name: '工作台',
    role: '项目与 Git 操作',
    text: '创建分支、上传文件生成 commit、发起 PR 都在这里完成。',
  },
  {
    name: '提交柜台',
    role: '登记任务成果',
    text: '只负责关联 PR、填写成果说明与佐证、提交审核，不创建 commit、不发起 PR。',
  },
  {
    name: '审核台',
    role: '委托人验收',
    text: '委托人在此查看成果与佐证，给出通过 / 退回 / 驳回的结论。',
  },
]

// 常见问题：覆盖最高频的几类困惑。
const faqs = [
  {
    q: '提交时提示「请求参数不合法 / PR 失败」？',
    a: '多半是任务分支还没有任何改动，或还没发起 PR。请先在工作台完成 commit 并创建 PR，再回提交柜台提交。',
  },
  {
    q: '我的委托被退回，状态显示「已要求修改」怎么办？',
    a: '先在工作台读完逐项反馈，更新分支与 PR，再回提交柜台重新提交。退回不是失败，PR 和提交材料都要同步更新。',
  },
  {
    q: '点「接取委托」提示已被别人接取？',
    a: '说明委托刚被其他冒险家抢先接取、而任务板还没刷新。返回悬赏任务板即可看到最新的可接取委托。',
  },
  {
    q: '佐证材料支持哪些格式？',
    a: '运行截图（图片）或文档说明都可以，单个委托最多 5 个文件、每个不超过 5MB。委托人会在审核台看到你上传的文件。',
  },
]

function backToHall() {
  router.push({ name: 'hall' })
}
</script>

<template>
  <main class="app-shell">
    <section class="scene work-scene help-route-mode" :style="{ backgroundImage: `url(${hallImg})` }">
      <button class="back-orb" type="button" aria-label="返回公会大厅" @click="backToHall">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回公会大厅</span>
      </button>

      <div class="standalone-page-panel compact">
        <section class="glass-ledger standalone-hero-card">
          <p class="kicker">公会指南</p>
          <h1>Git Guild 使用手册</h1>
          <p>三步看懂这座公会：先认清你的身份，再沿着对应的流程走，遇到卡点查一下常见问题。</p>
        </section>

        <section class="parchment-panel help-body">
          <!-- 身份速览 -->
          <div class="help-block">
            <p class="kicker">先认清身份</p>
            <h2>你是哪一种公会成员？</h2>
            <div class="role-grid">
              <article v-for="role in roles" :key="role.name" class="role-card">
                <span class="role-icon" aria-hidden="true">{{ role.icon }}</span>
                <h3>{{ role.name }}</h3>
                <p>{{ role.desc }}</p>
              </article>
            </div>
          </div>

          <!-- 冒险家路线 -->
          <div class="help-block">
            <p class="kicker">冒险家路线</p>
            <h2>从接取到结算</h2>
            <ol class="route-list">
              <li v-for="(step, idx) in adventurerSteps" :key="idx">
                <span class="route-where">{{ step.where }}</span>
                <span class="route-text">{{ step.text }}</span>
              </li>
            </ol>
          </div>

          <!-- 委托人路线 -->
          <div class="help-block">
            <p class="kicker">委托人路线</p>
            <h2>发布与验收</h2>
            <ol class="route-list">
              <li v-for="(step, idx) in maintainerSteps" :key="idx">
                <span class="route-where">{{ step.where }}</span>
                <span class="route-text">{{ step.text }}</span>
              </li>
            </ol>
          </div>

          <!-- 三个工作区分工 -->
          <div class="help-block">
            <p class="kicker">别记混了</p>
            <h2>工作台 · 提交柜台 · 审核台</h2>
            <div class="workspace-grid">
              <article v-for="ws in workspaces" :key="ws.name" class="workspace-card">
                <h3>{{ ws.name }}</h3>
                <span class="workspace-role">{{ ws.role }}</span>
                <p>{{ ws.text }}</p>
              </article>
            </div>
          </div>

          <!-- 常见问题 -->
          <div class="help-block">
            <p class="kicker">常见问题</p>
            <h2>遇到卡点先看这里</h2>
            <dl class="faq-list">
              <div v-for="(faq, idx) in faqs" :key="idx" class="faq-item">
                <dt>{{ faq.q }}</dt>
                <dd>{{ faq.a }}</dd>
              </div>
            </dl>
          </div>

          <div class="help-actions">
            <button class="quiet-action" type="button" @click="router.push({ name: 'front-desk' })">
              咨询 AI 向导
            </button>
            <button class="primary-action" type="button" @click="router.push({ name: 'quest-board' })">
              前往悬赏任务板 →
            </button>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.help-body {
  display: grid;
  gap: 26px;
}

.help-block h2 {
  margin: 2px 0 0;
  color: #4c250c;
  font-size: 1.3rem;
}

/* 身份卡片 */
.role-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.role-card {
  display: grid;
  gap: 6px;
  border: 1px solid rgba(90, 48, 20, 0.3);
  border-radius: 8px;
  padding: 14px;
  background: rgba(255, 248, 228, 0.5);
}

.role-icon {
  font-size: 1.5rem;
}

.role-card h3 {
  margin: 0;
  color: #4c250c;
  font-size: 1.05rem;
}

.role-card p {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.88rem;
  line-height: 1.5;
}

/* 路线步骤 */
.route-list {
  display: grid;
  gap: 10px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  counter-reset: route;
}

.route-list li {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  border-left: 3px solid rgba(135, 80, 18, 0.55);
  border-radius: 0 6px 6px 0;
  padding: 11px 14px 11px 44px;
  background: rgba(255, 248, 228, 0.46);
  counter-increment: route;
}

.route-list li::before {
  position: absolute;
  left: 10px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(180deg, #c98b3c, #8a5012);
  color: #fff6e2;
  font-size: 0.82rem;
  font-weight: 700;
  content: counter(route);
}

.route-where {
  flex: 0 0 auto;
  border: 1px solid rgba(135, 80, 18, 0.4);
  border-radius: 999px;
  padding: 3px 10px;
  color: #6e3a10;
  font-size: 0.74rem;
  font-weight: 700;
  white-space: nowrap;
  background: rgba(214, 142, 56, 0.16);
}

.route-text {
  color: #3d2515;
  font-size: 0.92rem;
  line-height: 1.5;
}

/* 工作区分工 */
.workspace-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.workspace-card {
  display: grid;
  gap: 5px;
  border: 1px solid rgba(90, 48, 20, 0.3);
  border-radius: 8px;
  padding: 14px;
  background: rgba(255, 248, 228, 0.5);
}

.workspace-card h3 {
  margin: 0;
  color: #4c250c;
  font-size: 1.04rem;
}

.workspace-role {
  color: #875012;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.workspace-card p {
  margin: 4px 0 0;
  color: var(--ink-soft);
  font-size: 0.86rem;
  line-height: 1.5;
}

/* 常见问题 */
.faq-list {
  display: grid;
  gap: 12px;
  margin: 14px 0 0;
}

.faq-item {
  border-bottom: 1px solid rgba(90, 48, 20, 0.24);
  padding-bottom: 12px;
}

.faq-item dt {
  color: #4c250c;
  font-weight: 700;
  font-size: 0.96rem;
}

.faq-item dd {
  margin: 6px 0 0;
  color: var(--ink-soft);
  font-size: 0.9rem;
  line-height: 1.55;
}

.help-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 4px;
}

@media (max-width: 720px) {
  .role-grid,
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
