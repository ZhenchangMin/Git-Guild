package com.gitguild.backend.message.service;

import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.message.domain.Message;
import com.gitguild.backend.message.domain.MessageReadState;
import com.gitguild.backend.message.domain.MessageThread;
import com.gitguild.backend.message.dto.MessageResponses.MessageItem;
import com.gitguild.backend.message.dto.MessageResponses.MessagePreview;
import com.gitguild.backend.message.dto.MessageResponses.MessageThreadDetail;
import com.gitguild.backend.message.dto.MessageResponses.MessageThreadSummary;
import com.gitguild.backend.message.dto.MessageResponses.Participant;
import com.gitguild.backend.message.repository.MessageReadStateRepository;
import com.gitguild.backend.message.repository.MessageRepository;
import com.gitguild.backend.message.repository.MessageThreadRepository;
import com.gitguild.backend.notification.domain.NotificationType;
import com.gitguild.backend.notification.service.NotificationService;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageServiceImpl implements MessageService {

    private static final List<AssignmentStatus> THREAD_ASSIGNMENT_STATUSES =
            List.of(AssignmentStatus.ACTIVE, AssignmentStatus.COMPLETED);

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final MessageReadStateRepository readStateRepository;
    private final QuestRepository questRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public MessageServiceImpl(
            MessageThreadRepository threadRepository,
            MessageRepository messageRepository,
            MessageReadStateRepository readStateRepository,
            QuestRepository questRepository,
            QuestAssignmentRepository assignmentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.readStateRepository = readStateRepository;
        this.questRepository = questRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageThreadSummary> listThreads(Long userId) {
        return threadRepository
                .findByPublisherUserIdOrAssigneeUserIdOrderByLastMessageAtDesc(userId, userId)
                .stream()
                .map(thread -> toSummary(thread, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MessageThreadDetail getThread(Long userId, Long threadId) {
        MessageThread thread = requireThreadForUser(userId, threadId);
        return toDetail(thread, userId);
    }

    @Override
    @Transactional
    public MessageThreadDetail openQuestThread(Long userId, Long questId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new BusinessException(
                        "QUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "委托不存在", "questId=" + questId));

        MessageThread thread = threadRepository.findByQuestQuestId(questId)
                .orElseGet(() -> createThread(userId, quest));
        ensureParticipant(thread, userId);
        return toDetail(thread, userId);
    }

    @Override
    @Transactional
    public MessageThreadDetail sendMessage(Long userId, Long threadId, String content) {
        MessageThread thread = requireThreadForUser(userId, threadId);
        String normalized = normalizeContent(content);
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "USER_NOT_FOUND", HttpStatus.NOT_FOUND, "当前用户不存在", "userId=" + userId));

        Message saved = messageRepository.save(new Message(thread, sender, normalized));
        thread.touch(saved.getCreatedAt());
        threadRepository.save(thread);
        markReadInternal(thread, sender, saved.getCreatedAt());

        User receiver = thread.counterpartOf(userId);
        try {
            notificationService.notify(
                    receiver,
                    NotificationType.MESSAGE_RECEIVED,
                    sender.getUsername() + " 给你在《" + thread.getQuest().getTitle() + "》发来一封信笺。",
                    "MESSAGE_THREAD",
                    thread.getThreadId());
        } catch (RuntimeException ignored) {
            // The message itself should not fail just because secondary notification delivery failed.
        }

        return toDetail(thread, userId);
    }

    @Override
    @Transactional
    public MessageThreadDetail markRead(Long userId, Long threadId) {
        MessageThread thread = requireThreadForUser(userId, threadId);
        User user = thread.getPublisher().getUserId().equals(userId) ? thread.getPublisher() : thread.getAssignee();
        markReadInternal(thread, user, OffsetDateTime.now());
        // 读完该会话的信笺，连带把"收到信笺"的站内通知一并置为已读，保持红点与实际一致。
        try {
            notificationService.markReadByRelated(userId, "MESSAGE_THREAD", thread.getThreadId());
        } catch (RuntimeException ignored) {
            // 通知联动失败不应影响信笺已读本身。
        }
        return toDetail(thread, userId);
    }

    private MessageThread createThread(Long userId, Quest quest) {
        QuestAssignment assignment = assignmentRepository
                .findFirstByQuestQuestIdAndStatusIn(quest.getQuestId(), THREAD_ASSIGNMENT_STATUSES)
                .orElseThrow(() -> new BusinessException(
                        "MESSAGE_THREAD_NOT_AVAILABLE",
                        HttpStatus.CONFLICT,
                        "该委托还没有可联系的接取者"));

        MessageThread thread = new MessageThread(quest, quest.getPublisher(), assignment.getAssignee());
        ensureParticipant(thread, userId);
        return threadRepository.save(thread);
    }

    private MessageThread requireThreadForUser(Long userId, Long threadId) {
        MessageThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new BusinessException(
                        "MESSAGE_THREAD_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "信笺会话不存在",
                        "threadId=" + threadId));
        ensureParticipant(thread, userId);
        return thread;
    }

    private void ensureParticipant(MessageThread thread, Long userId) {
        if (!thread.hasParticipant(userId)) {
            throw new BusinessException("MESSAGE_THREAD_FORBIDDEN", HttpStatus.FORBIDDEN, "无权查看该信笺会话");
        }
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            throw new BusinessException("MESSAGE_CONTENT_REQUIRED", HttpStatus.BAD_REQUEST, "信笺内容不能为空");
        }
        if (normalized.length() > Message.MAX_CONTENT_LENGTH) {
            throw new BusinessException("MESSAGE_CONTENT_TOO_LONG", HttpStatus.BAD_REQUEST, "信笺内容不能超过 1000 字");
        }
        return normalized;
    }

    private void markReadInternal(MessageThread thread, User user, OffsetDateTime readAt) {
        MessageReadState state = readStateRepository
                .findByThreadThreadIdAndUserUserId(thread.getThreadId(), user.getUserId())
                .orElseGet(() -> new MessageReadState(thread, user, readAt));
        state.markRead(readAt);
        readStateRepository.save(state);
    }

    private MessageThreadSummary toSummary(MessageThread thread, Long userId) {
        Message latest = messageRepository.findTopByThreadThreadIdOrderByCreatedAtDesc(thread.getThreadId()).orElse(null);
        return new MessageThreadSummary(
                thread.getThreadId(),
                thread.getQuest().getQuestId(),
                formatQuestCode(thread.getQuest().getQuestId()),
                thread.getQuest().getTitle(),
                participant(thread.counterpartOf(userId)),
                participant(thread.getPublisher()),
                participant(thread.getAssignee()),
                latest == null ? null : toPreview(latest, userId),
                unreadCount(thread, userId),
                thread.getLastMessageAt());
    }

    private MessageThreadDetail toDetail(MessageThread thread, Long userId) {
        List<MessageItem> messages = messageRepository
                .findByThreadThreadIdOrderByCreatedAtAsc(thread.getThreadId())
                .stream()
                .map(message -> toItem(message, userId))
                .toList();
        return new MessageThreadDetail(
                thread.getThreadId(),
                thread.getQuest().getQuestId(),
                formatQuestCode(thread.getQuest().getQuestId()),
                thread.getQuest().getTitle(),
                participant(thread.counterpartOf(userId)),
                participant(thread.getPublisher()),
                participant(thread.getAssignee()),
                messages,
                unreadCount(thread, userId),
                thread.getLastMessageAt());
    }

    private MessageItem toItem(Message message, Long userId) {
        User sender = message.getSender();
        return new MessageItem(
                message.getMessageId(),
                sender.getUserId(),
                sender.getUsername(),
                message.getContent(),
                message.getCreatedAt(),
                sender.getUserId().equals(userId));
    }

    private MessagePreview toPreview(Message message, Long userId) {
        return new MessagePreview(
                message.getContent(),
                message.getSender().getUsername(),
                message.getCreatedAt(),
                message.getSender().getUserId().equals(userId));
    }

    private Participant participant(User user) {
        return new Participant(user.getUserId(), user.getUsername(), user.getAvatarUrl());
    }

    private long unreadCount(MessageThread thread, Long userId) {
        return readStateRepository
                .findByThreadThreadIdAndUserUserId(thread.getThreadId(), userId)
                .map(state -> messageRepository.countByThreadThreadIdAndSenderUserIdNotAndCreatedAtAfter(
                        thread.getThreadId(), userId, state.getLastReadAt()))
                .orElseGet(() -> messageRepository.countByThreadThreadIdAndSenderUserIdNot(thread.getThreadId(), userId));
    }

    private String formatQuestCode(Long questId) {
        return questId == null ? "QST-????" : "QST-" + String.format("%04d", questId);
    }
}
