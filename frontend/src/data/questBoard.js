// 筛选分组。难度是后端固定枚举（A-D），故静态写死；
// 分类 / 标签来自 taxonomy 接口（quest-categories / quest-tags），
// 技术栈 / 状态来自当前委托实际携带的值——均由 QuestBoardPage 的
// visibleQuestFilterGroups 动态注入，这里不再放演示种子，避免出现
// 数据库里根本不存在的“假”可选项。
export const questFilterGroups = [
  { id: 'category', title: '分类', options: [] },
  { id: 'tag', title: '标签', options: [] },
  { id: 'difficulty', title: '难度', options: ['A', 'B', 'C', 'D'] },
  { id: 'stack', title: '技术栈', options: [] },
  { id: 'status', title: '状态', options: [] },
]
