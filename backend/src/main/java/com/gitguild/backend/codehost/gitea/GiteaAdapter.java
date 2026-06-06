package com.gitguild.backend.codehost.gitea;

import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.codehost.gitea.dto.FileCommitInfo;
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

    /**
     * 在指定仓库基于 {@code oldBranchName} 创建新分支 {@code newBranchName}（Issue #12）。
     *
     * <p>对应 Gitea API {@code POST /repos/{owner}/{repo}/branches}。
     * 当目标分支已存在时 Gitea 返回 409，调用方（task branch 服务）据此实现幂等。
     *
     * @param owner         Gitea 仓库 owner
     * @param repo          Gitea 仓库名
     * @param newBranchName 新分支名
     * @param oldBranchName 基础分支名（通常为仓库默认分支）
     * @return 创建后的分支元数据
     */
    BranchInfo createBranch(String owner, String repo, String newBranchName, String oldBranchName);

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

    /**
     * 把外部仓库迁入平台 Gitea，使委托人粘一个公网地址即可关联委托——
     * 之后建分支 / 提 PR / 审核全部落在平台可控副本上。
     *
     * <p>底层调用 Gitea {@code POST /repos/migrate}，{@code mirror=false}（完整可写副本，
     * 冒险家需 push 分支与 PR；mirror 是只读镜像，会废掉工作台写流程）。迁入目标 owner 取
     * token 自身用户（{@code GET /user} 的 login）。
     *
     * <p><b>幂等：</b>目标仓库（同 {@code repoName}）已存在时直接返回其元数据，不重复迁移、
     * 不抛 409。
     *
     * @param cloneAddr    源仓库克隆地址（如 {@code https://gitea.com/owner/repo.git}）
     * @param repoName     迁入平台后的仓库名（平台内唯一，建议确定性派生）
     * @param description  仓库描述（可选）
     * @param withMetadata 是否一并迁入 Issue / PR / Label / Milestone（仅对可识别的源平台有效）
     * @return 迁入后的平台仓库元数据，含 id、html_url、default_branch
     */
    RepositoryInfo migrateRepository(String cloneAddr, String repoName, String description, boolean withMetadata);

    /**
     * 在指定分支创建一个文件提交（Issue #13）。
     *
     * <p>MVP 工作台用它把冒险家的成果说明写入 task branch，形成真实 commit。
     */
    FileCommitInfo createFile(String owner, String repo, String branch, String path, String message, String content);

    /**
     * 基于 task branch 创建 Gitea PR（Issue #14）。
     */
    PrInfo createPullRequest(String owner, String repo, String title, String body, String sourceBranch, String targetBranch);

    /**
     * 合并指定 PR，供课程作业 MVP 审核闭环代理执行。
     */
    PrInfo mergePullRequest(String owner, String repo, int prNumber);
}
