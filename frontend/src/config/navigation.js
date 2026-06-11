export const roleNavigation = {
  VISITOR: [
    { label: '悬赏任务板', routeName: 'quest-board' },
    { label: '帮助说明', routeName: 'help' },
  ],
  ADVENTURER: [
    { label: '悬赏任务板', routeName: 'quest-board' },
    { label: '工作台', routeName: 'adventurer-workbench' },
    { label: '提交柜台', routeName: 'submission-counter' },
    { label: '成长档案', routeName: 'profile' },
  ],
  MAINTAINER: [
    { label: '仓库接入', routeName: 'repository-sync' },
    { label: '委托人工作台', routeName: 'maintainer-workbench' },
    { label: '完成工作台', routeName: 'adventurer-workbench' },
    { label: '提交审核', routeName: 'maintainer-review' },
  ],
  ADMIN: [
    { label: '任务审核', routeName: 'admin-review' },
    { label: '异常处理', routeName: 'admin-exceptions' },
    { label: '平台配置', routeName: 'admin-taxonomy' },
  ],
}
