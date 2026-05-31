package com.gitguild.backend.review.service;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.service.CodePullRequestSyncService;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.dto.SubmissionDraftResponse;
import com.gitguild.backend.review.dto.SubmissionDraftResponse.PullRequestOption;
import com.gitguild.backend.review.dto.SubmissionDraftResponse.RepositoryBrief;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionDraftServiceImpl implements SubmissionDraftService {

    private final QuestRepository questRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final CodePullRequestSyncService pullRequestSyncService;

    public SubmissionDraftServiceImpl(
            QuestRepository questRepository,
            QuestAssignmentRepository assignmentRepository,
            CodePullRequestSyncService pullRequestSyncService) {
        this.questRepository = questRepository;
        this.assignmentRepository = assignmentRepository;
        this.pullRequestSyncService = pullRequestSyncService;
    }

    @Override
    @Transactional
    public SubmissionDraftResponse getDraft(Long questId, Long currentUserId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new BusinessException("QUEST_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Quest not found", "questId=" + questId));

        // 只有 ACTIVE Assignee 能打开提交柜台（与 018 createSubmission 的归属校验口径一致）
        assignmentRepository
                .findByQuestAndAssigneeUserIdAndStatus(quest, currentUserId, AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("ASSIGNMENT_NOT_FOUND", HttpStatus.FORBIDDEN,
                        "Current user has no active assignment for this quest",
                        "Only an active assignee can open the submission draft"));

        CodeRepository repository = quest.getRepository();
        List<CodePullRequest> pullRequests = pullRequestSyncService.syncRepositoryPullRequests(repository);

        return new SubmissionDraftResponse(
                quest.getQuestId(),
                new RepositoryBrief(repository.getRepositoryId(), repository.getName(), repository.getSourceUrl()),
                repository.getDefaultBranch(),
                pullRequests.stream().map(this::toOption).toList(),
                quest.getCompletionCriteria());
    }

    private PullRequestOption toOption(CodePullRequest pr) {
        return new PullRequestOption(
                pr.getPullRequestId(),
                pr.getExternalPrId(),
                pr.getTitle(),
                pr.getStatus(),
                pr.getSourceBranch(),
                pr.getTargetBranch(),
                pr.getExternalUrl());
    }
}
