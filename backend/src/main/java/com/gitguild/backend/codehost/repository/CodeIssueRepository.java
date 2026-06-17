package com.gitguild.backend.codehost.repository;

import com.gitguild.backend.codehost.domain.CodeIssue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeIssueRepository extends JpaRepository<CodeIssue, Long> {

    Optional<CodeIssue> findByIssueIdAndRepositoryRepositoryId(Long issueId, Long repositoryId);

    Optional<CodeIssue> findByRepositoryRepositoryIdAndExternalIssueId(Long repositoryId, String externalIssueId);

    /** 某仓库下的全部 Issue，供仓库级联删除清理使用。 */
    List<CodeIssue> findByRepositoryRepositoryId(Long repositoryId);
}
