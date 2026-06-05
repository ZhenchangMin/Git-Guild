package com.gitguild.backend.codehost.gitea.dto;

/**
 * Gitea contents API 创建文件后返回的提交快照。
 */
public record FileCommitInfo(
        String commitSha,
        String branch,
        String path,
        String htmlUrl) {
}
