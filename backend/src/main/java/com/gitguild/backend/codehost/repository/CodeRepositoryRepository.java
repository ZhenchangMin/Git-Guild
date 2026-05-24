package com.gitguild.backend.codehost.repository;

import com.gitguild.backend.codehost.domain.CodeRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRepositoryRepository extends JpaRepository<CodeRepository, Long> {
}
