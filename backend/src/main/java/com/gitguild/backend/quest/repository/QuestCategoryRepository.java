package com.gitguild.backend.quest.repository;

import com.gitguild.backend.quest.domain.QuestCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestCategoryRepository extends JpaRepository<QuestCategory, Long> {

    boolean existsByNameIgnoreCase(String name);
}
