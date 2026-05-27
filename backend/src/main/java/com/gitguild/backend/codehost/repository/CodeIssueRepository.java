package com.gitguild.backend.codehost.repository;

import com.gitguild.backend.codehost.domain.CodeIssue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeIssueRepository extends JpaRepository<CodeIssue, Long> {

    Optional<CodeIssue> findByIssueIdAndRepositoryRepositoryId(Long issueId, Long repositoryId);
}
