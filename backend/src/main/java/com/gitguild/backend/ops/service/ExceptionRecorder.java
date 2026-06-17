package com.gitguild.backend.ops.service;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.ops.domain.ExceptionCategory;
import com.gitguild.backend.ops.domain.ExceptionStatus;
import com.gitguild.backend.ops.domain.PlatformException;
import com.gitguild.backend.ops.repository.PlatformExceptionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 异常写侧入口：由各业务流程在「检测到真实失败」时调用，落库供异常处理中心展示。
 *
 * <p>设计为一个独立的轻量 Bean，让 codehost 等模块只依赖一个 {@code record*} 方法，
 * 不感知异常实体细节——异常采集与各业务逻辑解耦。
 */
@Service
public class ExceptionRecorder {

    private static final String SYNC_TYPE = "仓库同步失败";

    private final PlatformExceptionRepository exceptionRepository;

    public ExceptionRecorder(PlatformExceptionRepository exceptionRepository) {
        this.exceptionRepository = exceptionRepository;
    }

    /**
     * 记录一次仓库同步失败。
     *
     * <p>去重：同一仓库若已有未闭环（UNRESOLVED/IN_REVIEW）的 SYNC 异常，则只向其追加日志、
     * 刷新发现时间，不再新建——避免连续失败刷爆队列。否则新建一条 UNRESOLVED 记录。
     *
     * @param repository 同步失败的仓库
     * @param detail     失败细节（异常 message），写入 reason 与日志
     */
    // REQUIRES_NEW：在独立事务里提交，即便调用方因同步失败回滚，异常记录也能留存。
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PlatformException recordSyncFailure(CodeRepository repository, String detail) {
        String safeDetail = detail == null || detail.isBlank() ? "外部代码托管平台请求失败" : detail.trim();

        PlatformException existing = exceptionRepository
                .findFirstByRepositoryIdAndCategoryAndStatusInOrderByDetectedAtDesc(
                        repository.getRepositoryId(),
                        ExceptionCategory.SYNC,
                        List.of(ExceptionStatus.UNRESOLVED, ExceptionStatus.IN_REVIEW))
                .orElse(null);

        if (existing != null) {
            existing.appendLog("sync failed · " + safeDetail);
            return exceptionRepository.save(existing);
        }

        PlatformException exception = new PlatformException(
                ExceptionCategory.SYNC,
                SYNC_TYPE,
                repository.getName() + " 同步失败",
                repository.getRepositoryId(),
                repository.getName(),
                null,
                "从外部代码托管平台同步该仓库的 Issue 时失败：" + safeDetail,
                "该仓库关联任务的 Issue / PR 状态停留在上一次成功同步的快照，新提交可能无法被识别。",
                "保留上一次成功同步的数据；外部接口恢复后在异常处理中心发起重试，不要清空已有快照。",
                true);
        exception.appendLog("sync failed · " + safeDetail);
        return exceptionRepository.save(exception);
    }
}
