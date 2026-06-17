package com.gitguild.backend.assistant.service;

import com.gitguild.backend.security.CurrentUserPrincipal;
import java.util.List;
import org.springframework.security.core.Authentication;

public record AssistantChatContext(
        String message,
        String page,
        Long userId,
        List<String> roles,
        UserProfileSnapshot userProfile,
        List<QuestRecommendationSnapshot> questBoardCandidates,
        List<QuestStatusSnapshot> questStatuses,
        List<SubmissionStatusSnapshot> submissionStatuses) {

    public AssistantChatContext {
        message = normalize(message);
        page = normalize(page);
        roles = copyNonBlank(roles);
        questBoardCandidates = questBoardCandidates == null ? List.of() : List.copyOf(questBoardCandidates);
        questStatuses = questStatuses == null ? List.of() : List.copyOf(questStatuses);
        submissionStatuses = submissionStatuses == null ? List.of() : List.copyOf(submissionStatuses);
    }

    public static AssistantChatContext from(String message, String page, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            return anonymous(message, page);
        }
        return new AssistantChatContext(
                message,
                page,
                principal.userId(),
                principal.roles(),
                null,
                List.of(),
                List.of(),
                List.of());
    }

    public static AssistantChatContext anonymous(String message, String page) {
        return new AssistantChatContext(message, page, null, List.of(), null, List.of(), List.of(), List.of());
    }

    public boolean authenticated() {
        return userId != null;
    }

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(currentRole -> currentRole.equals(role));
    }

    public String roleLabel() {
        if (!authenticated()) {
            return "游客";
        }
        if (hasRole("ROLE_ADMIN")) {
            return "管理员";
        }
        if (hasRole("ROLE_MAINTAINER")) {
            return "委托人";
        }
        return "冒险家";
    }

    public record UserProfileSnapshot(
            int level,
            int totalXp,
            int completedQuestCount,
            List<String> preferredTechStacks,
            String difficultyComfort) {

        public UserProfileSnapshot {
            preferredTechStacks = preferredTechStacks == null ? List.of() : List.copyOf(preferredTechStacks);
            difficultyComfort = normalize(difficultyComfort);
        }
    }

    public record QuestRecommendationSnapshot(
            Long questId,
            String title,
            String difficulty,
            List<String> techStack,
            int rewardXp,
            int estimatedHours,
            String categoryName,
            List<String> tagNames,
            String status,
            double matchScore,
            List<String> reasons) {

        public QuestRecommendationSnapshot {
            title = normalize(title);
            difficulty = normalize(difficulty);
            techStack = techStack == null ? List.of() : List.copyOf(techStack);
            categoryName = normalize(categoryName);
            tagNames = tagNames == null ? List.of() : List.copyOf(tagNames);
            status = normalize(status);
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }
    }

    public record QuestStatusSnapshot(
            Long questId,
            String title,
            String questStatus,
            Long assignmentId,
            String assignmentStatus,
            String taskBranch,
            String repositoryName,
            String repositoryDefaultBranch,
            String nextAction) {

        public QuestStatusSnapshot {
            title = normalize(title);
            questStatus = normalize(questStatus);
            assignmentStatus = normalize(assignmentStatus);
            taskBranch = normalize(taskBranch);
            repositoryName = normalize(repositoryName);
            repositoryDefaultBranch = normalize(repositoryDefaultBranch);
            nextAction = normalize(nextAction);
        }
    }

    public record SubmissionStatusSnapshot(
            Long submissionId,
            Long questId,
            String questTitle,
            String submissionStatus,
            String submittedAt,
            Long pullRequestId,
            String externalPrId,
            String prTitle,
            String prStatus,
            String sourceBranch,
            String targetBranch,
            boolean merged,
            String latestReviewDecision,
            String latestReviewSummary,
            String latestReviewedAt,
            String nextAction) {

        public SubmissionStatusSnapshot {
            questTitle = normalize(questTitle);
            submissionStatus = normalize(submissionStatus);
            submittedAt = normalize(submittedAt);
            externalPrId = normalize(externalPrId);
            prTitle = normalize(prTitle);
            prStatus = normalize(prStatus);
            sourceBranch = normalize(sourceBranch);
            targetBranch = normalize(targetBranch);
            latestReviewDecision = normalize(latestReviewDecision);
            latestReviewSummary = normalize(latestReviewSummary);
            latestReviewedAt = normalize(latestReviewedAt);
            nextAction = normalize(nextAction);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> copyNonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }
}
