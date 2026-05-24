package com.gitguild.backend.quest.repository;

import com.gitguild.backend.quest.domain.QuestTag;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestTagRepository extends JpaRepository<QuestTag, Long> {

    List<QuestTag> findByTagIdIn(Collection<Long> tagIds);
}
