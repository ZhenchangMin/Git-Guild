package com.gitguild.backend.codehost.gitea;

import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import java.util.List;

/**
 * Git-Guild 与 Gitea 的只读集成层。
 *
 * <p>Git-Guild 采用"用户自操作"模式：Adventurer 用自己的 Gitea 账号直接完成 commit 和
 * PR 操作，Git-Guild 不代理执行 Git 操作。本接口仅封装读取操作，任何写操作（上传文件、
 * 创建/合并 PR）均不属于此适配层的职责范围。
 *
 * <p><b>调用方须知：</b>底层使用系统级 admin token（非 Adventurer 个人 token），
 * 所有方法在 Gitea 返回 4xx 时抛出 {@code HttpClientErrorException}，调用方需自行
 * 转换为业务异常（见已知问题清单 P4-016 问题1）。
 */
public interface GiteaAdapter {

    /**
     * 获取 Gitea 仓库元数据。
     *
     * @param owner Gitea 仓库所有者用户名
     * @param repo  仓库名称
     * @return 仓库信息，含默认分支、是否为空仓库等
     */
    RepositoryInfo getRepository(String owner, String repo);

    /**
     * 列出仓库的开放 Issue，供 Guild Master 在发布 Quest 时关联使用。
     *
     * <p><b>已知限制：</b>当前仅返回前 50 条，不分页。Issue 超过 50 条的仓库结果不完整。
     *
     * @param owner Gitea 仓库所有者用户名
     * @param repo  仓库名称
     * @return 开放状态的 Issue 列表，Gitea 无数据时返回空列表
     */
    List<IssueInfo> listIssues(String owner, String repo);

    /**
     * 获取单个 PR 的状态快照，用于校验 Adventurer 提交的 Submission 是否关联了真实 PR。
     *
     * <p>核心用途：通过 {@code PrInfo.authorLogin()} 与
     * {@code CodeHostAccountBinding.externalUsername} 比对，
     * 验证"提交的 PR 确实属于该 Adventurer"。
     *
     * @param owner    Gitea 仓库所有者用户名
     * @param repo     仓库名称
     * @param prNumber Gitea PR 编号（即 {@code CodePullRequest.externalPrId} 的整数形式）
     * @return PR 状态快照，含合并状态和作者登录名
     */
    PrInfo getPullRequest(String owner, String repo, int prNumber);

    /**
     * 列出仓库的所有分支。
     *
     * <p><b>已知限制：</b>当前依赖 Gitea 默认分页（20 条），分支超过 20 条的仓库结果不完整。
     *
     * @param owner Gitea 仓库所有者用户名
     * @param repo  仓库名称
     * @return 分支列表，含分支名和最新 commit SHA，Gitea 无数据时返回空列表
     */
    List<BranchInfo> listBranches(String owner, String repo);
}
