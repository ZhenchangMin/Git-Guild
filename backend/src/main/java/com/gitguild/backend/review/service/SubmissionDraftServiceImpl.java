package com.gitguild.backend.review.service;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.quest.service.QuestTaskBranchService;
import com.gitguild.backend.review.dto.SubmissionDraftResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SubmissionDraftService} 的实现，编排"提交柜台开页"的一步式数据准备。
 *
 * <p><b>分支语义（平台代理 PR 后）：</b>提交时由平台依据冒险家的 task branch 自动创建/合并 PR，
 * 因此本类不再同步/回传 PR 候选；而是确保并回传该接取记录的 task branch（{@code branch} 字段），
 * 让冒险家清楚“把提交推到哪条分支、提交即基于它建 PR”。{@code pullRequests} 恒为空列表（字段暂留以兼容前端）。
 *
 * <p><b>业务不变量：</b>仅该 Quest 的 {@code ACTIVE} 接取记录持有者（即 Adventurer 本人）可取草稿，
 * 与 {@code SubmissionServiceImpl.createSubmission} 的成果提交归属校验口径一致。
 *
 * <p><b>边界错误模式：</b>Quest 不存在抛 {@code QUEST_NOT_FOUND}；调用者无有效接取记录抛
 * {@code ASSIGNMENT_NOT_FOUND}。task branch 确保是 best-effort：Gitea 暂不可达时降级回传仓库默认分支，
 * 不让开页因外部依赖而 500。
 */
@Service
public class SubmissionDraftServiceImpl implements SubmissionDraftService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionDraftServiceImpl.class);

    private final QuestRepository questRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final QuestTaskBranchService taskBranchService;

    public SubmissionDraftServiceImpl(
            QuestRepository questRepository,
            QuestAssignmentRepository assignmentRepository,
            QuestTaskBranchService taskBranchService) {
        this.questRepository = questRepository;
        this.assignmentRepository = assignmentRepository;
        this.taskBranchService = taskBranchService;
    }

    @Override
    @Transactional
    public SubmissionDraftResponse getDraft(Long questId, Long currentUserId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new BusinessException("QUEST_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Quest not found", "questId=" + questId));

        // 只有 ACTIVE Assignee 能打开提交柜台（与 018 createSubmission 的归属校验口径一致）
        QuestAssignment assignment = assignmentRepository
                .findByQuestAndAssigneeUserIdAndStatus(quest, currentUserId, AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("ASSIGNMENT_NOT_FOUND", HttpStatus.FORBIDDEN,
                        "Current user has no active assignment for this quest",
                        "Only an active assignee can open the submission draft"));

        CodeRepository repository = quest.getRepository();
        String branch = resolveTaskBranch(assignment, repository);

        return new SubmissionDraftResponse(
                quest.getQuestId(),
                new SubmissionDraftResponse.RepositoryBrief(repository.getRepositoryId(), repository.getName(), repository.getSourceUrl()),
                branch,
                List.of(),
                quest.getCompletionCriteria());
    }

    /** best-effort 确保 task branch；Gitea 暂不可达时降级为仓库默认分支，避免开页 500。 */
    private String resolveTaskBranch(QuestAssignment assignment, CodeRepository repository) {
        try {
            return taskBranchService.ensureTaskBranch(assignment);
        } catch (BusinessException ex) {
            log.warn("提交草稿确保 task branch 失败，降级为默认分支 assignmentId={}, code={}",
                    assignment.getAssignmentId(), ex.getCode());
            return repository.getDefaultBranch();
        }
    }
}
