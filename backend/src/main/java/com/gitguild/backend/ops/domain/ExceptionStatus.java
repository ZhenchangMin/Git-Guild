package com.gitguild.backend.ops.domain;

/**
 * 异常的处理状态。
 *
 * <ul>
 *   <li>{@link #UNRESOLVED} — 新发现、尚未处理。</li>
 *   <li>{@link #IN_REVIEW} — 已介入但需复核（如已发起重试、已退回补授权，等待外部结果）。</li>
 *   <li>{@link #RESOLVED} — 已闭环，无需进一步处理。</li>
 * </ul>
 */
public enum ExceptionStatus {
    UNRESOLVED,
    IN_REVIEW,
    RESOLVED
}
