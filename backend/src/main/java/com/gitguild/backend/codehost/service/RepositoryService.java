package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.dto.CreateRepositoryRequest;
import java.util.List;

/**
 * 仓库接入服务（Issue #9）：在本地 Gitea 创建仓库并持久化到 Git-Guild。
 */
public interface RepositoryService {

    /**
     * 在 Gitea admin 用户下创建仓库并写入本地库。
     *
     * @param currentUserId Git-Guild 调用者用户 ID（将成为仓库的业务 owner）
     * @param request       仓库名 + 可选描述
     * @return 已持久化的本地仓库实体
     */
    CodeRepository createRepository(Long currentUserId, CreateRepositoryRequest request);

    /**
     * 关联（接入）一个仓库源地址。
     *
     * <p>若源地址就在平台自己的 Gitea 上，直接登记（幂等）；否则自动调 Gitea migrate 把外部仓库
     * 迁入平台，再以平台副本地址登记——使委托人只需粘一个公网地址即可关联委托，下游建分支 / 提 PR /
     * 审核全部落在平台可控副本上。
     *
     * @param currentUserId Git-Guild 调用者用户 ID（将成为仓库的业务 owner）
     * @param sourceUrl     源仓库地址（必填）
     * @param name          仓库展示名（可选，留空则从地址推断）
     * @param hostType      托管类型（可选，默认 GITEA）
     * @return 已持久化的本地仓库实体（外部源时其 sourceUrl 为平台副本地址）
     */
    CodeRepository importRepository(Long currentUserId, String sourceUrl, String name, String hostType);

    /**
     * 列出当前用户有权限的仓库。
     *
     * @param currentUserId Git-Guild 用户 ID
     */
    List<CodeRepository> listRepositories(Long currentUserId);

    CodeRepository requireReadableRepository(Long currentUserId, Long repositoryId);

    CodeRepository requireWritableRepository(Long currentUserId, Long repositoryId);

    /**
     * 删除一个已接入的仓库，并级联清理其牵连的全部业务数据（委托、提交、审核、XP、贡献、Issue、PR）
     * 与平台 Gitea 副本。
     *
     * <p>仅 Guild Master / Admin 可调用。不存在的仓库抛 {@code REPOSITORY_NOT_FOUND}。
     *
     * @param currentUserId Git-Guild 调用者用户 ID
     * @param repositoryId  待删除仓库 ID
     */
    void deleteRepository(Long currentUserId, Long repositoryId);
}
