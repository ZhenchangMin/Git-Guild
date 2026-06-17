package com.gitguild.backend.assistant.service;

import com.gitguild.backend.assistant.dto.AssistantAnswerSource;
import java.util.List;

public record AssistantAnswerResult(
        String answer,
        AssistantAnswerSource source,
        List<String> suggestedQuestions) {
}
