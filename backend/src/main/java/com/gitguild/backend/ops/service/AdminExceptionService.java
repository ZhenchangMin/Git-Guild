package com.gitguild.backend.ops.service;

import com.gitguild.backend.ops.domain.PlatformException;
import com.gitguild.backend.ops.dto.ResolveExceptionRequest;
import java.util.List;

/**
 * 异常处理中心的管理员读侧与处置入口。所有方法均假设调用方已是 ADMIN（由控制器 {@code @PreAuthorize} 强制）。
 */
public interface AdminExceptionService {

    /** 按分类 / 状态筛选异常（均可空，空表示不限），按发现时间倒序。 */
    List<PlatformException> listExceptions(String category, String status);

    PlatformException getException(Long exceptionId);

    /** 执行处置动作：按动作语义流转为 RESOLVED 或 IN_REVIEW，并写入处置说明与日志。 */
    PlatformException resolve(Long adminId, Long exceptionId, ResolveExceptionRequest request);

    /** 对可重试的 SYNC 异常重新发起同步：成功置 RESOLVED，失败保持 IN_REVIEW 并记日志。 */
    PlatformException retry(Long adminId, Long exceptionId);
}
