package com.gitguild.backend.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.assistant.service.AssistantChatContext.QuestRecommendationSnapshot;
import com.gitguild.backend.assistant.service.AssistantChatContext.QuestStatusSnapshot;
import com.gitguild.backend.assistant.service.AssistantChatContext.SubmissionStatusSnapshot;
import com.gitguild.backend.assistant.service.AssistantChatContext.UserProfileSnapshot;
import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.growth.domain.ContributionRecord;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.ReviewRecord;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.domain.SubmissionStatus;
import com.gitguild.backend.review.repository.ReviewRecordRepository;
import com.gitguild.backend.review.repository.SubmissionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantContextAssembler {

    private static final int MAX_QUEST_SNAPSHOTS = 5;
    private static final int MAX_SUBMISSION_SNAPSHOTS = 5;
    private static final int MAX_QUEST_BOARD_CANDIDATES = 8;
    private static final int MAX_PROFILE_TECH_STACKS = 5;
    private static final int MAX_REVIEW_SUMMARY_CHARS = 120;
    private static final double TECH_WEIGHT = 0.6;
    private static final double DIFFICULTY_WEIGHT = 0.4;
    private static final double BEGINNER_BOOST = 0.2;

    private final QuestRepository questRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final GrowthProfileRepository growthProfileRepository;
    private final ContributionRecordRepository contributionRecordRepository;
    private final ObjectMapper objectMapper;

    public AssistantContextAssembler(
            QuestRepository questRepository,
            QuestAssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            ReviewRecordRepository reviewRecordRepository,
            GrowthProfileRepository growthProfileRepository,
            ContributionRecordRepository contributionRecordRepository,
            ObjectMapper objectMapper) {
        this.questRepository = questRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.growthProfileRepository = growthProfileRepository;
        this.contributionRecordRepository = contributionRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AssistantChatContext assemble(String message, String page, Authentication authentication) {
        AssistantChatContext base = AssistantChatContext.from(message, page, authentication);
        if (base.hasRole("ROLE_ADMIN")) {
            return base;
        }

        UserProfileSnapshot userProfile = base.authenticated() ? userProfile(base.userId()) : null;
        List<QuestRecommendationSnapshot> questBoardCandidates = questBoardCandidates(userProfile);

        if (!base.authenticated()) {
            return new AssistantChatContext(
                    base.message(),
                    base.page(),
                    base.userId(),
                    base.roles(),
                    userProfile,
                    questBoardCandidates,
                    List.of(),
                    List.of());
        }

        return new AssistantChatContext(
                base.message(),
                base.page(),
                base.userId(),
                base.roles(),
                userProfile,
                questBoardCandidates,
                questStatuses(base),
                submissionStatuses(base));
    }

    private List<QuestStatusSnapshot> questStatuses(AssistantChatContext context) {
        if (context.hasRole("ROLE_MAINTAINER")) {
            return questRepository.findByPublisherUserIdOrderByCreatedAtDesc(context.userId()).stream()
                    .limit(MAX_QUEST_SNAPSHOTS)
                    .map(this::toQuestSnapshot)
                    .toList();
        }

        return assignmentRepository
                .findByAssigneeUserIdAndStatusWithQuestOrderByAcceptedAtDesc(context.userId(), AssignmentStatus.ACTIVE)
                .stream()
                .limit(MAX_QUEST_SNAPSHOTS)
                .map(this::toAssignedQuestSnapshot)
                .toList();
    }

    private List<SubmissionStatusSnapshot> submissionStatuses(AssistantChatContext context) {
        List<Submission> submissions = context.hasRole("ROLE_MAINTAINER")
                ? submissionRepository.findReviewQueueForReviewer(context.userId(), false)
                : submissionRepository.findBySubmitterUserIdWithQuestOrderBySubmittedAtDesc(context.userId());
        return submissions.stream()
                .limit(MAX_SUBMISSION_SNAPSHOTS)
                .map(submission -> toSubmissionSnapshot(submission, context))
                .toList();
    }

    private UserProfileSnapshot userProfile(Long userId) {
        GrowthProfile profile = growthProfileRepository.findByUserUserId(userId).orElse(null);
        List<ContributionRecord> records = contributionRecordRepository.findByUserUserId(userId);
        RecommendationProfile recommendationProfile = recommendationProfile(records);
        return new UserProfileSnapshot(
                profile != null ? profile.getLevel() : 1,
                profile != null ? profile.getTotalXp() : 0,
                profile != null ? profile.getCompletedQuestCount() : 0,
                recommendationProfile.preferredTechStacks(),
                recommendationProfile.difficultyComfort());
    }

    private List<QuestRecommendationSnapshot> questBoardCandidates(UserProfileSnapshot userProfile) {
        RecommendationProfile profile = userProfile == null
                ? RecommendationProfile.empty()
                : new RecommendationProfile(userProfile.preferredTechStacks(), difficultyValue(userProfile.difficultyComfort()));
        return questRepository.findByStatusIn(List.of(QuestStatus.PUBLISHED, QuestStatus.IN_PROGRESS)).stream()
                .map(quest -> toQuestRecommendationSnapshot(quest, profile, userProfile))
                .sorted(Comparator.comparingDouble(QuestRecommendationSnapshot::matchScore).reversed())
                .limit(MAX_QUEST_BOARD_CANDIDATES)
                .toList();
    }

    private QuestStatusSnapshot toQuestSnapshot(Quest quest) {
        CodeRepository repository = quest.getRepository();
        return new QuestStatusSnapshot(
                quest.getQuestId(),
                quest.getTitle(),
                quest.getStatus().name(),
                null,
                "",
                "",
                repositoryName(repository),
                repositoryDefaultBranch(repository),
                questNextAction(quest));
    }

    private QuestStatusSnapshot toAssignedQuestSnapshot(QuestAssignment assignment) {
        Quest quest = assignment.getQuest();
        CodeRepository repository = quest.getRepository();
        return new QuestStatusSnapshot(
                quest.getQuestId(),
                quest.getTitle(),
                quest.getStatus().name(),
                assignment.getAssignmentId(),
                assignment.getStatus().name(),
                assignment.getTaskBranch(),
                repositoryName(repository),
                repositoryDefaultBranch(repository),
                assignedQuestNextAction(assignment));
    }

    private SubmissionStatusSnapshot toSubmissionSnapshot(Submission submission, AssistantChatContext context) {
        Quest quest = submission.getQuest();
        CodePullRequest pullRequest = submission.getPullRequest();
        ReviewRecord latestReview = latestReviewRecord(submission.getSubmissionId());
        return new SubmissionStatusSnapshot(
                submission.getSubmissionId(),
                quest.getQuestId(),
                quest.getTitle(),
                submission.getStatus().name(),
                submittedAtText(submission),
                pullRequest != null ? pullRequest.getPullRequestId() : null,
                pullRequest != null ? pullRequest.getExternalPrId() : "",
                pullRequest != null ? pullRequest.getTitle() : "",
                pullRequest != null ? pullRequest.getStatus() : "",
                pullRequest != null ? pullRequest.getSourceBranch() : "",
                pullRequest != null ? pullRequest.getTargetBranch() : "",
                pullRequest != null && pullRequest.isMerged(),
                latestReview != null ? latestReview.getDecision().name() : "",
                latestReview != null ? truncate(clean(latestReview.getSummary()), MAX_REVIEW_SUMMARY_CHARS) : "",
                latestReview != null ? latestReview.getReviewedAt().toString() : "",
                submissionNextAction(submission, context, pullRequest));
    }

    private QuestRecommendationSnapshot toQuestRecommendationSnapshot(
            Quest quest,
            RecommendationProfile profile,
            UserProfileSnapshot userProfile) {
        List<String> questTechs = parseTechStack(quest.getTechStackJson());
        double techMatch = computeTechMatch(profile.preferredTechStacks(), questTechs);
        double difficultyMatch = computeDifficultyMatch(profile.avgDifficulty(), difficultyValue(quest.getDifficulty()));
        double beginnerBoost = userProfile != null
                && userProfile.level() <= 2
                && quest.getDifficulty() == Difficulty.D
                ? BEGINNER_BOOST
                : 0.0;
        double score = round2(TECH_WEIGHT * techMatch + DIFFICULTY_WEIGHT * difficultyMatch + beginnerBoost);
        return new QuestRecommendationSnapshot(
                quest.getQuestId(),
                quest.getTitle(),
                quest.getDifficulty().name(),
                questTechs,
                quest.getRewardXp(),
                quest.getEstimatedHours(),
                quest.getCategory() != null ? quest.getCategory().getName() : "",
                tagNames(quest),
                quest.getStatus().name(),
                score,
                recommendationReasons(techMatch, difficultyMatch, beginnerBoost > 0));
    }

    private String questNextAction(Quest quest) {
        return switch (quest.getStatus()) {
            case DRAFT -> "补充委托信息后提交管理员审核";
            case PENDING_ADMIN_REVIEW -> "等待管理员审核，通过后会出现在悬赏任务板";
            case PUBLISHED -> "委托已发布，可等待冒险家接取";
            case IN_PROGRESS -> "已有冒险家接取，可关注提交审核台是否出现成果";
            case IN_REVIEW -> "有成果进入审核流程，请到提交审核台处理";
            case COMPLETED -> "委托已完成，可查看成果和成长记录";
            case REJECTED -> "查看退回原因，修改委托后重新提交审核";
            case CLOSED -> "委托已关闭或下架，通常不再继续流转";
        };
    }

    private String assignedQuestNextAction(QuestAssignment assignment) {
        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
            return "该委托已完成，可查看提交记录和成长记录";
        }
        if (assignment.getStatus() == AssignmentStatus.CANCELLED || assignment.getStatus() == AssignmentStatus.ABANDONED) {
            return "该委托已不在进行中，可回到悬赏任务板选择新的委托";
        }
        if (assignment.getTaskBranch() == null || assignment.getTaskBranch().isBlank()) {
            return "进入冒险家工作台准备任务分支，然后按页面提示 clone 仓库并开始开发";
        }
        return "在任务分支 " + assignment.getTaskBranch() + " 上完成修改并 push，完成后到提交柜台登记成果和 PR 信息";
    }

    private String submissionNextAction(
            Submission submission,
            AssistantChatContext context,
            CodePullRequest pullRequest) {
        SubmissionStatus status = submission.getStatus();
        boolean maintainer = context.hasRole("ROLE_MAINTAINER");
        boolean merged = pullRequest != null && pullRequest.isMerged();
        return switch (status) {
            case PENDING_REVIEW -> maintainer
                    ? "进入提交审核台查看成果说明和关联 PR，选择通过、退回修改或驳回；如 PR 可合并，可使用合并 PR 按钮"
                    : "等待委托人审核；可以检查成果说明和 PR 信息是否完整";
            case CHANGES_REQUESTED -> "根据最近审核意见修改代码或成果说明，然后重新提交";
            case APPROVED -> merged
                    ? "成果已通过且 PR 已合并，可查看成长记录"
                    : "成果已通过；如果 PR 仍未合并，委托人可在审核页按需使用合并 PR 按钮";
            case REJECTED -> "本次提交被驳回，需要重新确认委托验收标准后再处理";
        };
    }

    private ReviewRecord latestReviewRecord(Long submissionId) {
        if (submissionId == null) {
            return null;
        }
        List<ReviewRecord> records = reviewRecordRepository.findBySubmissionSubmissionIdOrderByReviewedAtDesc(submissionId);
        return records.isEmpty() ? null : records.get(0);
    }

    private List<String> tagNames(Quest quest) {
        return quest.getTags().stream()
                .map(tag -> clean(tag.getName()))
                .filter(name -> !name.isBlank())
                .sorted()
                .toList();
    }

    private String repositoryName(CodeRepository repository) {
        return repository == null ? "" : clean(repository.getName());
    }

    private String repositoryDefaultBranch(CodeRepository repository) {
        return repository == null ? "" : clean(repository.getDefaultBranch());
    }

    private String submittedAtText(Submission submission) {
        return submission.getSubmittedAt() == null ? "" : submission.getSubmittedAt().toString();
    }

    private RecommendationProfile recommendationProfile(List<ContributionRecord> records) {
        if (records == null || records.isEmpty()) {
            return RecommendationProfile.empty();
        }

        Map<String, Integer> techFrequencies = new HashMap<>();
        double totalDifficulty = 0;
        int count = 0;
        for (ContributionRecord record : records) {
            Quest quest = record.getQuest();
            for (String tech : parseTechStack(quest.getTechStackJson())) {
                techFrequencies.merge(tech, 1, Integer::sum);
            }
            totalDifficulty += difficultyValue(quest.getDifficulty());
            count++;
        }
        List<String> preferredTechStacks = techFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_PROFILE_TECH_STACKS)
                .map(Map.Entry::getKey)
                .toList();
        double avgDifficulty = count > 0 ? totalDifficulty / count : 0.0;
        return new RecommendationProfile(preferredTechStacks, avgDifficulty);
    }

    private List<String> recommendationReasons(double techMatch, double difficultyMatch, boolean beginnerBoost) {
        List<String> reasons = new ArrayList<>();
        if (techMatch >= 0.5) {
            reasons.add("技术栈匹配");
        }
        if (difficultyMatch >= 0.7) {
            reasons.add("难度适合");
        }
        if (beginnerBoost) {
            reasons.add("新手友好");
        }
        if (reasons.isEmpty()) {
            reasons.add("可尝试");
        }
        return reasons;
    }

    private double computeTechMatch(List<String> preferredTechStacks, List<String> questTechs) {
        if (preferredTechStacks.isEmpty() || questTechs.isEmpty()) {
            return 0.0;
        }
        long matched = questTechs.stream().filter(preferredTechStacks::contains).count();
        return (double) matched / questTechs.size();
    }

    private double computeDifficultyMatch(double avgDifficulty, int questDifficulty) {
        if (avgDifficulty <= 0) {
            return 0.5;
        }
        return 1.0 - (double) Math.abs(questDifficulty - avgDifficulty) / 3.0;
    }

    private List<String> parseTechStack(String techStackJson) {
        if (techStackJson == null || techStackJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    techStackJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private int difficultyValue(Difficulty difficulty) {
        return switch (difficulty) {
            case A -> 4;
            case B -> 3;
            case C -> 2;
            case D -> 1;
        };
    }

    private double difficultyValue(String difficultyComfort) {
        return switch (difficultyComfort) {
            case "A" -> 4.0;
            case "B" -> 3.0;
            case "C" -> 2.0;
            case "D" -> 1.0;
            default -> 0.0;
        };
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int maxChars) {
        String text = clean(value);
        if (maxChars <= 0 || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private record RecommendationProfile(List<String> preferredTechStacks, double avgDifficulty) {

        static RecommendationProfile empty() {
            return new RecommendationProfile(List.of(), 0.0);
        }

        String difficultyComfort() {
            if (avgDifficulty >= 3.5) {
                return "A";
            }
            if (avgDifficulty >= 2.5) {
                return "B";
            }
            if (avgDifficulty >= 1.5) {
                return "C";
            }
            if (avgDifficulty > 0) {
                return "D";
            }
            return "cold-start";
        }
    }
}
