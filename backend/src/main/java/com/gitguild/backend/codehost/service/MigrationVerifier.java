package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaRepoCoordinates;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import com.gitguild.backend.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 迁移完成校验器：解决「假成功」。
 *
 * <p>Gitea {@code /repos/migrate} 会先建好仓库记录、再在后台克隆。对<b>超大 / 不存在 / 私有</b>的源，
 * 它可能<b>先返回成功、实际克隆却卡死或失败</b>，仓库长期停留在「迁移中（空）」状态。若直接据此登记，
 * 就会出现「平台报接入完成、Gitea 却永远转圈」的假成功。
 *
 * <p>本类在迁移调用返回后<b>轮询</b>仓库真实状态：只有确认仓库已迁完（{@code empty == false}）才放行；
 * 轮询超时仍为空，则判定迁移未完成，<b>清理残留空壳</b>（best-effort）并抛出
 * {@code REPOSITORY_MIGRATION_INCOMPLETE}，由上层经全局异常处理转成前端可读的失败提示（弹窗）。
 *
 * <p>同步迁移（返回即非空）零等待直接放行，不影响正常小仓库的导入速度。
 */
@Component
public class MigrationVerifier {

    private final GiteaAdapter giteaAdapter;
    private final int pollAttempts;
    private final long pollIntervalMs;

    public MigrationVerifier(
            GiteaAdapter giteaAdapter,
            @Value("${gitea.migration-poll-attempts:8}") int pollAttempts,
            @Value("${gitea.migration-poll-interval-ms:1800}") long pollIntervalMs) {
        this.giteaAdapter = giteaAdapter;
        this.pollAttempts = Math.max(1, pollAttempts);
        this.pollIntervalMs = Math.max(0, pollIntervalMs);
    }

    /**
     * 确认迁移真正完成。
     *
     * @param migrated {@code migrateRepository} 返回的仓库元数据
     * @return 已确认迁完（非空）的仓库元数据
     * @throws BusinessException {@code REPOSITORY_MIGRATION_INCOMPLETE}：超时仍为空 / 仓库中途消失 / 校验被中断
     */
    public RepositoryInfo verifyMigrated(RepositoryInfo migrated) {
        if (migrated == null) {
            throw incomplete("migrate returned null");
        }
        if (!migrated.empty()) {
            return migrated; // 同步迁移已完成，零等待放行
        }
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(migrated.htmlUrl());
        for (int attempt = 0; attempt < pollAttempts; attempt++) {
            sleep(pollIntervalMs);
            RepositoryInfo current = fetchOrNull(coords);
            if (current == null) {
                throw incomplete(coords.owner() + "/" + coords.repo() + " disappeared during migration");
            }
            if (!current.empty()) {
                return current; // 迁移完成
            }
        }
        // 超时仍为空 → 判定未完成：清理残留空壳，抛错让前端提示用户
        safeDelete(coords);
        throw incomplete(coords.owner() + "/" + coords.repo()
                + " still empty after " + pollAttempts + " polls");
    }

    private RepositoryInfo fetchOrNull(GiteaRepoCoordinates coords) {
        try {
            return giteaAdapter.getRepository(coords.owner(), coords.repo());
        } catch (BusinessException e) {
            if ("CODE_HOST_RESOURCE_NOT_FOUND".equals(e.getCode())) {
                return null;
            }
            throw e;
        }
    }

    private void safeDelete(GiteaRepoCoordinates coords) {
        try {
            giteaAdapter.deleteRepository(coords.owner(), coords.repo());
        } catch (RuntimeException ignored) {
            // 清理失败不改变「迁移未完成」的结论；残壳会在下次导入复用前被清理
        }
    }

    private void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("REPOSITORY_MIGRATION_INCOMPLETE", HttpStatus.UNPROCESSABLE_ENTITY,
                    "仓库迁移校验被中断，请稍后重试。", "interrupted while polling migration status");
        }
    }

    private BusinessException incomplete(String detail) {
        return new BusinessException("REPOSITORY_MIGRATION_INCOMPLETE", HttpStatus.UNPROCESSABLE_ENTITY,
                "仓库迁移未在预期时间内完成：源仓库可能过大、不存在或为私有，平台尚未真正接入。"
                        + "请确认地址有效、仓库为公开仓库后重试；超大仓库建议精简历史后再导入。",
                detail);
    }
}
