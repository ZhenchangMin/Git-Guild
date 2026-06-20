package com.gitguild.backend.ops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.service.RepositorySyncService;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.ops.domain.ExceptionCategory;
import com.gitguild.backend.ops.domain.ExceptionStatus;
import com.gitguild.backend.ops.domain.PlatformException;
import com.gitguild.backend.ops.dto.ResolveExceptionRequest;
import com.gitguild.backend.ops.repository.PlatformExceptionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminExceptionServiceImplTest {

    @Mock private PlatformExceptionRepository exceptionRepository;
    @Mock private RepositorySyncService repositorySyncService;

    private AdminExceptionServiceImpl service() {
        return new AdminExceptionServiceImpl(exceptionRepository, repositorySyncService);
    }

    private PlatformException syncException() {
        return new PlatformException(
                ExceptionCategory.SYNC, "仓库同步失败", "demo 同步失败",
                1001L, "demo", null, "504", "影响", "建议", true);
    }

    @Test
    void resolveWithClosingActionMarksResolved() {
        PlatformException exception = syncException();
        when(exceptionRepository.findById(7L)).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlatformException result = service().resolve(99L, 7L,
                new ResolveExceptionRequest("KEEP_LAST", "保留上次成功数据"));

        // KEEP_LAST 直接闭环
        assertThat(result.getStatus()).isEqualTo(ExceptionStatus.RESOLVED);
        assertThat(result.getResolvedBy()).isEqualTo(99L);
        assertThat(result.getResolutionAction()).isEqualTo("KEEP_LAST");
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void resolveWithAwaitingActionMarksInReview() {
        PlatformException exception = syncException();
        when(exceptionRepository.findById(7L)).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlatformException result = service().resolve(99L, 7L,
                new ResolveExceptionRequest("REQUEST_GRANT", "要求补授权"));

        // REQUEST_GRANT 仅介入、等待外部 → 需复核
        assertThat(result.getStatus()).isEqualTo(ExceptionStatus.IN_REVIEW);
        assertThat(result.getResolvedAt()).isNull();
    }

    @Test
    void resolveRejectsAlreadyResolved() {
        PlatformException exception = syncException();
        exception.markResolved(1L, "MARK_RESOLVED", "done");
        when(exceptionRepository.findById(7L)).thenReturn(Optional.of(exception));

        assertThatThrownBy(() -> service().resolve(99L, 7L,
                new ResolveExceptionRequest("MARK_RESOLVED", "再次处理")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("EXCEPTION_ALREADY_RESOLVED");
    }

    @Test
    void retrySuccessClosesException() {
        PlatformException exception = syncException();
        when(exceptionRepository.findById(7L)).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repositorySyncService.syncRepository(1001L)).thenReturn(null); // 同步成功

        PlatformException result = service().retry(99L, 7L);

        assertThat(result.getStatus()).isEqualTo(ExceptionStatus.RESOLVED);
        assertThat(result.getResolutionAction()).isEqualTo("RETRY");
        verify(repositorySyncService).syncRepository(1001L);
    }

    @Test
    void retryFailureKeepsInReview() {
        PlatformException exception = syncException();
        when(exceptionRepository.findById(7L)).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repositorySyncService.syncRepository(1001L))
                .thenThrow(new RuntimeException("504 Gateway Timeout"));

        PlatformException result = service().retry(99L, 7L);

        // 仍失败 → 需复核
        assertThat(result.getStatus()).isEqualTo(ExceptionStatus.IN_REVIEW);
    }

    @Test
    void retryRejectsNonRetryable() {
        PlatformException relation = new PlatformException(
                ExceptionCategory.RELATION, "PR 链接不匹配", "x",
                null, null, "QST-1", "原因", null, null, false);
        when(exceptionRepository.findById(7L)).thenReturn(Optional.of(relation));

        assertThatThrownBy(() -> service().retry(99L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("EXCEPTION_NOT_RETRYABLE");
        verify(repositorySyncService, never()).syncRepository(any());
    }
}
