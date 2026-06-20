package com.gitguild.backend.quest.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.quest.dto.QuestResponses.MyAssignmentResponse;
import com.gitguild.backend.quest.service.QuestService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/quest-assignments")
public class QuestAssignmentController {

    private final QuestService questService;

    public QuestAssignmentController(QuestService questService) {
        this.questService = questService;
    }

    @GetMapping
    public ApiResponse<List<MyAssignmentResponse>> listMyActiveAssignments(Authentication authentication) {
        return ApiResponse.success(questService.listMyActiveAssignments(
                SecurityPrincipalUtils.currentUserId(authentication)));
    }
}
