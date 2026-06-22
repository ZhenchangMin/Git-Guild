package com.gitguild.backend.message.repository;

import com.gitguild.backend.message.domain.Message;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByThreadThreadIdOrderByCreatedAtAsc(Long threadId);

    Optional<Message> findTopByThreadThreadIdOrderByCreatedAtDesc(Long threadId);

    long countByThreadThreadIdAndSenderUserIdNot(Long threadId, Long senderId);

    long countByThreadThreadIdAndSenderUserIdNotAndCreatedAtAfter(
            Long threadId, Long senderId, OffsetDateTime createdAt);
}
