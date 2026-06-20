package com.gitguild.backend.codehost.repository;

import com.gitguild.backend.codehost.domain.CodeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRepositoryRepository extends JpaRepository<CodeRepository, Long> {

    /**
     * 按仓库 owner 列出所有仓库，供 Guild Master 管理自己接入的仓库。
     */
    List<CodeRepository> findByOwnerUserId(Long userId);

    Optional<CodeRepository> findFirstByHostTypeAndSourceUrlOrderByRepositoryIdAsc(String hostType, String sourceUrl);
}
