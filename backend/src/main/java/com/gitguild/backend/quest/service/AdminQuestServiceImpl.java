package com.gitguild.backend.quest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.AdminDecision;
import com.gitguild.backend.quest.domain.AdminReviewRecord;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.dto.AdminReviewRequest;
import com.gitguild.backend.quest.dto.ChecklistItemDto;
import com.gitguild.backend.quest.dto.QuestResponses.AdminQuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.AdminQuestSummaryResponse;
import com.gitguild.backend.quest.dto.QuestResponses.AdminReviewHistoryItem;
import com.gitguild.backend.quest.dto.QuestResponses.AdminReviewResponse;
import com.gitguild.backend.quest.dto.QuestResponses.UserBrief;
import com.gitguild.backend.quest.repository.AdminReviewRecordRepository;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminQuestServiceImpl implements AdminQuestService {

    /** APPROVE_PUBLISH 必须提交的检查清单项数（清晰度 3 项 + 合规性 3 项）。 */
    private static final int REQUIRED_CHECKLIST_SIZE = 6;

    private final QuestRepository questRepository;
    private final AdminReviewRecordRepository reviewRecordRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AdminQuestServiceImpl(
            QuestRepository questRepository,
            AdminReviewRecordRepository reviewRecordRepository,
            QuestAssignmentRepository assignmentRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.questRepository = questRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /** 管理员控制台「全部」视图覆盖的审核流水线状态（排除尚未提交的 DRAFT）。 */
    private static final List<QuestStatus> PIPELINE_STATUSES = List.of(
            QuestStatus.PENDING_ADMIN_REVIEW,
            QuestStatus.PUBLISHED,
            QuestStatus.REJECTED,
            QuestStatus.CLOSED);

    @Override
    @Transactional(readOnly = true)
    public AdminQuestPageResponse listQuests(String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(
                safePage - 1, safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));

        QuestStatus filter = parseStatus(status);
        Page<Quest> result = filter == null
                ? questRepository.findByStatusIn(PIPELINE_STATUSES, pageable)
                : questRepository.findByStatus(filter, pageable);

        return new AdminQuestPageResponse(
                result.getContent().stream().map(this::toSummary).toList(),
                safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    /** 解析状态筛选：null/空/ALL → 不限（返回全部流水线状态）；非法值 → 400。 */
    private QuestStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return QuestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                    "任务状态不合法", "status=" + status);
        }
    }

    @Override
    @Transactional
    public AdminReviewResponse reviewQuest(Long questId, Long adminId, AdminReviewRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "用户不存在", "userId=" + adminId));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "当前用户无权限", "只有管理员可以审核任务");
        }

        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new BusinessException("QUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "任务不存在", "questId=" + questId));

        if (request.getDecision() == AdminDecision.APPROVE_PUBLISH) {
            validateChecklist(request.getChecklist());
        }

        applyDecision(quest, request.getDecision());

        AdminReviewRecord record = new AdminReviewRecord(
                quest, admin, request.getDecision(), request.getReason(), request.isVisibleToPublisher(),
                toChecklistJson(request.getChecklist()));
        reviewRecordRepository.save(record);
        questRepository.save(quest);

        return new AdminReviewResponse(
                record.getAdminReviewId(),
                quest.getQuestId(),
                admin.getUserId(),
                record.getDecision(),
                record.getReason(),
                quest.getStatus(),
                record.getReviewedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminReviewHistoryItem> listReviewHistory(Long questId) {
        if (!questRepository.existsById(questId)) {
            throw new BusinessException("QUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "任务不存在", "questId=" + questId);
        }
        // 仓库方法按审核时刻降序返回，这里反转为升序，匹配时间线「自上而下越早」的展示顺序。
        List<AdminReviewRecord> descending = reviewRecordRepository.findByQuestQuestIdOrderByReviewedAtDesc(questId);
        List<AdminReviewHistoryItem> ascending = new ArrayList<>(descending.size());
        for (int i = descending.size() - 1; i >= 0; i--) {
            AdminReviewRecord record = descending.get(i);
            ascending.add(new AdminReviewHistoryItem(
                    record.getAdminReviewId(),
                    record.getDecision(),
                    record.getReason(),
                    new UserBrief(record.getAdmin().getUserId(), record.getAdmin().getUsername()),
                    record.isVisibleToPublisher(),
                    record.getReviewedAt(),
                    fromChecklistJson(record.getChecklistJson())));
        }
        return ascending;
    }

    /**
     * APPROVE_PUBLISH 必须随附全部 6 项检查清单，仅做结构校验——这些检查项只是提示管理员审阅时
     * 关注的要点展示，不需要管理员逐项操作，也不再要求逐项必须为「通过」。
     */
    private void validateChecklist(List<ChecklistItemDto> checklist) {
        boolean incomplete = checklist == null || checklist.size() != REQUIRED_CHECKLIST_SIZE;
        if (incomplete) {
            throw new BusinessException("CHECKLIST_INCOMPLETE", HttpStatus.UNPROCESSABLE_ENTITY,
                    "请先完成全部审核检查项后再通过上架",
                    "checklistSize=" + (checklist == null ? 0 : checklist.size()));
        }
    }

    private String toChecklistJson(List<ChecklistItemDto> checklist) {
        if (checklist == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(checklist);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private List<ChecklistItemDto> fromChecklistJson(String checklistJson) {
        if (checklistJson == null || checklistJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(checklistJson, new TypeReference<List<ChecklistItemDto>>() { });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private void applyDecision(Quest quest, AdminDecision decision) {
        switch (decision) {
            case APPROVE_PUBLISH -> {
                if (!quest.canBeApprovedOrRejected()) {
                    throw new BusinessException("QUEST_NOT_REVIEWABLE", HttpStatus.CONFLICT,
                            "当前任务状态不允许管理员审核", "currentStatus=" + quest.getStatus());
                }
                quest.approve();
            }
            case REJECT_PUBLISH -> {
                if (!quest.canBeApprovedOrRejected()) {
                    throw new BusinessException("QUEST_NOT_REVIEWABLE", HttpStatus.CONFLICT,
                            "当前任务状态不允许管理员审核", "currentStatus=" + quest.getStatus());
                }
                quest.reject();
            }
            case TAKE_DOWN -> {
                if (!quest.canBeTakenDown()) {
                    throw new BusinessException("QUEST_ALREADY_CLOSED", HttpStatus.CONFLICT,
                            "任务已下架", "questId=" + quest.getQuestId());
                }
                quest.takeDown();
                // Quest 强制下架时，同步取消所有 ACTIVE 接取记录，防止出现已 CLOSED 的 Quest 仍有孤立 Assignment。
                assignmentRepository.findByQuestAndStatus(quest, AssignmentStatus.ACTIVE)
                        .forEach(a -> {
                            a.cancel();
                            assignmentRepository.save(a);
                        });
            }
            case REOPEN -> {
                if (!quest.canBeReopened()) {
                    throw new BusinessException("QUEST_NOT_REOPENABLE", HttpStatus.CONFLICT,
                            "当前任务状态不允许重新上架", "currentStatus=" + quest.getStatus());
                }
                quest.reopen();
            }
        }
    }

    private AdminQuestSummaryResponse toSummary(Quest quest) {
        String preview = quest.getDescription() == null ? null
                : quest.getDescription().length() <= 80 ? quest.getDescription()
                : quest.getDescription().substring(0, 80);
        return new AdminQuestSummaryResponse(
                quest.getQuestId(),
                quest.getTitle(),
                preview,
                quest.getDifficulty(),
                quest.getRewardXp(),
                quest.getStatus(),
                new UserBrief(quest.getPublisher().getUserId(), quest.getPublisher().getUsername()),
                quest.getCreatedAt());
    }
}
