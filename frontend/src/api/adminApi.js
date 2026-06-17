import { request } from './httpClient'

// 管理员控制台接口层。路径对齐 docs/P4/临时文档/P4-005 前后端接口契约清单.md
// 与 docs/P3/API规范/（管理员审核 / 任务分类）。审核、分类、异常处理中心均已接真实后端。

export const adminApi = {
  // ── 任务审核（M1）──────────────────────────────────
  // 契约第 186 行：列表统一用 GET /admin/quests，不再使用 /admin/quest-review-applications。
  listAdminQuests(params) {
    const query = new URLSearchParams()
    Object.entries(params ?? {}).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') return
      query.set(key, String(value))
    })
    const suffix = query.toString() ? `?${query}` : ''
    return request(`/admin/quests${suffix}`)
  },
  getAdminQuest(questId) {
    return request(`/quests/${questId}`)
  },
  // 通过上架 / 退回补充 / 下架统一走单接口，按 decision 区分。
  submitAdminReview(questId, payload) {
    return request(`/quests/${questId}/admin-reviews`, { method: 'POST', body: payload })
  },

  // ── 异常处理中心（M3 / M4 / M5，已接真实后端）────────
  // 按分类 / 状态筛选异常列表（ALL 或空表示不限）。
  listExceptions(params) {
    return request(`/admin/exceptions${toQuery(params)}`)
  },
  getException(exceptionId) {
    return request(`/admin/exceptions/${exceptionId}`)
  },
  retryException(exceptionId) {
    return request(`/admin/exceptions/${exceptionId}/retry`, { method: 'POST' })
  },
  // action + comment：处理同步 / 关联 / 权限违规类异常。
  resolveException(exceptionId, payload) {
    return request(`/admin/exceptions/${exceptionId}/resolve`, { method: 'POST', body: payload })
  },

  // ── 平台配置：分类与标签（M2，已接真实后端）────────
  listCategories(params) {
    return request(`/quest-categories${toQuery(params)}`)
  },
  createCategory(payload) {
    return request('/quest-categories', { method: 'POST', body: payload })
  },
  updateCategory(categoryId, payload) {
    return request(`/quest-categories/${categoryId}`, { method: 'PATCH', body: payload })
  },
  listTags(params) {
    return request(`/quest-tags${toQuery(params)}`)
  },
  createTag(payload) {
    return request('/quest-tags', { method: 'POST', body: payload })
  },
  updateTag(tagId, payload) {
    return request(`/quest-tags/${tagId}`, { method: 'PATCH', body: payload })
  },
}

function toQuery(params) {
  const query = new URLSearchParams()
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    query.set(key, String(value))
  })
  const s = query.toString()
  return s ? `?${s}` : ''
}
