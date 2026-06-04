package com.gitguild.backend.codehost.repository;

import com.gitguild.backend.codehost.domain.CodeRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRepositoryRepository extends JpaRepository<CodeRepository, Long> {

    Optional<CodeRepository> findFirstByHostTypeAndSourceUrlOrderByRepositoryIdAsc(String hostType, String sourceUrl);
}
