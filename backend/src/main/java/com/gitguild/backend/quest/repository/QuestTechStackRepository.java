package com.gitguild.backend.quest.repository;

import com.gitguild.backend.quest.domain.QuestTechStack;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestTechStackRepository extends JpaRepository<QuestTechStack, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<QuestTechStack> findByNameIgnoreCase(String name);
}
