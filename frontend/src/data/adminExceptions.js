// 异常处理中心的静态映射（M3 同步异常 / M4 关联异常 / M5 权限违规）。
// 异常数据本身来自真实后端 /admin/exceptions；本文件只保留前端展示用的分类筛选项与处置动作枚举。

// 异常分类 → 标签筛选用。
export const exceptionCategories = [
  { key: 'ALL', label: '全部' },
  { key: 'SYNC', label: '同步异常' },
  { key: 'RELATION', label: '关联异常' },
  { key: 'PERMISSION', label: '权限违规' },
]

// 每类异常可执行的处理动作（resolve 的 action 取值）。
export const exceptionActions = {
  SYNC: [
    { value: 'RETRY', label: '重试同步' },
    { value: 'KEEP_LAST', label: '保留上次成功数据' },
    { value: 'MARK_RESOLVED', label: '标记已解决' },
  ],
  RELATION: [
    { value: 'REQUEST_FIX', label: '退回修正关联' },
    { value: 'REBIND', label: '管理员手动绑定' },
    { value: 'MARK_RESOLVED', label: '标记已解决' },
  ],
  PERMISSION: [
    { value: 'REQUEST_GRANT', label: '要求补授权' },
    { value: 'BLOCK', label: '拦截违规操作' },
    { value: 'MARK_RESOLVED', label: '标记已解决' },
  ],
}
