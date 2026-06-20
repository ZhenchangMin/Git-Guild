package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import java.util.List;

/**
 * 把 Gitea 上某仓库的 PR 同步进本地 {@code pull_requests} 表。
 *
 * <p>这是 P4-016 已知问题清单第 5 条预留的"未来的 PR 同步服务"，也是提交链路的前置：
 * 只有 PR 落入本地库后，Submission 才能引用本地 {@code pullRequestId}。
 *
 * <p><b>不变量：</b>同步对每个 PR 走"查找-或-新建"的 upsert，保证 DB 唯一约束
 * {@code uk_pr_repository_external (repository_id, external_pr_id)} 不被重复 INSERT 触发。
 *
 * <p><b>边界错误模式：</b>仓库 {@code sourceUrl} 无法解析出 owner/repo 时抛
 * {@code REPOSITORY_SOURCE_URL_INVALID}；Gitea 不可达 / 仓库不存在由
 * {@link com.gitguild.backend.codehost.gitea.GiteaAdapter} 抛 {@code CODE_HOST_*} 业务异常。
 */
public interface CodePullRequestSyncService {

    /**
     * 同步指定本地仓库的全部 PR（open / merged / closed）并返回 upsert 后的本地记录。
     *
     * @param repository 本地仓库实体（owner/repo 由其 {@code sourceUrl} 解析）
     * @return 该仓库当前的本地 PR 列表，Gitea 无 PR 时返回空列表
     */
    List<CodePullRequest> syncRepositoryPullRequests(CodeRepository repository);
}
