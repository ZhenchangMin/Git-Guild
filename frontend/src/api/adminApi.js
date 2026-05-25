import { requestMock } from './httpClient'

// 管理员控制台接口层。路径对齐 docs/P4/临时文档/P4-005 前后端接口契约清单.md
// 与 docs/P3/API规范/（管理员审核 / 任务分类）。当前 httpClient 为 mock 实现。

export const adminApi = {
  // ── 任务审核（M1）──────────────────────────────────
  // 契约第 186 行：列表统一用 GET /admin/quests，不再使用 /admin/quest-review-applications。
  listAdminQuests(params) {
    return requestMock('/admin/quests', { params })
  },
  getAdminQuest(questId) {
    return requestMock(`/admin/quests/${questId}`)
  },
  // 通过上架 / 退回补充 / 下架统一走单接口，按 decision 区分。
  submitAdminReview(questId, payload) {
    return requestMock(`/quests/${questId}/admin-reviews`, { method: 'POST', body: payload })
  },

  // ── 异常处理中心（M3 / M4 / M5）────────────────────
  listRepositoryExceptions(params) {
    return requestMock('/repository-exceptions', { params })
  },
  getRepositoryException(exceptionId) {
    return requestMock(`/repository-exceptions/${exceptionId}`)
  },
  retryRepositoryException(exceptionId) {
    return requestMock(`/repository-exceptions/${exceptionId}/retry`, { method: 'POST' })
  },
  // action + comment：处理同步 / 关联 / 权限违规类异常。
  resolveException(exceptionId, payload) {
    return requestMock(`/admin/exceptions/${exceptionId}/resolve`, { method: 'POST', body: payload })
  },

  // ── 平台配置：分类与标签（M2）──────────────────────
  listCategories(params) {
    return requestMock('/quest-categories', { params })
  },
  createCategory(payload) {
    return requestMock('/quest-categories', { method: 'POST', body: payload })
  },
  updateCategory(categoryId, payload) {
    return requestMock(`/quest-categories/${categoryId}`, { method: 'PATCH', body: payload })
  },
  listTags(params) {
    return requestMock('/quest-tags', { params })
  },
  createTag(payload) {
    return requestMock('/quest-tags', { method: 'POST', body: payload })
  },
  updateTag(tagId, payload) {
    return requestMock(`/quest-tags/${tagId}`, { method: 'PATCH', body: payload })
  },
}
