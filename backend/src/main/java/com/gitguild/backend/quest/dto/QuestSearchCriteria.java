package com.gitguild.backend.quest.dto;

import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.QuestStatus;
import java.util.List;

public record QuestSearchCriteria(
        String keyword,
        Long categoryId,
        List<Long> tagIds,
        Difficulty difficulty,
        List<String> techStack,
        QuestStatus status,
        String sortBy,
        String sortOrder,
        int page,
        int size) {
}
