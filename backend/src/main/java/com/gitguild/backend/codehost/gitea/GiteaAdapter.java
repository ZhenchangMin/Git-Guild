package com.gitguild.backend.codehost.gitea;

import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import java.util.List;

/**
 * Git-Guild 与 Gitea 的集成层（读写）。
 *
 * <p>Git-Guild 采用"平台代理受限写操作"模式（CONTEXT.md § Gitea 集成模型）：底层仓库仍由
 * 本地 Gitea 托管，Git-Guild 工作台在业务流程内代理创建仓库、创建分支、上传文件、创建 PR
 * 和同步 PR 状态。MVP 写操作范围限制为 Quest 工作流所需的调用。
 *
 * <p><b>调用方须知：</b>底层使用系统级 admin token（非 Adventurer 个人 token），
 * 所有方法在 Gitea 返回 4xx 时抛出（经 {@link GiteaAdapterImpl#execute} 统一转）
 * {@code BusinessException}（{@code CODE_HOST_RESOURCE_NOT_FOUND} /
 * {@code CODE_HOST_UNAVAILABLE}）。
 */
public interface GiteaAdapter {

    // ── 读操作 ──────────────────────────────────────────────────────────

    /**
     * 获取 Gitea 仓库元数据。
     */
    RepositoryInfo getRepository(String owner, String repo);

    /**
     * 列出仓库的开放 Issue，供 Guild Master 在发布 Quest 时关联使用。
     *
     * <p><b>已知限制：</b>当前仅返回前 50 条，不分页。
     */
    List<IssueInfo> listIssues(String owner, String repo);

    /**
     * 获取单个 PR 的状态快照。
     */
    PrInfo getPullRequest(String owner, String repo, int prNumber);

    /**
     * 列出仓库的所有分支。
     *
     * <p><b>已知限制：</b>当前依赖 Gitea 默认分页（20 条）。
     */
    List<BranchInfo> listBranches(String owner, String repo);

    /**
     * 列出仓库的所有 PR（含 open / closed / merged）。
     */
    List<PrInfo> listPulls(String owner, String repo);

    /**
     * 在指定仓库创建一个新的 Gitea Issue（Issue #11）。
     *
     * @param owner Gitea 仓库 owner
     * @param repo  Gitea 仓库名
     * @param title Issue 标题
     * @param body  Issue 正文（可选）
     * @return 创建后的 Issue 元数据
     */
    IssueInfo createIssue(String owner, String repo, String title, String body);

    // ── 平台代理写操作 ──────────────────────────────────────────────────

    /**
     * 在 Gitea admin 用户下创建新仓库（Issue #9）。
     *
     * <p>使用 admin token，仓库 Gitea 侧 owner 为 admin 用户，
     * 业务所有权由 {@code CodeRepository.owner} 记录（Guild Master）。
     *
     * @param name        仓库名（必填）
     * @param description 仓库描述（可选）
     * @return 创建后的仓库元数据，含 id、full_name、html_url、default_branch
     */
    RepositoryInfo createRepository(String name, String description);
}
