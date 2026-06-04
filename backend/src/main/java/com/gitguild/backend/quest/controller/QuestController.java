package com.gitguild.backend.quest.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.security.CurrentUserPrincipal;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.dto.CreateQuestRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AssignmentResponse;
import com.gitguild.backend.quest.dto.QuestResponses.CreateQuestResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestDetailResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.SubmitQuestResponse;
import com.gitguild.backend.quest.dto.QuestResponses.MyAssignmentsResponse;
import com.gitguild.backend.quest.dto.QuestSearchCriteria;
import com.gitguild.backend.quest.service.QuestService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quests")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateQuestResponse>> createQuest(
            Authentication authentication,
            @Valid @RequestBody CreateQuestRequest request) {
        CreateQuestResponse response = questService.createQuest(SecurityPrincipalUtils.currentUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("任务已创建，等待管理员审核", response));
    }

    @PostMapping("/{questId}/submit")
    public ApiResponse<SubmitQuestResponse> submitQuest(
            @PathVariable Long questId,
            Authentication authentication) {
        return ApiResponse.success("已提交审核", questService.submitQuest(questId, SecurityPrincipalUtils.currentUserId(authentication)));
    }

    @GetMapping
    public ApiResponse<QuestPageResponse> searchQuests(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tagIds,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String techStack,
            @RequestParam(required = false) QuestStatus status,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "4") int size) {
        QuestSearchCriteria criteria = new QuestSearchCriteria(
                keyword,
                categoryId,
                parseLongCsv(tagIds),
                difficulty,
                parseStringCsv(techStack),
                status,
                sortBy,
                sortOrder,
                page,
                size);
        return ApiResponse.success(questService.searchQuests(criteria));
    }

    @GetMapping("/{questId}")
    public ApiResponse<QuestDetailResponse> getQuestDetail(
            @PathVariable Long questId,
            Authentication authentication) {
        return ApiResponse.success(questService.getQuestDetail(questId, optionalUserId(authentication)));
    }

    @PostMapping("/{questId}/assignments")
    public ResponseEntity<ApiResponse<AssignmentResponse>> acceptQuest(
            @PathVariable Long questId,
            Authentication authentication) {
        AssignmentResponse response = questService.acceptQuest(questId, SecurityPrincipalUtils.currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("任务接取成功", response));
    }

    @GetMapping("/me/assignments")
    public ApiResponse<MyAssignmentsResponse> getMyAssignments(Authentication authentication) {
        return ApiResponse.success(questService.getMyAssignments(
                SecurityPrincipalUtils.currentUserId(authentication)));
    }

    private Long optionalUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }

    private List<Long> parseLongCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .toList();
    }

    private List<String> parseStringCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
