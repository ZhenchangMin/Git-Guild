package com.gitguild.backend.quest.service;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;

/**
 * Quest 提交闭环中“平台代理 PR”的封装：把冒险家的 task branch 提交为 Gitea PR，
 * 并在维护者审核通过时代理合并。
 *
 * <p>与 {@link QuestTaskBranchService} 一样，实现<b>不</b>标注 {@code @Transactional}：
 * 它在调用方（{@code createSubmission} / {@code reviewSubmission}）的事务内运行，
 * 让 Gitea 4xx 转出的 {@code BusinessException} 干净传播，而不越过事务代理边界把当前事务
 * 标记为 rollback-only。
 */
public interface QuestPullRequestService {

    /**
     * 为一次提交确保存在对应的 Gitea PR（head = task branch，base = 仓库默认分支），并 upsert 到本地。
     *
     * <p><b>幂等：</b>若该 head→base 的 open PR 已存在（Gitea 409），复用之而非报错。
     *
     * @throws com.gitguild.backend.common.BusinessException
     *         {@code TASK_BRANCH_EMPTY}(422) —— task 分支与 base 相同或无差异（未推送提交）；
     *         其余 {@code CODE_HOST_*} 透传。
     */
    CodePullRequest ensurePullRequestForSubmission(
            CodeRepository repository, String headBranch, String baseBranch, String title, String body);

    /**
     * 审核通过时代理合并 PR，并回写本地状态为 MERGED。
     *
     * <p>已合并则直接返回（重复通过安全）。
     *
     * @throws com.gitguild.backend.common.BusinessException
     *         {@code PR_MERGE_CONFLICT}(409) —— PR 存在冲突无法自动合并；其余 {@code CODE_HOST_*} 透传。
     */
    CodePullRequest mergeForApproval(CodePullRequest pullRequest, CodeRepository repository);
}
