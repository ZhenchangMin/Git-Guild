package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodeRepository;

/**
 * 仓库同步服务：从外部代码托管平台拉取 Issue 并 upsert 到本地。
 *
 * <p>从 {@code CodeHostController} 内联逻辑下沉而来，使「手动同步」与「异常中心重试」共用同一实现，
 * 并在失败时统一登记平台异常（供异常处理中心展示）。
 */
public interface RepositorySyncService {

    /**
     * 同步指定仓库的 Issue。
     *
     * <p>成功：标记 {@code SYNCED} 并返回仓库；失败：登记一条 SYNC 异常、标记 {@code FAILED}，
     * 再抛出原异常（让调用方/前端看到失败）。
     *
     * @param repositoryId 仓库 id
     * @return 同步后的仓库
     */
    CodeRepository syncRepository(Long repositoryId);
}
