package com.gitguild.backend.codehost.gitea.dto;

/**
 * Gitea PR 的状态快照。
 *
 * <p>{@code headBranch} 为源分支（裸分支名，取自 Gitea {@code head.ref}），
 * {@code baseBranch} 为目标分支（取自 {@code base.ref}）；二者共同映射到本地
 * {@code CodePullRequest} 的 {@code source_branch} / {@code target_branch}（均非空）。
 * {@code htmlUrl} 为 PR 的网页链接（{@code html_url}），映射到 {@code external_url}。
 */
public record PrInfo(
        int number,
        String title,
        String state,
        boolean merged,
        String headBranch,
        String baseBranch,
        String htmlUrl,
        String authorLogin) {
}
