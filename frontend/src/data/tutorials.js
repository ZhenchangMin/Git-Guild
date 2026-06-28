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

// 教程「已看过」标记按账号隔离：localStorage 同源共享，不带 scope 会让同一浏览器里
// 先登录的账号看完教程后，后续新账号被误判为「已看过」而不再触发首次引导。
export function getTutorialStorageKey(tutorialId, scope = 'guest') {
  return `${TUTORIAL_STORAGE_PREFIX}${scope}.${tutorialId}Seen`
}

export const tutorials = {
  hall: {
    title: '平台导览',
    dismissLabel: '跳过教程',
    steps: [
      {
        id: 'hall-drag',
        gesture: 'drag-horizontal',
        title: '拖动大厅查看全貌',
        body: '按住大厅空白区域左右拖动，可以查看分布在不同位置的功能入口。',
      },
      {
        id: 'hall-quest',
        target: 'hall-quest',
        title: '浏览可接取委托',
        body: '悬赏任务板汇总当前开放的委托。你可以搜索、筛选并进入详情，确认要求后再决定是否接取。',
      },
      {
        id: 'hall-workbench',
        target: 'hall-workbench',
        title: '进入工作台推进任务',
        body: '接取委托后，工作台会展示任务状态、仓库分支、Git 操作指引和后续提交入口。',
      },
      {
        id: 'hall-submission',
        target: 'hall-submission',
        placement: 'right-middle',
        title: '交付完成成果',
        body: '开发完成后，到提交柜台填写成果说明、上传佐证材料，并提交给维护者审核。',
      },
      {
        id: 'hall-leaderboard',
        target: 'hall-leaderboard',
        placement: 'left-middle',
        title: '查看成员排行',
        body: '排行榜按用户贡献进行排序，方便你比较不同成员的经验、等级和任务完成表现。',
      },
      {
        id: 'hall-desk',
        target: 'hall-desk',
        title: '向前台咨询问题',
        body: '前台提供平台流程问答。遇到仓库、任务、提交或审核问题时，可以直接提问。',
      },
      {
        id: 'hall-notification',
        target: 'hall-notification',
        title: '关注通知动态',
        body: '通知会提醒你任务状态、审核结果和系统事件。出现未读提示时，建议先查看最新进展。',
      },
      {
        id: 'hall-message',
        target: 'hall-message',
        title: '查看信笺',
        body: '信笺用于承载任务相关沟通。你可以在这里查看未读来信、联系委托人或回复冒险者。',
      },
      {
        id: 'hall-profile',
        target: 'hall-profile',
        title: '查看个人档案',
        body: '成长档案记录你的等级、经验和任务履历，是查看个人表现与历史贡献的入口。',
      },
      {
        id: 'hall-help',
        target: 'hall-help',
        title: '需要时打开帮助',
        body: '右上角问号会打开使用手册。需要复习当前页面操作时，可以在手册底部查看本页面教程。',
      },
    ],
  },

  questBoard: {
    title: '悬赏任务板',
    steps: [
      {
        id: 'quest-search',
        target: 'quest-search',
        title: '搜索目标委托',
        body: '输入任务标题、技术栈、仓库名称或验收关键词，快速缩小委托范围。',
      },
      {
        id: 'quest-filters',
        target: 'quest-filters',
        title: '组合筛选条件',
        body: '使用难度、分类和标签筛选委托，找到更匹配当前能力和工作安排的任务。',
      },
      {
        id: 'quest-card-overview',
        target: 'quest-primary-card',
        title: '阅读委托卡片',
        body: '卡片会展示标题、来源、技术栈、奖励、推荐理由和验收标准。先判断任务是否适合，再继续操作。',
        placement: 'right-middle',
      },
      {
        id: 'quest-detail-action',
        target: 'quest-detail-action',
        title: '查看完整详情',
        body: '详情页会提供更完整的目标、验收标准、仓库和 Issue 信息。建议先看清要求再接取。',
      },
      {
        id: 'quest-accept-action',
        target: 'quest-accept-action',
        title: '接取确认后的委托',
        body: '确认任务要求后，可以从这里进入接取流程。接取成功后，后续开发会在工作台继续。',
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
        body: '先阅读委托背景、奖励、难度和技术栈，判断这项任务是否适合接取。',
      },
      {
        id: 'quest-detail-criteria',
        target: 'quest-detail-criteria',
        title: '核对验收标准',
        body: '验收标准是维护者审核成果的依据。开始前请确认每一项要求都能被交付和验证。',
      },
      {
        id: 'quest-detail-links',
        target: 'quest-detail-links',
        title: '打开关联资源',
        body: '这里可以进入关联仓库和 Issue，查看代码位置、原始需求和上下文信息。',
      },
      {
        id: 'quest-detail-primary',
        target: 'quest-detail-primary',
        title: '继续当前流程',
        body: '根据任务状态，这里会显示接取、进入工作台、前往提交柜台或查看反馈等下一步操作。',
      },
    ],
  },

  frontDesk: {
    title: '前台问答',
    steps: [
      {
        id: 'frontdesk-input',
        target: 'frontdesk-input',
        title: '输入你的问题',
        body: '遇到平台流程、仓库接入、任务提交或审核问题时，可以在这里直接描述。',
      },
      {
        id: 'frontdesk-send',
        target: 'frontdesk-send',
        title: '发送给前台向导',
        body: '点击发送后，前台会结合平台流程和常见问题，给出可执行的下一步建议。',
      },
      {
        id: 'frontdesk-prompts',
        target: 'frontdesk-prompts',
        title: '使用推荐问题',
        body: '推荐气泡提供高频问题入口。点击后可以快速发起咨询，不必从头组织问题。',
      },
      {
        id: 'frontdesk-actions',
        target: 'frontdesk-actions',
        title: '根据回复快速跳转',
        body: '部分回复会附带相关页面入口。你可以直接跳到需要处理的任务、提交或帮助页面。',
      },
      {
        id: 'frontdesk-history',
        target: 'frontdesk-history',
        title: '查看对话历史',
        body: '历史入口会保留当前页面的问答记录，方便回看之前的问题、建议和跳转入口。',
      },
    ],
  },

  adventurerWorkbench: {
    title: '冒险者工作台',
    steps: [
      {
        id: 'workbench-task-list',
        target: 'workbench-task-list',
        title: '切换待办任务',
        body: '左侧列表汇总已接取任务、站内消息和审核反馈。选择一项后，右侧会显示对应详情。',
      },
      {
        id: 'workbench-task-overview',
        target: 'workbench-task-overview',
        title: '确认任务状态',
        body: '这里展示当前任务阶段、仓库、任务分支、PR 状态和下一步建议。开始操作前先确认信息无误。',
      },
      {
        id: 'workbench-clone-card',
        target: 'workbench-clone-card',
        title: '准备仓库与分支',
        body: '平台会为任务准备仓库地址和任务分支。请基于这里的信息开始开发，避免提交到错误位置。',
      },
      {
        id: 'workbench-copy-command',
        target: 'workbench-copy-command',
        title: '复制 Git 操作命令',
        body: '按步骤复制命令，完成克隆、切换分支、提交和推送，减少手动输入出错。',
      },
      {
        id: 'workbench-submit-counter',
        target: 'workbench-submit-counter',
        title: '前往提交柜台',
        body: '当成果准备好后，从这里进入提交柜台登记交付内容，并提交给维护者审核。',
      },
      {
        id: 'workbench-contact-client',
        target: 'workbench-contact-client',
        title: '联系委托相关人员',
        body: '如果任务目标、验收标准或审核反馈不清楚，可以通过信笺沟通后再继续推进。',
      },
    ],
  },

  maintainerWorkbench: {
    title: '维护者工作台',
    steps: [
      {
        id: 'maintainer-publish-entry',
        target: 'maintainer-publish-entry',
        title: '发布新的委托',
        body: '从这里把仓库需求发布为平台委托。发布后会进入审核流程，通过后才会对成员开放。',
      },
      {
        id: 'maintainer-review-entry',
        target: 'maintainer-review-entry',
        title: '审核成员提交',
        body: '成员提交成果后，你可以在这里查看说明、佐证材料和 PR，并给出审核结论。',
      },
      {
        id: 'maintainer-repo-sync-entry',
        target: 'maintainer-repo-sync-entry',
        title: '接入受托仓库',
        body: '仓库接入是发布委托的前置步骤。这里负责进入导入、创建和同步仓库的流程。',
      },
      {
        id: 'maintainer-adventure-entry',
        target: 'maintainer-adventure-entry',
        title: '切换到冒险者视角',
        body: '维护者也可以接取并完成其他人发布的委托，从冒险者工作台继续个人任务。',
      },
      {
        id: 'maintainer-my-quests',
        target: 'maintainer-my-quests',
        title: '跟踪已发布委托',
        body: '这里列出你发布过的委托及当前状态，便于确认任务是否已上架、进行中或需要处理。',
      },
      {
        id: 'maintainer-repositories',
        target: 'maintainer-repositories',
        title: '管理受托仓库',
        body: '受托仓库是委托来源。你可以查看已接入仓库，并进入新建或导入流程。',
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
        body: '可以新建平台仓库，也可以导入已有仓库。请根据项目来源选择合适方式。',
      },
      {
        id: 'repo-sync-form',
        target: 'repo-sync-form',
        title: '填写仓库信息',
        body: '导入时填写仓库地址和名称；新建时填写仓库名称与用途说明，便于后续管理和发布委托。',
      },
      {
        id: 'repo-sync-submit',
        target: 'repo-sync-submit',
        title: '确认接入或同步',
        body: '提交后平台会创建或导入仓库，并同步可用于发布委托的 Issue 信息。',
      },
      {
        id: 'repo-sync-next-step',
        target: 'repo-sync-next-step',
        title: '继续发布委托',
        body: '仓库就绪后，可以直接进入发布委托页，把仓库需求转化为可接取任务。',
      },
    ],
  },

  maintainerPublish: {
    title: '发布委托',
    steps: [
      {
        id: 'publish-repository',
        target: 'publish-repository',
        title: '选择目标仓库',
        body: '先选择委托所属仓库。仓库列表来自你已经接入平台的受托仓库。',
      },
      {
        id: 'publish-taxonomy',
        target: 'publish-taxonomy',
        title: '设置分类与标签',
        body: '分类和标签会影响任务检索、推荐和后续管理。请尽量使用能准确描述任务类型的标签。',
      },
      {
        id: 'publish-issue',
        target: 'publish-issue',
        title: '关联需求来源',
        body: '可以选择已有 OPEN Issue，也可以在发布时创建新的 Gitea Issue，用于保留原始需求上下文。',
      },
      {
        id: 'publish-task-basics',
        target: 'publish-task-basics',
        title: '完善任务信息',
        body: '标题应清楚说明要做什么；说明和验收标准用于明确交付范围；难度、技术栈、工时和 XP 会影响成员判断与任务奖励。',
      },
      {
        id: 'publish-submit',
        target: 'publish-submit',
        title: '提交发布审核',
        body: '确认信息无误后提交审核。审核通过后，委托会出现在悬赏任务板中。',
      },
    ],
  },

  maintainerReview: {
    title: '审核提交',
    steps: [
      {
        id: 'review-stats',
        target: 'review-stats',
        title: '查看审核概况',
        body: '顶部统计展示待审核、已处理和需修改的提交数量，帮助你快速掌握当前审核负载。',
      },
      {
        id: 'review-queue',
        target: 'review-queue',
        placement: 'right-middle',
        title: '浏览审核队列',
        body: '左侧队列按状态组织提交记录。选择不同记录后，右侧详情与审核表单会同步切换。',
      },
      {
        id: 'review-queue-card',
        target: 'review-queue-card',
        title: '选择提交记录',
        body: '提交卡片展示委托标题、提交人、提交时间和当前状态。先选中要处理的记录，再进入审核。',
      },
      {
        id: 'review-overview',
        target: 'review-overview',
        title: '核对提交对象',
        body: '这里展示委托标题、提交人、受托仓库、提交时间和奖励。审核前请确认这条提交对应正确任务。',
      },
      {
        id: 'review-links',
        target: 'review-links',
        title: '打开代码材料',
        body: '可以跳转到 Gitea PR 或任务分支，直接核对代码变更与提交内容。',
      },
      {
        id: 'review-deliverables',
        target: 'review-deliverables',
        title: '检查成果材料',
        body: '成果说明和佐证材料用于判断成员完成了什么、如何验证，以及是否覆盖验收标准。',
      },
      {
        id: 'review-pr-status',
        target: 'review-pr-status',
        title: '确认 PR 状态',
        body: 'PR 区域会展示 PR 标题、状态和分支信息。通过审核前请确认 PR 与本次委托一致。',
      },
      {
        id: 'review-merge',
        target: 'review-merge',
        title: '按需合并 PR',
        body: '审核通过不会自动合并 PR。如需完成仓库侧闭环，可以在这里触发合并并进行确认。',
      },
      {
        id: 'review-contact',
        target: 'review-contact',
        title: '联系提交人',
        body: '如果验收标准、PR 内容或证据不清楚，可以先通过信笺沟通，再给出审核结论。',
      },
      {
        id: 'review-decision',
        target: 'review-decision',
        title: '选择审核结论',
        body: '根据核对结果选择通过、要求修改或驳回。不同结论会影响提交人后续看到的任务状态。',
      },
      {
        id: 'review-feedback',
        target: 'review-feedback',
        title: '补充确认或反馈',
        body: '通过时请确认已核对 PR 和 Issue；要求修改或驳回时，请写清楚可执行的反馈意见。',
      },
      {
        id: 'review-submit-action',
        target: 'review-submit-action',
        title: '提交审核结果',
        body: '确认无误后提交审核结果。通过会推进任务结算；要求修改会让提交人继续补充后再提交。',
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
        body: '点击柜台上的提交单，展开当前委托的成果交付表单。',
      },
      {
        id: 'submission-form',
        target: 'submission-form',
        title: '填写成果说明',
        body: '确认仓库、分支和成果说明。说明应让维护者清楚知道你完成了哪些内容。',
      },
      {
        id: 'submission-evidence',
        target: 'submission-evidence',
        title: '上传佐证材料',
        body: '可以上传截图、日志或文档作为证据，帮助维护者更快验证成果质量。',
      },
      {
        id: 'submission-draft',
        target: 'submission-draft',
        title: '保存当前草稿',
        body: '如果还没准备好提交，可以先保存草稿，稍后继续补充说明和佐证材料。',
      },
      {
        id: 'submission-submit',
        target: 'submission-submit',
        title: '提交进入审核',
        body: '确认内容完整后提交审核。成果会进入维护者队列，等待审核反馈。',
      },
    ],
  },

  leaderboard: {
    title: '公会荣誉榜',
    steps: [
      {
        id: 'leaderboard-period',
        target: 'leaderboard-period',
        title: '选择时间范围',
        body: '选择“本周”“本月”或“全部”，查看对应时间范围内的成员排名。',
        advanceOn: 'event',
      },
      {
        id: 'leaderboard-refresh',
        target: 'leaderboard-refresh',
        title: '刷新当前榜单',
        body: '点击“刷新榜单”可重新获取当前时间范围的最新排名、等级和任务数据。',
        advanceOn: 'event',
      },
      {
        id: 'leaderboard-scroll',
        target: 'leaderboard-scroll',
        title: '滚动查看更多成员',
        body: '前三名会固定展示。将鼠标移入第四名之后的榜单区域并滚动滚轮，可以继续查看直到第五十名。',
        advanceOn: 'scroll',
      },
      {
        id: 'leaderboard-profile',
        target: 'leaderboard-profile',
        title: '访问冒险家成长档案',
        body: '排行榜中的冒险家名称都可以点击。点击高亮名称即可进入其主页查看该成员的贡献历程、成就与成长里程碑。',
        advanceOn: 'event',
      },
    ],
  },
}
