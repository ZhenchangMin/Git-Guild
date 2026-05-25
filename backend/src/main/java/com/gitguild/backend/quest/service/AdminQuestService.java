package com.gitguild.backend.quest.service;

import com.gitguild.backend.quest.dto.AdminReviewRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AdminQuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.AdminReviewResponse;

public interface AdminQuestService {

    AdminQuestPageResponse listPendingQuests(int page, int size);

    AdminReviewResponse reviewQuest(Long questId, Long adminId, AdminReviewRequest request);
}
