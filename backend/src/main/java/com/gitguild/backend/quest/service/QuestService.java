package com.gitguild.backend.quest.service;

import com.gitguild.backend.quest.dto.CreateQuestRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AdminReviewResponse;
import com.gitguild.backend.quest.dto.QuestResponses.AssignmentResponse;
import com.gitguild.backend.quest.dto.QuestResponses.CreateQuestResponse;
import com.gitguild.backend.quest.dto.QuestResponses.MyAssignmentResponse;
import com.gitguild.backend.quest.dto.QuestResponses.MyAssignmentsResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestDetailResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestSummaryResponse;
import com.gitguild.backend.quest.dto.QuestResponses.SubmitQuestResponse;
import com.gitguild.backend.quest.dto.QuestSearchCriteria;
import java.util.List;

public interface QuestService {

    CreateQuestResponse createQuest(Long publisherId, CreateQuestRequest request);

    SubmitQuestResponse submitQuest(Long questId, Long publisherId);

    /**
     * 删除发布者自己的委托（仅限可删除的终态：草稿 / 被退回 / 已下架），并级联清理其牵连数据。
     * 进行中、待审、已上架、已完成等"活跃或含已发放 XP"的委托不允许删除。
     */
    void deleteQuest(Long questId, Long publisherId);

    QuestPageResponse searchQuests(QuestSearchCriteria criteria);

    QuestDetailResponse getQuestDetail(Long questId, Long currentUserId);

    AssignmentResponse acceptQuest(Long questId, Long assigneeId);

    AssignmentResponse ensureTaskBranch(Long questId, Long assigneeId);

    List<MyAssignmentResponse> listMyActiveAssignments(Long assigneeId);

    MyAssignmentsResponse getMyAssignments(Long userId);

    /** 当前维护者发布的全部委托（含 DRAFT / 待审核 等所有状态），用于「我发布的委托」视图。 */
    List<QuestSummaryResponse> listMyPublishedQuests(Long publisherId);

    /** 某委托的 Admin 审核记录（含驳回原因）；仅发布者或管理员可查，发布者只见对其可见的意见。 */
    List<AdminReviewResponse> listQuestReviews(Long questId, Long currentUserId);
}
