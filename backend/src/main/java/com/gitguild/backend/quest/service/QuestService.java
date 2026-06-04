package com.gitguild.backend.quest.service;

import com.gitguild.backend.quest.dto.CreateQuestRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AssignmentResponse;
import com.gitguild.backend.quest.dto.QuestResponses.CreateQuestResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestDetailResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.SubmitQuestResponse;
import com.gitguild.backend.quest.dto.QuestSearchCriteria;

public interface QuestService {

    CreateQuestResponse createQuest(Long publisherId, CreateQuestRequest request);

    SubmitQuestResponse submitQuest(Long questId, Long publisherId);

    QuestPageResponse searchQuests(QuestSearchCriteria criteria);

    QuestDetailResponse getQuestDetail(Long questId, Long currentUserId);

    AssignmentResponse acceptQuest(Long questId, Long assigneeId);

    AssignmentResponse ensureTaskBranch(Long questId, Long assigneeId);
}
