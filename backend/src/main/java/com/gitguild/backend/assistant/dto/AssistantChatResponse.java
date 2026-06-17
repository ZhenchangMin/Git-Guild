package com.gitguild.backend.assistant.dto;

import java.util.List;

public record AssistantChatResponse(
        String answer,
        AssistantAnswerSource source,
        List<String> suggestedQuestions,
        List<AssistantAction> actions) {

    /**
     * Suggested UI action for the frontend to render below the answer.
     *
     * <p>The backend only provides a route hint. The frontend must not auto-navigate
     * when it receives this field; it should render a button or link and navigate
     * only after the user clicks it.
     */
    public record AssistantAction(String label, String routeName) {
    }
}
