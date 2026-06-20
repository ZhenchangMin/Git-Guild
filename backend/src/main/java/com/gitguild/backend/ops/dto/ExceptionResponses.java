package com.gitguild.backend.ops.dto;

import com.gitguild.backend.ops.domain.PlatformException;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 异常处理中心对外响应 DTO。状态/分类以枚举名返回，展示用的中文标签与色调由前端映射。
 */
public final class ExceptionResponses {

    private ExceptionResponses() {
    }

    public record ExceptionView(
            Long exceptionId,
            String category,
            String type,
            String title,
            String status,
            Long repositoryId,
            String repositoryName,
            String relatedQuest,
            String reason,
            String impact,
            String suggestion,
            boolean retryable,
            String resolutionAction,
            String resolutionComment,
            OffsetDateTime detectedAt,
            OffsetDateTime resolvedAt,
            List<String> logs) {

        public static ExceptionView from(PlatformException exception) {
            return new ExceptionView(
                    exception.getExceptionId(),
                    exception.getCategory().name(),
                    exception.getType(),
                    exception.getTitle(),
                    exception.getStatus().name(),
                    exception.getRepositoryId(),
                    exception.getRepositoryName(),
                    exception.getRelatedQuest(),
                    exception.getReason(),
                    exception.getImpact(),
                    exception.getSuggestion(),
                    exception.isRetryable(),
                    exception.getResolutionAction(),
                    exception.getResolutionComment(),
                    exception.getDetectedAt(),
                    exception.getResolvedAt(),
                    List.copyOf(exception.getLogs()));
        }
    }

    public record ExceptionListResponse(List<ExceptionView> items, int total) {
        public static ExceptionListResponse of(List<PlatformException> exceptions) {
            List<ExceptionView> views = exceptions.stream().map(ExceptionView::from).toList();
            return new ExceptionListResponse(views, views.size());
        }
    }
}
