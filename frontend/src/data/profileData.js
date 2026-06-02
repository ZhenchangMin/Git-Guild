// ─── 身份信息 ────────────────────────────────────────
export const profileIdentity = {
  userId: 5,
  name: 'Minerva Dawn',
  avatarUrl: '',               // 空串表示无头像，显示首字母占位
  motto: 'Every expert was once a beginner.',
  createdAt: '2026-04-12T08:00:00+08:00', // 用于计算 Code 龄
}

// ─── 成长摘要（fallback，后端接口失败时使用）─────────
export const growthFallback = {
  level: 3,
  totalXp: 720,
  nextLevelXp: 1000,
  completedQuestCount: 6,
}

// ─── 公会称号梯度 ───────────────────────────────────
export const titleThresholds = [
  { minLevel: 12, title: '公会大师' },
  { minLevel: 8,  title: '公会精英' },
  { minLevel: 5,  title: '代码工匠' },
  { minLevel: 3,  title: '前端协作学徒' },
  { minLevel: 2,  title: '协作学徒' },
  { minLevel: 1,  title: '见习冒险者' },
]

// ─── 统计指标（词云卡片）────────────────────────────
export const profileStats = [
  { key: 'completedQuestCount', label: '已完成任务', icon: 'scroll',   weight: 1.0 },
  { key: 'totalXp',             label: '累计 XP',   icon: 'star',     weight: 0.5 },
  { key: 'repoCount',           label: '贡献仓库数', icon: 'grid',    weight: 0.8, fallbackValue: 3 },
  { key: 'pendingReview',       label: '待审核',     icon: 'clock',   weight: 0.9, fallbackValue: 1 },
]

// ─── 贡献历程 ───────────────────────────────────────
export const contributionRecords = [
  {
    id: 'QST-0398',
    questTitle: '任务筛选空态优化',
    repository: 'git-guild / frontend',
    pullRequest: 'PR #12',
    difficulty: 'C',
    xp: 180,
    completedAt: '2026-05-10',
    summary: '补齐空结果提示、清空筛选入口和分页状态，帮助新手理解任务大厅筛选结果。',
    tags: ['Vue', 'CSS', '空状态'],
  },
  {
    id: 'QST-0401',
    questTitle: '审核反馈卡片重构',
    repository: 'git-guild / frontend',
    pullRequest: 'PR #15',
    difficulty: 'C',
    xp: 160,
    completedAt: '2026-05-14',
    summary: '将审核反馈从弹窗改为独立卡片，支持折叠与展开，提升移动端可读性。',
    tags: ['Vue', '组件', '响应式'],
  },
  {
    id: 'QST-0412',
    questTitle: 'Issue 同步状态页',
    repository: 'git-guild / backend',
    pullRequest: 'PR #18',
    difficulty: 'B',
    xp: 240,
    completedAt: '2026-05-18',
    summary: '展示仓库同步中的等待、成功和异常状态，给出重试入口与错误说明。',
    tags: ['Spring Boot', '异常处理', 'Gitea'],
  },
  {
    id: 'QST-0425',
    questTitle: 'XP 进度条动效',
    repository: 'git-guild / frontend',
    pullRequest: 'PR #20',
    difficulty: 'D',
    xp: 80,
    completedAt: '2026-05-22',
    summary: '为等级进度条添加填充动画和光晕效果，升级时触发粒子特效。',
    tags: ['CSS', '动画', '交互设计'],
  },
  {
    id: 'QST-0438',
    questTitle: '任务难度标签系统',
    repository: 'git-guild / backend',
    pullRequest: 'PR #22',
    difficulty: 'B',
    xp: 200,
    completedAt: '2026-05-26',
    summary: '实现 A/B/C/D 四级难度枚举，关联任务实体，支持按难度筛选。',
    tags: ['Java', '枚举', '数据库'],
  },
  {
    id: 'QST-0444',
    questTitle: '审核反馈归档',
    repository: 'git-guild / frontend',
    pullRequest: 'PR #23',
    difficulty: 'B',
    xp: 180,
    completedAt: '2026-05-30',
    summary: '按任务回看审核反馈，区分必须修改项和学习建议，便于重新提交。',
    tags: ['Vue', '前端交互', '状态管理'],
  },
]

// ─── 技能标签（§3.4 三阶品质边框）──────────────────
export const skillTagPool = [
  { name: 'Vue',          count: 4 },
  { name: 'CSS',          count: 2 },
  { name: 'Spring Boot',  count: 2 },
  { name: '异常处理',     count: 5 },
  { name: 'Gitea',        count: 1 },
  { name: 'Java',         count: 3 },
  { name: '动画',         count: 1 },
  { name: '交互设计',     count: 2 },
  { name: '状态管理',     count: 3 },
  { name: '数据库',       count: 1 },
  { name: '组件',         count: 4 },
  { name: '响应式',       count: 2 },
]

// ─── 成就徽章（§3.5 Tab 2）─────────────────────────
export const badgeShowcase = [
  { id: 'first-quest',      name: '初出茅庐', earned: true,  hint: '完成第一个任务',                earnedAt: '2026-05-10' },
  { id: 'three-streak',     name: '三连突破', earned: true,  hint: '连续完成 3 个任务',              earnedAt: '2026-05-18' },
  { id: 'frontend-focus',   name: '前端专注', earned: true,  hint: '在前端仓库完成 3 个任务',         earnedAt: '2026-05-22' },
  { id: 'backend-explorer', name: '后端探索', earned: true,  hint: '在后端仓库完成 1 个任务',         earnedAt: '2026-05-18' },
  { id: 'level-5',          name: '代码工匠', earned: false, hint: '达到 Level 5' },
  { id: 'ten-quests',       name: '公会支柱', earned: false, hint: '完成 10 个任务' },
]

// ─── 成长里程碑（§3.5 Tab 3）───────────────────────
export const milestoneEvents = [
  {
    type: 'first-quest',
    icon: 'scroll',
    title: '首次冒险',
    description: '完成 QST-0398「任务筛选空态优化」，获得 180 XP',
    date: '2026-05-10',
  },
  {
    type: 'level-up',
    icon: 'arrow-up',
    title: '等级提升 — Lv 1 → Lv 2',
    description: '累计 XP 突破 100，晋升为协作学徒',
    date: '2026-05-10',
  },
  {
    type: 'badge',
    icon: 'badge',
    title: '徽章达成 — 初出茅庐',
    description: '完成第一个任务，自动获得「初出茅庐」徽章',
    date: '2026-05-10',
  },
  {
    type: 'level-up',
    icon: 'arrow-up',
    title: '等级提升 — Lv 2 → Lv 3',
    description: '累计 XP 突破 200，晋升为前端协作学徒',
    date: '2026-05-14',
  },
  {
    type: 'streak',
    icon: 'flame',
    title: '三连突破',
    description: '连续完成 3 个任务，获得「三连突破」徽章',
    date: '2026-05-18',
  },
  {
    type: 'new-repo',
    icon: 'branch',
    title: '技术拓展 — 后端仓库',
    description: '首次在 git-guild / backend 仓库贡献',
    date: '2026-05-18',
  },
]

// ─── 难度曲线数据（§3.3 左栏折线图）────────────────
export const difficultyTrendData = [
  { date: '2026-05-10', difficulty: 'C', questTitle: '任务筛选空态优化', xp: 180 },
  { date: '2026-05-14', difficulty: 'C', questTitle: '审核反馈卡片重构', xp: 160 },
  { date: '2026-05-18', difficulty: 'B', questTitle: 'Issue 同步状态页',  xp: 240 },
  { date: '2026-05-22', difficulty: 'D', questTitle: 'XP 进度条动效',    xp: 80  },
  { date: '2026-05-26', difficulty: 'B', questTitle: '任务难度标签系统', xp: 200 },
  { date: '2026-05-30', difficulty: 'B', questTitle: '审核反馈归档',     xp: 180 },
]

// ─── 技能雷达数据（§3.3 右栏雷达图）────────────────
export const skillRadarPool = [
  { name: '前端组件',   value: 85, defaultSelected: true  },
  { name: '状态管理',   value: 72, defaultSelected: true  },
  { name: 'CSS 样式',   value: 68, defaultSelected: true  },
  { name: '后端集成',   value: 40, defaultSelected: true  },
  { name: '文档撰写',   value: 55, defaultSelected: true  },
  { name: '异常处理',   value: 78, defaultSelected: true  },
  { name: '表单校验',   value: 30, defaultSelected: false },
  { name: 'Gitea 集成', value: 25, defaultSelected: false },
  { name: '测试编写',   value: 35, defaultSelected: false },
  { name: '性能优化',   value: 42, defaultSelected: false },
  { name: '交互设计',   value: 60, defaultSelected: false },
  { name: '代码审查',   value: 48, defaultSelected: false },
]
