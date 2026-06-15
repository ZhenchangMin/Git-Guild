package com.gitguild.backend.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.assistant.service.AssistantChatContext.QuestRecommendationSnapshot;
import com.gitguild.backend.assistant.service.AssistantChatContext.QuestStatusSnapshot;
import com.gitguild.backend.assistant.service.AssistantChatContext.SubmissionStatusSnapshot;
import com.gitguild.backend.assistant.service.AssistantChatContext.UserProfileSnapshot;
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
import com.gitguild.backend.review.domain.Submission;
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
    private static final double TECH_WEIGHT = 0.6;
    private static final double DIFFICULTY_WEIGHT = 0.4;
    private static final double BEGINNER_BOOST = 0.2;

    private final QuestRepository questRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final GrowthProfileRepository growthProfileRepository;
    private final ContributionRecordRepository contributionRecordRepository;
    private final ObjectMapper objectMapper;

    public AssistantContextAssembler(
            QuestRepository questRepository,
            QuestAssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            GrowthProfileRepository growthProfileRepository,
            ContributionRecordRepository contributionRecordRepository,
            ObjectMapper objectMapper) {
        this.questRepository = questRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
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
                .map(this::toSubmissionSnapshot)
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
        return new QuestStatusSnapshot(
                quest.getQuestId(),
                quest.getTitle(),
                quest.getStatus().name());
    }

    private QuestStatusSnapshot toAssignedQuestSnapshot(QuestAssignment assignment) {
        Quest quest = assignment.getQuest();
        return new QuestStatusSnapshot(
                quest.getQuestId(),
                quest.getTitle(),
                "quest=" + quest.getStatus().name() + ",assignment=" + assignment.getStatus().name());
    }

    private SubmissionStatusSnapshot toSubmissionSnapshot(Submission submission) {
        return new SubmissionStatusSnapshot(
                submission.getSubmissionId(),
                submission.getQuest().getQuestId(),
                submission.getStatus().name());
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
                quest.getStatus().name(),
                score,
                recommendationReasons(techMatch, difficultyMatch, beginnerBoost > 0));
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
