package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;

/**
 * 本地 Issue 与 Gitea Issue 的同步与创建（Issue #11）。
 */
public interface CodeIssueService {

    /**
     * 在 Gitea 上创建 Issue 并持久化到本地库。
     *
     * @param repository 目标仓库（用于解析 Gitea owner/repo 和关联）
     * @param title      Issue 标题
     * @param body       Issue 正文（可选）
     * @return 已持久化的本地 CodeIssue
     */
    CodeIssue createFromGitea(CodeRepository repository, String title, String body);
}
