package com.gitguild.backend.assistant.service;

import java.util.Optional;

public interface AssistantAiClient {

    Optional<AssistantAnswerResult> tryAnswer(AssistantChatContext context);
}
