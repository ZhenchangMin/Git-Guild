package com.gitguild.backend.codehost.repository;

import com.gitguild.backend.codehost.domain.CodeHostAccountBinding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeHostAccountBindingRepository extends JpaRepository<CodeHostAccountBinding, Long> {

    Optional<CodeHostAccountBinding> findByUserUserIdAndHostType(Long userId, String hostType);
}
