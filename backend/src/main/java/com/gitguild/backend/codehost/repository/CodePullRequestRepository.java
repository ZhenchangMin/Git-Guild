package com.gitguild.backend.codehost.repository;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodePullRequestRepository extends JpaRepository<CodePullRequest, Long> {

    /**
     * 按仓库和 Gitea PR 编号查找已同步的 PR 记录。
     *
     * <p>同步服务的标准入口：先查找已有记录，存在则更新状态，不存在则新建，
     * 禁止不经查找直接 {@code save(new CodePullRequest(...))}（会触发唯一约束冲突）。
     *
     * @param repositoryId 本地仓库 ID
     * @param externalPrId Gitea PR 编号的字符串形式（即 {@code String.valueOf(prNumber)}）
     * @return 已同步的 PR 记录，若从未同步则为 {@code empty}
     */
    Optional<CodePullRequest> findByRepositoryRepositoryIdAndExternalPrId(Long repositoryId, String externalPrId);

    Optional<CodePullRequest> findByPullRequestIdAndRepositoryRepositoryId(Long pullRequestId, Long repositoryId);
}
