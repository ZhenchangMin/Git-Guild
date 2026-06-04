package com.gitguild.backend.quest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.codehost.service.CodeIssueService;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.domain.QuestTag;
import com.gitguild.backend.quest.dto.CreateQuestRequest;
import com.gitguild.backend.quest.dto.QuestResponses;
import com.gitguild.backend.quest.dto.QuestResponses.AssignmentResponse;
import com.gitguild.backend.quest.dto.QuestResponses.CreateQuestResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestDetailResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestSummaryResponse;
import com.gitguild.backend.quest.dto.QuestResponses.SubmitQuestResponse;
import com.gitguild.backend.quest.dto.QuestSearchCriteria;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestCategoryRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.quest.repository.QuestTagRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestServiceImpl implements QuestService {

    private static final Logger log = LoggerFactory.getLogger(QuestServiceImpl.class);

    private static final List<QuestStatus> ACTIVE_ISSUE_QUEST_STATUSES = List.of(
            QuestStatus.DRAFT,
            QuestStatus.PENDING_ADMIN_REVIEW,
            QuestStatus.PUBLISHED,
            QuestStatus.IN_PROGRESS);
    private static final List<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES = List.of(AssignmentStatus.ACTIVE);
    private static final List<AssignmentStatus> ANY_ASSIGNMENT_STATUSES = List.of(
            AssignmentStatus.ACTIVE,
            AssignmentStatus.ABANDONED,
            AssignmentStatus.COMPLETED,
            AssignmentStatus.CANCELLED);
    private static final List<QuestStatus> QUEST_BOARD_DEFAULT_STATUSES = List.of(
            QuestStatus.PUBLISHED,
            QuestStatus.IN_PROGRESS);
    private static final List<QuestStatus> PUBLIC_DETAIL_STATUSES = List.of(
            QuestStatus.PUBLISHED,
            QuestStatus.IN_PROGRESS,
            QuestStatus.COMPLETED);

    private final QuestRepository questRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final QuestCategoryRepository categoryRepository;
    private final QuestTagRepository tagRepository;
    private final CodeRepositoryRepository codeRepositoryRepository;
    private final CodeIssueRepository issueRepository;
    private final CodeIssueService codeIssueService;
    private final QuestTaskBranchService taskBranchService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public QuestServiceImpl(
            QuestRepository questRepository,
            QuestAssignmentRepository assignmentRepository,
            QuestCategoryRepository categoryRepository,
            QuestTagRepository tagRepository,
            CodeRepositoryRepository codeRepositoryRepository,
            CodeIssueRepository issueRepository,
            CodeIssueService codeIssueService,
            QuestTaskBranchService taskBranchService,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.questRepository = questRepository;
        this.assignmentRepository = assignmentRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.codeRepositoryRepository = codeRepositoryRepository;
        this.issueRepository = issueRepository;
        this.codeIssueService = codeIssueService;
        this.taskBranchService = taskBranchService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CreateQuestResponse createQuest(Long publisherId, CreateQuestRequest request) {
        User publisher = findUser(publisherId);
        if (publisher.getRole() != UserRole.MAINTAINER && publisher.getRole() != UserRole.ADMIN) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "当前用户无权限", "发布任务需要 MAINTAINER 或 ADMIN 角色");
        }

        CodeRepository repository = codeRepositoryRepository.findById(request.getRepositoryId())
                .orElseThrow(() -> new BusinessException("REPOSITORY_NOT_FOUND", HttpStatus.NOT_FOUND, "仓库不存在", "repositoryId=" + request.getRepositoryId()));

        // Issue #11：支持两种方式绑定 Issue
        //   (a) 提供 giteaIssueTitle → 在 Gitea 上创建新 Issue 并同步到本地
        //   (b) 提供 issueId → 关联已有本地 Issue
        CodeIssue issue;
        if (request.getGiteaIssueTitle() != null && !request.getGiteaIssueTitle().isBlank()) {
            issue = codeIssueService.createFromGitea(repository, request.getGiteaIssueTitle(), request.getGiteaIssueBody());
        } else if (request.getIssueId() != null) {
            issue = issueRepository.findByIssueIdAndRepositoryRepositoryId(request.getIssueId(), request.getRepositoryId())
                    .orElseThrow(() -> new BusinessException("ISSUE_NOT_FOUND", HttpStatus.NOT_FOUND, "Issue 不存在", "issueId=" + request.getIssueId()));
        } else {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                    "请求参数不合法", "必须提供 issueId 或 giteaIssueTitle");
        }
        if (!issue.canCreateQuest() || questRepository.existsByIssueAndStatusIn(issue, ACTIVE_ISSUE_QUEST_STATUSES)) {
            throw new BusinessException("ISSUE_NOT_AVAILABLE", HttpStatus.CONFLICT, "该 Issue 当前不可发布为任务", "Issue 已关闭或已关联未完成任务");
        }

        QuestCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "请求参数不合法", "categoryId 不存在"));
        if (!category.isEnabled()) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "请求参数不合法", "categoryId 已禁用");
        }

        Set<QuestTag> tags = findTags(request.getTagIds());
        Quest quest = new Quest(
                publisher,
                repository,
                issue,
                category,
                request.getTitle(),
                request.getDescription(),
                request.getCompletionCriteria(),
                request.getDifficulty(),
                toJson(request.getTechStack()),
                request.getRewardXp(),
                request.getEstimatedHours());
        quest.addTags(tags);
        Quest saved = questRepository.save(quest);

        CodeIssue savedIssue = saved.getIssue();
        CodeRepository savedRepo = saved.getRepository();
        return new CreateQuestResponse(
                saved.getQuestId(),
                saved.getTitle(),
                saved.getStatus(),
                savedRepo.getRepositoryId(),
                savedIssue.getIssueId(),
                savedIssue.getExternalIssueId(),
                savedIssue.getExternalUrl(),
                savedRepo.getDefaultBranch(),
                saved.getDifficulty(),
                saved.getRewardXp(),
                saved.getCreatedAt());
    }

    @Override
    @Transactional
    public SubmitQuestResponse submitQuest(Long questId, Long publisherId) {
        Quest quest = findQuest(questId);
        if (!quest.getPublisher().getUserId().equals(publisherId)) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "当前用户无权限", "只有发布者可以提交审核");
        }
        if (!quest.canSubmitForReview()) {
            throw new BusinessException("QUEST_NOT_SUBMITTABLE", HttpStatus.CONFLICT, "任务当前状态不可提交审核", "currentStatus=" + quest.getStatus());
        }
        quest.submitForReview();
        questRepository.save(quest);
        return new SubmitQuestResponse(quest.getQuestId(), quest.getStatus(), quest.getUpdatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestPageResponse searchQuests(QuestSearchCriteria criteria) {
        int page = Math.max(criteria.page(), 1);
        int size = Math.min(Math.max(criteria.size(), 1), 50);
        Sort sort = Sort.by(sortDirection(criteria.sortOrder()), sortProperty(criteria.sortBy()));
        Page<Quest> questPage = questRepository.findAll(toSpecification(criteria), PageRequest.of(page - 1, size, sort));
        return new QuestPageResponse(
                questPage.getContent().stream().map(this::toSummary).toList(),
                page,
                size,
                questPage.getTotalElements(),
                questPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestDetailResponse getQuestDetail(Long questId, Long currentUserId) {
        Quest quest = findQuest(questId);
        if (!isPublicDetailVisible(quest) && !canViewRestrictedQuest(quest, currentUserId)) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "当前用户无权限", "该任务当前不可公开查看");
        }
        return toDetail(quest);
    }

    @Override
    @Transactional
    public AssignmentResponse acceptQuest(Long questId, Long assigneeId) {
        User assignee = findUser(assigneeId);
        if (assignee.getRole() == UserRole.ADMIN) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "当前用户无权限", "管理员默认不参与普通任务接取");
        }
        Quest quest = findQuest(questId);
        if (!quest.canBeAccepted()) {
            throw new BusinessException("QUEST_NOT_ACCEPTABLE", HttpStatus.CONFLICT, "任务当前状态不可接取", "currentStatus=" + quest.getStatus());
        }
        if (assignmentRepository.existsByQuestAndAssigneeUserIdAndStatusIn(quest, assigneeId, ACTIVE_ASSIGNMENT_STATUSES)) {
            throw new BusinessException("DUPLICATE_ASSIGNMENT", HttpStatus.CONFLICT, "当前用户已接取该任务", "questId=" + questId);
        }

        QuestAssignment assignment = assignmentRepository.save(new QuestAssignment(quest, assignee));
        quest.markInProgress();
        questRepository.save(quest);

        // Issue #12：接取后尽力创建 task branch。Gitea 不可达不应阻塞接取——
        // 分支为后续工作台 commit/PR 的基础，可由 ensureTaskBranch 端点重试。
        String taskBranch = tryEnsureTaskBranch(assignment);

        return toAssignmentResponse(assignment, quest, assignee, taskBranch);
    }

    @Override
    @Transactional
    public AssignmentResponse ensureTaskBranch(Long questId, Long assigneeId) {
        User assignee = findUser(assigneeId);
        Quest quest = findQuest(questId);
        QuestAssignment assignment = assignmentRepository
                .findByQuestAndAssigneeUserIdAndStatus(quest, assigneeId, AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("ASSIGNMENT_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "未找到进行中的接取记录", "questId=" + questId + ", assigneeId=" + assigneeId));
        // 这里不吞异常：显式重试端点失败应反馈给调用方
        String taskBranch = taskBranchService.ensureTaskBranch(assignment);
        return toAssignmentResponse(assignment, quest, assignee, taskBranch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestResponses.MyAssignmentResponse> listMyActiveAssignments(Long assigneeId) {
        findUser(assigneeId);
        return assignmentRepository.findByAssigneeUserIdAndStatus(assigneeId, AssignmentStatus.ACTIVE)
                .stream()
                .map(this::toMyAssignmentResponse)
                .toList();
    }

    private String tryEnsureTaskBranch(QuestAssignment assignment) {
        try {
            return taskBranchService.ensureTaskBranch(assignment);
        } catch (BusinessException ex) {
            log.warn("接取后创建 task branch 失败 assignmentId={}, code={}",
                    assignment.getAssignmentId(), ex.getCode());
            return null;
        }
    }

    private AssignmentResponse toAssignmentResponse(
            QuestAssignment assignment, Quest quest, User assignee, String taskBranch) {
        return new AssignmentResponse(
                assignment.getAssignmentId(),
                quest.getQuestId(),
                new QuestResponses.UserBrief(assignee.getUserId(), assignee.getUsername()),
                quest.getStatus(),
                assignment.getStatus().name(),
                taskBranch,
                assignment.getAcceptedAt());
    }

    private QuestResponses.MyAssignmentResponse toMyAssignmentResponse(QuestAssignment assignment) {
        Quest quest = assignment.getQuest();
        return new QuestResponses.MyAssignmentResponse(
                assignment.getAssignmentId(),
                quest.getQuestId(),
                quest.getTitle(),
                quest.getCompletionCriteria(),
                quest.getDifficulty(),
                fromJson(quest.getTechStackJson()),
                quest.getRewardXp(),
                quest.getStatus(),
                assignment.getStatus().name(),
                assignment.getTaskBranch(),
                assignment.getAcceptedAt(),
                new QuestResponses.RepositoryBrief(
                        quest.getRepository().getRepositoryId(),
                        quest.getRepository().getName(),
                        quest.getRepository().getDefaultBranch(),
                        quest.getRepository().getSyncStatus()),
                new QuestResponses.IssueBrief(
                        quest.getIssue().getIssueId(),
                        quest.getIssue().getExternalIssueId(),
                        quest.getIssue().getTitle(),
                        quest.getIssue().getStatus(),
                        quest.getIssue().getExternalUrl()));
    }

    private Specification<Quest> toSpecification(QuestSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.status() == null) {
                predicates.add(root.get("status").in(QUEST_BOARD_DEFAULT_STATUSES));
            } else {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
                String pattern = "%" + criteria.keyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
            }
            if (criteria.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), criteria.categoryId()));
            }
            if (criteria.difficulty() != null) {
                predicates.add(cb.equal(root.get("difficulty"), criteria.difficulty()));
            }
            if (criteria.techStack() != null && !criteria.techStack().isEmpty()) {
                List<Predicate> techPredicates = criteria.techStack().stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> cb.like(root.get("techStackJson"), "%\"" + value + "\"%"))
                        .toList();
                if (!techPredicates.isEmpty()) {
                    predicates.add(cb.or(techPredicates.toArray(Predicate[]::new)));
                }
            }
            if (criteria.tagIds() != null && !criteria.tagIds().isEmpty()) {
                Join<Quest, QuestTag> tags = root.join("tags", JoinType.INNER);
                predicates.add(tags.get("tagId").in(criteria.tagIds()));
                query.groupBy(root.get("questId"));
                query.having(cb.equal(cb.countDistinct(tags.get("tagId")), (long) criteria.tagIds().size()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Set<QuestTag> findTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Set.of();
        }
        List<QuestTag> tags = tagRepository.findByTagIdIn(tagIds);
        if (tags.size() != new LinkedHashSet<>(tagIds).size()) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "请求参数不合法", "tagIds 包含不存在的标签");
        }
        if (tags.stream().anyMatch(tag -> !tag.isEnabled())) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "请求参数不合法", "tagIds 包含已禁用标签");
        }
        return new LinkedHashSet<>(tags);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "用户不存在", "userId=" + userId));
    }

    private Quest findQuest(Long questId) {
        return questRepository.findById(questId)
                .orElseThrow(() -> new BusinessException("QUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "任务不存在", "questId=" + questId));
    }

    private boolean isPublicDetailVisible(Quest quest) {
        return PUBLIC_DETAIL_STATUSES.contains(quest.getStatus());
    }

    private boolean canViewRestrictedQuest(Quest quest, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        User user = findUser(currentUserId);
        if (user.getRole() == UserRole.ADMIN || quest.getPublisher().getUserId().equals(currentUserId)) {
            return true;
        }
        return assignmentRepository.existsByQuestAndAssigneeUserIdAndStatusIn(quest, currentUserId, ANY_ASSIGNMENT_STATUSES);
    }

    private QuestSummaryResponse toSummary(Quest quest) {
        return new QuestSummaryResponse(
                quest.getQuestId(),
                quest.getTitle(),
                preview(quest.getDescription()),
                quest.getDifficulty(),
                fromJson(quest.getTechStackJson()),
                quest.getRewardXp(),
                quest.getEstimatedHours(),
                quest.getStatus(),
                new QuestResponses.CategoryBrief(quest.getCategory().getCategoryId(), quest.getCategory().getName()),
                tagResponses(quest),
                new QuestResponses.RepositoryBrief(
                        quest.getRepository().getRepositoryId(),
                        quest.getRepository().getName(),
                        quest.getRepository().getDefaultBranch(),
                        quest.getRepository().getSyncStatus()),
                quest.getCreatedAt());
    }

    private QuestDetailResponse toDetail(Quest quest) {
        QuestResponses.AssignmentBrief assignment = assignmentRepository
                .findFirstByQuestQuestIdAndStatus(quest.getQuestId(), AssignmentStatus.ACTIVE)
                .map(value -> new QuestResponses.AssignmentBrief(
                        true,
                        new QuestResponses.UserBrief(value.getAssignee().getUserId(), value.getAssignee().getUsername()),
                        value.getAcceptedAt()))
                .orElse(new QuestResponses.AssignmentBrief(false, null, null));
        return new QuestDetailResponse(
                quest.getQuestId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getCompletionCriteria(),
                quest.getDifficulty(),
                fromJson(quest.getTechStackJson()),
                quest.getEstimatedHours(),
                quest.getRewardXp(),
                quest.getStatus(),
                new QuestResponses.UserBrief(quest.getPublisher().getUserId(), quest.getPublisher().getUsername()),
                new QuestResponses.RepositoryBrief(quest.getRepository().getRepositoryId(), quest.getRepository().getName(), quest.getRepository().getDefaultBranch(), quest.getRepository().getSyncStatus()),
                new QuestResponses.IssueBrief(quest.getIssue().getIssueId(), quest.getIssue().getExternalIssueId(), quest.getIssue().getTitle(), quest.getIssue().getStatus(), quest.getIssue().getExternalUrl()),
                new QuestResponses.CategoryBrief(quest.getCategory().getCategoryId(), quest.getCategory().getName()),
                tagResponses(quest),
                assignment,
                quest.getCreatedAt(),
                quest.getUpdatedAt());
    }

    private List<QuestResponses.TagBrief> tagResponses(Quest quest) {
        return quest.getTags().stream()
                .sorted(Comparator.comparing(QuestTag::getTagId))
                .map(tag -> new QuestResponses.TagBrief(tag.getTagId(), tag.getName(), tag.getColor()))
                .toList();
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "请求参数不合法", "techStack 无法序列化");
        }
    }

    private List<String> fromJson(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String preview(String description) {
        if (description == null || description.length() <= 80) {
            return description;
        }
        return description.substring(0, 80);
    }

    private Sort.Direction sortDirection(String sortOrder) {
        return "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private String sortProperty(String sortBy) {
        if ("rewardXp".equals(sortBy)) {
            return "rewardXp";
        }
        if ("estimatedHours".equals(sortBy)) {
            return "estimatedHours";
        }
        return "createdAt";
    }
}
