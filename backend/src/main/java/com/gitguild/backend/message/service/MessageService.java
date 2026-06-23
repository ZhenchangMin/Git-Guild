package com.gitguild.backend.message.service;

import com.gitguild.backend.message.dto.MessageResponses.MessageThreadDetail;
import com.gitguild.backend.message.dto.MessageResponses.MessageThreadSummary;
import java.util.List;

public interface MessageService {

    List<MessageThreadSummary> listThreads(Long userId);

    MessageThreadDetail getThread(Long userId, Long threadId);

    MessageThreadDetail openQuestThread(Long userId, Long questId);

    MessageThreadDetail sendMessage(Long userId, Long threadId, String content);

    MessageThreadDetail markRead(Long userId, Long threadId);
}
