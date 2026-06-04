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

/**
 * {@link SubmissionDraftService} 的实现，编排"提交柜台开页"的一步式数据准备。
 *
 * <p><b>隐藏的复杂度（反直觉点）：</b>PR 同步被刻意内聚在此读路径里——Adventurer 一打开提交柜台，
 * 本类即触发 {@link com.gitguild.backend.codehost.service.CodePullRequestSyncService} 把该 Quest
 * 所属仓库的 PR 懒同步落库，再回传候选列表。前端因此"开页即拿到带本地 {@code pullRequestId} 的候选 PR、
 * 选一个直接提交"，无需单独的仓库同步调用（详见《P4-018 提交草稿端点与 PR 同步空洞修复设计》决策 2）。
 *
 * <p><b>业务不变量：</b>仅该 Quest 的 {@code ACTIVE} 接取记录持有者（即 Adventurer 本人）可取草稿，
 * 与 {@code SubmissionServiceImpl.createSubmission} 的成果提交归属校验口径一致。
 *
 * <p><b>边界错误模式：</b>Quest 不存在抛 {@code QUEST_NOT_FOUND}；调用者无有效接取记录抛
 * {@code ASSIGNMENT_NOT_FOUND}。返回的 {@code branch} 因接取记录无分支字段，暂回退为仓库默认分支
 * （见设计文档延期项 B）。
 */
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
                new SubmissionDraftResponse.RepositoryBrief(repository.getRepositoryId(), repository.getName(), repository.getSourceUrl()),
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
