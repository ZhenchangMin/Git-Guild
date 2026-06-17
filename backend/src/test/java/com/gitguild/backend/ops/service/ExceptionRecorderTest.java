package com.gitguild.backend.ops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.ops.domain.ExceptionCategory;
import com.gitguild.backend.ops.domain.ExceptionStatus;
import com.gitguild.backend.ops.domain.PlatformException;
import com.gitguild.backend.ops.repository.PlatformExceptionRepository;
import com.gitguild.backend.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExceptionRecorderTest {

    @Mock private PlatformExceptionRepository exceptionRepository;

    private CodeRepository repository() {
        User owner = org.mockito.Mockito.mock(User.class);
        CodeRepository repo = new CodeRepository(owner, "demo", "GITEA", "http://localhost:3000/spike-admin/demo");
        repo.setRepositoryId(1001L);
        return repo;
    }

    @Test
    void createsNewSyncExceptionWhenNoneOpen() {
        when(exceptionRepository.findFirstByRepositoryIdAndCategoryAndStatusInOrderByDetectedAtDesc(
                eq(1001L), eq(ExceptionCategory.SYNC), any())).thenReturn(Optional.empty());
        when(exceptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlatformException result = new ExceptionRecorder(exceptionRepository)
                .recordSyncFailure(repository(), "504 Gateway Timeout");

        assertThat(result.getCategory()).isEqualTo(ExceptionCategory.SYNC);
        assertThat(result.getStatus()).isEqualTo(ExceptionStatus.UNRESOLVED);
        assertThat(result.isRetryable()).isTrue();
        assertThat(result.getRepositoryId()).isEqualTo(1001L);
        assertThat(result.getLogs()).hasSize(1);
        assertThat(result.getLogs().get(0)).contains("504 Gateway Timeout");
    }

    @Test
    void appendsToExistingOpenExceptionInsteadOfCreating() {
        PlatformException existing = new PlatformException(
                ExceptionCategory.SYNC, "仓库同步失败", "demo 同步失败",
                1001L, "demo", null, "旧原因", "影响", "建议", true);
        when(exceptionRepository.findFirstByRepositoryIdAndCategoryAndStatusInOrderByDetectedAtDesc(
                eq(1001L), eq(ExceptionCategory.SYNC), any())).thenReturn(Optional.of(existing));
        when(exceptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlatformException result = new ExceptionRecorder(exceptionRepository)
                .recordSyncFailure(repository(), "再次 504");

        // 复用既有未闭环异常：只追加日志，不新建
        assertThat(result).isSameAs(existing);
        assertThat(result.getLogs()).hasSize(1);
        assertThat(result.getLogs().get(0)).contains("再次 504");
        ArgumentCaptor<PlatformException> captor = ArgumentCaptor.forClass(PlatformException.class);
        verify(exceptionRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void doesNotQueryWithNullStatusList() {
        // 健壮性：去重查询应始终传入未闭环状态集合，而非 null
        when(exceptionRepository.findFirstByRepositoryIdAndCategoryAndStatusInOrderByDetectedAtDesc(
                eq(1001L), eq(ExceptionCategory.SYNC), eq(List.of(ExceptionStatus.UNRESOLVED, ExceptionStatus.IN_REVIEW))))
                .thenReturn(Optional.empty());
        when(exceptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        new ExceptionRecorder(exceptionRepository).recordSyncFailure(repository(), "x");

        verify(exceptionRepository, never())
                .findFirstByRepositoryIdAndCategoryAndStatusInOrderByDetectedAtDesc(any(), any(), eq(null));
    }
}
