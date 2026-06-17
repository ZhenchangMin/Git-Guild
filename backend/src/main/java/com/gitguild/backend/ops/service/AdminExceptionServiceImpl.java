package com.gitguild.backend.ops.service;

import com.gitguild.backend.codehost.service.RepositorySyncService;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.ops.domain.ExceptionCategory;
import com.gitguild.backend.ops.domain.ExceptionStatus;
import com.gitguild.backend.ops.domain.PlatformException;
import com.gitguild.backend.ops.dto.ResolveExceptionRequest;
import com.gitguild.backend.ops.repository.PlatformExceptionRepository;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AdminExceptionService} 默认实现。
 */
@Service
public class AdminExceptionServiceImpl implements AdminExceptionService {

    /** 这些动作只是「介入但等待外部结果」，处置后置为 IN_REVIEW（需复核）；其余动作直接闭环为 RESOLVED。 */
    private static final Set<String> IN_REVIEW_ACTIONS = Set.of("REQUEST_FIX", "REQUEST_GRANT", "BLOCK");

    private final PlatformExceptionRepository exceptionRepository;
    private final RepositorySyncService repositorySyncService;

    public AdminExceptionServiceImpl(
            PlatformExceptionRepository exceptionRepository,
            RepositorySyncService repositorySyncService) {
        this.exceptionRepository = exceptionRepository;
        this.repositorySyncService = repositorySyncService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformException> listExceptions(String category, String status) {
        return exceptionRepository.search(parseCategory(category), parseStatus(status));
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformException getException(Long exceptionId) {
        return findOrThrow(exceptionId);
    }

    @Override
    @Transactional
    public PlatformException resolve(Long adminId, Long exceptionId, ResolveExceptionRequest request) {
        PlatformException exception = findOrThrow(exceptionId);
        if (exception.getStatus() == ExceptionStatus.RESOLVED) {
            throw new BusinessException("EXCEPTION_ALREADY_RESOLVED", HttpStatus.CONFLICT,
                    "该异常已解决，无需重复处理", "exceptionId=" + exceptionId);
        }
        String action = request.action().trim();
        String comment = request.comment().trim();

        boolean keepOpen = IN_REVIEW_ACTIONS.contains(action);
        exception.appendLog("resolved by admin#" + adminId + " · " + action);
        if (keepOpen) {
            exception.markInReview(adminId, action, comment);
        } else {
            exception.markResolved(adminId, action, comment);
        }
        return exceptionRepository.save(exception);
    }

    @Override
    @Transactional
    public PlatformException retry(Long adminId, Long exceptionId) {
        PlatformException exception = findOrThrow(exceptionId);
        if (!exception.isRetryable() || exception.getRepositoryId() == null) {
            throw new BusinessException("EXCEPTION_NOT_RETRYABLE", HttpStatus.BAD_REQUEST,
                    "该异常不支持重试", "exceptionId=" + exceptionId);
        }
        exception.appendLog("retry triggered by admin#" + adminId);
        try {
            repositorySyncService.syncRepository(exception.getRepositoryId());
            // 同步成功 → 异常闭环。
            exception.appendLog("retry succeeded · repository re-synced");
            exception.markResolved(adminId, "RETRY", "管理员发起重试，仓库已重新同步成功。");
        } catch (RuntimeException ex) {
            // 仍失败 → 保持需复核，等待外部接口恢复。注意 syncRepository 会再登记/更新一条 SYNC 异常。
            exception.appendLog("retry failed · " + safe(ex.getMessage()));
            exception.markInReview(adminId, "RETRY", "管理员发起重试但仍失败，保留上一次成功同步数据，待外部接口恢复。");
        }
        return exceptionRepository.save(exception);
    }

    private PlatformException findOrThrow(Long exceptionId) {
        return exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new BusinessException("EXCEPTION_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "异常记录不存在", "exceptionId=" + exceptionId));
    }

    private ExceptionCategory parseCategory(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        try {
            return ExceptionCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                    "异常分类不合法", "category=" + category);
        }
    }

    private ExceptionStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return ExceptionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                    "异常状态不合法", "status=" + status);
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "未知错误" : value;
    }
}
