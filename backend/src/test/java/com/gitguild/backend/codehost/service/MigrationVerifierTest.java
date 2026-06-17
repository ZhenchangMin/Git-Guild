package com.gitguild.backend.codehost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import com.gitguild.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MigrationVerifierTest {

    @Mock private GiteaAdapter giteaAdapter;

    // 轮询 2 次、间隔 0ms，便于快速测试
    private MigrationVerifier verifier() {
        return new MigrationVerifier(giteaAdapter, 2, 0);
    }

    private RepositoryInfo repo(boolean empty) {
        return new RepositoryInfo(1L, "demo", "spike-admin/demo", "main", empty,
                "http://localhost:3000/spike-admin/demo");
    }

    @Test
    void returnsImmediatelyWhenMigrateAlreadyComplete() {
        RepositoryInfo done = repo(false);

        assertThat(verifier().verifyMigrated(done)).isSameAs(done);
        // 同步迁移已完成，零等待，不应再去查 Gitea
        verify(giteaAdapter, never()).getRepository(eq("spike-admin"), eq("demo"));
    }

    @Test
    void pollsUntilRepositoryBecomesNonEmpty() {
        RepositoryInfo ready = repo(false);
        when(giteaAdapter.getRepository("spike-admin", "demo"))
                .thenReturn(repo(true))   // 第 1 次仍在迁移
                .thenReturn(ready);       // 第 2 次迁完

        assertThat(verifier().verifyMigrated(repo(true))).isSameAs(ready);
    }

    @Test
    void throwsIncompleteAndCleansUpWhenStillEmptyAfterTimeout() {
        when(giteaAdapter.getRepository("spike-admin", "demo")).thenReturn(repo(true));

        assertThatThrownBy(() -> verifier().verifyMigrated(repo(true)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("REPOSITORY_MIGRATION_INCOMPLETE");
        // 超时仍空 → 清理残留空壳
        verify(giteaAdapter).deleteRepository("spike-admin", "demo");
    }

    @Test
    void throwsIncompleteWhenRepositoryDisappearsDuringMigration() {
        when(giteaAdapter.getRepository("spike-admin", "demo"))
                .thenThrow(new BusinessException("CODE_HOST_RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "代码托管资源不存在", "repo gone"));

        assertThatThrownBy(() -> verifier().verifyMigrated(repo(true)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("REPOSITORY_MIGRATION_INCOMPLETE");
    }
}
