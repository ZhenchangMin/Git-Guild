package com.gitguild.backend.ops.repository;

import com.gitguild.backend.ops.domain.ExceptionCategory;
import com.gitguild.backend.ops.domain.ExceptionStatus;
import com.gitguild.backend.ops.domain.PlatformException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 平台异常数据访问。
 */
public interface PlatformExceptionRepository extends JpaRepository<PlatformException, Long> {

    /** 全量按发现时间倒序（异常处理中心默认视图）。 */
    List<PlatformException> findAllByOrderByDetectedAtDesc();

    List<PlatformException> findByCategoryOrderByDetectedAtDesc(ExceptionCategory category);

    List<PlatformException> findByStatusOrderByDetectedAtDesc(ExceptionStatus status);

    List<PlatformException> findByCategoryAndStatusOrderByDetectedAtDesc(
            ExceptionCategory category, ExceptionStatus status);

    /**
     * 取某仓库在某分类下「仍未闭环」的异常，用于去重——优先复用最新一条。
     */
    Optional<PlatformException> findFirstByRepositoryIdAndCategoryAndStatusInOrderByDetectedAtDesc(
            Long repositoryId, ExceptionCategory category, List<ExceptionStatus> statuses);

    default List<PlatformException> search(ExceptionCategory category, ExceptionStatus status) {
        if (category != null && status != null) {
            return findByCategoryAndStatusOrderByDetectedAtDesc(category, status);
        }
        if (category != null) {
            return findByCategoryOrderByDetectedAtDesc(category);
        }
        if (status != null) {
            return findByStatusOrderByDetectedAtDesc(status);
        }
        return findAll(Sort.by(Sort.Direction.DESC, "detectedAt"));
    }
}
