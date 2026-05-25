package com.gitguild.backend.codehost.repository;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodePullRequestRepository extends JpaRepository<CodePullRequest, Long> {

    Optional<CodePullRequest> findByRepositoryRepositoryIdAndExternalPrId(Long repositoryId, String externalPrId);
}
