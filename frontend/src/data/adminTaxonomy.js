// 平台配置数据（M2 维护任务分类 / 标签 / 难度）。
// 字段对齐 docs/P3/API规范-“任务分类”部分.md：
//   分类 { categoryId, name(1-32, 唯一), description(0-200), enabled, questCount }
//   标签 { tagId, name, color, enabled, questCount }
// 难度为固定枚举（契约：A/B/C/D），此处只读展示。

export const questCategories = [
  {
    categoryId: 1,
    name: '前端体验',
    description: '围绕页面交互、组件与可访问性的任务。',
    enabled: true,
    questCount: 12,
  },
  {
    categoryId: 2,
    name: '后端服务',
    description: '接口、数据持久化与业务规则相关的任务。',
    enabled: true,
    questCount: 9,
  },
  {
    categoryId: 3,
    name: '测试与质量',
    description: '单元测试、集成测试与质量保障相关的任务。',
    enabled: true,
    questCount: 3,
  },
  {
    categoryId: 4,
    name: '文档与示例',
    description: '使用说明、示例与新手引导相关的任务。',
    enabled: false,
    questCount: 0,
  },
]

export const questTags = [
  { tagId: 1, name: '新手友好', color: '#43613a', enabled: true, questCount: 8 },
  { tagId: 2, name: 'good first issue', color: '#1f3c63', enabled: true, questCount: 6 },
  { tagId: 3, name: 'Vue', color: '#2f7d5b', enabled: true, questCount: 7 },
  { tagId: 4, name: 'Spring Boot', color: '#6e2a24', enabled: true, questCount: 5 },
  { tagId: 5, name: '待重构', color: '#875012', enabled: false, questCount: 0 },
]

// 难度选项（固定枚举，仅展示口径）。
export const questDifficulties = [
  { code: 'A', label: '入门', hint: '熟悉流程即可完成，适合首次贡献。' },
  { code: 'B', label: '初级', hint: '需要少量定位与改动。' },
  { code: 'C', label: '中级', hint: '涉及多文件或一定设计取舍。' },
  { code: 'D', label: '进阶', hint: '需要跨模块理解与较强工程能力。' },
]

// 标签预设色板：在公会羊皮纸 + 暗底主题下都能保持可读的中高饱和度色，
// 按色相分组（绿 / 青蓝 / 紫 / 红橙 / 金棕 / 灰蓝），覆盖常见标签语义。
// 不在此列表中的颜色由“自定义取色”补足，因此色彩选择实际不受限。
export const tagPalette = [
  '#43613a', // 苔绿
  '#5a6b2f', // 橄榄
  '#2f7d5b', // 翡翠
  '#2c6e6a', // 松石青
  '#1f3c63', // 深蓝
  '#102544', // 藏青
  '#3a5fa0', // 钢蓝
  '#5b3a78', // 紫罗兰
  '#8a4a86', // 品红紫
  '#6e2a24', // 酒红
  '#a63d3d', // 砖红
  '#b9742a', // 橙
  '#875012', // 古铜
  '#c8a13a', // 金
]
