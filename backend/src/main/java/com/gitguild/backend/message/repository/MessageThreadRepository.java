package com.gitguild.backend.message.repository;

import com.gitguild.backend.message.domain.MessageThread;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageThreadRepository extends JpaRepository<MessageThread, Long> {

    Optional<MessageThread> findByQuestQuestId(Long questId);

    List<MessageThread> findByPublisherUserIdOrAssigneeUserIdOrderByLastMessageAtDesc(
            Long publisherId, Long assigneeId);
}
