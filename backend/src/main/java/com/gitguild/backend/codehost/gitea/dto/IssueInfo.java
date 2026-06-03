package com.gitguild.backend.codehost.gitea.dto;

/**
 * Gitea Issue 的快照。
 *
 * @param number  Gitea 侧 Issue 编号（external_issue_id）
 * @param body    Issue 正文，创建时使用；列表查询可能为 null
 */
public record IssueInfo(
        long number,
        String title,
        String body,
        String state,
        String htmlUrl) {
}
