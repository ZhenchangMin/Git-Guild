package com.gitguild.backend.ops.domain;

/**
 * 异常处理中心的异常分类（对齐前端「异常处理中心」M3/M4/M5 三大类）。
 *
 * <ul>
 *   <li>{@link #SYNC} — 仓库同步异常（拉取 Issue / PR 时外部 Gitea 失败）。当前唯一有真实事件源的分类。</li>
 *   <li>{@link #RELATION} — 关联异常（PR 链接不匹配 / 重复接取）。暂未接入真实事件源。</li>
 *   <li>{@link #PERMISSION} — 权限违规（越权发布 / 越权操作受限仓库）。暂未接入真实事件源。</li>
 * </ul>
 */
public enum ExceptionCategory {
    SYNC,
    RELATION,
    PERMISSION
}
