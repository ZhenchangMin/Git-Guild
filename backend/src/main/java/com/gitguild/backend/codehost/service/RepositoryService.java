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
     * 列出当前用户有权限的仓库。
     *
     * @param currentUserId Git-Guild 用户 ID
     */
    List<CodeRepository> listRepositories(Long currentUserId);
}
