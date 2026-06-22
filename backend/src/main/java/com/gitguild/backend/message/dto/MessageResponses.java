package com.gitguild.backend.message.dto;

import java.time.OffsetDateTime;
import java.util.List;

public final class MessageResponses {

    private MessageResponses() {
    }

    public record Participant(Long userId, String username, String avatarUrl) {
    }

    public record MessageItem(
            Long messageId,
            Long senderId,
            String senderName,
            String content,
            OffsetDateTime createdAt,
            boolean mine) {
    }

    public record MessagePreview(
            String content,
            String senderName,
            OffsetDateTime createdAt,
            boolean mine) {
    }

    public record MessageThreadSummary(
            Long threadId,
            Long questId,
            String questCode,
            String questTitle,
            Participant counterpart,
            Participant publisher,
            Participant assignee,
            MessagePreview latestMessage,
            long unreadCount,
            OffsetDateTime lastMessageAt) {
    }

    public record MessageThreadDetail(
            Long threadId,
            Long questId,
            String questCode,
            String questTitle,
            Participant counterpart,
            Participant publisher,
            Participant assignee,
            List<MessageItem> messages,
            long unreadCount,
            OffsetDateTime lastMessageAt) {
    }
}
