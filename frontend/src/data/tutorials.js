export const TUTORIAL_LAUNCH_QUERY = 'tutorial'
export const TUTORIAL_CLOSE_EVENT = 'gitguild:tutorial-close'
export const TUTORIAL_COMPLETE_STEP_EVENT = 'gitguild:tutorial-complete-step'
export const TUTORIAL_STEP_EVENT = 'gitguild:tutorial-step'
export const TUTORIAL_STORAGE_PREFIX = 'gitguild.tutorial.'

export const routeTutorialMap = {
  hall: 'hall',
  'quest-board': 'questBoard',
  'quest-detail': 'questDetail',
  'front-desk': 'frontDesk',
  'adventurer-workbench': 'adventurerWorkbench',
  'maintainer-workbench': 'maintainerWorkbench',
  'repository-sync': 'repositorySync',
  'maintainer-publish': 'maintainerPublish',
  'maintainer-review': 'maintainerReview',
  'submission-counter': 'submissionCounter',
  leaderboard: 'leaderboard',
}

export function getTutorialStorageKey(tutorialId) {
  return `${TUTORIAL_STORAGE_PREFIX}${tutorialId}Seen`
}

export const tutorials = {
  hall: {
    title: '公会大厅导览',
    dismissLabel: '跳过教程',
    steps: [
      {
        id: 'hall-drag',
        gesture: 'drag-horizontal',
        title: '先拖动大厅背景',
        body: '按住大厅空白处左右拖动，可以看到完整的公会大厅。后面的功能入口分布在不同位置。',
      },
      {
        id: 'hall-quest',
        target: 'hall-quest',
        title: '从悬赏任务板开始',
        body: '这里是业务起点。你可以搜索关键词、筛选条件，打开委托详情后再决定是否接取。',
      },
      {
        id: 'hall-workbench',
        target: 'hall-workbench',
        title: '接取后进入工作台',
        body: '工作台用于查看当前任务、复制仓库和分支命令，并跟随 Git 流程推进开发。',
      },
      {
        id: 'hall-submission',
        target: 'hall-submission',
        placement: 'right-middle',
        title: '完成后到提交柜台',
        body: '开发完成后，在提交柜台填写成果说明、上传证据，并敲钟提交给维护者审核。',
      },
      {
        id: 'hall-leaderboard',
        target: 'hall-leaderboard',
        placement: 'left-middle',
        title: '查看成长反馈',
        body: '排行榜展示经验、等级和排名。完成委托并通过审核后，这里会体现你的成长记录。',
      },
      {
        id: 'hall-desk',
        target: 'hall-desk',
        title: '遇到问题去前台',
        body: '前台是辅助问答入口。你可以直接输入问题，也可以点击推荐气泡快速询问。',
      },
      {
        id: 'hall-notification',
        target: 'hall-notification',
        title: '留意左上角通知',
        body: '通知会提示审核结果、任务状态变化和系统提醒。看到红点时，可以先点开确认最新进展。',
      },
      {
        id: 'hall-message',
        target: 'hall-message',
        title: '查看站内信笺',
        body: '信笺用于查看任务相关沟通和未读消息，方便在课程展示时说明协作反馈如何流转。',
      },
      {
        id: 'hall-profile',
        target: 'hall-profile',
        title: '进入成长档案',
        body: '成长档案记录等级、经验和个人完成情况，是排行榜之外查看个人成长的入口。',
      },
      {
        id: 'hall-help',
        target: 'hall-help',
        title: '以后从这里找帮助',
        body: '右上角问号会进入帮助页。帮助页里的“查看教程”按钮可以重新播放这套大厅教程。',
      },
    ],
  },

  questBoard: {
    title: '悬赏任务板',
    steps: [
      {
        id: 'quest-search',
        target: 'quest-search',
        title: '搜索关键词',
        body: '输入任务标题、技术栈或仓库名称，可以快速缩小委托范围。',
      },
      {
        id: 'quest-filters',
        target: 'quest-filters',
        title: '选择筛选条件',
        body: '用难度、分类、标签等条件组合筛选，找到适合当前展示或练习目标的委托。',
      },
      {
        id: 'quest-card-overview',
        target: 'quest-primary-card',
        title: '先读懂委托卡片',
        body: '卡片会集中展示委托标题、仓库来源、技术栈、奖励、推荐理由和验收标准。先判断任务是否适合，再决定下一步操作。',
        placement: 'right-middle',
      },
      {
        id: 'quest-detail-action',
        target: 'quest-detail-action',
        title: '先查看详情',
        body: '详情页会展示任务目标、完成标准、仓库和 Issue 信息。建议先看清要求再接取。',
      },
      {
        id: 'quest-accept-action',
        target: 'quest-accept-action',
        title: '确认后接取',
        body: '接取入口会带你进入详情确认流程。接取成功后，后续开发会在工作台继续。',
      },
    ],
  },

  questDetail: {
    title: '委托详情',
    steps: [
      {
        id: 'quest-detail-summary',
        target: 'quest-detail-summary',
        title: '确认任务目标',
        body: '先阅读委托背景、奖励、难度和技术栈，判断是否适合接取。',
      },
      {
        id: 'quest-detail-criteria',
        target: 'quest-detail-criteria',
        title: '核对完成标准',
        body: '完成标准是维护者审核的依据。提交前应逐项确认这些要求已经满足。',
      },
      {
        id: 'quest-detail-links',
        target: 'quest-detail-links',
        title: '查看仓库和 Issue',
        body: '这里可以打开关联仓库和 Issue，用于了解代码位置与原始需求。',
      },
      {
        id: 'quest-detail-primary',
        target: 'quest-detail-primary',
        title: '进入下一步',
        body: '可接取时这里会显示接取按钮；已接取后会指向工作台、提交柜台或反馈页。',
      },
    ],
  },

  frontDesk: {
    title: '前台问答',
    steps: [
      {
        id: 'frontdesk-input',
        target: 'frontdesk-input',
        title: '在下方输入问题',
        body: '遇到平台流程、仓库接入或提交问题时，可以直接在这里描述你的问题。',
      },
      {
        id: 'frontdesk-send',
        target: 'frontdesk-send',
        title: '发送给前台向导',
        body: '点击发送后，前台会结合当前平台流程给出下一步建议。',
      },
      {
        id: 'frontdesk-prompts',
        target: 'frontdesk-prompts',
        title: '也可以点快捷气泡',
        body: '右侧气泡是常见问题入口，适合课程展示时快速触发问答。',
      },
      {
        id: 'frontdesk-actions',
        target: 'frontdesk-actions',
        title: '使用回复下方快捷跳转',
        body: '看板娘回答后，回复气泡下方可能出现相关页面入口，可以直接跳到下一步要处理的功能。',
      },
      {
        id: 'frontdesk-history',
        target: 'frontdesk-history',
        title: '查看历史记录',
        body: '历史入口会保留当前页面的对话，方便回看刚才问过的问题和建议。',
      },
    ],
  },

  adventurerWorkbench: {
    title: '冒险者工作台',
    steps: [
      {
        id: 'workbench-task-list',
        target: 'workbench-task-list',
        title: '选择当前任务',
        body: '左侧任务列表用于切换已接取的委托、站内消息和审核反馈。',
      },
      {
        id: 'workbench-task-overview',
        target: 'workbench-task-overview',
        title: '先看任务当前状态',
        body: '右侧第一块会说明当前委托所处阶段、关联仓库、任务分支、PR 状态和下一步。开始操作前先确认这里的信息。',
      },
      {
        id: 'workbench-clone-card',
        target: 'workbench-clone-card',
        title: '准备仓库和分支',
        body: '这里展示平台准备好的仓库地址和任务分支，是开始开发前最重要的信息。',
      },
      {
        id: 'workbench-copy-command',
        target: 'workbench-copy-command',
        title: '复制 Git 命令',
        body: '按步骤复制命令，完成克隆、切换分支、提交和推送。',
      },
      {
        id: 'workbench-submit-counter',
        target: 'workbench-submit-counter',
        title: '完成后去提交柜台',
        body: '当 PR 状态满足提交条件后，从这里前往提交柜台登记成果。',
      },
      {
        id: 'workbench-contact-client',
        target: 'workbench-contact-client',
        title: '需要沟通时联系委托人',
        body: '如果任务目标、验收标准或审核反馈不清楚，可以从这里进入站内信，与委托相关人员沟通。',
      },
    ],
  },

  maintainerWorkbench: {
    title: '维护者工作台',
    steps: [
      {
        id: 'maintainer-publish-entry',
        target: 'maintainer-publish-entry',
        title: '发布新委托',
        body: '这个入口用于把仓库 Issue 发布成平台委托，并提交给管理员审核。',
      },
      {
        id: 'maintainer-review-entry',
        target: 'maintainer-review-entry',
        title: '审核提交',
        body: '冒险者提交成果后，维护者在这里查看证据、PR 和审核表单。',
      },
      {
        id: 'maintainer-repo-sync-entry',
        target: 'maintainer-repo-sync-entry',
        title: '仓库接入口',
        body: '这里仅作为入口说明。具体导入、创建和同步操作会在仓库接入页内提示。',
      },
      {
        id: 'maintainer-adventure-entry',
        target: 'maintainer-adventure-entry',
        title: '也能做冒险者任务',
        body: '维护者账号也可以进入冒险者工作台，接取并完成其他人发布的委托。',
      },
      {
        id: 'maintainer-my-quests',
        target: 'maintainer-my-quests',
        title: '查看我发布的委托',
        body: '下方会列出你发布过的委托和当前状态，方便确认是否已上架、进行中或被退回。',
      },
      {
        id: 'maintainer-repositories',
        target: 'maintainer-repositories',
        title: '管理受托仓库',
        body: '受托仓库是发布委托的来源。这里可以查看已接入仓库，并进入新建或导入流程。',
      },
    ],
  },

  repositorySync: {
    title: '仓库接入',
    steps: [
      {
        id: 'repo-sync-mode',
        target: 'repo-sync-mode',
        title: '选择接入方式',
        body: '可以新建空仓库，也可以导入已有仓库。课程展示时按演示材料选择即可。',
      },
      {
        id: 'repo-sync-form',
        target: 'repo-sync-form',
        title: '填写仓库信息',
        body: '导入时填写仓库地址和名称；新建时填写仓库名称和用途说明。',
      },
      {
        id: 'repo-sync-submit',
        target: 'repo-sync-submit',
        title: '确认接入或同步',
        body: '提交后平台会创建或导入仓库，并尝试同步 Issue 信息。',
      },
      {
        id: 'repo-sync-next-step',
        target: 'repo-sync-next-step',
        title: '下一步发布委托',
        body: '仓库就绪后，可以直接进入发布委托页，把 Issue 转成平台任务。',
      },
    ],
  },

  maintainerPublish: {
    title: '发布委托',
    steps: [
      {
        id: 'publish-repository',
        target: 'publish-repository',
        title: '选择仓库',
        body: '先选择要发布任务的仓库。仓库列表来自维护者已经接入的平台仓库。',
      },
      {
        id: 'publish-taxonomy',
        target: 'publish-taxonomy',
        title: '补充分级和标签',
        body: '分类、标签、工时和 XP 会影响任务展示与课程评价口径。',
      },
      {
        id: 'publish-issue',
        target: 'publish-issue',
        title: '关联或新建 Issue',
        body: '可以选择已有 OPEN Issue，也可以在发布时创建新的 Gitea Issue。',
      },
      {
        id: 'publish-task-basics',
        target: 'publish-task-basics',
        title: '填写任务基础信息',
        body: '任务标题要让冒险者一眼看懂要做什么；任务说明补充背景和目标；难度、技术栈、预估工时和 XP 用于标记任务复杂度与奖励。',
      },
      {
        id: 'publish-submit',
        target: 'publish-submit',
        title: '提交审核',
        body: '发布后会先进入管理员审核流程，通过后才会出现在悬赏任务板。',
      },
    ],
  },

  maintainerReview: {
    title: '审核提交',
    steps: [
      {
        id: 'review-stats',
        target: 'review-stats',
        title: '先看审核概况',
        body: '顶部统计帮助你判断当前有多少待审核、已审核和需退回修改的提交，适合课程展示时快速说明审核压力。',
      },
      {
        id: 'review-queue',
        target: 'review-queue',
        placement: 'right-middle',
        title: '查看审核队列',
        body: '左侧队列按待审核和已审核分组，维护者可以从这里切换不同成果提交。',
      },
      {
        id: 'review-queue-card',
        target: 'review-queue-card',
        title: '选择一条提交记录',
        body: '提交卡片会显示委托标题、提交人、提交时间和当前状态。点击不同记录后，右侧详情与审核表单会同步切换。',
      },
      {
        id: 'review-overview',
        target: 'review-overview',
        title: '核对任务与提交人',
        body: '详情顶部展示委托标题、提交人、受托仓库、提交时间和奖励 XP。审核前先确认这条提交对应的是正确任务。',
      },
      {
        id: 'review-links',
        target: 'review-links',
        title: '打开仓库侧材料',
        body: '这里可以跳转到 Gitea PR 或任务分支，方便维护者直接核对代码变更。',
      },
      {
        id: 'review-deliverables',
        target: 'review-deliverables',
        title: '阅读成果材料',
        body: '成果说明和佐证材料用于判断冒险者完成了什么、如何验证，以及是否覆盖委托验收标准。',
      },
      {
        id: 'review-pr-status',
        target: 'review-pr-status',
        title: '确认 PR 状态',
        body: 'PR 区域会列出 PR 标题、当前状态和源/目标分支。通过审核前要确认 PR 与本次委托一致。',
      },
      {
        id: 'review-merge',
        target: 'review-merge',
        title: '必要时合并 PR',
        body: '平台审核通过不会自动合并 PR。需要完成仓库侧闭环时，可以在这里触发合并并进行二次确认。',
      },
      {
        id: 'review-contact',
        target: 'review-contact',
        title: '需要说明时联系冒险者',
        body: '如果验收标准、PR 内容或证据不清楚，可以通过站内信笺联系提交人，避免直接给出误判。',
      },
      {
        id: 'review-decision',
        target: 'review-decision',
        title: '选择审核结论',
        body: '右侧表单用于选择审核通过、要求修改或驳回。不同结论会影响冒险者后续看到的任务状态。',
      },
      {
        id: 'review-feedback',
        target: 'review-feedback',
        title: '补充确认或反馈',
        body: '通过时需要确认已核对 PR 和 Issue；退回或驳回时则需要写清楚冒险者能理解的修改意见。',
      },
      {
        id: 'review-submit-action',
        target: 'review-submit-action',
        title: '提交审核结果',
        body: '确认无误后提交审核结果。课程展示时可以说明：审核通过会结算任务，要求修改会让冒险者重新提交。',
      },
    ],
  },

  submissionCounter: {
    title: '提交柜台',
    steps: [
      {
        id: 'submission-open-sheet',
        target: 'submission-open-sheet',
        title: '打开提交单',
        body: '点击柜台上的羊皮纸，展开当前委托的成果提交单。',
      },
      {
        id: 'submission-form',
        target: 'submission-form',
        title: '填写成果说明',
        body: '确认仓库、分支和成果说明。说明需要让维护者看懂你完成了什么。',
      },
      {
        id: 'submission-evidence',
        target: 'submission-evidence',
        title: '上传证据',
        body: '可以上传截图、日志或文档作为佐证，帮助维护者快速判断成果质量。',
      },
      {
        id: 'submission-draft',
        target: 'submission-draft',
        title: '必要时保存草稿',
        body: '还没准备好提交时，可以先保存草稿，稍后回到柜台继续编辑。',
      },
      {
        id: 'submission-submit',
        target: 'submission-submit',
        title: '敲钟提交',
        body: '确认无误后敲钟提交，成果会进入维护者审核队列。',
      },
    ],
  },

  leaderboard: {
    title: '排行榜',
    steps: [
      {
        id: 'leaderboard-list',
        target: 'leaderboard-list',
        title: '查看排名',
        body: '排行榜按经验展示成员成长情况，适合课程展示阶段体现任务完成反馈。',
      },
      {
        id: 'leaderboard-metrics',
        target: 'leaderboard-metrics',
        title: '理解等级和经验',
        body: '等级和 XP 反映通过审核的委托成果，是平台的成长激励。',
      },
      {
        id: 'leaderboard-refresh',
        target: 'leaderboard-refresh',
        title: '刷新最新数据',
        body: '如果刚完成审核或演示数据变动，可以点击这里重新拉取排行榜。',
      },
    ],
  },
}
